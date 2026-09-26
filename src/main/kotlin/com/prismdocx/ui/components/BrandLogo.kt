package com.prismdocx.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.prismdocx.resources.Res
import com.prismdocx.resources.prism_docx_logo_dark
import com.prismdocx.resources.prism_docx_logo_light
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun BrandLogo(darkTheme: Boolean, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(if (darkTheme) Res.drawable.prism_docx_logo_dark else Res.drawable.prism_docx_logo_light),
        contentDescription = "Prism.DOCX",
        contentScale = ContentScale.Fit,
        modifier = modifier,
    )
}
