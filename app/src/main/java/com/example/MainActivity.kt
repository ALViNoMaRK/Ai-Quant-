package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.StockIntelViewModel

enum class MainNavigationItem(val label: String, val icon: ImageVector, val testTag: String) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard, "nav_dashboard"),
    SCANNER("Scanner", Icons.Default.Search, "nav_scanner"),
    RESEARCH("Research", Icons.Default.Assessment, "nav_research"),
    PORTFOLIO("Portfolio", Icons.Default.PieChart, "nav_portfolio"),
    BIG_MONEY("13F Flow", Icons.Default.AccountBalance, "nav_big_money"),
    ALERTS("Health", Icons.Default.Sensors, "nav_alerts"),
    AI_CHAT("AI Assist", Icons.Default.AutoAwesome, "nav_ai_chat")
}

class MainActivity : ComponentActivity() {
    private val viewModel: StockIntelViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = true) {
                StockIntelApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun StockIntelApp(viewModel: StockIntelViewModel) {
    var currentNav by remember { mutableStateOf(MainNavigationItem.DASHBOARD) }
    var selectedStockForResearch by remember { mutableStateOf("NVDA") }
    var oppInitialTab by remember { mutableStateOf(0) }
    var showOpportunitiesScreen by remember { mutableStateOf(false) }

    fun navigateToStockResearch(symbol: String) {
        selectedStockForResearch = symbol
        viewModel.selectStock(symbol)
        showOpportunitiesScreen = false
        currentNav = MainNavigationItem.RESEARCH
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = TerminalBgDark,
        bottomBar = {
            NavigationBar(
                containerColor = TerminalSurfaceDark,
                contentColor = CyanAccent,
                tonalElevation = 8.dp
            ) {
                MainNavigationItem.entries.forEach { item ->
                    val isSelected = currentNav == item && !showOpportunitiesScreen
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            showOpportunitiesScreen = false
                            currentNav = item
                        },
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = {
                            Text(
                                text = item.label,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = TerminalBgDark,
                            selectedTextColor = CyanAccent,
                            indicatorColor = CyanAccent,
                            unselectedIconColor = TextSecondaryDark,
                            unselectedTextColor = TextSecondaryDark
                        ),
                        modifier = Modifier.testTag(item.testTag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (showOpportunitiesScreen) {
                OpportunitiesAndDeteriorationScreen(
                    viewModel = viewModel,
                    initialTab = oppInitialTab,
                    onNavigateToResearch = { navigateToStockResearch(it) }
                )
            } else {
                when (currentNav) {
                    MainNavigationItem.DASHBOARD -> DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToResearch = { navigateToStockResearch(it) },
                        onNavigateToOpportunities = {
                            oppInitialTab = 0
                            showOpportunitiesScreen = true
                        },
                        onNavigateToDeteriorating = {
                            oppInitialTab = 1
                            showOpportunitiesScreen = true
                        },
                        onNavigateToAlerts = { currentNav = MainNavigationItem.ALERTS }
                    )
                    MainNavigationItem.SCANNER -> StockScannerScreen(
                        viewModel = viewModel,
                        onNavigateToResearch = { navigateToStockResearch(it) }
                    )
                    MainNavigationItem.RESEARCH -> StockResearchScreen(
                        symbol = selectedStockForResearch,
                        viewModel = viewModel,
                        onBack = { currentNav = MainNavigationItem.DASHBOARD }
                    )
                    MainNavigationItem.PORTFOLIO -> PortfolioAndWatchlistScreen(
                        viewModel = viewModel,
                        onNavigateToResearch = { navigateToStockResearch(it) }
                    )
                    MainNavigationItem.BIG_MONEY -> BigMoneyScreen(
                        viewModel = viewModel,
                        onNavigateToResearch = { navigateToStockResearch(it) }
                    )
                    MainNavigationItem.ALERTS -> AlertsAndHealthScreen(
                        viewModel = viewModel,
                        onNavigateToResearch = { navigateToStockResearch(it) }
                    )
                    MainNavigationItem.AI_CHAT -> AiChatScreen(
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}
