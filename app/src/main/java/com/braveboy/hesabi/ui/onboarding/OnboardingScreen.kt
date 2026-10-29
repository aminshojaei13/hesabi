package com.braveboy.hesabi.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.braveboy.hesabi.di.AppContainer
import com.braveboy.hesabi.ui.components.HesabiButton
import com.braveboy.hesabi.ui.components.HesabiCard
import com.braveboy.hesabi.ui.components.InfoBanner
import com.braveboy.hesabi.ui.hesabiViewModel
import com.braveboy.hesabi.ui.settings.SettingsViewModel
import com.braveboy.hesabi.ui.theme.HesabiSpacing
import com.braveboy.hesabi.ui.theme.HesabiTheme

@Composable
fun OnboardingScreen(
    container: AppContainer,
    modifier: Modifier = Modifier,
) {
    val viewModel: SettingsViewModel = hesabiViewModel(container)
    OnboardingContent(
        onStart = viewModel::completeOnboarding,
        modifier = modifier,
    )
}

@Composable
fun OnboardingContent(
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = HesabiSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(56.dp))
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(92.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "ح",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Spacer(Modifier.height(HesabiSpacing.lg))
        Text(
            text = "حسابی",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "دفترچهٔ هزینهٔ فارسی، ساده و خصوصی",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(HesabiSpacing.xl))
        HesabiCard(modifier = Modifier.fillMaxWidth()) {
            Feature(
                icon = Icons.Rounded.EditNote,
                title = "بعد از ذخیره هم قابل ویرایش",
                text = "هر تراکنش را باز کن، تغییر بده یا با یک ضربه واگرد کن.",
            )
            Feature(
                icon = Icons.Rounded.Sms,
                title = "پیامک بانکی، اختیاری",
                text = "خودکار یا با چسباندن متن؛ هیچ پیامکی بدون تأیید ثبت نمی‌شود.",
            )
            Feature(
                icon = Icons.Rounded.Insights,
                title = "گزارش ماهانهٔ واقعی",
                text = "انتقال بین حساب‌ها، هزینه حساب نمی‌شود. بودجه هم داری.",
            )
            Feature(
                icon = Icons.Rounded.Backup,
                title = "پشتیبان رمزدار",
                text = "همه‌چیز در یک فایل، با رمز خودت. خروجی CSV هم هست.",
            )
        }
        Spacer(Modifier.height(HesabiSpacing.lg))
        InfoBanner(
            text = "بدون حساب کاربری، بدون اینترنت، بدون تبلیغ. داده‌ها روی همین گوشی می‌مانند.",
            icon = Icons.Rounded.Lock,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(HesabiSpacing.xl))
        HesabiButton(
            text = "شروع",
            onClick = onStart,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(HesabiSpacing.md))
        Text(
            text = "در قدم بعد می‌توانی حساب‌ها و دسته‌ها را دلخواه تغییر بدهی.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(HesabiSpacing.xxl))
    }
}

@Composable
private fun Feature(icon: ImageVector, title: String, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = HesabiSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.md),
        verticalAlignment = Alignment.Top,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(38.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Column {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(
                text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(name = "خوش‌آمد", heightDp = 900, showBackground = true)
@Composable
private fun OnboardingPreview() {
    HesabiTheme {
        androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(modifier = Modifier.fillMaxSize()) {
                OnboardingContent(onStart = {})
            }
        }
    }
}

@Preview(
    name = "خوش‌آمد - تیره",
    heightDp = 900,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Composable
private fun OnboardingDarkPreview() {
    HesabiTheme(themeMode = com.braveboy.hesabi.domain.model.ThemeMode.DARK) {
        androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(modifier = Modifier.fillMaxSize()) {
                OnboardingContent(onStart = {})
            }
        }
    }
}
