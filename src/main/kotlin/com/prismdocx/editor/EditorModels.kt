package com.prismdocx.editor

import com.prismdocx.metadata.MetadataSnapshot
import java.io.File

data class OpenedDocument(val file: File, val metadata: MetadataSnapshot)
data class SaveTarget(val file: File, val overwrite: Boolean)
data class EditorNotice(val text: String, val isError: Boolean = false)

/** Действие сохраняется до ответа пользователя, а не выполняется при показе диалога. */
data class PendingAction(
    val message: String,
    val confirm: () -> Unit,
    val cancel: () -> Unit = {},
)
