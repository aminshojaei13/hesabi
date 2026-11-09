package com.braveboy.hesabi.ui.importer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.braveboy.hesabi.data.importer.CsvTableParser
import com.braveboy.hesabi.data.importer.ImportMapping
import com.braveboy.hesabi.data.importer.ImportSummary
import com.braveboy.hesabi.data.importer.TransactionImporter
import com.braveboy.hesabi.data.importer.XlsxTableReader
import com.braveboy.hesabi.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream

data class ImportUiState(
    val fileName: String? = null,
    val headers: List<String> = emptyList(),
    val previewRows: List<List<String>> = emptyList(),
    val mapping: ImportMapping = ImportMapping(),
    val rowCount: Int = 0,
    val createCategories: Boolean = true,
    val createAccounts: Boolean = false,
    val busy: Boolean = false,
    val summary: ImportSummary? = null,
    val error: String? = null,
) {
    val hasFile: Boolean get() = headers.isNotEmpty()
}

/**
 * CSV/XLSX migration wizard state. The full parsed table stays in memory only for the
 * duration of the wizard; nothing is written until the user presses "ورود داده".
 */
class ImportViewModel(
    private val importer: TransactionImporter,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ImportUiState())
    val state: StateFlow<ImportUiState> = _state.asStateFlow()

    private var table: CsvTableParser.Table? = null

    fun loadFile(fileName: String, bytes: ByteArray) {
        _state.update { it.copy(busy = true, error = null, summary = null, fileName = fileName) }
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.Default) {
                    val parsed = if (looksLikeXlsx(bytes)) {
                        XlsxTableReader.read(ByteArrayInputStream(bytes))
                    } else {
                        CsvTableParser.parse(bytes.toString(Charsets.UTF_8))
                    }
                    require(parsed.headers.isNotEmpty()) { "فایل خالی است یا ستون‌بندی ندارد." }
                    parsed
                }
            }.onSuccess { parsed ->
                table = parsed
                val preview = importer.preview(parsed)
                _state.update {
                    it.copy(
                        headers = parsed.headers,
                        previewRows = preview.sampleRows,
                        mapping = preview.mapping,
                        rowCount = parsed.rows.size,
                        busy = false,
                    )
                }
            }.onFailure { error ->
                _state.update { it.copy(busy = false, error = error.message ?: "فایل خوانده نشد.") }
            }
        }
    }

    fun updateMapping(mapping: ImportMapping) {
        _state.update { it.copy(mapping = mapping, error = null) }
    }

    fun setCreateCategories(value: Boolean) {
        _state.update { it.copy(createCategories = value) }
    }

    fun setCreateAccounts(value: Boolean) {
        _state.update { it.copy(createAccounts = value) }
    }

    fun runImport() {
        val current = _state.value
        val parsed = table ?: return
        if (current.busy) return
        _state.update { it.copy(busy = true, error = null, summary = null) }
        viewModelScope.launch {
            runCatching {
                val settings = settingsRepository.current()
                importer.import(
                    table = parsed,
                    mapping = current.mapping,
                    createMissingCategories = current.createCategories,
                    createMissingAccounts = current.createAccounts,
                    defaultAccountId = settings.defaultAccountId,
                )
            }.onSuccess { summary ->
                _state.update { it.copy(busy = false, summary = summary) }
            }.onFailure { error ->
                _state.update { it.copy(busy = false, error = error.message ?: "ورود داده انجام نشد.") }
            }
        }
    }

    fun dismissError() {
        _state.update { it.copy(error = null) }
    }

    private fun looksLikeXlsx(bytes: ByteArray): Boolean =
        bytes.size > 4 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte()
}
