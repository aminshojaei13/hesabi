package com.braveboy.hesabi.ui.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Payment
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.braveboy.hesabi.BuildConfig
import com.braveboy.hesabi.ui.components.ConfirmDialog
import com.braveboy.hesabi.ui.components.CountBadge
import com.braveboy.hesabi.ui.components.HesabiCard
import com.braveboy.hesabi.ui.components.InfoBanner
import com.braveboy.hesabi.ui.components.SettingsRow
import com.braveboy.hesabi.ui.theme.HesabiSpacing
import com.braveboy.hesabi.ui.theme.HesabiTheme

@Composable
fun MoreScreen(
    pendingSmsCount: Int,
    onAccounts: () -> Unit,
    onCategories: () -> Unit,
    onBudget: () -> Unit,
    onDebts: () -> Unit,
    onRecurring: () -> Unit,
    onSms: () -> Unit,
    onBackup: () -> Unit,
    onSettings: () -> Unit,
    onAssistant: () -> Unit = {},
    onAssets: () -> Unit = {},
    onTrash: () -> Unit = {},
    onImport: () -> Unit = {},
    onMyData: () -> Unit = {},
    onAnnual: () -> Unit = {},
    onCalendar: () -> Unit = {},
    onSimulator: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var showAbout by remember { mutableStateOf(false) }
    MoreContent(
        pendingSmsCount = pendingSmsCount,
        onAccounts = onAccounts,
        onCategories = onCategories,
        onBudget = onBudget,
        onDebts = onDebts,
        onRecurring = onRecurring,
        onSms = onSms,
        onBackup = onBackup,
        onSettings = onSettings,
        onAssistant = onAssistant,
        onAssets = onAssets,
        onTrash = onTrash,
        onImport = onImport,
        onMyData = onMyData,
        onAnnual = onAnnual,
        onCalendar = onCalendar,
        onSimulator = onSimulator,
        onAbout = { showAbout = true },
        modifier = modifier,
    )
    if (showAbout) {
        ConfirmDialog(
            title = "حسابی ${BuildConfig.VERSION_NAME}",
            message = "دفترچهٔ هزینهٔ فارسی، محلی و بدون حساب کاربری.\n\n" +
                "همهٔ داده‌ها روی همین گوشی می‌مانند. برای انتقال به گوشی دیگر از پشتیبان رمزدار استفاده کن.\n\n" +
                "ساخته‌شده با Jetpack Compose و Room.",
            confirmLabel = "باشه",
            onConfirm = { showAbout = false },
            onDismiss = { showAbout = false },
        )
    }
}

@Composable
fun MoreContent(
    pendingSmsCount: Int,
    onAccounts: () -> Unit,
    onCategories: () -> Unit,
    onBudget: () -> Unit,
    onDebts: () -> Unit,
    onRecurring: () -> Unit,
    onSms: () -> Unit,
    onBackup: () -> Unit,
    onSettings: () -> Unit,
    onAbout: () -> Unit,
    onAssistant: () -> Unit = {},
    onAssets: () -> Unit = {},
    onTrash: () -> Unit = {},
    onImport: () -> Unit = {},
    onMyData: () -> Unit = {},
    onAnnual: () -> Unit = {},
    onCalendar: () -> Unit = {},
    onSimulator: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = HesabiSpacing.lg)
            .padding(top = HesabiSpacing.md, bottom = 108.dp),
        verticalArrangement = Arrangement.spacedBy(HesabiSpacing.lg),
    ) {
        Column {
            Text("بیشتر", style = MaterialTheme.typography.headlineSmall)
            Text(
                text = "مدیریت حساب‌ها، داده‌ها و ظاهر برنامه",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        MenuCard {
            SettingsRow(
                title = "حساب‌ها",
                subtitle = "نقدی، کارت و کیف پول",
                icon = Icons.Rounded.AccountBalanceWallet,
                onClick = onAccounts,
            )
            RowDivider()
            SettingsRow(
                title = "دسته‌ها",
                subtitle = "دسته‌های هزینه و درآمد",
                icon = Icons.Rounded.Category,
                onClick = onCategories,
            )
            RowDivider()
            SettingsRow(
                title = "بودجه",
                subtitle = "سقف ماهانه و بودجهٔ هر دسته",
                icon = Icons.Rounded.Savings,
                onClick = onBudget,
            )
            RowDivider()
            SettingsRow(
                title = "تعهدات، چک‌ها و وام‌ها",
                subtitle = "بدهی و طلب، چک‌های صیادی و اقساط",
                icon = Icons.Rounded.Payment,
                onClick = onDebts,
            )
            RowDivider()
            SettingsRow(
                title = "تراکنش‌های دوره‌ای و ثابت",
                subtitle = "اجاره، قبوض، اشتراک‌ها و حقوق منظم",
                icon = Icons.Rounded.Repeat,
                onClick = onRecurring,
            )
        }
        MenuCard {
            SettingsRow(
                title = "دستیار مالی",
                subtitle = "ثبت با جمله و پرسش‌های مالی، کاملاً محلی",
                icon = Icons.Rounded.AutoAwesome,
                onClick = onAssistant,
            )
            RowDivider()
            SettingsRow(
                title = "دارایی‌ها",
                subtitle = "طلا، ارز، ملک و سود/زیان",
                icon = Icons.Rounded.AccountBalance,
                onClick = onAssets,
            )
            RowDivider()
            SettingsRow(
                title = "تقویم سررسید",
                subtitle = "نمای ماهانهٔ چک، قسط و تعهدات",
                icon = Icons.Rounded.CalendarMonth,
                onClick = onCalendar,
            )
            RowDivider()
            SettingsRow(
                title = "می‌توانم بخرم؟",
                subtitle = "اثر خرید روی خرج امن",
                icon = Icons.Rounded.Calculate,
                onClick = onSimulator,
            )
            RowDivider()
            SettingsRow(
                title = "حساب سال من",
                subtitle = "گزارش سالانه و خروجی PDF/Excel",
                icon = Icons.Rounded.Insights,
                onClick = onAnnual,
            )
        }
        MenuCard {
            SettingsRow(
                title = "پیامک‌های بانکی",
                subtitle = "ثبت خودکار اختیاری، بدون خواندن دفتر پیامک",
                icon = Icons.Rounded.Sms,
                onClick = onSms,
                trailing = {
                    if (pendingSmsCount > 0) CountBadge(pendingSmsCount) else DefaultChevron()
                },
            )
            RowDivider()
            SettingsRow(
                title = "پشتیبان و خروجی",
                subtitle = "فایل رمزدار، CSV، PDF و Excel",
                icon = Icons.Rounded.Backup,
                onClick = onBackup,
            )
            RowDivider()
            SettingsRow(
                title = "ورود از فایل",
                subtitle = "مهاجرت از CSV یا اکسل",
                icon = Icons.Rounded.UploadFile,
                onClick = onImport,
            )
            RowDivider()
            SettingsRow(
                title = "سطل بازیافت",
                subtitle = "بازگردانی ۳۰ روزهٔ تراکنش‌ها",
                icon = Icons.Rounded.DeleteForever,
                onClick = onTrash,
            )
        }
        MenuCard {
            SettingsRow(
                title = "دادهٔ من کجاست؟",
                subtitle = "شفافیت داده، رمزنگاری و حذف کامل",
                icon = Icons.Rounded.Lock,
                onClick = onMyData,
            )
            RowDivider()
            SettingsRow(
                title = "تنظیمات",
                subtitle = "ظاهر، واحد پول و حریم خصوصی",
                icon = Icons.Rounded.Settings,
                onClick = onSettings,
            )
            RowDivider()
            SettingsRow(
                title = "دربارهٔ حسابی",
                subtitle = "نسخه ${BuildConfig.VERSION_NAME}",
                icon = Icons.Rounded.Info,
                onClick = onAbout,
            )
        }
        InfoBanner(
            text = "هیچ داده‌ای به اینترنت فرستاده نمی‌شود. حسابی آفلاین کار می‌کند.",
            modifier = Modifier.fillMaxWidth(),
        )
        TextButton(onClick = onAbout, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "ساخته‌شده برای فارسی‌زبان‌ها",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MenuCard(content: @Composable () -> Unit) {
    HesabiCard(contentPadding = PaddingValues(vertical = HesabiSpacing.xs)) {
        content()
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = HesabiSpacing.lg),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@Composable
private fun DefaultChevron() {
    androidx.compose.material3.Icon(
        imageVector = Icons.Rounded.ChevronLeft,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Preview(name = "بیشتر", heightDp = 850, showBackground = true)
@Composable
private fun MorePreview() {
    HesabiTheme {
        androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(modifier = Modifier.fillMaxSize()) {
                MoreContent(
                    pendingSmsCount = 3,
                    onAccounts = {},
                    onCategories = {},
                    onBudget = {},
                    onDebts = {},
                    onRecurring = {},
                    onSms = {},
                    onBackup = {},
                    onSettings = {},
                    onAbout = {},
                )
            }
        }
    }
}

@Preview(
    name = "بیشتر - تیره",
    heightDp = 850,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Composable
private fun MoreDarkPreview() {
    HesabiTheme(themeMode = com.braveboy.hesabi.domain.model.ThemeMode.DARK) {
        androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(modifier = Modifier.fillMaxSize()) {
                MoreContent(0, {}, {}, {}, {}, {}, {}, {}, {}, {})
            }
        }
    }
}
