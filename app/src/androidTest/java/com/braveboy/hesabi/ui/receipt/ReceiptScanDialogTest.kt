package com.braveboy.hesabi.ui.receipt

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.braveboy.hesabi.domain.logic.ReceiptOcrResult
import com.braveboy.hesabi.domain.model.Category
import com.braveboy.hesabi.domain.model.CategoryKind
import com.braveboy.hesabi.ui.entry.ReceiptScanState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class ReceiptScanDialogTest {

    @get:Rule
    val compose = createComposeRule()

    private val categories = listOf(
        Category("c1", "خرید", CategoryKind.EXPENSE, "cart", 0),
        Category("c2", "حمل‌ونقل", CategoryKind.EXPENSE, "bus", 0),
    )

    private fun show(
        scanning: Boolean = false,
        result: ReceiptOcrResult = ReceiptOcrResult(amountToman = 125_000, merchant = "سوپرمارکت افق"),
        onUse: (Long, String, String?) -> Unit = { _, _, _ -> },
        onDismiss: () -> Unit = {},
    ) {
        compose.setContent {
            MaterialTheme {
                ReceiptScanDialog(
                    scan = ReceiptScanState(
                        imagePath = "/nonexistent/receipt.hsr",
                        scanning = scanning,
                        result = result,
                    ),
                    categories = categories,
                    onDismiss = onDismiss,
                    onUse = onUse,
                )
            }
        }
    }

    @Test
    fun confirmsTheOcrAmountAndMerchant() {
        var applied: Triple<Long, String, String?>? = null
        show(onUse = { amount, note, category -> applied = Triple(amount, note, category) })

        compose.onNodeWithText("۱۲۵٬۰۰۰").assertDoesNotExist()
        compose.onNodeWithText("125000").assertExists()
        compose.onNodeWithText("سوپرمارکت افق").assertExists()
        compose.onNodeWithText("استفاده").performClick()

        assertEquals(125_000L, applied?.first)
        assertEquals("سوپرمارکت افق", applied?.second)
        assertNull(applied?.third)
    }

    @Test
    fun theUserEditedAmountWinsOverOcr() {
        var applied: Pair<Long, String>? = null
        show(onUse = { amount, note, _ -> applied = amount to note })

        compose.onNodeWithText("125000").performTextReplacement("240000")
        compose.onNodeWithText("استفاده").performClick()

        assertEquals(240_000L, applied?.first)
    }

    @Test
    fun aZeroAmountCannotBeConfirmed() {
        show()

        compose.onNodeWithText("125000").performTextReplacement("0")
        compose.onNodeWithText("استفاده").assertIsNotEnabled()
    }

    @Test
    fun scanningOnlyShowsProgress() {
        show(scanning = true)

        compose.onNodeWithText("در حال خواندن رسید روی همین گوشی…").assertExists()
        compose.onNodeWithText("استفاده").assertDoesNotExist()
    }

    @Test
    fun choosingACategoryPassesItsId() {
        var applied: Triple<Long, String, String?>? = null
        show(onUse = { amount, note, category -> applied = Triple(amount, note, category) })

        compose.onNodeWithText("حمل‌ونقل").performClick()
        compose.onNodeWithText("استفاده").performClick()

        assertEquals("c2", applied?.third)
    }

    @Test
    fun cancelDoesNotApplyAnything() {
        var dismissed = false
        show(onDismiss = { dismissed = true })

        compose.onNodeWithText("لغو").performClick()

        assertEquals(true, dismissed)
    }
}
