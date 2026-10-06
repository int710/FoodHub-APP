@file:Suppress("SameParameterValue")

package com.example.foodhubapp.feature.admin.overview.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodhubapp.feature.admin.navigation.AdminAmber
import com.example.foodhubapp.feature.admin.navigation.AdminAmberSoft
import com.example.foodhubapp.feature.admin.navigation.AdminMuted
import com.example.foodhubapp.feature.admin.navigation.AdminSurface

@Composable
internal fun AdminMetric(title: String, value: String, caption: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = AdminSurface), shape = RoundedCornerShape(8.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(32.dp).background(color.copy(alpha = 0.12f), RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
                    Icon(icon, null, Modifier.size(18.dp), tint = color)
                }
                Spacer(Modifier.weight(1f))
                Text(caption, color = color, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(title, color = AdminMuted, fontSize = 12.sp)
        }
    }
}

@Composable
internal fun AdminAlert(title: String, message: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(AdminAmberSoft, RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.WarningAmber, null, tint = AdminAmber)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(message, color = AdminMuted, fontSize = 12.sp)
        }
        Icon(Icons.Default.ChevronRight, null, tint = AdminMuted)
    }
}

