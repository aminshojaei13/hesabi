package com.braveboy.hesabi.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.braveboy.hesabi.core.money.Currency
import com.braveboy.hesabi.domain.model.AppSettings
import com.braveboy.hesabi.domain.model.Account
import com.braveboy.hesabi.domain.model.ThemeMode
import com.braveboy.hesabi.domain.repository.AccountRepository
import com.braveboy.hesabi.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val accounts: List<Account> = emptyList(),
    val message: String? = null,
    val busy: Boolean = false,
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    accountRepository: AccountRepository,
    private val clearAllData: suspend () -> Unit,
) : ViewModel() {

    private val message = MutableStateFlow<String?>(null)
    private val busy = MutableStateFlow(false)

    val state: StateFlow<SettingsUiState> = combine(
        settingsRepository.settings,
        accountRepository.observeAll(),
        message,
        busy,
    ) { settings, accounts, currentMessage, working ->
        SettingsUiState(settings = settings, accounts = accounts, message = currentMessage, busy = working)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setThemeMode(mode: ThemeMode) = launchSetting { settingsRepository.setThemeMode(mode) }

    fun setDynamicColor(enabled: Boolean) = launchSetting { settingsRepository.setDynamicColor(enabled) }

    fun setCurrency(currency: Currency) = launchSetting { settingsRepository.setCurrency(currency) }

    fun setHideAmounts(hidden: Boolean) = launchSetting { settingsRepository.setHideAmounts(hidden) }

    fun setSmsCapture(enabled: Boolean) = launchSetting { settingsRepository.setSmsCaptureEnabled(enabled) }

    fun setAmountsAreRial(enabled: Boolean) = launchSetting { settingsRepository.setAmountsAreRial(enabled) }

    fun setDefaultAccount(id: String?) = launchSetting { settingsRepository.setDefaultAccountId(id) }

    fun setPaydayDayOfMonth(day: Int) = launchSetting { settingsRepository.setPaydayDayOfMonth(day) }

    fun setAppLockEnabled(enabled: Boolean) = launchSetting { settingsRepository.setAppLockEnabled(enabled) }

    fun setPinCodeHash(hash: String) = launchSetting {
        settingsRepository.setPinCodeHash(hash)
        settingsRepository.setAppLockEnabled(hash.isNotBlank())
    }

    fun setBiometricEnabled(enabled: Boolean) = launchSetting { settingsRepository.setBiometricEnabled(enabled) }

    fun setRemindersEnabled(enabled: Boolean) = launchSetting { settingsRepository.setRemindersEnabled(enabled) }

    fun setMonthlySalary(amountToman: Long) = launchSetting { settingsRepository.setMonthlySalary(amountToman) }

    fun setSalaryAccountId(id: String?) = launchSetting { settingsRepository.setSalaryAccountId(id) }

    fun setAutoDepositSalary(enabled: Boolean) = launchSetting { settingsRepository.setAutoDepositSalary(enabled) }

    fun completeOnboarding() {
        viewModelScope.launch { settingsRepository.setOnboarded(true) }
    }

    fun clearAllData() {
        viewModelScope.launch {
            busy.value = true
            clearAllData.invoke()
            message.value = "همهٔ داده‌ها پاک شد و حساب‌های پیش‌فرض بازسازی شدند."
            busy.value = false
        }
    }

    fun dismissMessage() {
        message.value = null
    }

    fun showMessage(text: String) {
        message.value = text
    }

    private fun launchSetting(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
