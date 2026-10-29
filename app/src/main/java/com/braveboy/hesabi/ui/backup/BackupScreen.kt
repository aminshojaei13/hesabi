package com.braveboy.hesabi.ui.backup

import android.content.ContentResolver
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.braveboy.hesabi.core.date.JalaliDate
import com.braveboy.hesabi.di.AppContainer
import com.braveboy.hesabi.domain.model.BackupSummary
import com.braveboy.hesabi.ui.components.ConfirmDialog
import com.braveboy.hesabi.ui.components.HesabiButton
import com.braveboy.hesabi.ui.components.HesabiButtonVariant
import com.braveboy.hesabi.ui.components.HesabiCard
import com.braveboy.hesabi.ui.components.HesabiTextField
import com.braveboy.hesabi.ui.components.HesabiTopBar
import com.braveboy.hesabi.ui.components.InfoBanner
import com.braveboy.hesabi.ui.components.SectionHeader
import com.braveboy.hesabi.ui.hesabiViewModel
import com.braveboy.hesabi.ui.theme.HesabiSpacing
import com.braveboy.hesabi.ui.theme.HesabiTheme
import java.io.IOException
import java.time.Instant
import java.time.ZoneId

@Composable
fun BackupScreen(
    container: AppContainer,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: BackupViewModel = hesabiViewModel(container)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var password by remember { mutableStateOf("") }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        if (uri != null) {
            viewModel.exportEncrypted(password.toCharArray()) {
                openOutput(context.contentResolver, uri)
            }
        }
    }
    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        if (uri != null) {
            viewModel.exportCsv { openOutput(context.contentResolver, uri) }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) pendingImportUri = uri
    }

    BackupContent(
        state = state,
        password = password,
        onPasswordChange = { password = it },
        onExport = {
            exportLauncher.launch("hesabi-backup-${JalaliDate.today().toStorage()}.hesbin")
        },
        onExportCsv = {
            csvLauncher.launch("hesabi-${JalaliDate.today().toStorage()}.csv")
        },
        onPickImport = {
            importLauncher.launch(arrayOf("application/octet-stream", "application/json", "*/*"))
        },
        onDismissNotice = viewModel::clearMessages,
        onBack = onBack,
        modifier = modifier,
    )

    pendingImportUri?.let { uri ->
        ConfirmDialog(
            title = "بازیابی از پشتیبان",
            message = "همهٔ داده‌های فعلی با محتوای این فایل جایگزین می‌شوند. این کار برگشت‌پذیر نیست. مطمئنی؟",
            confirmLabel = "جایگزین کن",
            dangerous = true,
            onConfirm = {
                viewModel.importEncrypted(password.toCharArray()) {
                    context.contentResolver.openInputStream(uri)
                        ?: throw IOException("فایل قابل خواندن نیست.")
                }
                pendingImportUri = null
            },
            onDismiss = { pendingImportUri = null },
        )
    }
}

private fun openOutput(resolver: ContentResolver, uri: Uri) =
    resolver.openOutputStream(uri) ?: throw IOException("نوشتن فایل ممکن نشد.")

@Composable
fun BackupContent(
    state: BackupUiState,
    password: String,
    onPasswordChange: (String) -> Unit,
    onExport: () -> Unit,
    onExportCsv: () -> Unit,
    onPickImport: () -> Unit,
    onDismissNotice: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val passwordOk = password.length >= 6
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = HesabiSpacing.lg)
            .padding(bottom = HesabiSpacing.xxl),
        verticalArrangement = Arrangement.spacedBy(HesabiSpacing.lg),
    ) {
        HesabiTopBar(title = "پشتیبان و خروجی", subtitle = "داده‌ها مال تو هستند", onBack = onBack)
        if (state.error != null || state.message != null) {
            InfoBanner(
                text = state.error ?: state.message.orEmpty(),
                containerColor = if (state.error != null) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                },
                contentColor = if (state.error != null) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                },
                actionLabel = "باشه",
                onAction = onDismissNotice,
            )
        }
        HesabiCard {
            SectionHeader("پشتیبان رمزدار")
            Text(
                text = "یک فایل واحد با همهٔ حساب‌ها، دسته‌ها، تراکنش‌ها و بودجه‌ها. فایل با AES-256-GCM رمز می‌شود و بدون رمز قابل خواندن نیست.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            androidx.compose.foundation.layout.Spacer(Modifier.padding(top = HesabiSpacing.xs))
            HesabiTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = "رمز پشتیبان",
                placeholder = "حداقل ۶ کاراکتر",
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = PasswordVisualTransformation(),
                error = if (password.isNotEmpty() && !passwordOk) "رمز باید حداقل ۶ کاراکتر باشد." else null,
            )
            if (passwordOk) {
                InfoBanner(
                    text = "این رمز را جایی امن نگه دار؛ بازیابی بدون آن ممکن نیست.",
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.sm),
            ) {
                HesabiButton(
                    text = "ساخت پشتیبان",
                    onClick = onExport,
                    enabled = passwordOk && !state.busy,
                    loading = state.busy,
                    icon = Icons.Rounded.FileDownload,
                    modifier = Modifier.weight(1f),
                )
                HesabiButton(
                    text = "بازیابی",
                    onClick = onPickImport,
                    variant = HesabiButtonVariant.SECONDARY,
                    enabled = passwordOk && !state.busy,
                    icon = Icons.Rounded.FileUpload,
                    modifier = Modifier.weight(1f),
                )
            }
            val lastBackup = state.lastBackupAtMillis?.takeIf { it > 0 }?.let { millis ->
                Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
            }
            HorizontalDivider(
                modifier = Modifier.padding(vertical = HesabiSpacing.sm),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            Text(
                text = lastBackup?.let { "آخرین پشتیبان: " + JalaliDate.fromGregorian(it).formatLong() }
                    ?: "هنوز پشتیبانی ساخته نشده است.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        HesabiCard {
            SectionHeader("خروجی CSV")
            Text(
                text = "برای باز کردن در اکسل یا Google Sheets. فایل CSV رمز ندارد؛ آن را با احتیاط به اشتراک بگذار.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            androidx.compose.foundation.layout.Spacer(Modifier.padding(top = HesabiSpacing.xs))
            HesabiButton(
                text = "خروجی CSV",
                onClick = onExportCsv,
                variant = HesabiButtonVariant.TONAL,
                enabled = !state.busy,
                icon = Icons.Rounded.TableChart,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        InfoBanner(
            text = "بازیابی، همهٔ داده‌های فعلی را با فایل پشتیبان جایگزین می‌کند. قبل از بازیابی یک پشتیبان تازه بساز.",
        )
    }
}

@Preview(name = "پشتیبان", heightDp = 900, showBackground = true)
@Composable
private fun BackupPreview() {
    HesabiTheme {
        androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(modifier = Modifier.fillMaxSize()) {
                BackupContent(
                    state = BackupUiState(
                        lastBackupAtMillis = 1_760_000_000_000,
                        message = "پشتیبان رمزدار ساخته شد: ۱۲ تراکنش.",
                        lastSummary = BackupSummary(12, 2, 14, 1_760_000_000_000),
                    ),
                    password = "secret12",
                    onPasswordChange = {},
                    onExport = {},
                    onExportCsv = {},
                    onPickImport = {},
                    onDismissNotice = {},
                    onBack = {},
                )
            }
        }
    }
}

@Preview(
    name = "پشتیبان - تیره",
    heightDp = 900,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Composable
private fun BackupDarkPreview() {
    HesabiTheme(themeMode = com.braveboy.hesabi.domain.model.ThemeMode.DARK) {
        androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(modifier = Modifier.fillMaxSize()) {
                BackupContent(
                    state = BackupUiState(),
                    password = "",
                    onPasswordChange = {},
                    onExport = {},
                    onExportCsv = {},
                    onPickImport = {},
                    onDismissNotice = {},
                    onBack = {},
                )
            }
        }
    }
}
