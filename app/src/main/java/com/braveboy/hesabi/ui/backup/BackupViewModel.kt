package com.braveboy.hesabi.ui.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.braveboy.hesabi.domain.model.BackupSummary
import com.braveboy.hesabi.domain.repository.BackupImportMode
import com.braveboy.hesabi.domain.repository.BackupRepository
import com.braveboy.hesabi.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

data class BackupUiState(
    val lastBackupAtMillis: Long? = null,
    val busy: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    val lastSummary: BackupSummary? = null,
)

class BackupViewModel(
    private val backupRepository: BackupRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val status = MutableStateFlow(BackupUiState())

    val state: StateFlow<BackupUiState> = combine(
        settingsRepository.settings.map { it.lastBackupAtMillis },
        status,
    ) { lastBackup, current ->
        current.copy(lastBackupAtMillis = lastBackup)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BackupUiState())

    fun exportEncrypted(password: CharArray, openOutput: () -> OutputStream) {
        viewModelScope.launch {
            status.value = status.value.copy(busy = true, message = null, error = null)
            try {
                val summary = withContext(Dispatchers.IO) {
                    openOutput().use { out -> backupRepository.exportEncrypted(out, password) }
                }
                val receiptsNote = if (summary.receiptCount > 0) {
                    " ${summary.receiptCount} تصویر رسید هم داخل فایل است."
                } else {
                    ""
                }
                val skippedNote = if (summary.skippedReceipts > 0) {
                    " ${summary.skippedReceipts} تصویر بزرگ جا نشد؛ برای پشتیبان کوچک‌تر، رسیدهای قدیمی را حذف کن."
                } else {
                    ""
                }
                status.value = status.value.copy(
                    busy = false,
                    message = "پشتیبان رمزدار ساخته شد: ${summary.transactionCount} تراکنش.$receiptsNote$skippedNote",
                    lastSummary = summary,
                )
            } catch (error: Exception) {
                status.value = status.value.copy(busy = false, error = error.message ?: "ساخت پشتیبان ناموفق بود.")
            } finally {
                password.fill('\u0000')
            }
        }
    }

    fun exportCsv(openOutput: () -> OutputStream) {
        viewModelScope.launch {
            status.value = status.value.copy(busy = true, message = null, error = null)
            try {
                val rows = withContext(Dispatchers.IO) {
                    openOutput().use { out -> backupRepository.exportCsv(out) }
                }
                status.value = status.value.copy(busy = false, message = "فایل CSV با ${rows} ردیف ساخته شد.")
            } catch (error: Exception) {
                status.value = status.value.copy(busy = false, error = error.message ?: "ساخت CSV ناموفق بود.")
            }
        }
    }

    fun importEncrypted(
        password: CharArray,
        mode: BackupImportMode = BackupImportMode.REPLACE,
        openInput: () -> InputStream,
    ) {
        viewModelScope.launch {
            status.value = status.value.copy(busy = true, message = null, error = null)
            try {
                val result = withContext(Dispatchers.IO) {
                    openInput().use { input -> backupRepository.importEncrypted(input, password, mode) }
                }
                val modeLabel = if (mode == BackupImportMode.REPLACE) "جایگزینی" else "ادغام"
                status.value = status.value.copy(
                    busy = false,
                    message = "بازیابی ($modeLabel) کامل شد: ${result.summary.transactionCount} تراکنش، " +
                        "${result.summary.accountCount} حساب، ${result.summary.receiptCount} رسید.",
                    lastSummary = result.summary,
                )
            } catch (error: Exception) {
                status.value = status.value.copy(busy = false, error = error.message ?: "بازیابی ناموفق بود.")
            } finally {
                password.fill('\u0000')
            }
        }
    }

    fun clearMessages() {
        status.value = status.value.copy(message = null, error = null)
    }
}
