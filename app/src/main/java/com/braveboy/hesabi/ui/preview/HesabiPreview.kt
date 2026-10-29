package com.braveboy.hesabi.ui.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.braveboy.hesabi.domain.model.ThemeMode
import com.braveboy.hesabi.ui.theme.HesabiSpacing
import com.braveboy.hesabi.ui.theme.HesabiTheme

/**
 * Shared preview harness: RTL, Persian font, light/dark, and a phone-width surface.
 * Every reusable component preview goes through this so screenshots stay comparable.
 */
@Composable
fun HesabiPreview(
    dark: Boolean = false,
    width: Dp = 380.dp,
    spacing: Dp = HesabiSpacing.lg,
    content: @Composable ColumnScope.() -> Unit,
) {
    HesabiTheme(themeMode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface {
                Column(
                    modifier = Modifier
                        .width(width)
                        .padding(HesabiSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(spacing),
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
fun PreviewRow(content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Row(modifier = Modifier.fillMaxWidth()) { content() }
}
