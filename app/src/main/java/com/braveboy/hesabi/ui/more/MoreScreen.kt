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
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.Info
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
    onSms: () -> Unit,
    onBackup: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showAbout by remember { mutableStateOf(false) }
    MoreContent(
        pendingSmsCount = pendingSmsCount,
        onAccounts = onAccounts,
        onCategories = onCategories,
        onBudget = onBudget,
        onSms = onSms,
        onBackup = onBackup,
        onSettings = onSettings,
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
    onSms: () -> Unit,
    onBackup: () -> Unit,
    onSettings: () -> Unit,
    onAbout: () -> Unit,
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
                subtitle = "فایل رمزدار یا CSV",
                icon = Icons.Rounded.Backup,
                onClick = onBackup,
            )
        }
        MenuCard {
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
                MoreContent(0, {}, {}, {}, {}, {}, {}, {})
            }
        }
    }
}
