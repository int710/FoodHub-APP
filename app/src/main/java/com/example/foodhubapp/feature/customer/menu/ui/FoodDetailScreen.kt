package com.example.foodhubapp.feature.customer.menu.ui

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.foodhubapp.R
import com.example.foodhubapp.feature.customer.menu.data.FoodReview
import com.example.foodhubapp.theme.AppBackground
import com.example.foodhubapp.theme.Brand
import com.example.foodhubapp.theme.BrandSoft
import com.example.foodhubapp.theme.CaptionBrown
import com.example.foodhubapp.theme.FoodHubAppTheme
import com.example.foodhubapp.theme.HeadingFont
import com.example.foodhubapp.theme.InputBackground
import com.example.foodhubapp.theme.Neutral
import com.example.foodhubapp.theme.OnSurfaceVariant
import com.example.foodhubapp.theme.Warning
import java.text.NumberFormat
import java.util.Locale

/**
 * Lựa chọn tùy chọn của món ăn (ví dụ: Size M, Thêm phô mai, Trứng ốp la,...).
 */
data class FoodOption(
    val id: String, 
    val name: String, 
    val priceAdd: Long
)

/**
 * Nhóm các tùy chọn của món ăn (ví dụ: Chọn Kích Cỡ, Topping Thêm).
 * @param multiple Cho phép chọn nhiều option hay chỉ chọn 1 option.
 * @param required Bắt buộc phải chọn ít nhất 1 option hay tùy chọn tự do.
 */
data class FoodOptionGroup(
    val id: String,
    val name: String,
    val multiple: Boolean,
    val required: Boolean,
    val options: List<FoodOption>
)

/**
 * Thông tin chi tiết hoàn chỉnh của món ăn.
 */
data class FoodDetail(
    val id: String,
    val name: String,
    val description: String,
    val basePrice: Long,
    val rating: String,
    val available: Boolean = true,
    val groups: List<FoodOptionGroup> = emptyList(),
    val imageUrl: String? = null,
    val salePrice: Long? = null
)

/**
 * Dữ liệu giỏ hàng người dùng chọn để thêm vào giỏ.
 */
data class FoodCartSelection(
    val menuItemId: String, 
    val quantity: Int, 
    val variantOptionIds: List<String>, 
    val note: String
)

// Dữ liệu mẫu dùng cho Compose Preview
val PreviewBurger = FoodDetail(
    "preview-burger",
    "Truffle Smash Burger Đặc Biệt",
    "Bò tươi nướng áp chảo smash mọng nước, hòa quyện sốt nấm truffle đen, hành tây caramen và bánh mì brioche nướng giòn.",
    145000,
    "4.9",
    groups = listOf(
        FoodOptionGroup(
            "size", "Chọn kích cỡ", false, true, listOf(
                FoodOption("single", "Tiêu chuẩn (Single Patty)", 0),
                FoodOption("double", "Cỡ lớn (Double Patty)", 45000),
                FoodOption("triple", "Siêu thịt (Triple Patty)", 85000)
            )
        ), 
        FoodOptionGroup(
            "topping", "Topping thêm", true, false, listOf(
                FoodOption("cheddar", "Phô mai Cheddar tan chảy", 15000),
                FoodOption("bacon", "Thịt xông khói giòn", 20000),
                FoodOption("egg", "Trứng ốp la lòng đào", 12000),
                FoodOption("truffle", "Sốt nấm Truffle thêm", 15000)
            )
        )
    )
)

/**
 * Hàm định dạng tiền tệ Việt Nam (Ví dụ: 145000 -> "145.000đ").
 */
private fun money(value: Long) =
    NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")).format(value) + "đ"

/**
 * Màn hình Chi tiết Món ăn (FoodDetailScreen)
 * Giúp người dùng xem ảnh, mô tả, chọn size/topping, nhập ghi chú cho bếp và bấm Thêm vào giỏ hàng.
 */
@Composable
fun FoodDetailScreen(
    food: FoodDetail,
    onBackClick: () -> Unit,
    onAddToCart: (FoodCartSelection) -> Unit,
    reviews: List<FoodReview> = emptyList(),
    onCartClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    isAdding: Boolean = false,
    snackbarHost: @Composable () -> Unit = {},
    image: @Composable BoxScope.() -> Unit = {
        FoodImage(
            food.imageUrl,
            food.name,
            Modifier.fillMaxSize()
        )
    }
) {
    // Trạng thái số lượng món ăn (mặc định 1)
    var quantity by rememberSaveable(food.id) { mutableIntStateOf(1) }
    // Trạng thái ghi chú cho bếp
    var note by rememberSaveable(food.id) { mutableStateOf("") }
    // Trạng thái danh sách ID các tùy chọn (size/topping) được chọn
    var selected by rememberSaveable(food.id) { mutableStateOf(emptyList<String>()) }
    
    // Lấy toàn bộ danh sách tùy chọn của món
    val options = food.groups.flatMap { it.options }
    val selectedIds = selected.filter { id -> options.any { it.id == id } }
    
    // Tính tổng tiền = (Giá gốc/khuyến mãi + Giá các tùy chọn thêm) * Số lượng
    val total = ((food.salePrice ?: food.basePrice) + options.filter { it.id in selectedIds }
        .sumOf { it.priceAdd }) * quantity
        
    // Kiểm tra tính hợp lệ: Món phải còn hàng VÀ đã chọn đủ các nhóm bắt buộc (required)
    val valid = food.available && food.groups.all { group ->
        !group.required || group.options.any { it.id in selected }
    }
    
    val context = LocalContext.current

    Scaffold(
        modifier = modifier,
        containerColor = AppBackground,
        snackbarHost = snackbarHost,
        bottomBar = {
            // Thanh công cụ cố định ở đáy màn hình: Tăng/giảm số lượng & Nút Thêm vào giỏ
            Surface(color = Color.White, shadowElevation = 10.dp) {
                Row(
                    Modifier
                        .navigationBarsPadding()
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Cụm nút Nút Trừ (-) / Số lượng / Nút Cộng (+)
                    Row(
                        Modifier.height(54.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Color(0xFFE0DBD8)), color = Color.White) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { quantity-- },
                                    enabled = quantity > 1 && !isAdding,
                                    modifier = Modifier.semantics { contentDescription = "Giảm số lượng" },
                                ) { Text("−", fontSize = 22.sp, color = OnSurfaceVariant) }
                                Text("$quantity", Modifier.width(26.dp), fontWeight = FontWeight.Bold, color = Neutral)
                                IconButton(onClick = { quantity++ }, enabled = quantity < 99 && !isAdding) {
                                    Icon(Icons.Default.Add, "Tăng số lượng", tint = Brand)
                                }
                            }
                        }
                    }
                    
                    // Nút Thêm Vào Giỏ Hàng (Hiển thị tổng tiền và trạng thái loading khi đang thêm)
                    Button(
                        onClick = {
                            onAddToCart(
                                FoodCartSelection(
                                    food.id,
                                    quantity,
                                    selectedIds,
                                    note.trim()
                                )
                            )
                        },
                        enabled = valid && !isAdding,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Brand),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 56.dp)
                    ) {
                        if (isAdding) CircularProgressIndicator(
                            Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        else Icon(Icons.Default.ShoppingCart, null, Modifier.size(18.dp), tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text(if (food.available) "Thêm vào giỏ" else "Tạm hết món", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.weight(1f))
                        Text(money(total), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // 1. Ảnh món ăn và các nút công cụ TopBar
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
                    .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            ) {
                image()
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .48f)))))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Nút Quay Lại
                    Surface(shape = CircleShape, color = Color.White.copy(alpha = .9f)) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                "Quay lại"
                            )
                        }
                    }
                    // Nút Chia Sẻ món ăn qua ứng dụng khác
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(shape = CircleShape, color = Color.White.copy(alpha = .9f)) {
                            IconButton(onClick = onCartClick) {
                                Icon(Icons.Default.ShoppingCart, "Mở giỏ hàng")
                            }
                        }
                        Surface(shape = CircleShape, color = Color.White.copy(alpha = .9f)) {
                            IconButton(onClick = {
                                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "${food.name} - ${money(food.basePrice)}"
                                    )
                                }, "Chia sẻ món ăn"))
                            }) { Icon(Icons.Default.Share, "Chia sẻ món ăn") }
                        }
                    }
                }
                Row(
                    Modifier.align(Alignment.BottomStart).padding(18.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DetailTag("Bán chạy", Icons.Default.LocalFireDepartment)
                    if (food.rating.toDoubleOrNull()?.let { it >= 4.5 } == true) DetailTag("Được yêu thích", Icons.Default.Star)
                }
            }
            
            // 2. Thông tin tên, giá, đánh giá và mô tả món ăn
            Column(Modifier.padding(horizontal = 18.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Text(
                        food.name,
                        modifier = Modifier.weight(1f),
                        fontFamily = HeadingFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 25.sp,
                        lineHeight = 31.sp,
                        color = Neutral,
                    )
                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 14.dp)) {
                        Text(money(food.salePrice ?: food.basePrice), fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Brand)
                        if (food.salePrice != null) Text(
                            money(food.basePrice),
                            color = CaptionBrown,
                            style = MaterialTheme.typography.bodySmall.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough),
                        )
                    }
                }
                Text(food.description, color = OnSurfaceVariant, lineHeight = 22.sp, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoChip(Icons.Default.Star, "${food.rating} điểm", Warning)
                    InfoChip(Icons.Default.AccessTime, "8–12 phút", OnSurfaceVariant)
                    InfoChip(Icons.Default.CheckCircle, if (food.available) "Đang bán" else "Tạm hết", if (food.available) Color(0xFF087A55) else Brand)
                }

                if (reviews.isNotEmpty()) {
                    Text("Đánh giá (${reviews.size})", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    reviews.take(5).forEach { review ->
                        Surface(color = Color.White, shape = RoundedCornerShape(8.dp)) {
                            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Image(
                                        painterResource(R.drawable.avatar_user),
                                        null,
                                        Modifier.size(32.dp).clip(CircleShape),
                                        contentScale = ContentScale.Crop,
                                    )
                                    Text(review.customerName, Modifier.weight(1f).padding(start = 9.dp), fontWeight = FontWeight.SemiBold)
                                    Text("★ ${review.rating}/5", color = Warning, fontWeight = FontWeight.Bold)
                                }
                                if (review.comment.isNotBlank()) Text(review.comment, color = OnSurfaceVariant)
                            }
                        }
                    }
                }
                
                HorizontalDivider(
                    Modifier.padding(vertical = 8.dp),
                    thickness = 6.dp,
                    color = InputBackground
                )
                
                // 3. Các nhóm tùy chọn (Size, Topping, Số lượng)
                food.groups.forEach { group ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            group.name,
                            Modifier.weight(1f),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp
                        )
                        Text(
                            if (group.required) "Bắt buộc" else "Tùy chọn",
                            Modifier
                                .background(
                                    if (group.required) BrandSoft else InputBackground,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(8.dp),
                            fontSize = 11.sp,
                            color = OnSurfaceVariant
                        )
                    }
                    
                    // Danh sách các tùy chọn bên trong nhóm
                    group.options.forEach { option ->
                        val checked = option.id in selected
                        // Cấu hình chọn đơn (Radio) hoặc chọn nhiều (Checkbox)
                        val choose = {
                            selected = if (group.multiple) {
                                if (checked) selected - option.id else selected + option.id
                            } else selected.filterNot { id -> group.options.any { it.id == id } } + option.id
                        }
                        val interaction = if (group.multiple) Modifier.toggleable(
                            checked,
                            enabled = !isAdding,
                            role = Role.Checkbox,
                            onValueChange = { choose() }
                        )
                        else Modifier.selectable(
                            checked,
                            enabled = !isAdding,
                            role = Role.RadioButton,
                            onClick = { choose() }
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (checked) BrandSoft.copy(alpha = .35f) else Color.White,
                            border = BorderStroke(1.dp, if (checked) Brand else Color(0xFFE7E2DF)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                interaction.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (group.multiple) Checkbox(
                                    checked,
                                    null,
                                    enabled = !isAdding,
                                    colors = CheckboxDefaults.colors(checkedColor = Brand)
                                )
                                else RadioButton(
                                    checked,
                                    null,
                                    enabled = !isAdding,
                                    colors = RadioButtonDefaults.colors(selectedColor = Brand)
                                )
                                Text(option.name, Modifier.weight(1f), fontSize = 14.sp)
                                Text(
                                    if (option.priceAdd == 0L) "Đã bao gồm" else "+${money(option.priceAdd)}",
                                    Modifier.padding(start = 8.dp),
                                    fontSize = 12.sp,
                                    color = Brand
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                
                // 4. Khung nhập Ghi chú cho bếp (Ít cay, không hành, v.v.)
                Text("Ghi chú cho bếp", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    note,
                    { note = it.take(255) },
                    enabled = !isAdding,
                    placeholder = { Text("Ít sốt, không hành tây, v.v.") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    supportingText = { Text("${note.length}/255") },
                    minLines = 2
                )
            }
        }
    }
}

@Composable
private fun DetailTag(label: String, icon: ImageVector) {
    Surface(color = Color.Black.copy(alpha = .7f), shape = RoundedCornerShape(9.dp)) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(14.dp), tint = Color.White)
            Spacer(Modifier.width(5.dp))
            Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun InfoChip(icon: ImageVector, label: String, tint: Color) {
    Surface(color = Color.White, shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, Color(0xFFE8E3E0))) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(14.dp), tint = tint)
            Spacer(Modifier.width(4.dp))
            Text(label, color = OnSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FoodDetailPreview() {
    FoodHubAppTheme {
        FoodDetailScreen(PreviewBurger, {}, {}, image = {
            Image(
                painterResource(R.drawable.food_detail_burger),
                PreviewBurger.name,
                Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop)
        })
    }
}

/**
 * Composable hỗ trợ tải và hiển thị ảnh món ăn bất đồng bộ từ URL (sử dụng thư viện Coil)
 */
@Composable
fun FoodImage(url: String?, name: String, modifier: Modifier = Modifier) {
    SubcomposeAsyncImage(
        model = url,
        contentDescription = name,
        modifier = modifier.background(InputBackground),
        contentScale = ContentScale.Crop,
        loading = {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.size(24.dp), color = Brand)
            }
        },
        error = {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Chưa có ảnh món", color = OnSurfaceVariant, fontSize = 12.sp)
            }
        }
    )
}
