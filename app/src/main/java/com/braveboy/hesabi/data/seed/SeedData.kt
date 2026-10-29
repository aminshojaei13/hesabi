package com.braveboy.hesabi.data.seed

import com.braveboy.hesabi.domain.model.Account
import com.braveboy.hesabi.domain.model.AccountKind
import com.braveboy.hesabi.domain.model.Category
import com.braveboy.hesabi.domain.model.CategoryKind

/**
 * First-run data. IDs are stable so imports and future sync stay idempotent.
 * Colors and icon keys are the contract shared with the design system.
 */
object SeedData {

    val accounts = listOf(
        Account(
            id = "acc_cash",
            name = "نقدی",
            kind = AccountKind.CASH,
            colorArgb = 0xFF12AFC0,
            openingBalanceToman = 0,
            sortOrder = 0,
        ),
        Account(
            id = "acc_card",
            name = "کارت بانکی",
            kind = AccountKind.CARD,
            colorArgb = 0xFF6E7B80,
            openingBalanceToman = 0,
            sortOrder = 1,
        ),
    )

    val expenseCategories = listOf(
        category("cat_food", "خوراک", "restaurant", 0xFFD6455D, 0),
        category("cat_transport", "حمل‌ونقل", "directions_car", 0xFF3B82F6, 1),
        category("cat_home", "خانه", "home", 0xFFC9820A, 2),
        category("cat_health", "سلامت", "favorite", 0xFF1F9D6B, 3),
        category("cat_shopping", "خرید", "shopping_bag", 0xFF8B5CF6, 4),
        category("cat_bills", "قبض‌ها", "receipt_long", 0xFF0EA5E9, 5),
        category("cat_fun", "سرگرمی", "celebration", 0xFFEC4899, 6),
        category("cat_education", "آموزش", "school", 0xFF6366F1, 7),
        category("cat_gift", "هدیه", "card_giftcard", 0xFFF59E0B, 8),
        category("cat_other", "سایر", "more_horiz", 0xFF8A9A9E, 9),
    )

    val incomeCategories = listOf(
        category("cat_salary", "حقوق", "payments", 0xFF1F9D6B, 0, CategoryKind.INCOME),
        category("cat_business", "فروش", "storefront", 0xFF0EA5E9, 1, CategoryKind.INCOME),
        category("cat_gift_income", "هدیه", "card_giftcard", 0xFFF59E0B, 2, CategoryKind.INCOME),
        category("cat_other_income", "سایر", "more_horiz", 0xFF8A9A9E, 3, CategoryKind.INCOME),
    )

    val categories = expenseCategories + incomeCategories

    private fun category(
        id: String,
        name: String,
        icon: String,
        color: Long,
        order: Int,
        kind: CategoryKind = CategoryKind.EXPENSE,
    ) = Category(
        id = id,
        name = name,
        kind = kind,
        iconKey = icon,
        colorArgb = color,
        sortOrder = order,
    )
}
