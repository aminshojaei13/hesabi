package com.braveboy.hesabi.ui.privacy

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.braveboy.hesabi.core.diagnostics.CrashLogger
import com.braveboy.hesabi.di.AppContainer
import com.braveboy.hesabi.domain.model.AppSettings
import com.braveboy.hesabi.ui.components.ConfirmDialog
import com.braveboy.hesabi.ui.components.HesabiButton
import com.braveboy.hesabi.ui.components.HesabiButtonVariant
import com.braveboy.hesabi.ui.components.HesabiCard
import com.braveboy.hesabi.ui.components.HesabiTopBar
import com.braveboy.hesabi.ui.components.InfoBanner
import com.braveboy.hesabi.ui.components.SectionHeader
import com.braveboy.hesabi.ui.security.SecureScreen
import com.braveboy.hesabi.ui.theme.HesabiSpacing
import kotlinx.coroutines.launch

private data class DataInventory(
    val transactions: Int = 0,
    val accounts: Int = 0,
    val categories: Int = 0,
    val assets: Int = 0,
    val goals: Int = 0,
    val lastBackupAtMillis: Long? = null,
    val lastCrash: String? = null,
    val loaded: Boolean = false,
)

/**
 * "دادهٔ من کجاست؟" — the transparency page promised in the blueprint.
 * It lists what is stored, what leaves the device (nothing), and how to own/delete it.
 */
@Composable
fun MyDataScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SecureScreen()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var inventory by remember { mutableStateOf(DataInventory()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val settings: AppSettings = container.settingsRepository.current()
        inventory = DataInventory(
            transactions = container.transactionRepository.getAll().size,
            accounts = container.accountRepository.getAll().size,
            categories = container.categoryRepository.getAll().size,
            assets = container.assetRepository.getAll().size,
            goals = container.savingsGoalRepository.getAll().size,
            lastBackupAtMillis = settings.lastBackupAtMillis,
            lastCrash = CrashLogger.lastCrash(context),
            loaded = true,
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        HesabiTopBar(
            title = "دادهٔ من کجاست؟",
            subtitle = "شفاف، بدون تبلیغ و بدون فروش داده",
            onBack = onBack,
        )
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = HesabiSpacing.lg)
                .padding(bottom = HesabiSpacing.xxl),
            verticalArrangement = Arrangement.spacedBy(HesabiSpacing.md),
        ) {
            message?.let {
                InfoBanner(text = it, actionLabel = "باشه", onAction = { message = null })
            }
            HesabiCard {
                SectionHeader("روی این گوشی")
                InfoLine("پایگاه دادهٔ خصوصی برنامه", "فقط در فضای اختصاصی حسابی")
                InfoLine("تصاویر رسید", "با کلید امن سیستم‌عامل (Keystore) رمز می‌شوند")
                InfoLine("پشتیبان خودکار سیستم", "غیرفعال است تا دادهٔ مالی به ابر نرود")
                InfoLine("متن پیامک", "شمارهٔ کارت و حساب پوشانده می‌شود")
                InfoLine(
                    "حجم داده",
                    "${inventory.transactions} تراکنش، ${inventory.accounts} حساب، " +
                        "${inventory.categories} دسته، ${inventory.goals} هدف، ${inventory.assets} دارایی",
                    emphasize = true,
                )
            }
            HesabiCard {
                SectionHeader("در اینترنت")
                InfoBanner(
                    text = "هیچ داده‌ای به اینترنت فرستاده نمی‌شود. حسابی برای کار کردن به اینترنت نیاز ندارد.",
                    icon = Icons.Rounded.Lock,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                InfoLine("دستیار مالی", "پردازش متن روی همین دستگاه انجام می‌شود.")
                InfoLine("پیامک بانکی", "پیامک فقط روی گوشی خوانده و تجزیه می‌شود.")
                InfoLine("قیمت دارایی", "فعلاً به‌صورت دستی وارد می‌شود؛ منبع و زمان ثبت می‌شود.")
                InfoLine("همگام‌سازی ابری", "در نسخهٔ ۱٫۰ وجود ندارد.")
            }
            HesabiCard {
                SectionHeader("مالکیت و حذف")
                InfoLine(
                    "پشتیبان رمزدار",
                    inventory.lastBackupAtMillis?.let { "آخرین پشتیبان ثبت شده است." }
                        ?: "هنوز پشتیبان نگرفته‌ای.",
                )
                Row(horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.sm)) {
                    HesabiButton(
                        text = "پشتیبان و خروجی",
                        onClick = onOpenBackup,
                        variant = HesabiButtonVariant.SECONDARY,
                        modifier = Modifier.weight(1f),
                    )
                    HesabiButton(
                        text = "تنظیمات امنیت",
                        onClick = onOpenSettings,
                        variant = HesabiButtonVariant.SECONDARY,
                        modifier = Modifier.weight(1f),
                    )
                }
                HesabiButton(
                    text = "پاک کردن همهٔ داده‌ها",
                    onClick = { showDeleteConfirm = true },
                    variant = HesabiButtonVariant.DANGER,
                    icon = Icons.Rounded.DeleteForever,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            inventory.lastCrash?.let { crash ->
                HesabiCard {
                    SectionHeader("آخرین گزارش خطا (محلی)")
                    Text(
                        text = crash.take(500),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "این گزارش فقط روی گوشی ذخیره شده و به جایی ارسال نمی‌شود.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        ConfirmDialog(
            title = "پاک کردن همهٔ داده‌ها",
            message = "همهٔ تراکنش‌ها، حساب‌ها، بودجه‌ها، اهداف، دارایی‌ها و تنظیمات پاک می‌شوند. " +
                "این کار برگشت‌پذیر نیست؛ اگر لازم است اول پشتیبان بگیر.",
            confirmLabel = "همه را پاک کن",
            dangerous = true,
            onConfirm = {
                scope.launch {
                    container.clearAllData()
                    inventory = DataInventory()
                    message = "همهٔ داده‌ها پاک شد و حساب‌های پیش‌فرض بازسازی شدند."
                }
                showDeleteConfirm = false
            },
            onDismiss = { showDeleteConfirm = false },
        )
    }
}

@Composable
private fun InfoLine(label: String, value: String, emphasize: Boolean = false) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = if (emphasize) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}
