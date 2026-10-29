package com.braveboy.hesabi.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
    Tab(Routes.TRANSACTIONS, "تراکنش‌ها", Icons.Rounded.ReceiptLong),
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
    val pendingSms by container.smsRepository.observePendingCount()
        .collectAsStateWithLifecycle(initialValue = 0)
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = tabs.any { it.route == currentRoute }
    val showFab = currentRoute == Routes.HOME || currentRoute == Routes.TRANSACTIONS

    LaunchedEffect(sharedText) {
        if (!sharedText.isNullOrBlank()) {
            navController.navigate(Routes.SMS)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        val selected = currentRoute == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navigateTab(navController, tab.route) },
                            icon = {
                                if (tab.route == Routes.MORE && pendingSms > 0) {
                                    BadgedBox(badge = { Badge { Text(pendingSms.toString()) } }) {
                                        Icon(tab.icon, contentDescription = null)
                                    }
                                } else {
                                    Icon(tab.icon, contentDescription = null)
                                }
                            },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (showFab) {
                ExtendedFloatingActionButton(
                    onClick = { navController.navigate(Routes.newEntry(TransactionType.EXPENSE)) },
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text("ثبت") },
                    modifier = Modifier.padding(bottom = 4.dp),
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
