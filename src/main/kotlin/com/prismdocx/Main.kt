package com.prismdocx

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.prismdocx.resources.Res
import com.prismdocx.resources.prism_docx_app_icon
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.prismdocx.ui.App
import java.awt.Dimension

private const val APP_VERSION = "1.0.0"

fun main() = application {
    var closeRequested by remember { mutableStateOf(false) }
    Window(
        onCloseRequest = { closeRequested = true },
        title = "Prism.DOCX MHS v$APP_VERSION — метаданные",
        icon = painterResource(Res.drawable.prism_docx_app_icon),
        state = rememberWindowState(width = 1200.dp, height = 850.dp),
    ) {
        LaunchedEffect(Unit) { window.minimumSize = Dimension(820, 640) }
        App(
            onExit = ::exitApplication,
            closeRequested = closeRequested,
            onCancelClose = { closeRequested = false },
        )
    }
}
