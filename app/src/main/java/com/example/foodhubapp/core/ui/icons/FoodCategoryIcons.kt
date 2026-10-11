package com.example.foodhubapp.core.ui.icons

import androidx.compose.ui.graphics.vector.ImageVector
import java.text.Normalizer

data class FoodCategoryIconOption(
    val key: String,
    val label: String,
    val icon: ImageVector,
)

val foodCategoryIconOptions = listOf(
    FoodCategoryIconOption("restaurant", "Món ăn", TablerIcons.Restaurant),
    FoodCategoryIconOption("soup", "Món nước", TablerIcons.CategorySoup),
    FoodCategoryIconOption("meat", "Món thịt", TablerIcons.CategoryMeat),
    FoodCategoryIconOption("vegetarian", "Món chay", TablerIcons.CategoryVegetarian),
    FoodCategoryIconOption("drink", "Đồ uống", TablerIcons.CategoryDrink),
    FoodCategoryIconOption("dessert", "Tráng miệng", TablerIcons.CategoryDessert),
    FoodCategoryIconOption("coffee", "Cà phê", TablerIcons.CategoryCoffee),
)

fun foodCategoryIcon(value: String, categoryName: String = ""): ImageVector {
    val key = foodCategoryIconKey(value, categoryName)
    return foodCategoryIconOptions.firstOrNull { it.key == key }?.icon ?: TablerIcons.Restaurant
}

fun foodCategoryIconKey(value: String, categoryName: String = ""): String {
    val normalized = Normalizer.normalize("$value $categoryName", Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .lowercase()
    return when {
        listOf("coffee", "ca phe").any(normalized::contains) -> "coffee"
        listOf("drink", "do uong", "nuoc", "tra ").any(normalized::contains) -> "drink"
        listOf("dessert", "trang mieng", "banh", "kem").any(normalized::contains) -> "dessert"
        listOf("vegetarian", "mon chay", "rau").any(normalized::contains) -> "vegetarian"
        listOf("meat", "thit", "nuong").any(normalized::contains) -> "meat"
        listOf("soup", "pho", "bun", "mien", "mon nuoc").any(normalized::contains) -> "soup"
        else -> "restaurant"
    }
}
