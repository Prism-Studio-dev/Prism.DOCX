package com.prismdocx.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.prismdocx.docx.DocxRepository
import com.prismdocx.editor.EditorController
import com.prismdocx.editor.OpenedDocument
import com.prismdocx.platform.DesktopFileDialogs
import com.prismdocx.ui.theme.PrismTheme

/** Composition root: связывает UI, контроллер и платформенные зависимости. */
@Composable
fun App(
    onExit: () -> Unit = {},
    closeRequested: Boolean = false,
    onCancelClose: () -> Unit = {},
    initialDocument: OpenedDocument? = null,
) {
    val scope = rememberCoroutineScope()
    val controller = remember(scope) {
        EditorController(DocxRepository(), scope, initialDocument = initialDocument)
    }
    LaunchedEffect(closeRequested) {
        if (closeRequested) controller.requestClose(onExit, onCancelClose)
    }
    PrismTheme(controller.darkTheme) {
        EditorScreen(
            controller = controller,
            closeRequested = closeRequested,
            onOpen = {
                DesktopFileDialogs.chooseDocument()?.let(controller::requestOpen)
            },
            onSave = {
                controller.source?.let { source ->
                    DesktopFileDialogs.chooseSaveTarget(source)?.let(controller::save)
                }
            },
        )
    }
}
