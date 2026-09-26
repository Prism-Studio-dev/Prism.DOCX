package com.prismdocx.editor

import com.prismdocx.metadata.MetadataEditor
import com.prismdocx.metadata.MetadataPart
import com.prismdocx.metadata.MetadataRepository
import com.prismdocx.metadata.MetadataSnapshot
import com.prismdocx.metadata.metadataFields
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.io.File
import java.io.IOException
import kotlin.coroutines.CoroutineContext
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EditorControllerTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val repository = FakeRepository()
    private val title = metadataFields.single { it.key == "title" }
    private val source = File("original.docx")

    @AfterTest fun cleanup() { scope.cancel() }

    private fun controller(dispatcher: CoroutineDispatcher = Dispatchers.Unconfined) =
        EditorController(repository, scope, dispatcher, OpenedDocument(source, MetadataEditor.empty()))

    @Test fun cancellingReplacementKeepsUnsavedChangesAndDoesNotReadAnotherFile() {
        val editor = controller()
        editor.changeField(title, "Unsaved")
        editor.requestOpen(File("another.docx"))
        assertNotNull(editor.pendingAction)
        assertEquals(0, repository.reads)
        editor.cancelPending()
        assertEquals(source, editor.source)
        assertEquals("Unsaved", editor.values.getValue(title.key))
        assertTrue(editor.dirty)
        assertEquals(0, repository.reads)
    }

    @Test fun failedReplacementKeepsCurrentDocumentAndDraft() {
        val editor = controller()
        editor.changeField(title, "Keep this")
        repository.failure = IOException("Cannot read")
        editor.requestOpen(File("broken.docx"))
        editor.confirmPending()
        assertEquals(source, editor.source)
        assertEquals("Keep this", editor.values.getValue(title.key))
        assertTrue(editor.dirty)
        assertFalse(editor.busy)
        assertEquals(EditorNotice("Cannot read", true), editor.notice)
    }

    @Test fun successfulOpenReplacesDocumentOnlyAfterBackgroundReadFinishes() {
        val dispatcher = QueuedDispatcher()
        val editor = controller(dispatcher)
        val next = File("next.docx")
        repository.loaded = MetadataEditor.setValue(MetadataEditor.empty(), title, "Loaded")
        editor.requestOpen(next)
        assertTrue(editor.busy)
        assertEquals(source, editor.source)
        editor.requestOpen(File("ignored.docx"))
        assertEquals(0, repository.reads)
        dispatcher.runAll()
        assertEquals(next, editor.source)
        assertEquals("Loaded", editor.values.getValue(title.key))
        assertFalse(editor.dirty)
        assertFalse(editor.busy)
        assertEquals(1, repository.reads)
    }

    @Test fun successfulSaveEstablishesResetBaselineButFailedSaveDoesNot() {
        val editor = controller()
        editor.changeField(title, "Saved")
        val target = SaveTarget(File("copy.docx"), overwrite = true)
        editor.save(target)
        assertEquals(target, repository.savedTarget)
        assertFalse(editor.dirty)
        editor.changeField(title, "Unwritten")
        repository.failure = IOException("Disk full")
        editor.save(target)
        assertTrue(editor.dirty)
        editor.requestReset()
        editor.confirmPending()
        assertEquals("Saved", editor.values.getValue(title.key))
        assertFalse(editor.dirty)
        assertEquals(source, editor.source)
    }

    @Test fun invalidXmlDraftBlocksFormAndSaveWithoutLosingValidMetadata() {
        val editor = controller()
        editor.changeField(title, "Before")
        editor.changeXml("<broken>")
        editor.changeField(title, "Must be ignored")
        editor.selectXmlPart(MetadataPart.APP)
        editor.applyXml()
        editor.save(SaveTarget(File("copy.docx"), false))
        assertEquals("<broken>", editor.xmlDraft)
        assertEquals(MetadataPart.CORE, editor.xmlPart)
        assertEquals("Before", editor.values.getValue(title.key))
        assertFalse(editor.canSave)
        assertNull(repository.savedTarget)
        editor.discardXml()
        assertTrue(editor.canSave)
    }

    @Test fun applyingXmlRefreshesFieldsAndValidationTogether() {
        val editor = controller()
        val changed = MetadataEditor.setValue(editor.draft, title, "From XML")
        editor.changeXml(changed.xml.getValue(MetadataPart.CORE))
        editor.applyXml()
        assertNull(editor.xmlDraft)
        assertEquals("From XML", editor.values.getValue(title.key))
        val pages = metadataFields.single { it.key == "Pages" }
        editor.changeField(pages, "-1")
        assertFalse(editor.canSave)
        editor.changeField(pages, "12")
        assertTrue(editor.canSave)
    }

    @Test fun closingDirtySessionRequiresExplicitConfirmation() {
        val editor = controller()
        var exited = false
        var cancelled = false
        editor.changeField(title, "Changed")
        editor.requestClose({ exited = true }, { cancelled = true })
        assertFalse(exited)
        editor.cancelPending()
        assertTrue(cancelled)
        assertTrue(editor.dirty)
        editor.requestClose({ exited = true }, {})
        editor.confirmPending()
        assertTrue(exited)
    }

    @Test fun coroutineCancellationIsNotShownAsDocumentError() {
        val editor = controller()
        repository.failure = CancellationException("Screen disposed")
        editor.requestOpen(File("cancelled.docx"))
        assertFalse(editor.busy)
        assertNull(editor.notice)
        assertEquals(source, editor.source)
    }

    private class FakeRepository : MetadataRepository {
        var loaded = MetadataEditor.empty()
        var failure: Exception? = null
        var reads = 0
        var savedTarget: SaveTarget? = null

        override fun read(file: File): MetadataSnapshot {
            reads++
            failure?.let { throw it }
            return loaded
        }

        override fun write(source: File, target: File, snapshot: MetadataSnapshot, overwrite: Boolean) {
            failure?.let { throw it }
            savedTarget = SaveTarget(target, overwrite)
        }
    }

    /** Позволяет проверить состояние во время чтения без sleeps и фоновых гонок в тесте. */
    private class QueuedDispatcher : CoroutineDispatcher() {
        private val tasks = ArrayDeque<Runnable>()
        override fun dispatch(context: CoroutineContext, block: Runnable) { tasks.addLast(block) }
        fun runAll() {
            while (tasks.isNotEmpty()) tasks.removeFirst().run()
        }
    }
}
