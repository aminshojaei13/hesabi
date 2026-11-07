package com.braveboy.hesabi.ui.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.braveboy.hesabi.ui.backup.BackupScreen
import com.braveboy.hesabi.ui.budget.BudgetScreen
import com.braveboy.hesabi.ui.categories.CategoriesScreen
import com.braveboy.hesabi.ui.entry.AddEditScreen
import com.braveboy.hesabi.ui.home.HomeScreen
import com.braveboy.hesabi.ui.more.MoreScreen
import com.braveboy.hesabi.ui.onboarding.OnboardingScreen
import com.braveboy.hesabi.ui.reports.ReportsScreen
import com.braveboy.hesabi.ui.settings.SettingsScreen
import com.braveboy.hesabi.ui.sms.SmsScreen
import com.braveboy.hesabi.ui.theme.HesabiTheme
import com.braveboy.hesabi.ui.transactions.TransactionsScreen
import com.braveboy.hesabi.ui.voice.VoiceEntrySheet

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
    const val ENTRY = "entry?editId={editId}&type={type}"

    fun newEntry(type: TransactionType): String = "entry?type=${type.name}"
    fun editEntry(id: String): String = "entry?editId=$id&type=${TransactionType.EXPENSE.name}"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

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
) {
    val settings by container.settingsRepository.settings.collectAsStateWithLifecycle(initialValue = initialSettings)
    HesabiTheme(
        themeMode = settings.themeMode,
        dynamicColor = settings.dynamicColor,
        currency = settings.currency,
        hideAmounts = settings.hideAmounts,
    ) {
        if (!settings.onboarded) {
            OnboardingScreen(container = container)
        } else {
            MainScaffold(
                container = container,
                sharedText = sharedText,
                onSharedTextConsumed = onSharedTextConsumed,
            )
        }
    }
}

@Composable
private fun MainScaffold(
    container: AppContainer,
    sharedText: String?,
    onSharedTextConsumed: () -> Unit,
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                HesabiFloatingNavigationBar(
                    currentRoute = currentRoute,
                    pendingSmsCount = pendingSms,
                    onTabSelected = { route -> navigateTab(navController, route) },
                    onAddClick = { navController.navigate(Routes.newEntry(TransactionType.EXPENSE)) },
                    onVoiceClick = { showVoiceSheet = true },
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
                        onAdd = { type -> navController.navigate(Routes.newEntry(type)) },
                        onSeeAll = { navigateTab(navController, Routes.TRANSACTIONS) },
                        onTransactionClick = { id -> navController.navigate(Routes.editEntry(id)) },
                        onOpenSms = { navController.navigate(Routes.SMS) },
                        onOpenBudget = { navController.navigate(Routes.BUDGET) },
                        onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                        onOpenVoice = { showVoiceSheet = true },
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
                        onSms = { navController.navigate(Routes.SMS) },
                        onBackup = { navController.navigate(Routes.BACKUP) },
                        onSettings = { navController.navigate(Routes.SETTINGS) },
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
    onVoiceClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .shadow(12.dp, RoundedCornerShape(32.dp)),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Home Tab
            HesabiNavItem(
                label = "خانه",
                icon = Icons.Rounded.Home,
                selected = currentRoute == Routes.HOME,
                onClick = { onTabSelected(Routes.HOME) },
            )

            // Transactions Tab
            HesabiNavItem(
                label = "تراکنش‌ها",
                icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                selected = currentRoute == Routes.TRANSACTIONS,
                onClick = { onTabSelected(Routes.TRANSACTIONS) },
            )

            // Center Floating Mic & Add Actions
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    onClick = onVoiceClick,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondary,
                    shadowElevation = 6.dp,
                    modifier = Modifier.size(42.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.Mic,
                            contentDescription = "ثبت صوتی",
                            tint = MaterialTheme.colorScheme.onSecondary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Surface(
                    onClick = onAddClick,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 6.dp,
                    modifier = Modifier.size(46.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = "ثبت دستی",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }

            // Reports Tab
            HesabiNavItem(
                label = "گزارش",
                icon = Icons.Rounded.BarChart,
                selected = currentRoute == Routes.REPORTS,
                onClick = { onTabSelected(Routes.REPORTS) },
            )

            // More Tab
            HesabiNavItem(
                label = "بیشتر",
                icon = Icons.Rounded.MoreHoriz,
                selected = currentRoute == Routes.MORE,
                badgeCount = pendingSmsCount,
                onClick = { onTabSelected(Routes.MORE) },
            )
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
        shape = RoundedCornerShape(20.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (badgeCount > 0 && !selected) {
                BadgedBox(badge = { Badge { Text(badgeCount.toString()) } }) {
                    Icon(
                        icon,
                        contentDescription = label,
                        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            } else {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
            if (selected) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
