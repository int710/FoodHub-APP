package com.example.foodhubapp.feature.shared.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
            modifier = modifier.fillMaxWidth(.92f),
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            tonalElevation = 8.dp,
            shadowElevation = 18.dp,
        ) {
            Column(Modifier.fillMaxWidth().padding(22.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        eyebrow?.let {
                            Text(it.uppercase(), color = Brand, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp)
                            Spacer(Modifier.size(4.dp))
                        }
                        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                    }
                    Spacer(Modifier.width(10.dp))
                    Surface(shape = CircleShape, color = BrandSoft.copy(alpha = .55f)) {
                        IconButton(onClick = onDismissRequest, enabled = dismissEnabled, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Close, "Đóng", tint = OnSurfaceVariant, modifier = Modifier.size(19.dp))
                        }
                    }
                }
                Spacer(Modifier.size(18.dp))
                content()
                Spacer(Modifier.size(20.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, content = actions)
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
            modifier = modifier.fillMaxWidth(.92f),
            shape = shape,
            color = containerColor,
            tonalElevation = tonalElevation,
            shadowElevation = 18.dp,
        ) {
            Column(Modifier.fillMaxWidth().padding(22.dp)) {
                icon?.let {
                    Surface(shape = CircleShape, color = BrandSoft, contentColor = iconContentColor) {
                        Column(Modifier.padding(10.dp), content = { it() })
                    }
                    Spacer(Modifier.size(12.dp))
                }
                title?.let {
                    Surface(color = Color.Transparent, contentColor = titleContentColor) { it() }
                    Spacer(Modifier.size(14.dp))
                }
                text?.let {
                    Surface(color = Color.Transparent, contentColor = textContentColor) { it() }
                    Spacer(Modifier.size(20.dp))
                }
                Row(
                    Modifier.fillMaxWidth(),
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
