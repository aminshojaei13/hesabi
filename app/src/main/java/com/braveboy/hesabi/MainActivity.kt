package com.braveboy.hesabi

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.braveboy.hesabi.di.AppContainer
import com.braveboy.hesabi.domain.model.AppSettings
import com.braveboy.hesabi.ui.navigation.HesabiRoot
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val container: AppContainer by lazy { (application as HesabiApp).container }

    private var initialSettings by mutableStateOf<AppSettings?>(null)
    private var sharedText by mutableStateOf<String?>(null)
    private var openSmsRequested by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        sharedText = intent.readSharedText()
        openSmsRequested = intent.getBooleanExtra(
            com.braveboy.hesabi.data.sms.SmsNotifier.EXTRA_OPEN_SMS,
            false,
        )

        val ready = mutableStateOf(false)
        splash.setKeepOnScreenCondition { !ready.value }

        lifecycleScope.launch {
            container.ensureSeeded()
            initialSettings = container.settingsRepository.current()
            ready.value = true
        }

        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                val loaded = initialSettings
                if (loaded == null) {
                    MaterialTheme {
                        Surface(modifier = Modifier.fillMaxSize(), content = {})
                    }
                } else {
                    HesabiRoot(
                        container = container,
                        initialSettings = loaded,
                        sharedText = sharedText,
                        onSharedTextConsumed = { sharedText = null },
                        openSmsRequested = openSmsRequested,
                        onOpenSmsConsumed = { openSmsRequested = false },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        sharedText = intent.readSharedText()
        openSmsRequested = intent.getBooleanExtra(
            com.braveboy.hesabi.data.sms.SmsNotifier.EXTRA_OPEN_SMS,
            false,
        )
    }
}

/** Supports both the share sheet and text selection menu ("Share with Hesabi"). */
private fun Intent.readSharedText(): String? = when (action) {
    Intent.ACTION_SEND -> getStringExtra(Intent.EXTRA_TEXT)
    Intent.ACTION_PROCESS_TEXT -> getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
    else -> null
}?.trim()?.takeIf { it.isNotEmpty() }?.take(4000)
