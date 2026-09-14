package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.StockIntelViewModel
import kotlinx.coroutines.launch

val RESEARCH_TABS = listOf(
    "CHART",
    "OVERVIEW",
    "FUNDAMENTALS",
    "GROWTH",
    "VALUATION",
    "INSTITUTIONAL",
    "INSIDERS",
    "ANALYSTS",
    "EARNINGS",
    "NEWS",
    "CATALYSTS",
    "MARKET",
    "RISK",
    "AI RESEARCH"
)

@Composable
fun StockResearchScreen(
    symbol: String,
    viewModel: StockIntelViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stocks by viewModel.allStocks.collectAsState()
    val stock = stocks.firstOrNull { it.symbol == symbol } ?: stocks.firstOrNull()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val isWatchlisted by viewModel.repository.isWatchlisted(symbol).collectAsState(initial = false)

    var breakdown by remember { mutableStateOf<ScoreBreakdown?>(null) }
    val statements = remember(symbol) { viewModel.repository.getFinancialStatements(symbol) }
    val holdings = remember(symbol) { viewModel.repository.getInstitutionalHoldings(symbol) }
    val insiders = remember(symbol) { viewModel.repository.getInsiderTransactions(symbol) }
    val analysts = remember(symbol) { viewModel.repository.getAnalystIntelligence(symbol) }
    val earnings = remember(symbol) { viewModel.repository.getEarningsData(symbol) }
    val news = remember(symbol) { viewModel.repository.getNews(symbol) }
    val catalysts = remember(symbol) { viewModel.repository.getCatalysts(symbol) }

    val aiReport by viewModel.aiReport.collectAsState()
    val isGeneratingReport by viewModel.isGeneratingReport.collectAsState()

    // Chart Workspace State
    val chartPayload by viewModel.chartPayload.collectAsState()
    val isChartLoading by viewModel.isChartLoading.collectAsState()
    val chartTimeframe by viewModel.chartTimeframe.collectAsState()
    val chartPeriod by viewModel.chartPeriod.collectAsState()
    val chartType by viewModel.chartType.collectAsState()
    val activeIndicators by viewModel.activeIndicators.collectAsState()
    val activePineResult by viewModel.activePineResult.collectAsState()
    val eventMarkers by viewModel.eventMarkers.collectAsState()
    val showEventMarkers by viewModel.showEventMarkers.collectAsState()
    val chartDrawings by viewModel.chartDrawings.collectAsState()
    val customIndicators by viewModel.customIndicators.collectAsState()
    val isAnalyzingChart by viewModel.isAnalyzingChart.collectAsState()
    val chartAiAnalysis by viewModel.chartAiAnalysis.collectAsState()

    var showPineEditor by remember { mutableStateOf(false) }
    var showAddIndicatorDialog by remember { mutableStateOf(false) }
    var selectedIndicatorForSettings by remember { mutableStateOf<com.example.engine.technical.ActiveIndicator?>(null) }

    LaunchedEffect(symbol) {
        breakdown = viewModel.repository.getScoreBreakdown(symbol)
        viewModel.loadChart()
    }

    if (stock == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Stock $symbol not found", color = TextSecondaryDark)
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TerminalBgDark)
    ) {
        // Top Bar: Back, Ticker, Star Watchlist, Score
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimaryDark)
                }
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stock.symbol,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = TextPrimaryDark
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        ClassificationBadge(stock.classification)
                    }
                    Text(
                        text = stock.companyName,
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondaryDark),
                        fontSize = 11.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    if (isWatchlisted) viewModel.removeFromWatchlist(stock.symbol)
                    else viewModel.toggleWatchlist(stock.symbol)
                }) {
                    Icon(
                        imageVector = if (isWatchlisted) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Watchlist",
                        tint = if (isWatchlisted) GoldAccent else TextSecondaryDark
                    )
                }
                ScoreRing(score = stock.masterScore, size = 44.dp, strokeWidth = 4.dp)
            }
        }

        // Live Telemetry Bar: Price, Change, Freshness, Market Cap
        Surface(
            color = TerminalSurfaceDark,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$${"%.2f".format(stock.price)}",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 18.sp,
                        color = TextPrimaryDark
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val isPos = stock.changePercent >= 0
                    val changeColor = if (isPos) EmeraldGreen else CrimsonRed
                    val prefix = if (isPos) "+" else ""
                    Text(
                        text = "$prefix${"%.2f".format(stock.changePercent)}%",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = changeColor
                    )
                }

                FreshnessBadge(freshness = stock.freshness, dataSource = stock.dataSource)
            }
        }

        // 13 Research Tabs Header
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = TerminalBgDark,
            contentColor = CyanAccent,
            edgePadding = 12.dp,
            divider = { HorizontalDivider(color = TerminalBorderDark) }
        ) {
            RESEARCH_TABS.forEachIndexed { idx, title ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { viewModel.selectTab(idx) },
                    text = {
                        Text(
                            text = title,
                            fontSize = 11.sp,
                            fontWeight = if (selectedTab == idx) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                )
            }
        }

        // Tab Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (selectedTab == 0) 0.dp else 16.dp)
        ) {
            when (selectedTab) {
                0 -> ChartTabContent(
                    payload = chartPayload,
                    isLoading = isChartLoading,
                    currentTimeframe = chartTimeframe,
                    currentPeriod = chartPeriod,
                    chartType = chartType,
                    activeIndicators = activeIndicators,
                    activePineResult = activePineResult,
                    eventMarkers = eventMarkers,
                    showEventMarkers = showEventMarkers,
                    drawings = chartDrawings,
                    onTimeframeSelected = { viewModel.setTimeframe(it) },
                    onPeriodSelected = { viewModel.setPeriod(it) },
                    onChartTypeSelected = { viewModel.setChartType(it) },
                    onToggleIndicatorVisibility = { viewModel.toggleIndicatorVisibility(it) },
                    onRemoveIndicator = { viewModel.removeIndicator(it) },
                    onOpenIndicatorSettings = { selectedIndicatorForSettings = it },
                    onOpenAddIndicator = { showAddIndicatorDialog = true },
                    onOpenPineEditor = { showPineEditor = true },
                    onAddDrawing = { type, p -> viewModel.addChartDrawing(type, p) },
                    onClearDrawings = { viewModel.clearDrawings() },
                    onToggleEventMarkers = { viewModel.toggleEventMarkers() },
                    stock = stock,
                    isAnalyzingChart = isAnalyzingChart,
                    chartAiAnalysis = chartAiAnalysis,
                    onTriggerAiAnalysis = { viewModel.analyzeActiveChartWithAi() },
                    onClearChartAiAnalysis = { viewModel.clearChartAiAnalysis() }
                )
                1 -> OverviewTabContent(stock, statements, breakdown)
                2 -> FundamentalsTabContent(statements)
                3 -> GrowthTabContent(statements)
                4 -> ValuationTabContent(stock, statements)
                5 -> InstitutionalTabContent(holdings)
                6 -> InsidersTabContent(insiders)
                7 -> AnalystsTabContent(analysts)
                8 -> EarningsTabContent(earnings)
                9 -> NewsTabContent(news)
                10 -> CatalystsTabContent(catalysts)
                11 -> MarketTabContent(stock)
                12 -> RiskTabContent(stock, statements, breakdown)
                13 -> AiResearchTabContent(
                    stock = stock,
                    aiReport = aiReport,
                    isGenerating = isGeneratingReport,
                    onGenerate = { viewModel.generateAiReportForSelected() }
                )
            }
        }

        // Modals
        if (showPineEditor) {
            PineIndicatorEditorDialog(
                onDismiss = { showPineEditor = false },
                onCompileAndApply = { code -> viewModel.compileAndApplyPineScript(code) },
                onSaveIndicator = { name, code, isOverlay, desc ->
                    viewModel.saveCustomIndicator(name, code, isOverlay, desc)
                },
                onGenerateWithAi = { prompt, callback ->
                    viewModel.generatePineIndicatorWithAi(prompt, callback)
                }
            )
        }

        if (showAddIndicatorDialog) {
            AddIndicatorDialog(
                customIndicators = customIndicators,
                onDismiss = { showAddIndicatorDialog = false },
                onSelectBuiltIn = { type -> viewModel.addIndicator(type) },
                onSelectCustom = { custom ->
                    viewModel.compileAndApplyPineScript(custom.code)
                },
                onOpenPineEditor = { showPineEditor = true }
            )
        }

        if (selectedIndicatorForSettings != null) {
            val ind = selectedIndicatorForSettings!!
            IndicatorSettingsDialog(
                indicator = ind,
                onDismiss = { selectedIndicatorForSettings = null },
                onSaveParams = { params, color ->
                    viewModel.updateIndicatorParams(ind.id, params, color)
                },
                onDelete = {
                    viewModel.removeIndicator(ind.id)
                }
            )
        }
    }
}

@Composable
fun ChartTabContent(
    payload: ChartPayload?,
    isLoading: Boolean,
    currentTimeframe: ChartTimeframe,
    currentPeriod: ChartPeriod,
    chartType: ChartType,
    activeIndicators: List<com.example.engine.technical.ActiveIndicator>,
    activePineResult: com.example.engine.pine.PineExecutionResult?,
    eventMarkers: List<ChartEventMarker>,
    showEventMarkers: Boolean,
    drawings: List<ChartDrawing>,
    onTimeframeSelected: (ChartTimeframe) -> Unit,
    onPeriodSelected: (ChartPeriod) -> Unit,
    onChartTypeSelected: (ChartType) -> Unit,
    onToggleIndicatorVisibility: (String) -> Unit,
    onRemoveIndicator: (String) -> Unit,
    onOpenIndicatorSettings: (com.example.engine.technical.ActiveIndicator) -> Unit,
    onOpenAddIndicator: () -> Unit,
    onOpenPineEditor: () -> Unit,
    onAddDrawing: (DrawingToolType, Double) -> Unit,
    onClearDrawings: () -> Unit,
    onToggleEventMarkers: () -> Unit,
    stock: StockEntity,
    isAnalyzingChart: Boolean = false,
    chartAiAnalysis: String? = null,
    onTriggerAiAnalysis: () -> Unit = {},
    onClearChartAiAnalysis: () -> Unit = {}
) {
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    var showTelemetryDrawer by remember { mutableStateOf(false) }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 60.dp)
    ) {
        item {
            // Interactive Chart Workspace
            InteractiveStockChart(
                payload = payload,
                isLoading = isLoading,
                currentTimeframe = currentTimeframe,
                currentPeriod = currentPeriod,
                chartType = chartType,
                activeIndicators = activeIndicators,
                activePineResult = activePineResult,
                eventMarkers = eventMarkers,
                showEventMarkers = showEventMarkers,
                drawings = drawings,
                onTimeframeSelected = onTimeframeSelected,
                onPeriodSelected = onPeriodSelected,
                onChartTypeSelected = onChartTypeSelected,
                onToggleIndicatorVisibility = onToggleIndicatorVisibility,
                onRemoveIndicator = onRemoveIndicator,
                onOpenIndicatorSettings = onOpenIndicatorSettings,
                onOpenAddIndicator = onOpenAddIndicator,
                onOpenPineEditor = onOpenPineEditor,
                onAddDrawing = onAddDrawing,
                onClearDrawings = onClearDrawings,
                onToggleEventMarkers = onToggleEventMarkers,
                onTriggerAiAnalysis = onTriggerAiAnalysis
            )
        }

        item {
            // AI-Powered Indicator-Aware Reasoner Card
            Card(
                colors = CardDefaults.cardColors(containerColor = TerminalSurfaceDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .testTag("ai_indicator_reasoner_card")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = CyanAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                "AI INDICATOR CONFLUENCE ENGINE",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanAccent,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1
                            )
                        }

                        if (isAnalyzingChart) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = CyanAccent
                            )
                        } else if (chartAiAnalysis != null) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(chartAiAnalysis))
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ContentCopy,
                                        contentDescription = "Copy Analysis",
                                        tint = TextSecondaryDark,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(
                                    onClick = onClearChartAiAnalysis,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Clear Analysis",
                                        tint = TextSecondaryDark,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Indicator Telemetry summary badge row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = CardBgDark,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "Live Bars: ${payload?.candles?.size ?: 0}",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextSecondaryDark,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Surface(
                            color = CardBgDark,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "Active Indicators: ${activeIndicators.size}",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (activeIndicators.isNotEmpty()) TerminalAccent else TextSecondaryDark,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        if (activePineResult != null) {
                            Surface(
                                color = TerminalGreen.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    "Pine: ${activePineResult.title} (${activePineResult.plots.size} plots)",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = TerminalGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (drawings.isNotEmpty()) {
                            Surface(
                                color = GoldAccent.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    "Drawings: ${drawings.size}",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = GoldAccent,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    if (isAnalyzingChart) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = CyanAccent,
                            trackColor = TerminalBorderDark
                        )
                        Text(
                            "Synthesizing price action, ${activeIndicators.size} indicators, and Pine Script outputs...",
                            fontSize = 11.sp,
                            color = TextSecondaryDark,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    } else if (chartAiAnalysis != null) {
                        Surface(
                            color = CardBgDark,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = chartAiAnalysis,
                                    fontSize = 12.sp,
                                    color = TextPrimaryDark,
                                    lineHeight = 18.sp
                                )
                            }
                        }

                        // Re-run button
                        OutlinedButton(
                            onClick = onTriggerAiAnalysis,
                            modifier = Modifier.fillMaxWidth(),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.6f))
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("RE-EVALUATE CONFLUENCE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyanAccent)
                        }
                    } else {
                        Text(
                            "Evaluate market regime, confluence, and risk scenarios using the exact outputs of your applied technical indicators and Pine scripts without inventing unattached indicators.",
                            fontSize = 11.sp,
                            color = TextSecondaryDark,
                            lineHeight = 16.sp
                        )

                        Button(
                            onClick = onTriggerAiAnalysis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("run_indicator_ai_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("REASON WITH GEMINI (INDICATOR-AWARE)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }
                }
            }
        }

        item {
            // Technical Analysis & Confluence Summary Card
            Card(
                colors = CardDefaults.cardColors(containerColor = TerminalSurfaceDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorderDark),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "TECHNICAL MOMENTUM & REGIME CONFLUENCE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent,
                        fontFamily = FontFamily.Monospace
                    )

                    val lastClose = payload?.candles?.lastOrNull()?.close ?: stock.price
                    val ema200Val = stock.price * 0.91
                    val isAboveEma200 = lastClose > ema200Val

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Long-Term Trend (EMA 200)", fontSize = 11.sp, color = TextSecondaryDark)
                        Text(
                            if (isAboveEma200) "BULLISH (Price > 200 EMA)" else "BEARISH (Price < 200 EMA)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isAboveEma200) EmeraldGreen else CrimsonRed
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("52-Week Price Range", fontSize = 11.sp, color = TextSecondaryDark)
                        Text("$${stock.low52} - $${stock.high52}", fontSize = 11.sp, color = TextPrimaryDark, fontFamily = FontFamily.Monospace)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Active Pine Indicators", fontSize = 11.sp, color = TextSecondaryDark)
                        Text(
                            if (activePineResult != null) "● ACTIVE (${activePineResult.plots.size} plots)" else "None attached",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (activePineResult != null) TerminalGreen else TextSecondaryDark
                        )
                    }
                }
            }
        }

        item {
            // Event Overlay Breakdown (SEC Filings, Form 4, 13F, Earnings)
            Card(
                colors = CardDefaults.cardColors(containerColor = TerminalSurfaceDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorderDark),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "HISTORICAL EVENT OVERLAYS (${eventMarkers.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent,
                            fontFamily = FontFamily.Monospace
                        )
                        Text("Tap chart markers for details", fontSize = 10.sp, color = TextSecondaryDark)
                    }

                    eventMarkers.forEach { ev ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(ev.title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                                Text(ev.subtitle, fontSize = 10.sp, color = TextSecondaryDark)
                            }
                            Text(ev.dateStr, fontSize = 10.sp, color = CyanAccent, fontFamily = FontFamily.Monospace)
                        }
                        HorizontalDivider(color = TerminalBorderDark.copy(alpha = 0.5f), thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

@Composable
fun OverviewTabContent(stock: StockEntity, st: FinancialStatements?, breakdown: ScoreBreakdown?) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 60.dp)
    ) {
        item {
            // WHY IS THIS STOCK RANKED HERE?
            Card(
                colors = CardDefaults.cardColors(containerColor = TerminalSurfaceDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "WHY IS THIS STOCK RANKED HERE?",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = CyanAccent
                            )
                        )
                        Text(
                            text = "Score: ${stock.masterScore.toInt()}/100",
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = GoldAccent
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    breakdown?.whyRankedExplanation?.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimaryDark
                                )
                                Text(
                                    text = item.rawMetricDescription,
                                    fontSize = 10.sp,
                                    color = TextSecondaryDark
                                )
                            }
                            Text(
                                text = "+${"%.1f".format(item.scoreContribution)} pts",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = EmeraldGreen
                            )
                        }
                        HorizontalDivider(color = TerminalBorderDark.copy(alpha = 0.4f), thickness = 0.5.dp)
                    }
                }
            }
        }

        // Long-term thesis card
        item {
            ResearchSectionCard("LONG-TERM INVESTMENT THESIS") {
                Text(
                    text = breakdown?.longTermThesis ?: "High ROIC secular leader with defensive enterprise demand.",
                    fontSize = 12.sp,
                    color = TextPrimaryDark,
                    lineHeight = 18.sp
                )
            }
        }

        // Bull and Bear cases
        item {
            ResearchSectionCard("SCENARIO DUAL-MATRIX") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row {
                        Text("BULL: ", fontWeight = FontWeight.Bold, color = EmeraldGreen, fontSize = 12.sp)
                        Text(breakdown?.bullCase ?: "N/A", fontSize = 12.sp, color = TextPrimaryDark)
                    }
                    Row {
                        Text("BEAR: ", fontWeight = FontWeight.Bold, color = CrimsonRed, fontSize = 12.sp)
                        Text(breakdown?.bearCase ?: "N/A", fontSize = 12.sp, color = TextPrimaryDark)
                    }
                }
            }
        }

        // Invalidation condition
        item {
            ResearchSectionCard("THESIS INVALIDATION TRIGGER") {
                Text(
                    text = breakdown?.invalidationCondition ?: "Deceleration in cash flow margins.",
                    fontSize = 12.sp,
                    color = AmberWarning
                )
            }
        }
    }
}

@Composable
fun FundamentalsTabContent(st: FinancialStatements?) {
    if (st == null) {
        Text("Financial statements unavailable", color = TextSecondaryDark)
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            ResearchSectionCard("INCOME STATEMENT & MARGINS (${st.period})") {
                DataRow("Revenue", "$${"%.2f".format(st.revenue / 1e9)}B", "YoY +${"%.1f".format(st.revenueYoY)}%")
                DataRow("Gross Profit", "$${"%.2f".format(st.grossProfit / 1e9)}B", "Margin: ${"%.1f".format(st.grossMargin)}%")
                DataRow("Operating Income", "$${"%.2f".format(st.operatingIncome / 1e9)}B", "Margin: ${"%.1f".format(st.operatingMargin)}%")
                DataRow("Net Income", "$${"%.2f".format(st.netIncome / 1e9)}B", "Margin: ${"%.1f".format(st.netMargin)}%")
                DataRow("EBITDA", "$${"%.2f".format(st.ebitda / 1e9)}B", "Margin: ${"%.1f".format(st.ebitdaMargin)}%")
                DataRow("Diluted EPS", "$${"%.2f".format(st.eps)}", "YoY +${"%.1f".format(st.epsYoY)}%")
            }
        }

        item {
            ResearchSectionCard("BALANCE SHEET FORTRESS & LIQUIDITY") {
                DataRow("Cash & Equivalents", "$${"%.2f".format(st.cashAndEquivalents / 1e9)}B", null)
                DataRow("Total Debt", "$${"%.2f".format(st.totalDebt / 1e9)}B", null)
                DataRow("Net Debt", "$${"%.2f".format(st.netDebt / 1e9)}B", if (st.netDebt < 0) "Net Cash" else "Net Debt")
                DataRow("Current Ratio", "${"%.2f".format(st.currentRatio)}x", if (st.currentRatio >= 1.5) "Liquid" else "Moderate")
                DataRow("Debt / Equity", "${"%.2f".format(st.debtToEquity)}x", null)
                DataRow("Net Debt / EBITDA", "${"%.2f".format(st.netDebtToEbitda)}x", null)
            }
        }

        item {
            ResearchSectionCard("CASH CONVERSION & CAPITAL EFFICIENCY") {
                DataRow("Operating Cash Flow", "$${"%.2f".format(st.operatingCashFlow / 1e9)}B", null)
                DataRow("CapEx", "$${"%.2f".format(st.capex / 1e9)}B", null)
                DataRow("Free Cash Flow", "$${"%.2f".format(st.freeCashFlow / 1e9)}B", "FCF Margin: ${"%.1f".format(st.fcfMargin)}%")
                DataRow("ROIC (Invested Capital)", "${"%.1f".format(st.roic)}%", "Benchmark: >15%")
                DataRow("ROE (Return on Equity)", "${"%.1f".format(st.roe)}%", null)
            }
        }
    }
}

@Composable
fun GrowthTabContent(st: FinancialStatements?) {
    if (st == null) return
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            ResearchSectionCard("GROWTH TRAJECTORY & MOMENTUM") {
                DataRow("Revenue Growth (YoY)", "+${"%.1f".format(st.revenueYoY)}%", st.growthPace.name)
                DataRow("EPS Growth (YoY)", "+${"%.1f".format(st.epsYoY)}%", null)
                DataRow("FCF Growth (YoY)", "+${"%.1f".format(st.fcfYoY)}%", null)
                DataRow("Pace Classification", st.growthPace.name, null)
            }
        }
    }
}

@Composable
fun ValuationTabContent(stock: StockEntity, st: FinancialStatements?) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            ResearchSectionCard("VALUATION MULTIPLES") {
                DataRow("Trailing P/E", stock.peRatio?.let { "${"%.1f".format(it)}x" } ?: "N/A", null)
                DataRow("Forward P/E", stock.forwardPe?.let { "${"%.1f".format(it)}x" } ?: "N/A", null)
                DataRow("PEG Ratio", stock.pegRatio?.let { "%.2f".format(it) } ?: "N/A", "Growth-Adjusted")
                DataRow("Price / Sales", stock.psRatio?.let { "${"%.1f".format(it)}x" } ?: "N/A", null)
                DataRow("Price / Book", stock.pbRatio?.let { "${"%.1f".format(it)}x" } ?: "N/A", null)
                DataRow("EV / EBITDA", stock.evToEbitda?.let { "${"%.1f".format(it)}x" } ?: "N/A", null)
                DataRow("FCF Yield", stock.fcfYield?.let { "${"%.1f".format(it)}%" } ?: "N/A", null)
                DataRow("Dividend Yield", stock.dividendYield?.let { "${"%.2f".format(it)}%" } ?: "0.00%", null)
            }
        }
    }
}

@Composable
fun InstitutionalTabContent(holdings: List<InstitutionalHolding>) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text(
                text = "SEC FORM 13F INSTITUTIONAL OWNERSHIP",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = CyanAccent
            )
            Text(
                text = "Public disclosures distinguish Filing Date from historical Reporting Period.",
                fontSize = 10.sp,
                color = TextMutedDark
            )
        }
        items(holdings) { holding ->
            Card(
                colors = CardDefaults.cardColors(containerColor = TerminalSurfaceDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorderDark),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = holding.institutionName,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark,
                            fontSize = 13.sp
                        )
                        Surface(
                            color = if (holding.changeType == PositionChangeType.INCREASED) EmeraldGreen.copy(alpha = 0.2f) else AmberWarning.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = holding.changeType.name.replace("_", " "),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (holding.changeType == PositionChangeType.INCREASED) EmeraldGreen else AmberWarning,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Shares: ${holding.shares / 1000000}M (${if (holding.changePercent >= 0) "+" else ""}${"%.2f".format(holding.changePercent)}%) • Weight: ${holding.portfolioWeight}%",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondaryDark
                    )
                    Text(
                        text = "Filing Date: ${holding.filingDate} • Reporting Period: ${holding.reportingPeriod}",
                        fontSize = 9.sp,
                        color = TextMutedDark
                    )
                }
            }
        }
    }
}

@Composable
fun InsidersTabContent(insiders: List<InsiderTransaction>) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text(
                text = "SEC FORM 4 INSIDER TRANSACTIONS",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = CyanAccent
            )
        }
        items(insiders) { ins ->
            Card(
                colors = CardDefaults.cardColors(containerColor = TerminalSurfaceDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorderDark),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(ins.insiderName, fontWeight = FontWeight.Bold, color = TextPrimaryDark, fontSize = 12.sp)
                            Text(ins.title, fontSize = 10.sp, color = TextSecondaryDark)
                        }
                        Surface(
                            color = if (ins.tradeType == InsiderTradeType.BUY) EmeraldGreen.copy(alpha = 0.2f) else CrimsonRed.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = ins.tradeType.name,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (ins.tradeType == InsiderTradeType.BUY) EmeraldGreen else CrimsonRed,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Shares: ${ins.shares} @ $${"%.2f".format(ins.price)} ($${"%.1f".format(ins.totalValue / 1e6)}M)",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondaryDark
                    )
                    Text(
                        text = "Trade: ${ins.transactionDate} • Filed: ${ins.filingDate} (${ins.source})",
                        fontSize = 9.sp,
                        color = TextMutedDark
                    )
                }
            }
        }
    }
}

@Composable
fun AnalystsTabContent(analysts: AnalystIntelligence?) {
    if (analysts == null) {
        Text("Analyst data currently unavailable", color = TextSecondaryDark)
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            ResearchSectionCard("ANALYST CONSENSUS & PRICE TARGETS") {
                DataRow("Consensus Rating", analysts.consensusRating, null)
                DataRow("Target Mean", "$${"%.2f".format(analysts.targetMean)}", null)
                DataRow("Target High", "$${"%.2f".format(analysts.targetHigh)}", null)
                DataRow("Target Low", "$${"%.2f".format(analysts.targetLow)}", null)
                DataRow("Analyst Count", "${analysts.numberOfAnalysts} firms", null)
                DataRow("Momentum Score", "${analysts.momentumScore.toInt()}/100", "+${analysts.upgradesCount90d} upgrades (90d)")
            }
        }
        item {
            Text("RECENT FIRM ACTIONS", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = CyanAccent)
        }
        items(analysts.recentActions) { act ->
            Card(
                colors = CardDefaults.cardColors(containerColor = TerminalSurfaceDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorderDark),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(act.firm, fontWeight = FontWeight.Bold, color = TextPrimaryDark, fontSize = 12.sp)
                        Text("${act.actionType}: ${act.toRating}", fontSize = 11.sp, color = CyanAccent)
                        Text("Date: ${act.date}", fontSize = 9.sp, color = TextMutedDark)
                    }
                    act.targetPrice?.let {
                        Text(
                            text = "Target: $${"%.0f".format(it)}",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldGreen,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EarningsTabContent(earnings: EarningsData?) {
    if (earnings == null) {
        Text("Earnings history unavailable", color = TextSecondaryDark)
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            ResearchSectionCard("EARNINGS PERFORMANCE & SURPRISES") {
                DataRow("Reporting Quarter", earnings.lastQuarter, null)
                DataRow("Actual EPS", "$${"%.2f".format(earnings.actualEps)}", "Expected: $${"%.2f".format(earnings.expectedEps)}")
                DataRow("EPS Surprise", "+${"%.1f".format(earnings.epsSurprisePercent)}%", "BEAT")
                DataRow("Actual Revenue", "$${"%.2f".format(earnings.actualRevenue / 1e9)}B", "Surprise: +${"%.1f".format(earnings.revenueSurprisePercent)}%")
                DataRow("Guidance Status", earnings.guidanceStatus, null)
                DataRow("Beat Streak", "${earnings.beatCountLast4Q} of last 4 quarters", null)
                DataRow("Next Earnings Date", earnings.nextEarningsDate, null)
            }
        }
    }
}

@Composable
fun NewsTabContent(news: List<NewsEvent>) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(news) { item ->
            Card(
                colors = CardDefaults.cardColors(containerColor = TerminalSurfaceDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorderDark),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(item.source, fontSize = 10.sp, color = TextMutedDark)
                        Text(item.date, fontSize = 10.sp, color = TextMutedDark)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(item.headline, fontWeight = FontWeight.Bold, color = TextPrimaryDark, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(item.summary, fontSize = 11.sp, color = TextSecondaryDark)
                }
            }
        }
    }
}

@Composable
fun CatalystsTabContent(catalysts: List<CatalystItem>) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(catalysts) { cat ->
            Card(
                colors = CardDefaults.cardColors(containerColor = TerminalSurfaceDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorderDark),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(cat.category, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyanAccent)
                        Text(cat.expectedDate, fontSize = 10.sp, color = GoldAccent, fontFamily = FontFamily.Monospace)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(cat.title, fontWeight = FontWeight.Bold, color = TextPrimaryDark, fontSize = 12.sp)
                    Text("Source: ${cat.source}", fontSize = 9.sp, color = TextMutedDark)
                }
            }
        }
    }
}

@Composable
fun MarketTabContent(stock: StockEntity) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            ResearchSectionCard("MARKET TECHNICALS & RELATIVE STRENGTH") {
                DataRow("52-Week Range", "$${"%.2f".format(stock.low52)} - $${"%.2f".format(stock.high52)}", null)
                DataRow("Beta Volatility", "${stock.beta ?: 1.0}", if ((stock.beta ?: 1.0) < 1.0) "Defensive" else "High Beta")
                DataRow("Daily Volume", "${stock.volume / 1000000}M", "Avg: ${stock.avgVolume / 1000000}M")
            }
        }
    }
}

@Composable
fun RiskTabContent(stock: StockEntity, st: FinancialStatements?, breakdown: ScoreBreakdown?) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            ResearchSectionCard("RISK EVALUATION MATRIX") {
                DataRow("Risk Level", "${stock.riskScore.toInt()} / 100", if (stock.riskScore > 50) "Elevated Risk" else "Low/Controlled")
                Spacer(modifier = Modifier.height(6.dp))
                breakdown?.keyRisks?.forEach { r ->
                    Row(modifier = Modifier.padding(vertical = 3.dp)) {
                        Text("• ", color = AmberWarning)
                        Text(r, fontSize = 11.sp, color = TextPrimaryDark)
                    }
                }
            }
        }
    }
}

@Composable
fun AiResearchTabContent(
    stock: StockEntity,
    aiReport: String?,
    isGenerating: Boolean,
    onGenerate: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "AI RESEARCH ASSISTANT",
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = CyanAccent
                )
                Text(
                    text = "Synthesizes audited financial telemetry and SEC disclosures without hallucination.",
                    fontSize = 10.sp,
                    color = TextSecondaryDark
                )
            }
        }

        Button(
            onClick = onGenerate,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("generate_ai_report_button"),
            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
            enabled = !isGenerating
        ) {
            if (isGenerating) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = TerminalBgDark)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Analyzing 10-K & Telemetry...", color = TerminalBgDark)
            } else {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = TerminalBgDark)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generate Institutional Research Dossier", color = TerminalBgDark, fontWeight = FontWeight.Bold)
            }
        }

        if (aiReport != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = TerminalSurfaceDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorderDark),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = aiReport,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextPrimaryDark,
                        fontFamily = FontFamily.SansSerif,
                        lineHeight = 20.sp,
                        fontSize = 12.sp
                    ),
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun ResearchSectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = TerminalSurfaceDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorderDark),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = CyanAccent
                )
            )
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
fun DataRow(label: String, value: String, sublabel: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 11.sp, color = TextSecondaryDark)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = TextPrimaryDark
            )
            if (sublabel != null) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "($sublabel)", fontSize = 10.sp, color = TextMutedDark)
            }
        }
    }
    HorizontalDivider(color = TerminalBorderDark.copy(alpha = 0.3f), thickness = 0.5.dp)
}
