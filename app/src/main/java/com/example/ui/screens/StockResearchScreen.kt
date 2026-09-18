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
    "QUANT ENGINE",
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
    var analysts by remember(symbol) { mutableStateOf(viewModel.repository.getAnalystIntelligence(symbol)) }
    val earnings = remember(symbol) { viewModel.repository.getEarningsData(symbol) }
    val news = remember(symbol) { viewModel.repository.getNews(symbol) }
    val catalysts = remember(symbol) { viewModel.repository.getCatalysts(symbol) }

    val aiReport by viewModel.aiReport.collectAsState()
    val isGeneratingReport by viewModel.isGeneratingReport.collectAsState()
    val selectedHorizon by viewModel.selectedHorizon.collectAsState()

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

    LaunchedEffect(symbol, selectedHorizon) {
        breakdown = viewModel.repository.getScoreBreakdown(symbol, selectedHorizon)
        viewModel.loadChart()
        try {
            val live = viewModel.repository.fetchLiveAnalystIntelligence(symbol)
            analysts = live
        } catch (_: Exception) {}
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
                1 -> {
                    val computedQuant = remember(stock, statements, holdings, insiders, selectedHorizon) {
                        com.example.engine.QuantEngine.calculateQuantScore(
                            stock = stock,
                            financials = statements,
                            institutionalHoldings = holdings,
                            insiderTransactions = insiders,
                            prices = emptyList(),
                            horizon = selectedHorizon
                        )
                    }
                    OverviewTabContent(
                        stock = stock,
                        st = statements,
                        breakdown = breakdown,
                        quantScore = computedQuant,
                        selectedHorizon = selectedHorizon,
                        onSelectHorizon = { viewModel.selectHorizon(it) },
                        onOpenQuantEngine = { viewModel.selectTab(2) }
                    )
                }
                2 -> QuantIntelligenceScreen(
                    stock = stock,
                    financials = statements,
                    institutionalHoldings = holdings,
                    insiderTransactions = insiders,
                    allStocks = viewModel.allStocks.collectAsState().value,
                    onSelectStock = { viewModel.selectStock(it) }
                )
                3 -> FundamentalsTabContent(statements)
                4 -> GrowthTabContent(statements)
                5 -> ValuationTabContent(stock, statements)
                6 -> InstitutionalTabContent(holdings)
                7 -> InsidersTabContent(insiders)
                8 -> AnalystsTabContent(analysts)
                9 -> EarningsTabContent(earnings)
                10 -> NewsTabContent(news)
                11 -> CatalystsTabContent(catalysts)
                12 -> MarketTabContent(stock)
                13 -> RiskTabContent(stock, statements, breakdown)
                14 -> AiResearchTabContent(
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalBgDark)
    ) {
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
            onTriggerAiAnalysis = onTriggerAiAnalysis,
            isAnalyzingChart = isAnalyzingChart,
            chartAiAnalysis = chartAiAnalysis,
            onClearChartAiAnalysis = onClearChartAiAnalysis,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun OverviewTabContent(
    stock: StockEntity,
    st: FinancialStatements?,
    breakdown: ScoreBreakdown?,
    quantScore: com.example.data.model.QuantScore? = null,
    selectedHorizon: AnalysisHorizon = AnalysisHorizon.ONE_YEAR,
    onSelectHorizon: (AnalysisHorizon) -> Unit = {},
    onOpenQuantEngine: () -> Unit = {}
) {
    val activeScore = breakdown?.horizonScores?.get(selectedHorizon) ?: stock.masterScore
    val confidence = breakdown?.confidenceScore ?: ConfidenceScore(80, ConfidenceLevel.HIGH, emptyList())

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 60.dp)
    ) {
        // Dual Engine Showcase Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = TerminalSurfaceDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorderDark),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DUAL INDEPENDENT INTELLIGENCE SCORES",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = TextSecondaryDark
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = CyanAccent.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "STRICT SCORE SEPARATION",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = CyanAccent,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 1. Investment Intelligence Score
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = TerminalSurfaceElevated,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "1. INVESTMENT INTEL",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = EmeraldGreen
                                )
                                Spacer(Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = "${activeScore.toInt()}",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = EmeraldGreen
                                    )
                                    Text(
                                        text = "/100",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = TextSecondaryDark,
                                        modifier = Modifier.padding(bottom = 3.dp, start = 2.dp)
                                    )
                                }
                                Text(
                                    text = "Fundamental & Moat",
                                    fontSize = 9.sp,
                                    color = TextSecondaryDark
                                )
                            }
                        }

                        // 2. Quantitative Intelligence Score
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = TerminalSurfaceElevated,
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onOpenQuantEngine() }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "2. QUANT INTEL",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = CyanAccent
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = "Open Quant Engine",
                                        tint = CyanAccent,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.Bottom) {
                                    val qScore = quantScore?.score?.toInt() ?: (activeScore * 0.92).toInt().coerceIn(30, 95)
                                    Text(
                                        text = "$qScore",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = CyanAccent
                                    )
                                    Text(
                                        text = "/100",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = TextSecondaryDark,
                                        modifier = Modifier.padding(bottom = 3.dp, start = 2.dp)
                                    )
                                }
                                Text(
                                    text = quantScore?.let { "${it.regime.trend} • Inspect →" } ?: "Statistical Math Engine →",
                                    fontSize = 9.sp,
                                    color = CyanAccent
                                )
                            }
                        }
                    }
                }
            }
        }
        // Multi-Horizon Selector Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = TerminalSurfaceDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorderDark),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TIME HORIZON SCORING ENGINE",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = CyanAccent
                            )
                        )
                        Text(
                            text = "Active: ${selectedHorizon.displayName}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent
                        )
                    }

                    Text(
                        text = selectedHorizon.description,
                        fontSize = 11.sp,
                        color = TextSecondaryDark,
                        lineHeight = 16.sp
                    )

                    // 6 Horizon Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AnalysisHorizon.entries.forEach { h ->
                            val isSelected = h == selectedHorizon
                            val hScore = breakdown?.horizonScores?.get(h)?.toInt() ?: stock.masterScore.toInt()
                            val scoreColor = when {
                                hScore >= 80 -> EmeraldGreen
                                hScore >= 70 -> CyanAccent
                                hScore >= 60 -> GoldAccent
                                else -> CrimsonRed
                            }

                            Surface(
                                color = if (isSelected) CyanAccent.copy(alpha = 0.15f) else CardBgDark,
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) CyanAccent else TerminalBorderDark
                                ),
                                modifier = Modifier.clickable { onSelectHorizon(h) }
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = h.shortLabel,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) CyanAccent else TextPrimaryDark
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "$hScore",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = scoreColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Master Score & Confidence Separated Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = TerminalSurfaceDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "QUANTITATIVE MASTER SCORE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = CyanAccent
                            )
                            Spacer(Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${activeScore.toInt()}",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (activeScore >= 80) EmeraldGreen else if (activeScore >= 60) GoldAccent else CrimsonRed
                                )
                                Text(
                                    text = "/100",
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextSecondaryDark
                                )
                                Spacer(Modifier.width(10.dp))
                                ClassificationBadge(InvestmentClassification.fromScore(activeScore).label)
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "CONFIDENCE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = TextSecondaryDark
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "${confidence.percentage}%",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (confidence.percentage >= 80) EmeraldGreen else if (confidence.percentage >= 50) GoldAccent else CrimsonRed
                            )
                            Text(
                                text = confidence.level.name,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondaryDark
                            )
                        }
                    }

                    HorizontalDivider(color = TerminalBorderDark.copy(alpha = 0.5f), thickness = 0.5.dp)

                    // Data Completeness & Verification Audit Checklist
                    Text(
                        text = "DATA VERIFICATION & COMPLETENESS AUDIT",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondaryDark
                    )

                    confidence.completenessFactors.forEach { (factorName, isVerified) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = factorName,
                                fontSize = 11.sp,
                                color = if (isVerified) TextPrimaryDark else TextSecondaryDark
                            )
                            Text(
                                text = if (isVerified) "VERIFIED" else "MISSING",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (isVerified) EmeraldGreen else CrimsonRed
                            )
                        }
                    }
                }
            }
        }

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
                            text = "Score: ${activeScore.toInt()}/100",
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
                DataRow("Target Mean", analysts.targetMean?.let { "$${"%.2f".format(it)}" } ?: "N/A", null)
                DataRow("Target High", analysts.targetHigh?.let { "$${"%.2f".format(it)}" } ?: "N/A", null)
                DataRow("Target Low", analysts.targetLow?.let { "$${"%.2f".format(it)}" } ?: "N/A", null)
                analysts.targetMedian?.let {
                    DataRow("Target Median", "$${"%.2f".format(it)}", null)
                }
                DataRow("Analyst Count", "${analysts.numberOfAnalysts} firms", null)
                DataRow(
                    "Momentum Score",
                    analysts.momentumScore?.let { "${it.toInt()}/100" } ?: "N/A",
                    "+${analysts.upgradesCount90d} up / -${analysts.downgradesCount90d} down (90d)"
                )
                if (analysts.asOfDate.isNotEmpty()) {
                    DataRow("As Of Period", analysts.asOfDate, analysts.freshness.label)
                }
                if (analysts.provider.isNotEmpty()) {
                    DataRow("Feed Provider", analysts.provider, null)
                }
            }
        }
        analysts.statusMessage?.let { msg ->
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CyanAccent.copy(alpha = 0.1f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                        Text(msg, fontSize = 10.sp, color = TextPrimaryDark, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("RECENT FIRM ACTIONS (<= 90 DAYS)", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = CyanAccent)
                Text("${analysts.recentActions.size} actions", fontSize = 10.sp, color = TextMutedDark)
            }
        }
        if (analysts.recentActions.isEmpty()) {
            item {
                Text("No rating changes or revisions in the last 90 days", fontSize = 11.sp, color = TextMutedDark)
            }
        } else {
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
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(act.firm, fontWeight = FontWeight.Bold, color = TextPrimaryDark, fontSize = 12.sp)
                                if (act.freshness == DataFreshness.LIVE) {
                                    Surface(
                                        color = EmeraldGreen.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text("LIVE", color = EmeraldGreen, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                    }
                                }
                            }
                            Text("${act.actionType}: ${act.toRating}", fontSize = 11.sp, color = CyanAccent)
                            Text("Date: ${act.date} • ${act.provider}", fontSize = 9.sp, color = TextMutedDark)
                        }
                        act.targetPrice?.let {
                            Text(
                                text = "Target: $${"%.2f".format(it)}",
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
        if (analysts.historicalActions.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("HISTORICAL ACTIONS (> 90 DAYS - STALE)", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = GoldAccent)
                    Surface(
                        color = CrimsonRed.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("STALE", color = CrimsonRed, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                    }
                }
            }
            items(analysts.historicalActions) { act ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = TerminalSurfaceDark.copy(alpha = 0.6f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorderDark.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(act.firm, fontWeight = FontWeight.SemiBold, color = TextSecondaryDark, fontSize = 11.sp)
                            Text("${act.actionType}: ${act.toRating}", fontSize = 10.sp, color = TextMutedDark)
                            Text("Date: ${act.date}", fontSize = 9.sp, color = TextMutedDark)
                        }
                        act.targetPrice?.let {
                            Text(
                                text = "$${"%.2f".format(it)}",
                                fontFamily = FontFamily.Monospace,
                                color = TextMutedDark,
                                fontSize = 11.sp
                            )
                        }
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
