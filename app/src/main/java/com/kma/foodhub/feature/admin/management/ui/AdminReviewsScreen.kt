package com.kma.foodhub.feature.admin.management.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.kma.foodhub.core.network.resolveMediaUrl
import com.kma.foodhub.core.ui.icons.TablerIcons
import com.kma.foodhub.feature.admin.management.model.AdminReview
import com.kma.foodhub.feature.admin.management.viewmodel.AdminManagementState
import com.kma.foodhub.feature.admin.navigation.*

@Composable
fun AdminReviewsScreen(
    state: AdminManagementState,
    onLoad: (Int?, String?) -> Unit,
    onDelete: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf<Int?>(null) }
    var pendingDelete by remember { mutableStateOf<AdminReview?>(null) }
    LaunchedEffect(Unit) { onLoad(null, null) }

    pendingDelete?.let { review ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            icon = { Icon(TablerIcons.Delete, null, tint = AdminRed) },
            title = { Text("Xóa đánh giá?") },
            text = { Text("Đánh giá của ${review.customerName} cho món ${review.menuItemName} sẽ bị xóa khỏi hệ thống.") },
            confirmButton = { TextButton(onClick = { onDelete(review.id); pendingDelete = null }) { Text("Xóa", color = AdminRed) } },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Giữ lại") } },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(AdminBackground),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            val average = if (state.reviews.isEmpty()) 0.0 else state.reviews.map { it.rating }.average()
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("Đánh giá", state.reviews.size.toString(), Modifier.weight(1f))
                MetricCard("Điểm trung bình", String.format("%.1f / 5", average), Modifier.weight(1f))
            }
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Tên món, khách hàng hoặc mã đơn") },
                leadingIcon = { Icon(TablerIcons.Search, null) },
                trailingIcon = {
                    IconButton(onClick = { onLoad(rating, query) }) { Icon(TablerIcons.Search, "Tìm") }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
            )
        }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = rating == null, onClick = { rating = null; onLoad(null, query) }, label = { Text("Tất cả") })
                (5 downTo 1).forEach { value ->
                    FilterChip(
                        selected = rating == value,
                        onClick = { rating = value; onLoad(value, query) },
                        label = { Text("$value sao") },
                        leadingIcon = { Icon(TablerIcons.StarFilled, null, Modifier.size(15.dp), tint = Color(0xFFF2A900)) },
                    )
                }
            }
        }
        if (state.isLoadingReviews) {
            item { Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        } else if (state.reviews.isEmpty()) {
            item { Text("Chưa có đánh giá phù hợp", color = AdminMuted, modifier = Modifier.padding(24.dp)) }
        } else {
            items(state.reviews, key = { it.id }) { review ->
                ReviewCard(review, state.busyReviewId == review.id) { pendingDelete = review }
            }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier) {
    Card(modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = AdminSurface)) {
        Column(Modifier.padding(16.dp)) {
            Text(value, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = AdminPrimary)
            Text(label, color = AdminMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ReviewCard(review: AdminReview, busy: Boolean, onDelete: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = AdminSurface), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FoodThumbnail(review.menuItemImage, review.menuItemName)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(review.menuItemName, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(review.customerName, color = AdminMuted, fontSize = 12.sp)
                    Text(review.orderCode, color = AdminPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
                if (busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                else IconButton(onClick = onDelete) { Icon(TablerIcons.Delete, "Xóa đánh giá", tint = AdminRed) }
            }
            Row {
                repeat(5) { index ->
                    Icon(
                        if (index < review.rating) TablerIcons.StarFilled else TablerIcons.Star,
                        null,
                        Modifier.size(19.dp),
                        tint = if (index < review.rating) Color(0xFFF2A900) else AdminBorder,
                    )
                }
            }
            if (review.comment.isNotBlank()) Text(review.comment, color = AdminText, lineHeight = 20.sp)
            if (review.images.isNotEmpty()) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    review.images.forEach { image ->
                        SubcomposeAsyncImage(
                            model = resolveMediaUrl(image), contentDescription = "Ảnh đánh giá",
                            modifier = Modifier.size(82.dp).clip(RoundedCornerShape(12.dp)).background(AdminPrimarySoft),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FoodThumbnail(url: String?, name: String) {
    SubcomposeAsyncImage(
        model = resolveMediaUrl(url), contentDescription = name,
        modifier = Modifier.size(58.dp).clip(RoundedCornerShape(12.dp)).background(AdminPrimarySoft),
        contentScale = ContentScale.Crop,
        error = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(TablerIcons.Restaurant, null, tint = AdminPrimary) } },
    )
}
