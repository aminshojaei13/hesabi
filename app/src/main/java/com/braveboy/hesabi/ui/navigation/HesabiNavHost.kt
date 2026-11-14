package com.braveboy.hesabi.ui.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.braveboy.hesabi.domain.model.AppSettings
import com.braveboy.hesabi.domain.model.TransactionType
import com.braveboy.hesabi.di.AppContainer
import com.braveboy.hesabi.ui.accounts.AccountsScreen
import com.braveboy.hesabi.ui.annual.AnnualReportScreen
import com.braveboy.hesabi.ui.assets.AssetsScreen
import com.braveboy.hesabi.ui.assistant.AssistantScreen
import com.braveboy.hesabi.ui.calendar.DueCalendarScreen
import com.braveboy.hesabi.ui.importer.ImportScreen
import com.braveboy.hesabi.ui.privacy.MyDataScreen
import com.braveboy.hesabi.ui.simulator.PurchaseSimulatorScreen
import com.braveboy.hesabi.ui.trash.TrashScreen
import com.braveboy.hesabi.ui.backup.BackupScreen
import com.braveboy.hesabi.ui.budget.BudgetScreen
import com.braveboy.hesabi.ui.categories.CategoriesScreen
import com.braveboy.hesabi.ui.debts.DebtsAndCommitmentsScreen
import com.braveboy.hesabi.ui.entry.AddEditScreen
import com.braveboy.hesabi.ui.entry.QuickEntryScreen
import com.braveboy.hesabi.ui.home.HomeScreen
import com.braveboy.hesabi.ui.more.MoreScreen
import com.braveboy.hesabi.ui.onboarding.OnboardingScreen
import com.braveboy.hesabi.ui.recurring.RecurringTransactionsScreen
import com.braveboy.hesabi.ui.reports.ReportsScreen
import com.braveboy.hesabi.ui.security.AppLockScreen
import com.braveboy.hesabi.ui.security.findActivity
import com.braveboy.hesabi.ui.settings.SettingsScreen
import com.braveboy.hesabi.ui.sms.SmsScreen
import com.braveboy.hesabi.ui.theme.HesabiTheme
import com.braveboy.hesabi.ui.transactions.TransactionsScreen
import com.braveboy.hesabi.ui.voice.VoiceEntrySheet
import kotlinx.coroutines.launch

object Routes {
    const val HOME = "home"
    const val TRANSACTIONS = "transactions"
    const val REPORTS = "reports"
    const val MORE = "more"
    const val ACCOUNTS = "accounts"
    const val CATEGORIES = "categories"
    const val BUDGET = "budget"
    const val SMS = "sms"
    const val BACKUP = "backup"
    const val SETTINGS = "settings"
    const val DEBTS = "debts"
    const val RECURRING = "recurring"
    const val ENTRY = "entry?editId={editId}&type={type}"
    const val QUICK = "quick?type={type}"
    const val ASSISTANT = "assistant"
    const val ASSETS = "assets"
    const val TRASH = "trash"
    const val IMPORT = "import"
    const val MY_DATA = "my_data"
    const val ANNUAL = "annual"
    const val CALENDAR = "calendar"
    const val SIMULATOR = "simulator"

    fun newEntry(type: TransactionType): String = "entry?type=${type.name}"
    fun quickEntry(type: TransactionType): String = "quick?type=${type.name}"
    fun editEntry(id: String): String = "entry?editId=$id&type=${TransactionType.EXPENSE.name}"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

/** Re-lock the app after it has been in the background for this long. */
private const val APP_LOCK_GRACE_MILLIS = 30_000L

private val tabs = listOf(
    Tab(Routes.HOME, "خانه", Icons.Rounded.Home),
    Tab(Routes.TRANSACTIONS, "تراکنش‌ها", Icons.AutoMirrored.Rounded.ReceiptLong),
    Tab(Routes.REPORTS, "گزارش", Icons.Rounded.BarChart),
    Tab(Routes.MORE, "بیشتر", Icons.Rounded.MoreHoriz),
)

@Composable
fun HesabiRoot(
    container: AppContainer,
    initialSettings: AppSettings,
    sharedText: String?,
    onSharedTextConsumed: () -> Unit,
    openSmsRequested: Boolean = false,
    onOpenSmsConsumed: () -> Unit = {},
) {
    val settings by container.settingsRepository.settings.collectAsStateWithLifecycle(initialValue = initialSettings)
    var isAppLocked by remember(settings.appLockEnabled) {
        mutableStateOf(settings.appLockEnabled && settings.pinCodeHash.isNotBlank())
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.runtime.SideEffect {
        val window = context.findActivity()?.window
        if (settings.appLockEnabled || settings.hideAmounts) {
            window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
    var backgroundedAtMillis by remember { mutableLongStateOf(0L) }

    DisposableEffect(lifecycleOwner, settings.appLockEnabled, settings.pinCodeHash) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> backgroundedAtMillis = System.currentTimeMillis()
                Lifecycle.Event.ON_START -> {
                    val lockConfigured = settings.appLockEnabled && settings.pinCodeHash.isNotBlank()
                    val elapsed = if (backgroundedAtMillis > 0L) {
                        System.currentTimeMillis() - backgroundedAtMillis
                    } else {
                        0L
                    }
                    if (lockConfigured && backgroundedAtMillis > 0L && elapsed >= APP_LOCK_GRACE_MILLIS) {
                        isAppLocked = true
                    }
                    backgroundedAtMillis = 0L
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    HesabiTheme(
        themeMode = settings.themeMode,
        dynamicColor = settings.dynamicColor,
        currency = settings.currency,
        hideAmounts = settings.hideAmounts,
    ) {
        if (isAppLocked) {
            AppLockScreen(
                pinCodeHash = settings.pinCodeHash,
                biometricEnabled = settings.biometricEnabled,
                failedAttempts = settings.pinFailedAttempts,
                lockedUntilMillis = settings.pinLockedUntilMillis,
                onUnlocked = {
                    isAppLocked = false
                    container.applicationScope.launch {
                        container.settingsRepository.setPinFailedAttempts(0)
                        container.settingsRepository.setPinLockedUntil(0L)
                    }
                },
                onAttemptStateChanged = { attempts, lockedUntil ->
                    container.applicationScope.launch {
                        container.settingsRepository.setPinFailedAttempts(attempts)
                        container.settingsRepository.setPinLockedUntil(lockedUntil)
                    }
                },
            )
        } else if (!settings.onboarded) {
            OnboardingScreen(container = container)
        } else {
            MainScaffold(
                container = container,
                sharedText = sharedText,
                onSharedTextConsumed = onSharedTextConsumed,
                openSmsRequested = openSmsRequested,
                onOpenSmsConsumed = onOpenSmsConsumed,
            )
        }
    }
}

@Composable
private fun MainScaffold(
    container: AppContainer,
    sharedText: String?,
    onSharedTextConsumed: () -> Unit,
    openSmsRequested: Boolean,
    onOpenSmsConsumed: () -> Unit,
) {
    val navController = rememberNavController()
    var showVoiceSheet by remember { mutableStateOf(false) }
    val pendingSms by container.smsRepository.observePendingCount()
        .collectAsStateWithLifecycle(initialValue = 0)
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = tabs.any { it.route == currentRoute }

    LaunchedEffect(sharedText) {
        if (!sharedText.isNullOrBlank()) {
            navController.navigate(Routes.SMS)
        }
    }

    LaunchedEffect(openSmsRequested) {
        if (openSmsRequested) {
            navController.navigate(Routes.SMS)
            onOpenSmsConsumed()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (showBottomBar) {
                androidx.compose.material3.FloatingActionButton(
                    onClick = { showVoiceSheet = true },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shape = CircleShape,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Mic,
                        contentDescription = "ثبت صوتی",
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        },
        floatingActionButtonPosition = androidx.compose.material3.FabPosition.Start,
        bottomBar = {
            if (showBottomBar) {
                HesabiFloatingNavigationBar(
                    currentRoute = currentRoute,
                    pendingSmsCount = pendingSms,
                    onTabSelected = { route -> navigateTab(navController, route) },
                    onAddClick = { navController.navigate(Routes.quickEntry(TransactionType.EXPENSE)) },
                )
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            NavHost(
                navController = navController,
                startDestination = Routes.HOME,
            ) {
                composable(Routes.HOME) {
                    HomeScreen(
                        container = container,
                        onAdd = { type -> navController.navigate(Routes.quickEntry(type)) },
                        onSeeAll = { navigateTab(navController, Routes.TRANSACTIONS) },
                        onTransactionClick = { id -> navController.navigate(Routes.editEntry(id)) },
                        onOpenSms = { navController.navigate(Routes.SMS) },
                        onOpenBudget = { navController.navigate(Routes.BUDGET) },
                        onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                        onOpenVoice = { showVoiceSheet = true },
                        onOpenAssistant = { navController.navigate(Routes.ASSISTANT) },
                    )
                }
                composable(Routes.TRANSACTIONS) {
                    TransactionsScreen(
                        container = container,
                        onTransactionClick = { id -> navController.navigate(Routes.editEntry(id)) },
                    )
                }
                composable(Routes.REPORTS) {
                    ReportsScreen(
                        container = container,
                        onOpenBudget = { navController.navigate(Routes.BUDGET) },
                    )
                }
                composable(Routes.MORE) {
                    MoreScreen(
                        pendingSmsCount = pendingSms,
                        onAccounts = { navController.navigate(Routes.ACCOUNTS) },
                        onCategories = { navController.navigate(Routes.CATEGORIES) },
                        onBudget = { navController.navigate(Routes.BUDGET) },
                        onDebts = { navController.navigate(Routes.DEBTS) },
                        onRecurring = { navController.navigate(Routes.RECURRING) },
                        onSms = { navController.navigate(Routes.SMS) },
                        onBackup = { navController.navigate(Routes.BACKUP) },
                        onSettings = { navController.navigate(Routes.SETTINGS) },
                        onAssistant = { navController.navigate(Routes.ASSISTANT) },
                        onAssets = { navController.navigate(Routes.ASSETS) },
                        onTrash = { navController.navigate(Routes.TRASH) },
                        onImport = { navController.navigate(Routes.IMPORT) },
                        onMyData = { navController.navigate(Routes.MY_DATA) },
                        onAnnual = { navController.navigate(Routes.ANNUAL) },
                        onCalendar = { navController.navigate(Routes.CALENDAR) },
                        onSimulator = { navController.navigate(Routes.SIMULATOR) },
                    )
                }
                composable(
                    route = Routes.QUICK,
                    arguments = listOf(
                        navArgument("type") {
                            type = NavType.StringType
                            defaultValue = TransactionType.EXPENSE.name
                        },
                    ),
                ) { entry ->
                    val type = entry.arguments?.getString("type")
                        ?.let { runCatching { TransactionType.valueOf(it) }.getOrNull() }
                    QuickEntryScreen(
                        container = container,
                        initialType = type,
                        onBack = { navController.popBackStack() },
                        onDone = { navController.popBackStack() },
                        onOpenFullForm = { formType ->
                            navController.navigate(Routes.newEntry(formType)) {
                                popUpTo(Routes.QUICK) { inclusive = true }
                            }
                        },
                    )
                }
                composable(
                    route = Routes.ENTRY,
                    arguments = listOf(
                        navArgument("editId") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        },
                        navArgument("type") {
                            type = NavType.StringType
                            defaultValue = TransactionType.EXPENSE.name
                        },
                    ),
                ) { entry ->
                    val editId = entry.arguments?.getString("editId")
                    val initialType = entry.arguments?.getString("type")
                        ?.let { runCatching { TransactionType.valueOf(it) }.getOrNull() }
                    AddEditScreen(
                        container = container,
                        transactionId = editId,
                        initialType = if (editId == null) initialType else null,
                        onBack = { navController.popBackStack() },
                        onDone = { navController.popBackStack() },
                    )
                }
                composable(Routes.ACCOUNTS) {
                    AccountsScreen(container = container, onBack = { navController.popBackStack() })
                }
                composable(Routes.CATEGORIES) {
                    CategoriesScreen(container = container, onBack = { navController.popBackStack() })
                }
                composable(Routes.BUDGET) {
                    BudgetScreen(container = container, onBack = { navController.popBackStack() })
                }
                composable(Routes.SMS) {
                    SmsScreen(
                        container = container,
                        onBack = { navController.popBackStack() },
                        initialText = sharedText,
                        onInitialTextConsumed = onSharedTextConsumed,
                    )
                }
                composable(Routes.BACKUP) {
                    BackupScreen(container = container, onBack = { navController.popBackStack() })
                }
                composable(Routes.SETTINGS) {
                    SettingsScreen(container = container, onBack = { navController.popBackStack() })
                }
                composable(Routes.DEBTS) {
                    DebtsAndCommitmentsScreen(container = container, onBack = { navController.popBackStack() })
                }
                composable(Routes.RECURRING) {
                    RecurringTransactionsScreen(container = container, onBack = { navController.popBackStack() })
                }
                composable(Routes.ASSISTANT) {
                    AssistantScreen(container = container, onBack = { navController.popBackStack() })
                }
                composable(Routes.ASSETS) {
                    AssetsScreen(container = container, onBack = { navController.popBackStack() })
                }
                composable(Routes.TRASH) {
                    TrashScreen(container = container, onBack = { navController.popBackStack() })
                }
                composable(Routes.IMPORT) {
                    ImportScreen(container = container, onBack = { navController.popBackStack() })
                }
                composable(Routes.MY_DATA) {
                    MyDataScreen(
                        container = container,
                        onBack = { navController.popBackStack() },
                        onOpenBackup = { navController.navigate(Routes.BACKUP) },
                        onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    )
                }
                composable(Routes.ANNUAL) {
                    AnnualReportScreen(container = container, onBack = { navController.popBackStack() })
                }
                composable(Routes.CALENDAR) {
                    DueCalendarScreen(container = container, onBack = { navController.popBackStack() })
                }
                composable(Routes.SIMULATOR) {
                    PurchaseSimulatorScreen(
                        container = container,
                        onBack = { navController.popBackStack() },
                        onAddExpense = {
                            navController.navigate(Routes.quickEntry(TransactionType.EXPENSE))
                        },
                    )
                }
            }
        }
    }

    if (showVoiceSheet) {
        VoiceEntrySheet(
            container = container,
            onDismiss = { showVoiceSheet = false },
            onSaved = { showVoiceSheet = false },
        )
    }
}

private fun navigateTab(navController: NavHostController, route: String) {
    navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun HesabiFloatingNavigationBar(
    currentRoute: String?,
    pendingSmsCount: Int,
    onTabSelected: (String) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lumina = com.braveboy.hesabi.ui.theme.LocalHesabiLumina.current

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        shape = RoundedCornerShape(32.dp),
        color = lumina.navDockBg,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, lumina.navDockBorder),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Glowing Action Capsule on Left (RTL Left)
            Surface(
                onClick = onAddClick,
                shape = RoundedCornerShape(22.dp),
                color = lumina.voicePillBg,
                border = BorderStroke(1.dp, lumina.voicePillBorder),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "ورود دستی",
                        style = MaterialTheme.typography.labelMedium,
                        color = lumina.voicePillIcon,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            // Center Glowing Circular Play/Action Button
            Surface(
                onClick = onAddClick,
                shape = CircleShape,
                color = Color(0xFF10B981),
                shadowElevation = 8.dp,
                modifier = Modifier.size(48.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.radialGradient(
                                colors = listOf(Color(0xFF34D399), Color(0xFF10B981), Color(0xFF059669)),
                            )
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        androidx.compose.material.icons.Icons.Rounded.PlayArrow,
                        contentDescription = "ثبت سریع",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }

            // Right Items (Wallet & Home)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                HesabiNavItem(
                    label = "تراکنش‌ها",
                    icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                    selected = currentRoute == Routes.TRANSACTIONS,
                    onClick = { onTabSelected(Routes.TRANSACTIONS) },
                )

                HesabiNavItem(
                    label = "خانه",
                    icon = Icons.Rounded.Home,
                    selected = currentRoute == Routes.HOME,
                    onClick = { onTabSelected(Routes.HOME) },
                )
            }
        }
    }
}

@Composable
private fun HesabiNavItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeCount: Int = 0,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            if (badgeCount > 0 && !selected) {
                BadgedBox(badge = { Badge { Text(badgeCount.toString()) } }) {
                    Icon(
                        icon,
                        contentDescription = label,
                        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
                }
            } else {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.height(3.dp))
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            } else {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
