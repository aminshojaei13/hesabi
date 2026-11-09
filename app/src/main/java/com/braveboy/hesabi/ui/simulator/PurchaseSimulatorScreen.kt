package com.braveboy.hesabi.ui.simulator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.braveboy.hesabi.core.money.Money
import com.braveboy.hesabi.di.AppContainer
import com.braveboy.hesabi.ui.components.HesabiButton
import com.braveboy.hesabi.ui.components.HesabiButtonVariant
import com.braveboy.hesabi.ui.components.HesabiCard
import com.braveboy.hesabi.ui.components.HesabiTextField
import com.braveboy.hesabi.ui.components.HesabiTopBar
import com.braveboy.hesabi.ui.components.InfoBanner
import com.braveboy.hesabi.ui.components.SectionHeader
import com.braveboy.hesabi.ui.hesabiViewModel
import com.braveboy.hesabi.ui.theme.HesabiSpacing

@Composable
fun PurchaseSimulatorScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onAddExpense: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: PurchaseSimulatorViewModel = hesabiViewModel(container)
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        HesabiTopBar(
            title = "می‌توانم بخرم؟",
            subtitle = "اثر خرید روی خرج امن و هدف‌ها",
            onBack = onBack,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = HesabiSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(HesabiSpacing.md),
        ) {
            HesabiTextField(
                value = state.amountText,
                onValueChange = viewModel::setAmount,
                label = "مبلغ خرید (تومان)",
                placeholder = "مثلاً ۸٬۵۰۰٬۰۰۰",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            if (!state.loading) {
                HesabiCard {
                    SectionHeader("خرج امن امروز")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text("قبل از خرید", style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = state.impact?.let {
                                    Money.format(it.safeToSpendTodayBeforeToman)
                                } ?: "—",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Column {
                            Text("بعد از خرید", style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = state.impact?.let {
                                    Money.format(it.safeToSpendTodayAfterToman)
                                } ?: "—",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
                state.impact?.let { impact ->
                    HesabiCard {
                        Text(
                            text = impact.verdict,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (impact.daysOfRunwayLost > 0) {
                            Text(
                                text = "این خرید حدود ${impact.daysOfRunwayLost} روز از توان خرج روزانه را مصرف می‌کند.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (impact.affectsGoals) {
                            Text(
                                text = "روی هدف پس‌انداز اثر می‌گذارد؛ تقریباً ${impact.goalDelayDays} روز تأخیر.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        impact.alternatives.forEach { alternative ->
                            Text(
                                text = "• $alternative",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                InfoBanner(
                    text = "این عددها با دادهٔ واقعی همین گوشی محاسبه شده‌اند؛ هیچ تخمین ساختگی وارد نمی‌شود.",
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.sm)) {
                HesabiButton(
                    text = "ثبت این خرید",
                    onClick = onAddExpense,
                    enabled = state.amountToman > 0,
                    modifier = Modifier.weight(1f),
                )
                HesabiButton(
                    text = "پاک کردن",
                    onClick = viewModel::clearAmount,
                    variant = HesabiButtonVariant.SECONDARY,
                    modifier = Modifier.weight(1f),
                )
            }
            state.error?.let { InfoBanner(text = it) }
        }
    }
}

@Preview(name = "شبیه‌ساز خرید", showBackground = true)
@Composable
private fun SimulatorPreview() {
    HesabiThemePreview()
}

@Composable
private fun HesabiThemePreview() {
    com.braveboy.hesabi.ui.theme.HesabiTheme {
        androidx.compose.runtime.CompositionLocalProvider(
            LocalLayoutDirection provides LayoutDirection.Rtl,
        ) {
            androidx.compose.material3.Surface(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.padding(HesabiSpacing.lg)) {
                    Text("پیش‌نمایش", style = MaterialTheme.typography.titleLarge)
                    HesabiCard {
                        Text("مبلغ ۸٬۵۰۰٬۰۰۰ تومان")
                    }
                }
            }
        }
    }
}
