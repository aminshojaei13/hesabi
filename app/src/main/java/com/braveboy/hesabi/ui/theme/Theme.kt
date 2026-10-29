package com.braveboy.hesabi.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.braveboy.hesabi.core.money.Currency
import com.braveboy.hesabi.domain.model.ThemeMode

@Immutable
data class HesabiDisplayPreferences(
    val currency: Currency = Currency.TOMAN,
    val hideAmounts: Boolean = false,
)

val LocalHesabiSemantic = staticCompositionLocalOf { LightSemantic }
val LocalHesabiDisplay = staticCompositionLocalOf { HesabiDisplayPreferences() }

@Composable
fun HesabiTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    currency: Currency = Currency.TOMAN,
    hideAmounts: Boolean = false,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    val colorScheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (dark) DarkScheme else LightScheme
    }
    val semantic = if (dark) DarkSemantic else LightSemantic

    // The product is Persian-first: the layout is always RTL, even on a device
    // whose system locale is English, so mirrored rows and chevrons stay correct.
    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl,
        LocalHesabiSemantic provides semantic,
        LocalHesabiDisplay provides HesabiDisplayPreferences(currency = currency, hideAmounts = hideAmounts),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = HesabiTypography,
            shapes = HesabiShapes,
            content = content,
        )
    }
}
