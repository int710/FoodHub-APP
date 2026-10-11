@file:Suppress("SameParameterValue")

package com.kma.foodhub.feature.admin.components

import com.kma.foodhub.core.ui.icons.TablerIcons

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.kma.foodhub.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kma.foodhub.feature.admin.navigation.AdminBlue
import com.kma.foodhub.feature.admin.navigation.AdminBlueSoft
import com.kma.foodhub.feature.admin.navigation.AdminMuted
import com.kma.foodhub.feature.admin.navigation.AdminPrimary
import com.kma.foodhub.feature.admin.navigation.AdminPrimarySoft
import java.util.Locale

@Composable
internal fun AdminSectionHeader(title: String, action: String? = null, onAction: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        if (action != null) TextButton(onClick = onAction) { Text(action) }
    }
}

@Composable
internal fun AdminSearchField(value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        leadingIcon = { Icon(TablerIcons.Search, null) },
        placeholder = { Text(placeholder) },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
    )
}

@Composable
internal fun AdminAvatar(name: String) {
    Image(
        painter = painterResource(R.drawable.avatar_user),
        contentDescription = "Ảnh đại diện $name",
        modifier = Modifier.size(42.dp).clip(CircleShape),
        contentScale = ContentScale.Crop,
    )
}

@Composable
internal fun AdminPlaceholder(icon: ImageVector, title: String, message: String, button: String, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(64.dp).background(AdminPrimarySoft, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(30.dp), tint = AdminPrimary)
        }
        Spacer(Modifier.height(14.dp))
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(message, color = AdminMuted)
        Spacer(Modifier.height(18.dp))
        ElevatedButton(onClick = onClick) { Text(button) }
    }
}

internal fun Long.vnd(): String = String.format(Locale.forLanguageTag("vi-VN"), "%,d đ", this)

