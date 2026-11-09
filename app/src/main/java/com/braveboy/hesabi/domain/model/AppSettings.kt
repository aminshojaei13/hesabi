package com.braveboy.hesabi.domain.model

import com.braveboy.hesabi.core.money.Currency

enum class ThemeMode(val label: String) {
    SYSTEM("سیستم"),
    LIGHT("روشن"),
    DARK("تیره"),
}

data class AppSettings(
    val onboarded: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val currency: Currency = Currency.TOMAN,
    val hideAmounts: Boolean = false,
    val smsCaptureEnabled: Boolean = false,
    val amountsAreRial: Boolean = true,
    val lastBackupAtMillis: Long? = null,
    val defaultAccountId: String? = null,
    val paydayDayOfMonth: Int = 1,
    val appLockEnabled: Boolean = false,
    val pinCodeHash: String = "",
    val pinFailedAttempts: Int = 0,
    val pinLockedUntilMillis: Long = 0L,
    val biometricEnabled: Boolean = false,
    val remindersEnabled: Boolean = true,
    val monthlySalaryToman: Long = 0L,
    val salaryAccountId: String? = null,
    val autoDepositSalary: Boolean = false,
    /** Local is the default; the cloud option is a declaration of intent for now. */
    val aiMode: AiMode = AiMode.LOCAL,
    val lastAssistantUseAtMillis: Long? = null,
    val smsRationaleAccepted: Boolean = false,
    val assetPricesAutoUpdate: Boolean = false,
)

data class AccountWithBalance(
    val account: Account,
    val balanceToman: Long,
)

data class BackupSummary(
    val transactionCount: Int,
    val accountCount: Int,
    val categoryCount: Int,
    val exportedAtMillis: Long,
    val receiptCount: Int = 0,
    val skippedReceipts: Int = 0,
)

data class ImportResult(
    val summary: BackupSummary,
    val schemaVersion: Int,
)
