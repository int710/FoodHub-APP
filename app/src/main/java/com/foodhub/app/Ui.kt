package com.foodhub.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val Ink = Color(0xFF22242B)
internal val Muted = Color(0xFF797A83)
internal val BurntOrange = Color(0xFFBA3900)
internal val PaleBlue = Color(0xFFF4F6FF)
internal val Green = Color(0xFF087F5B)

@Composable
internal fun Label(
    text: String,
    size: TextUnit = 14.sp,
    color: Color = Ink,
    bold: Boolean = false,
    modifier: Modifier = Modifier,
    align: TextAlign? = null,
    lineHeight: TextUnit = (size.value * 1.25f).sp,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = size,
        lineHeight = lineHeight,
        fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
        textAlign = align,
    )
}

@Composable
internal fun GlyphCircle(
    glyph: String,
    size: Dp,
    background: Color,
    foreground: Color,
    fontSize: TextUnit = 20.sp,
    onClick: (() -> Unit)? = null,
) {
    Box(
        modifier = Modifier.size(size).background(background, CircleShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Label(glyph, size = fontSize, color = foreground, bold = true)
    }
}

@Composable
internal fun Dot(color: Color, size: Dp = 5.dp) {
    Box(Modifier.size(size).background(color, CircleShape))
}

@Composable
internal fun ThreeDots(color: Color = Muted) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { Dot(color, 4.dp) }
    }
}
