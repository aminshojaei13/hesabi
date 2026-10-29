package com.braveboy.hesabi.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.ui.graphics.vector.ImageVector
import com.braveboy.hesabi.domain.model.AccountKind

/** Icon keys are part of the data contract (seed data + backups). */
fun categoryIcon(key: String): ImageVector = when (key) {
    "restaurant" -> Icons.Rounded.Restaurant
    "directions_car" -> Icons.Rounded.DirectionsCar
    "home" -> Icons.Rounded.Home
    "favorite" -> Icons.Rounded.Favorite
    "shopping_bag" -> Icons.Rounded.ShoppingBag
    "receipt_long" -> Icons.Rounded.ReceiptLong
    "celebration" -> Icons.Rounded.Celebration
    "school" -> Icons.Rounded.School
    "card_giftcard" -> Icons.Rounded.CardGiftcard
    "payments" -> Icons.Rounded.Payments
    "storefront" -> Icons.Rounded.Storefront
    else -> Icons.Rounded.MoreHoriz
}

fun accountIcon(kind: AccountKind): ImageVector = when (kind) {
    AccountKind.CASH -> Icons.Rounded.Wallet
    AccountKind.CARD -> Icons.Rounded.CreditCard
    AccountKind.BANK -> Icons.Rounded.AccountBalanceWallet
    AccountKind.WALLET -> Icons.Rounded.Payments
}

val FallbackCategoryIcon: ImageVector = Icons.Rounded.Receipt

/** Icon choices offered by the category editor. Keys are the persistence contract. */
val CategoryIconChoices: List<Pair<String, String>> = listOf(
    "restaurant" to "خوراک",
    "directions_car" to "حمل‌ونقل",
    "home" to "خانه",
    "favorite" to "سلامت",
    "shopping_bag" to "خرید",
    "receipt_long" to "قبض",
    "celebration" to "سرگرمی",
    "school" to "آموزش",
    "card_giftcard" to "هدیه",
    "payments" to "حقوق",
    "storefront" to "فروش",
    "more_horiz" to "سایر",
)

/** Palette shared by accounts and categories; stable ARGB values for backups. */
val CategoryColorChoices: List<Long> = listOf(
    0xFFD6455D,
    0xFFEC4899,
    0xFFC9820A,
    0xFFF59E0B,
    0xFF1F9D6B,
    0xFF12AFC0,
    0xFF0EA5E9,
    0xFF3B82F6,
    0xFF6366F1,
    0xFF8B5CF6,
    0xFF6E7B80,
    0xFF8A9A9E,
)
