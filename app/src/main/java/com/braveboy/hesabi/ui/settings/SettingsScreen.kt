package com.braveboy.hesabi.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.braveboy.hesabi.core.money.Currency
import com.braveboy.hesabi.core.money.Digits
import com.braveboy.hesabi.core.security.SecurityManager
import com.braveboy.hesabi.di.AppContainer
import com.braveboy.hesabi.domain.model.AiMode
import com.braveboy.hesabi.domain.model.AppSettings
import com.braveboy.hesabi.domain.model.ThemeMode
import com.braveboy.hesabi.ui.components.ConfirmDialog
import com.braveboy.hesabi.ui.components.HesabiButton
import com.braveboy.hesabi.ui.components.HesabiButtonVariant
import com.braveboy.hesabi.ui.components.HesabiCard
import com.braveboy.hesabi.ui.components.HesabiTopBar
import com.braveboy.hesabi.ui.components.InfoBanner
import com.braveboy.hesabi.ui.components.SectionHeader
import com.braveboy.hesabi.ui.components.FilterChipRow
import com.braveboy.hesabi.ui.components.SettingsRow
import com.braveboy.hesabi.ui.hesabiViewModel
import com.braveboy.hesabi.ui.security.SecureScreen
import com.braveboy.hesabi.ui.preview.PreviewData
import com.braveboy.hesabi.ui.security.CurrentPinDialog
import com.braveboy.hesabi.ui.security.PinSetupDialog
import com.braveboy.hesabi.ui.theme.HesabiSpacing
import com.braveboy.hesabi.ui.theme.HesabiTheme

@Composable
fun SettingsScreen(
    container: AppContainer,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: SettingsViewModel = hesabiViewModel(container)
    val state by viewModel.state.collectAsStateWithLifecycle()
    SecureScreen()
    val context = LocalContext.current
    var showClearConfirm by remember { mutableStateOf(false) }
    var showAccountPicker by remember { mutableStateOf(false) }
    var showPaydayPicker by remember { mutableStateOf(false) }
    var showSalarySetup by remember { mutableStateOf(false) }
    var showPinSetup by remember { mutableStateOf(false) }
    var showCurrentPinForChange by remember { mutableStateOf(false) }
    var showCurrentPinForDisable by remember { mutableStateOf(false) }
    var showSmsRationale by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            viewModel.setSmsCapture(true)
        } else {
            viewModel.showMessage("اجازهٔ پیامک داده نشد. ثبت خودکار خاموش می‌ماند.")
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            viewModel.setRemindersEnabled(true)
        } else {
            viewModel.showMessage(
                "اجازهٔ اعلان داده نشد. برای فعال‌کردن یادآور، اعلان برنامه را از تنظیمات گوشی روشن کن.",
            )
        }
    }

    SettingsContent(
        state = state,
        onBack = onBack,
        onThemeMode = viewModel::setThemeMode,
        onDynamicColor = viewModel::setDynamicColor,
        onCurrency = viewModel::setCurrency,
        onHideAmounts = viewModel::setHideAmounts,
        onSmsCapture = { enabled ->
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) ==
                PackageManager.PERMISSION_GRANTED
            if (enabled && !granted) {
                // Explain the exact permission before the system dialog (market review
                // and Play/Market policy both expect an in-app rationale).
                showSmsRationale = true
            } else {
                viewModel.setSmsCapture(enabled)
            }
        },
        onAmountsAreRial = viewModel::setAmountsAreRial,
        onPickAccount = { showAccountPicker = true },
        onPickPayday = { showPaydayPicker = true },
        onSetupSalary = { showSalarySetup = true },
        onSetupPin = {
            if (state.settings.appLockEnabled) {
                showCurrentPinForChange = true
            } else {
                showPinSetup = true
            }
        },
        onDisableAppLock = {
            if (state.settings.appLockEnabled) {
                showCurrentPinForDisable = true
            } else {
                viewModel.setAppLockEnabled(false)
            }
        },
        onBiometricToggle = viewModel::setBiometricEnabled,
        onRemindersToggle = { enabled ->
            if (!enabled) {
                viewModel.setRemindersEnabled(false)
            } else {
                val notificationPermissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
                when {
                    !notificationPermissionGranted ->
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    !NotificationManagerCompat.from(context).areNotificationsEnabled() ->
                        viewModel.showMessage(
                            "اعلان‌های برنامه در تنظیمات گوشی خاموش است. برای فعال‌کردن یادآور، آن را روشن کن.",
                        )
                    else -> viewModel.setRemindersEnabled(true)
                }
            }
        },
        onClearData = { showClearConfirm = true },
        onDismissMessage = viewModel::dismissMessage,
        onAiMode = viewModel::setAiMode,
        onAssetPricesAutoUpdate = viewModel::setAssetPricesAutoUpdate,
        onOpenPrivacy = { showPrivacy = true },
        modifier = modifier,
    )

    if (showPrivacy) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showPrivacy = false },
            title = { Text("سیاست حریم خصوصی حسابی") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(HesabiSpacing.sm),
                ) {
                    androidx.compose.material3.Text(
                        style = MaterialTheme.typography.bodySmall,
                        text = "۱. حسابی هیچ حساب کاربری، تبلیغ و ابزار ردیابی ندارد و داده‌های مالی را به سرور نمی‌فرستد. تنها ارتباط اختیاری، دریافت قیمت عمومی طلا و ارز از TGJU است که هیچ شناسه یا مبلغی همراه ندارد.\n\n" +
                            "۲. تراکنش‌ها، حساب‌ها، بودجه، تعهدات، پیامک‌های تأییدشده و تصاویر رسید فقط در فضای اختصاصی برنامه روی همین گوشی ذخیره می‌شوند. کل پایگاه داده با SQLCipher رمز می‌شود، متن یادداشت‌ها و نام اشخاص با کلید امن سیستم‌عامل رمز می‌شوند و تصاویر رسید هم رمزنگاری‌شده‌اند.\n\n" +
                            "۳. پشتیبان خودکار سیستم‌عامل غیرفعال است. پشتیبان دستی فقط با رمز خودت ساخته می‌شود و کلید آن پیش ما نیست.\n\n" +
                            "۴. مجوز پیامک فقط برای خواندن پیامک‌های تازهٔ بانکی جهت پیشنهاد تراکنش است؛ دفتر پیامک خوانده نمی‌شود و شمارهٔ کارت پیش از ذخیره پوشانده می‌شود. مجوز اعلان برای یادآور سررسید، مجوز میکروفن برای ثبت صوتی، اثر انگشت برای قفل برنامه و اینترنت فقط برای قیمت عمومی طلا و ارز است. اسکن رسید با مدل داخلی گوشی انجام می‌شود و تصویر جایی فرستاده نمی‌شود.\n\n" +
                            "۵. دستیار مالی به‌صورت پیش‌فرض محلی کار می‌کند و متن جمله از دستگاه خارج نمی‌شود.\n\n" +
                            "۶. هر زمان می‌توانی از «پشتیبان و خروجی» داده‌ات را ببری یا از همین صفحه همه‌چیز را برای همیشه پاک کنی.\n\n" +
                            "۷. گزارش خطای محلی فقط نوع خطا را نگه می‌دارد و مبلغ یا متن یادداشت در آن نیست.\n\n" +
                            "۸. این سیاست با انتشار هر نسخه بازبینی می‌شود؛ نسخهٔ حقوقی نهایی پیش از انتشار در کافه‌بازار پیوست می‌شود.",
                    )
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showPrivacy = false }) { Text("باشه") }
            },
        )
    }

    if (showSmsRationale) {
        ConfirmDialog(
            title = "چرا اجازهٔ پیامک؟",
            message = "حسابی فقط پیامک‌های تازهٔ بانکی را می‌خواند تا مبلغ را پیشنهاد کند؛ " +
                "دفتر پیامک را نمی‌خواند و متن پیامک را جایی نمی‌فرستد. " +
                "شمارهٔ کارت و حساب پیش از ذخیره پوشانده می‌شود. " +
                "اگر اجازه ندهی، می‌توانی متن پیامک را خودت از صفحهٔ پیامک‌ها بچسبانی.",
            confirmLabel = "اجازه می‌دهم",
            onConfirm = {
                showSmsRationale = false
                permissionLauncher.launch(Manifest.permission.RECEIVE_SMS)
            },
            onDismiss = { showSmsRationale = false },
        )
    }

    if (showSalarySetup) {
        SalarySetupDialog(
            initialSalaryToman = state.settings.monthlySalaryToman,
            initialPayday = state.settings.paydayDayOfMonth,
            initialAccountId = state.settings.salaryAccountId ?: state.settings.defaultAccountId,
            initialAutoDeposit = state.settings.autoDepositSalary,
            accounts = state.accounts,
            onDismiss = { showSalarySetup = false },
            onSave = { salary, payday, accountId, autoDeposit ->
                viewModel.setMonthlySalary(salary)
                viewModel.setPaydayDayOfMonth(payday)
                viewModel.setSalaryAccountId(accountId)
                viewModel.setAutoDepositSalary(autoDeposit)
                showSalarySetup = false
            },
        )
    }

    if (showAccountPicker) {
        AlertDialog(
            onDismissRequest = { showAccountPicker = false },
            title = { Text("حساب پیش‌فرض") },
            text = {
                Column {
                    SettingsRow(
                        title = "هیچ‌کدام",
                        trailing = {
                            RadioButton(
                                selected = state.settings.defaultAccountId == null,
                                onClick = {
                                    viewModel.setDefaultAccount(null)
                                    showAccountPicker = false
                                },
                            )
                        },
                        onClick = {
                            viewModel.setDefaultAccount(null)
                            showAccountPicker = false
                        },
                    )
                    state.accounts.forEach { account ->
                        SettingsRow(
                            title = account.name,
                            subtitle = account.kind.label,
                            trailing = {
                                RadioButton(
                                    selected = state.settings.defaultAccountId == account.id,
                                    onClick = {
                                        viewModel.setDefaultAccount(account.id)
                                        showAccountPicker = false
                                    },
                                )
                            },
                            onClick = {
                                viewModel.setDefaultAccount(account.id)
                                showAccountPicker = false
                            },
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAccountPicker = false }) { Text("بستن") }
            },
        )
    }

    if (showPaydayPicker) {
        val commonPaydays = listOf(1, 20, 25, 26, 28, 30)
        AlertDialog(
            onDismissRequest = { showPaydayPicker = false },
            title = { Text("روز دریافت حقوق") },
            text = {
                Column {
                    Text(
                        text = "محاسبه خرج امن امروز و دوره مالی بر اساس این روز انجام می‌شود.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = HesabiSpacing.sm),
                    )
                    commonPaydays.forEach { day ->
                        val isSelected = state.settings.paydayDayOfMonth == day
                        SettingsRow(
                            title = if (day == 1) "اول هر ماه (تقویم رسمی)" else "${Digits.toPersian(day.toString())}ام هر ماه",
                            trailing = {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        viewModel.setPaydayDayOfMonth(day)
                                        showPaydayPicker = false
                                    },
                                )
                            },
                            onClick = {
                                viewModel.setPaydayDayOfMonth(day)
                                showPaydayPicker = false
                            },
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPaydayPicker = false }) { Text("بستن") }
            },
        )
    }

    if (showClearConfirm) {
        ConfirmDialog(
            title = "پاک کردن همهٔ داده‌ها",
            message = "همهٔ تراکنش‌ها، حساب‌ها، دسته‌ها، بودجه‌ها و پیامک‌ها حذف می‌شوند و برنامه از ابتدا شروع می‌کند. " +
                "اگر پشتیبان نداری، این کار برگشت‌پذیر نیست.",
            confirmLabel = "همه را پاک کن",
            dangerous = true,
            onConfirm = {
                showClearConfirm = false
                viewModel.clearAllData()
            },
            onDismiss = { showClearConfirm = false },
        )
    }

    if (showPinSetup) {
        PinSetupDialog(
            onDismiss = { showPinSetup = false },
            onPinConfirmed = { hash ->
                viewModel.setPinCodeHash(hash)
                showPinSetup = false
            },
        )
    }

    if (showCurrentPinForChange) {
        CurrentPinDialog(
            pinCodeHash = state.settings.pinCodeHash,
            title = "تأیید رمز فعلی برای تغییر رمز",
            onConfirmed = { _, _ ->
                showCurrentPinForChange = false
                showPinSetup = true
            },
            onDismiss = { showCurrentPinForChange = false },
        )
    }

    if (showCurrentPinForDisable) {
        CurrentPinDialog(
            pinCodeHash = state.settings.pinCodeHash,
            title = "تأیید رمز فعلی برای خاموش‌کردن قفل",
            onConfirmed = { pin, legacyHash ->
                viewModel.disableAppLockAfterVerification(pin, legacyHash)
                showCurrentPinForDisable = false
            },
            onDismiss = { showCurrentPinForDisable = false },
        )
    }
}

@Composable
fun SettingsContent(
    state: SettingsUiState,
    onBack: () -> Unit,
    onThemeMode: (ThemeMode) -> Unit,
    onDynamicColor: (Boolean) -> Unit,
    onCurrency: (Currency) -> Unit,
    onHideAmounts: (Boolean) -> Unit,
    onSmsCapture: (Boolean) -> Unit,
    onAmountsAreRial: (Boolean) -> Unit,
    onPickAccount: () -> Unit,
    onPickPayday: () -> Unit,
    onSetupSalary: () -> Unit,
    onSetupPin: () -> Unit,
    onDisableAppLock: () -> Unit,
    onBiometricToggle: (Boolean) -> Unit,
    onRemindersToggle: (Boolean) -> Unit,
    onClearData: () -> Unit,
    onDismissMessage: () -> Unit,
    onAiMode: (AiMode) -> Unit = {},
    onAssetPricesAutoUpdate: (Boolean) -> Unit = {},
    onOpenPrivacy: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val settings = state.settings
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = HesabiSpacing.lg)
            .padding(bottom = HesabiSpacing.xxl),
        verticalArrangement = Arrangement.spacedBy(HesabiSpacing.lg),
    ) {
        HesabiTopBar(title = "تنظیمات", subtitle = "ظاهر و رفتار برنامه", onBack = onBack)
        if (state.message != null) {
            InfoBanner(
                text = state.message,
                actionLabel = "باشه",
                onAction = onDismissMessage,
            )
        }
        HesabiCard {
            SectionHeader("ظاهر")
            Text("حالت رنگ", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.sm)) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = settings.themeMode == mode,
                        onClick = { onThemeMode(mode) },
                        label = { Text(mode.label) },
                    )
                }
            }
            SwitchRow(
                title = "رنگ‌های سیستم",
                subtitle = "پالت اندروید ۱۲ به بالا (Dynamic Color)",
                checked = settings.dynamicColor,
                onCheckedChange = onDynamicColor,
            )
            SwitchRow(
                title = "پنهان‌کردن مبالغ",
                subtitle = "برای وقتی در جمع هستی",
                checked = settings.hideAmounts,
                onCheckedChange = onHideAmounts,
            )
            InfoBanner(
                text = "رنگ سبز/قرمز درآمد و هزینه همیشه ثابت می‌ماند تا با پالت سیستم گم نشود.",
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        HesabiCard {
            SectionHeader("پول")
            Text("واحد نمایش", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.sm)) {
                Currency.entries.forEach { currency ->
                    FilterChip(
                        selected = settings.currency == currency,
                        onClick = { onCurrency(currency) },
                        label = { Text(currency.labelFa) },
                    )
                }
            }
            Text(
                text = "مبالغ همیشه به تومان ذخیره می‌شوند؛ ریال فقط برای نمایش و خواندن پیامک بانکی است.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SwitchRow(
                title = "پیامک‌های بانکی به ریال هستند",
                subtitle = "معمولاً بانک‌ها ریال می‌نویسند",
                checked = settings.amountsAreRial,
                onCheckedChange = onAmountsAreRial,
            )
        }
        HesabiCard {
            SectionHeader("حقوق ماهانه و کنترل نقدینگی")
            SettingsRow(
                title = "حقوق ماهانه و چرخه نقدینگی",
                subtitle = if (settings.monthlySalaryToman > 0) {
                    "${com.braveboy.hesabi.core.money.Money.format(settings.monthlySalaryToman, withUnit = true)} • ${Digits.toPersian(settings.paydayDayOfMonth.toString())}ام ماه"
                } else {
                    "ثبت نشده (تعیین حقوق پایه جهت کنترل نقدینگی)"
                },
                icon = Icons.Rounded.Payments,
                onClick = onSetupSalary,
            )
            SettingsRow(
                title = "حساب پیش‌فرض",
                subtitle = state.accounts.firstOrNull { it.id == settings.defaultAccountId }?.name ?: "انتخاب نشده",
                icon = Icons.Rounded.AccountBalanceWallet,
                onClick = onPickAccount,
            )
        }
        HesabiCard {
            SectionHeader("پیامک بانکی")
            SwitchRow(
                title = "دریافت خودکار پیامک",
                subtitle = "اختیاری؛ فقط با اجازهٔ تو فعال می‌شود",
                checked = settings.smsCaptureEnabled,
                onCheckedChange = onSmsCapture,
            )
            InfoBanner(
                text = "حسابی دفتر پیامک را نمی‌خواند. فقط پیامک‌های تازهٔ بانکی را بعد از اجازهٔ سیستمی می‌بیند و همه‌چیز محلی می‌ماند.",
                icon = Icons.Rounded.Sms,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        HesabiCard {
            SectionHeader("امنیت و قفل برنامه")
            SwitchRow(
                title = "قفل برنامه با رمز عبور",
                subtitle = if (settings.appLockEnabled) "فعال با رمز ۴ رقمی" else "غیرفعال",
                checked = settings.appLockEnabled,
                onCheckedChange = { enabled ->
                    if (enabled) onSetupPin() else onDisableAppLock()
                },
            )
            if (settings.appLockEnabled) {
                SettingsRow(
                    title = "تغییر رمز عبور",
                    subtitle = "تعیین مجدد پین‌کد ۴ رقمی",
                    icon = Icons.Rounded.Lock,
                    onClick = onSetupPin,
                )
                SwitchRow(
                    title = "ورود با اثر انگشت / چهره",
                    subtitle = "احراز هویت بیومتریک دستگاه",
                    checked = settings.biometricEnabled,
                    onCheckedChange = onBiometricToggle,
                )
            }
        }
        HesabiCard {
            SectionHeader("دستیار هوشمند")
            Text(
                text = "پیش‌فرض «فقط روی گوشی» است: هیچ متن یا تراکنشی برای پردازش ارسال نمی‌شود.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FilterChipRow(
                options = AiMode.entries,
                selected = settings.aiMode,
                onSelect = onAiMode,
                label = { it.labelFa },
            )
            if (settings.aiMode == AiMode.CLOUD) {
                InfoBanner(
                    text = "حالت ابری در این نسخه فقط یک انتخاب است و ارسال واقعی انجام نمی‌شود؛ " +
                        "پیش از فعال‌سازی، دقیقاً مشخص می‌شود چه داده‌ای می‌رود.",
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
        HesabiCard {
            SectionHeader("دارایی و قیمت لحظه‌ای")
            SwitchRow(
                title = "به‌روزرسانی خودکار قیمت طلا و ارز",
                subtitle = "از منبع عمومی TGJU، بدون حساب کاربری؛ فقط قیمت خوانده می‌شود",
                checked = settings.assetPricesAutoUpdate,
                onCheckedChange = onAssetPricesAutoUpdate,
            )
            Text(
                text = "قیمت‌ها همیشه با نام منبع و ساعت به‌روزرسانی نمایش داده می‌شوند و " +
                    "در نبود اینترنت، آخرین قیمت ذخیره‌شده به‌عنوان «کش» استفاده می‌شود. " +
                    "ارزش ملک، خودرو و سکه را خودت وارد می‌کنی.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        HesabiCard {
            SectionHeader("یادآورها و هشدارها")
            SwitchRow(
                title = "یادآور سررسید چک‌ها و اقساط",
                subtitle = "اعلان محلی ۲ روز قبل از موعد سررسید",
                checked = settings.remindersEnabled,
                onCheckedChange = onRemindersToggle,
            )
        }
        HesabiCard {
            SectionHeader("داده‌ها")
            SettingsRow(
                title = "سیاست حریم خصوصی",
                subtitle = "چه چیزی ذخیره می‌شود و چه چیزی نمی‌شود",
                icon = Icons.Rounded.Lock,
                onClick = onOpenPrivacy,
            )
            SettingsRow(
                title = "پاک کردن همهٔ داده‌ها",
                subtitle = "بازگشت به حالت اولیه",
                icon = Icons.Rounded.DeleteForever,
                onClick = onClearData,
            )
            HesabiButton(
                text = "پاک کردن همهٔ داده‌ها",
                onClick = onClearData,
                variant = HesabiButtonVariant.DANGER,
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        InfoBanner(
            text = "حسابی حساب کاربری ندارد، تبلیغ ندارد و هیچ داده‌ای را به سرور نمی‌فرستد.",
            icon = Icons.Rounded.Payments,
        )
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = checked, onClick = { onCheckedChange(!checked) })
            .padding(vertical = HesabiSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(HesabiSpacing.sm),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Preview(name = "تنظیمات", heightDp = 1100, showBackground = true)
@Composable
private fun SettingsPreview() {
    val state = SettingsUiState(
        settings = AppSettings(
            themeMode = ThemeMode.SYSTEM,
            dynamicColor = true,
            currency = Currency.TOMAN,
            hideAmounts = false,
            smsCaptureEnabled = true,
            amountsAreRial = true,
            defaultAccountId = "acc_card",
        ),
        accounts = PreviewData.accounts,
    )
    HesabiTheme {
        androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(modifier = Modifier.fillMaxSize()) {
                SettingsContent(state, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
            }
        }
    }
}

@Preview(
    name = "تنظیمات - تیره",
    heightDp = 1100,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Composable
private fun SettingsDarkPreview() {
    HesabiTheme(themeMode = com.braveboy.hesabi.domain.model.ThemeMode.DARK) {
        androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(modifier = Modifier.fillMaxSize()) {
                SettingsContent(
                    SettingsUiState(accounts = PreviewData.accounts),
                    {},
                    {},
                    {},
                    {},
                    {},
                    {},
                    {},
                    {},
                    {},
                    {},
                    {},
                    {},
                    {},
                    {},
                    {},
                    {},
                )
            }
        }
    }
}
