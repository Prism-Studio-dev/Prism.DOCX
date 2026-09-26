package com.prismdocx.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.awtTransferable
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTargetDropEvent
import java.io.File
import com.prismdocx.platform.droppedDocx

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
internal fun DocxDropArea(
    enabled: Boolean,
    onFile: (File) -> Unit,
    onError: (String) -> Unit,
    content: @Composable () -> Unit,
) {
    var hovering by remember { mutableStateOf(false) }
    val currentEnabled by rememberUpdatedState(enabled)
    val currentOnFile by rememberUpdatedState(onFile)
    val currentOnError by rememberUpdatedState(onError)
    val target = remember {
        object : DragAndDropTarget {
            override fun onEntered(event: DragAndDropEvent) { hovering = currentEnabled }
            override fun onExited(event: DragAndDropEvent) { hovering = false }
            override fun onEnded(event: DragAndDropEvent) { hovering = false }
            override fun onDrop(event: DragAndDropEvent): Boolean {
                hovering = false
                if (!currentEnabled) return false
                return try {
                    // Запрашиваем копирование: проводник не должен перемещать исходный файл.
                    val native = event.nativeEvent as? DropTargetDropEvent ?: return false
                    if (native.sourceActions and DnDConstants.ACTION_COPY == 0) return false
                    native.acceptDrop(DnDConstants.ACTION_COPY)
                    currentOnFile(droppedDocx(event.awtTransferable))
                    true
                } catch (error: Exception) {
                    currentOnError(error.message ?: "Не удалось прочитать перетащенный файл.")
                    false
                }
            }
        }
    }
    Box(
        Modifier.fillMaxSize().dragAndDropTarget(
            shouldStartDragAndDrop = { event ->
                currentEnabled && runCatching {
                    event.awtTransferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)
                }.getOrDefault(false)
            },
            target = target,
        ),
    ) {
        content()
        if (hovering && enabled) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)).padding(24.dp),
                contentAlignment = Alignment.Center) {
                Surface(
                    shape = RectangleShape,
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Column(Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text("Отпустите DOCX здесь", style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.height(12.dp))
                        Text("Документ откроется для редактирования метаданных.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
