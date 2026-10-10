package com.example.foodhubapp.feature.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.foodhubapp.theme.Brand
import com.example.foodhubapp.theme.BrandSoft
import com.example.foodhubapp.theme.OnSurfaceVariant

@Composable
fun FoodHubDialog(
    onDismissRequest: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    dismissEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
    actions: @Composable RowScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = { if (dismissEnabled) onDismissRequest() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = modifier.fillMaxWidth(.92f).heightIn(max = 760.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            tonalElevation = 8.dp,
            shadowElevation = 18.dp,
        ) {
            Column(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().background(Brand).padding(start = 20.dp, end = 10.dp, top = 13.dp, bottom = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        eyebrow?.let {
                            Text(it.uppercase(), color = Color.White.copy(alpha = .78f), fontWeight = FontWeight.ExtraBold, fontSize = 10.sp)
                            Spacer(Modifier.size(4.dp))
                        }
                        Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                    }
                    Spacer(Modifier.width(10.dp))
                    Surface(shape = CircleShape, color = Color.White.copy(alpha = .14f)) {
                        IconButton(onClick = onDismissRequest, enabled = dismissEnabled, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Close, "Đóng", tint = Color.White, modifier = Modifier.size(19.dp))
                        }
                    }
                }
                Column(Modifier.fillMaxWidth().padding(20.dp), content = content)
                HorizontalDivider(color = Color(0xFFECE8E5))
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions,
                )
            }
        }
    }
}

@Composable
fun FoodHubAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    shape: Shape = RoundedCornerShape(28.dp),
    containerColor: Color = Color.White,
    iconContentColor: Color = MaterialTheme.colorScheme.secondary,
    titleContentColor: Color = MaterialTheme.colorScheme.onSurface,
    textContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    tonalElevation: Dp = 8.dp,
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false),
) {
    Dialog(onDismissRequest = onDismissRequest, properties = properties) {
        Surface(
            modifier = modifier.fillMaxWidth(.92f).heightIn(max = 760.dp),
            shape = shape,
            color = containerColor,
            tonalElevation = tonalElevation,
            shadowElevation = 18.dp,
        ) {
            Column(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().background(Brand).padding(start = 20.dp, end = 10.dp, top = 13.dp, bottom = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    icon?.let {
                        Surface(shape = CircleShape, color = Color.White.copy(alpha = .14f), contentColor = Color.White) {
                            Column(Modifier.padding(8.dp), content = { it() })
                        }
                        Spacer(Modifier.width(10.dp))
                    }
                    title?.let {
                        Surface(Modifier.weight(1f), color = Color.Transparent, contentColor = Color.White) { it() }
                    } ?: Spacer(Modifier.weight(1f))
                    IconButton(onClick = onDismissRequest, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Close, "Đóng", tint = Color.White, modifier = Modifier.size(19.dp))
                    }
                }
                text?.let {
                    Surface(
                        Modifier.fillMaxWidth().padding(20.dp),
                        color = Color.Transparent,
                        contentColor = textContentColor,
                    ) { it() }
                }
                HorizontalDivider(color = Color(0xFFECE8E5))
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    dismissButton?.let {
                        it()
                        Spacer(Modifier.weight(1f))
                    }
                    confirmButton()
                }
            }
        }
    }
}
