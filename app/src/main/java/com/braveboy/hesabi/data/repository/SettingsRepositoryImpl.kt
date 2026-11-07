package com.braveboy.hesabi.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.braveboy.hesabi.core.money.Currency
import com.braveboy.hesabi.domain.model.AppSettings
import com.braveboy.hesabi.domain.model.ThemeMode
import com.braveboy.hesabi.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.hesabiSettings: DataStore<Preferences> by preferencesDataStore(name = "hesabi_settings")

class SettingsRepositoryImpl(
    private val context: Context,
) : SettingsRepository {

    private object Keys {
        val onboarded = booleanPreferencesKey("onboarded")
        val themeMode = stringPreferencesKey("theme_mode")
        val dynamicColor = booleanPreferencesKey("dynamic_color")
        val currency = stringPreferencesKey("currency")
        val hideAmounts = booleanPreferencesKey("hide_amounts")
        val smsCapture = booleanPreferencesKey("sms_capture")
        val amountsAreRial = booleanPreferencesKey("amounts_are_rial")
        val lastBackupAt = longPreferencesKey("last_backup_at")
        val defaultAccountId = stringPreferencesKey("default_account_id")
        val paydayDayOfMonth = intPreferencesKey("payday_day_of_month")
        val appLockEnabled = booleanPreferencesKey("app_lock_enabled")
        val pinCodeHash = stringPreferencesKey("pin_code_hash")
        val biometricEnabled = booleanPreferencesKey("biometric_enabled")
        val remindersEnabled = booleanPreferencesKey("reminders_enabled")
        val monthlySalary = longPreferencesKey("monthly_salary")
        val salaryAccountId = stringPreferencesKey("salary_account_id")
        val autoDepositSalary = booleanPreferencesKey("auto_deposit_salary")
    }

    override val settings: Flow<AppSettings> = context.hesabiSettings.data
        .catch { error -> if (error is IOException) emit(androidx.datastore.preferences.core.emptyPreferences()) else throw error }
        .map { prefs ->
            AppSettings(
                onboarded = prefs[Keys.onboarded] ?: false,
                themeMode = ThemeMode.entries.firstOrNull { it.name == prefs[Keys.themeMode] } ?: ThemeMode.SYSTEM,
                dynamicColor = prefs[Keys.dynamicColor] ?: false,
                currency = Currency.entries.firstOrNull { it.name == prefs[Keys.currency] } ?: Currency.TOMAN,
                hideAmounts = prefs[Keys.hideAmounts] ?: false,
                smsCaptureEnabled = prefs[Keys.smsCapture] ?: false,
                amountsAreRial = prefs[Keys.amountsAreRial] ?: true,
                lastBackupAtMillis = prefs[Keys.lastBackupAt],
                defaultAccountId = prefs[Keys.defaultAccountId],
                paydayDayOfMonth = (prefs[Keys.paydayDayOfMonth] ?: 1).coerceIn(1, 31),
                appLockEnabled = prefs[Keys.appLockEnabled] ?: false,
                pinCodeHash = prefs[Keys.pinCodeHash] ?: "",
                biometricEnabled = prefs[Keys.biometricEnabled] ?: false,
                remindersEnabled = prefs[Keys.remindersEnabled] ?: true,
                monthlySalaryToman = prefs[Keys.monthlySalary] ?: 0L,
                salaryAccountId = prefs[Keys.salaryAccountId],
                autoDepositSalary = prefs[Keys.autoDepositSalary] ?: false,
            )
        }

    override suspend fun current(): AppSettings = settings.first()

    override suspend fun setOnboarded(value: Boolean) = edit { it[Keys.onboarded] = value }

    override suspend fun setThemeMode(mode: ThemeMode) = edit { it[Keys.themeMode] = mode.name }

    override suspend fun setDynamicColor(enabled: Boolean) = edit { it[Keys.dynamicColor] = enabled }

    override suspend fun setCurrency(currency: Currency) = edit { it[Keys.currency] = currency.name }

    override suspend fun setHideAmounts(hidden: Boolean) = edit { it[Keys.hideAmounts] = hidden }

    override suspend fun setSmsCaptureEnabled(enabled: Boolean) = edit { it[Keys.smsCapture] = enabled }

    override suspend fun setAmountsAreRial(enabled: Boolean) = edit { it[Keys.amountsAreRial] = enabled }

    override suspend fun setLastBackupAt(millis: Long) = edit { it[Keys.lastBackupAt] = millis }

    override suspend fun setDefaultAccountId(id: String?) = edit {
        if (id == null) it.remove(Keys.defaultAccountId) else it[Keys.defaultAccountId] = id
    }

    override suspend fun setPaydayDayOfMonth(day: Int) = edit {
        it[Keys.paydayDayOfMonth] = day.coerceIn(1, 31)
    }

    override suspend fun setAppLockEnabled(enabled: Boolean) = edit {
        it[Keys.appLockEnabled] = enabled
    }

    override suspend fun setPinCodeHash(hash: String) = edit {
        it[Keys.pinCodeHash] = hash
    }

    override suspend fun setBiometricEnabled(enabled: Boolean) = edit {
        it[Keys.biometricEnabled] = enabled
    }

    override suspend fun setRemindersEnabled(enabled: Boolean) = edit {
        it[Keys.remindersEnabled] = enabled
    }

    override suspend fun setMonthlySalary(amountToman: Long) = edit {
        it[Keys.monthlySalary] = amountToman.coerceAtLeast(0L)
    }

    override suspend fun setSalaryAccountId(id: String?) = edit {
        if (id == null) it.remove(Keys.salaryAccountId) else it[Keys.salaryAccountId] = id
    }

    override suspend fun setAutoDepositSalary(enabled: Boolean) = edit {
        it[Keys.autoDepositSalary] = enabled
    }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.hesabiSettings.edit { block(it) }
    }
}
