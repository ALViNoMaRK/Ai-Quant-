package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.remote.ProviderHealth
import com.example.data.repository.StockRepository
import com.example.engine.DeteriorationSignal
import com.example.engine.OpportunitySignal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ChatMessage(
    val sender: String, // "USER" or "AI"
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

class StockIntelViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    val repository = StockRepository(database)

    val allStocks = repository.allStocksFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val watchlist = repository.watchlistFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val portfolio = repository.portfolioFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val alerts = repository.alertsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val customIndicators = repository.customIndicatorsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val chartTemplates = repository.chartTemplatesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val macroData = repository.macroData
    val providerHealths = repository.providerHealths
    val currentModel = repository.currentModel
    val customWeights = repository.customWeights

    // Charting Workspace State
    private val _chartTimeframe = MutableStateFlow(ChartTimeframe.D1)
    val chartTimeframe: StateFlow<ChartTimeframe> = _chartTimeframe.asStateFlow()

    private val _chartPeriod = MutableStateFlow(ChartPeriod.M3)
    val chartPeriod: StateFlow<ChartPeriod> = _chartPeriod.asStateFlow()

    private val _chartType = MutableStateFlow(ChartType.CANDLESTICK)
    val chartType: StateFlow<ChartType> = _chartType.asStateFlow()

    private val _chartPayload = MutableStateFlow<ChartPayload?>(null)
    val chartPayload: StateFlow<ChartPayload?> = _chartPayload.asStateFlow()

    private val _isChartLoading = MutableStateFlow(false)
    val isChartLoading: StateFlow<Boolean> = _isChartLoading.asStateFlow()

    private val _chartDrawings = MutableStateFlow<List<ChartDrawing>>(emptyList())
    val chartDrawings: StateFlow<List<ChartDrawing>> = _chartDrawings.asStateFlow()

    private val _showEventMarkers = MutableStateFlow(true)
    val showEventMarkers: StateFlow<Boolean> = _showEventMarkers.asStateFlow()

    private val _eventMarkers = MutableStateFlow<List<ChartEventMarker>>(emptyList())
    val eventMarkers: StateFlow<List<ChartEventMarker>> = _eventMarkers.asStateFlow()

    private val _activeIndicators = MutableStateFlow<List<com.example.engine.technical.ActiveIndicator>>(emptyList())
    val activeIndicators: StateFlow<List<com.example.engine.technical.ActiveIndicator>> = _activeIndicators.asStateFlow()

    private val _activePineResult = MutableStateFlow<com.example.engine.pine.PineExecutionResult?>(null)
    val activePineResult: StateFlow<com.example.engine.pine.PineExecutionResult?> = _activePineResult.asStateFlow()

    private val pineParser = com.example.engine.pine.PineParser()
    private val pineSandbox = com.example.engine.pine.IndicatorSandbox()

    private val _opportunities = MutableStateFlow<List<OpportunitySignal>>(emptyList())
    val opportunities: StateFlow<List<OpportunitySignal>> = _opportunities.asStateFlow()

    private val _deteriorating = MutableStateFlow<List<DeteriorationSignal>>(emptyList())
    val deteriorating: StateFlow<List<DeteriorationSignal>> = _deteriorating.asStateFlow()

    private val _selectedStockSymbol = MutableStateFlow<String?>("NVDA")
    val selectedStockSymbol: StateFlow<String?> = _selectedStockSymbol.asStateFlow()

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _aiReport = MutableStateFlow<String?>(null)
    val aiReport: StateFlow<String?> = _aiReport.asStateFlow()

    private val _isGeneratingReport = MutableStateFlow(false)
    val isGeneratingReport: StateFlow<Boolean> = _isGeneratingReport.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                sender = "AI",
                message = "Welcome to Institutional Stock Intelligence. Ask any question regarding stock rankings, fundamental drivers, 13F institutional accumulation, insider filings, or thesis deterioration."
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isResolvingStock = MutableStateFlow(false)
    val isResolvingStock: StateFlow<Boolean> = _isResolvingStock.asStateFlow()

    private val _resolveStockError = MutableStateFlow<String?>(null)
    val resolveStockError: StateFlow<String?> = _resolveStockError.asStateFlow()

    private val _selectedHorizon = MutableStateFlow(AnalysisHorizon.ONE_YEAR)
    val selectedHorizon: StateFlow<AnalysisHorizon> = _selectedHorizon.asStateFlow()

    fun selectHorizon(horizon: AnalysisHorizon) {
        _selectedHorizon.value = horizon
    }

    init {
        viewModelScope.launch {
            repository.initializeUniverseIfEmpty()
            refreshSignals()
            loadChart()
        }
        viewModelScope.launch(Dispatchers.IO) {
            repository.checkAllProviderHealth()
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        _resolveStockError.value = null
    }

    fun resolveStock(query: String, onResolved: ((String) -> Unit)? = null) {
        val sym = query.trim().uppercase()
        if (sym.isBlank()) return
        viewModelScope.launch {
            _isResolvingStock.value = true
            _resolveStockError.value = null
            val result = repository.resolveAndInsertStock(sym)
            _isResolvingStock.value = false
            result.onSuccess { stock ->
                selectStock(stock.symbol)
                onResolved?.invoke(stock.symbol)
            }.onFailure { err ->
                _resolveStockError.value = err.message ?: "Failed to resolve '$sym' from connected financial APIs."
            }
        }
    }

    fun selectStock(symbol: String) {
        _selectedStockSymbol.value = symbol
        _selectedTab.value = 0
        _aiReport.value = null
        loadChart()
    }

    fun selectTab(index: Int) {
        _selectedTab.value = index
        if (index == 1) { // 1 is CHART tab
            loadChart()
        }
    }

    fun loadChart() {
        val sym = _selectedStockSymbol.value ?: "NVDA"
        val tf = _chartTimeframe.value
        val pr = _chartPeriod.value

        viewModelScope.launch {
            _isChartLoading.value = true
            val res = repository.getChartCandles(sym, tf.intervalParam, pr.rangeParam)
            res.onSuccess { payload ->
                _chartPayload.value = payload
                val lastPrice = payload.candles.lastOrNull()?.close ?: 150.0
                _eventMarkers.value = repository.getChartEventMarkers(sym, lastPrice)
                // Execute Pine script if active
                recomputeActivePine()
            }
            _isChartLoading.value = false
        }
    }

    fun setTimeframe(tf: ChartTimeframe) {
        _chartTimeframe.value = tf
        loadChart()
    }

    fun setPeriod(p: ChartPeriod) {
        _chartPeriod.value = p
        loadChart()
    }

    fun setChartType(type: ChartType) {
        _chartType.value = type
    }

    fun toggleEventMarkers() {
        _showEventMarkers.value = !_showEventMarkers.value
    }

    fun toggleIndicatorVisibility(id: String) {
        _activeIndicators.value = _activeIndicators.value.map {
            if (it.id == id) it.copy(isVisible = !it.isVisible) else it
        }
    }

    fun removeIndicator(id: String) {
        _activeIndicators.value = _activeIndicators.value.filter { it.id != id }
    }

    fun clearAllIndicators() {
        _activeIndicators.value = emptyList()
    }

    private val indicatorPalette = listOf(
        0xFF06B6D4, // Aqua
        0xFFF59E0B, // Gold
        0xFF38BDF8, // Sky Blue
        0xFFA855F7, // Purple
        0xFF10B981, // Emerald
        0xFFEC4899, // Pink
        0xFFEAB308, // Yellow
        0xFF8B5CF6, // Violet
        0xFF3B82F6  // Blue
    )

    fun addIndicator(
        type: com.example.engine.technical.BuiltInIndicatorType,
        customParams: Map<String, String>? = null,
        customColor: Long? = null
    ) {
        val totalCount = _activeIndicators.value.size
        val sameTypeCount = _activeIndicators.value.count { it.type == type }
        val id = "${type.name.lowercase()}_${System.currentTimeMillis() % 100000}_${sameTypeCount + 1}"
        val params = customParams ?: type.defaultParams

        val title = when (type) {
            com.example.engine.technical.BuiltInIndicatorType.MOVING_AVERAGE -> {
                val method = params["method"] ?: "EMA"
                val period = params["period"] ?: "20"
                "$method $period"
            }
            com.example.engine.technical.BuiltInIndicatorType.EMA -> "EMA ${params["period"] ?: params["length"] ?: "200"}"
            com.example.engine.technical.BuiltInIndicatorType.SMA -> "SMA ${params["period"] ?: params["length"] ?: "50"}"
            com.example.engine.technical.BuiltInIndicatorType.SMMA -> "SMMA ${params["period"] ?: params["length"] ?: "20"}"
            com.example.engine.technical.BuiltInIndicatorType.LWMA -> "LWMA ${params["period"] ?: params["length"] ?: "20"}"
            com.example.engine.technical.BuiltInIndicatorType.BOLLINGER_BANDS -> "BB (${params["period"] ?: "20"}, ${params["stdDev"] ?: "2.0"})"
            com.example.engine.technical.BuiltInIndicatorType.RSI -> "RSI ${params["period"] ?: params["length"] ?: "14"}"
            com.example.engine.technical.BuiltInIndicatorType.MACD -> "MACD (${params["fast"] ?: "12"}, ${params["slow"] ?: "26"}, ${params["signal"] ?: "9"})"
            com.example.engine.technical.BuiltInIndicatorType.STOCHASTIC -> "Stoch (${params["periodK"] ?: "14"}, ${params["smoothK"] ?: "3"})"
            com.example.engine.technical.BuiltInIndicatorType.ADX -> "ADX ${params["period"] ?: "14"}"
            com.example.engine.technical.BuiltInIndicatorType.CCI -> "CCI ${params["period"] ?: "14"}"
            com.example.engine.technical.BuiltInIndicatorType.WILLIAMS_R -> "%R ${params["period"] ?: "14"}"
            com.example.engine.technical.BuiltInIndicatorType.MOMENTUM -> "Mom ${params["period"] ?: "14"}"
            com.example.engine.technical.BuiltInIndicatorType.STANDARD_DEVIATION -> "StdDev ${params["period"] ?: "20"}"
            com.example.engine.technical.BuiltInIndicatorType.ATR -> "ATR ${params["period"] ?: "14"}"
            else -> type.displayName
        }

        val assignedColor = customColor ?: indicatorPalette[totalCount % indicatorPalette.size]

        val newInd = com.example.engine.technical.ActiveIndicator(
            id = id,
            type = type,
            title = title,
            params = params,
            color = assignedColor
        )
        _activeIndicators.value = _activeIndicators.value + newInd
    }

    fun updateIndicatorParams(id: String, params: Map<String, String>, color: Long? = null) {
        _activeIndicators.value = _activeIndicators.value.map {
            if (it.id == id) {
                val updatedColor = color ?: it.color
                val updatedTitle = when (it.type) {
                    com.example.engine.technical.BuiltInIndicatorType.MOVING_AVERAGE -> {
                        val method = params["method"] ?: "EMA"
                        val period = params["period"] ?: "20"
                        "$method $period"
                    }
                    com.example.engine.technical.BuiltInIndicatorType.EMA -> "EMA ${params["period"] ?: params["length"] ?: "200"}"
                    com.example.engine.technical.BuiltInIndicatorType.SMA -> "SMA ${params["period"] ?: params["length"] ?: "50"}"
                    com.example.engine.technical.BuiltInIndicatorType.SMMA -> "SMMA ${params["period"] ?: params["length"] ?: "20"}"
                    com.example.engine.technical.BuiltInIndicatorType.LWMA -> "LWMA ${params["period"] ?: params["length"] ?: "20"}"
                    com.example.engine.technical.BuiltInIndicatorType.BOLLINGER_BANDS -> "BB (${params["period"] ?: "20"}, ${params["stdDev"] ?: "2.0"})"
                    com.example.engine.technical.BuiltInIndicatorType.RSI -> "RSI ${params["period"] ?: params["length"] ?: "14"}"
                    else -> it.title
                }
                it.copy(params = params, color = updatedColor, title = updatedTitle)
            } else it
        }
    }

    private var currentPineCode: String? = null

    private val _isAnalyzingChart = MutableStateFlow(false)
    val isAnalyzingChart: StateFlow<Boolean> = _isAnalyzingChart.asStateFlow()

    private val _chartAiAnalysis = MutableStateFlow<String?>(null)
    val chartAiAnalysis: StateFlow<String?> = _chartAiAnalysis.asStateFlow()

    fun clearChartAiAnalysis() {
        _chartAiAnalysis.value = null
    }

    fun compileAndApplyPineScript(code: String): com.example.engine.pine.PineCompilationResult {
        currentPineCode = code
        val comp = pineParser.parse(code)
        if (comp.success) {
            viewModelScope.launch(Dispatchers.Default) {
                val candles = _chartPayload.value?.candles ?: emptyList()
                val exec = pineSandbox.execute(comp, candles)
                _activePineResult.value = exec
            }
        }
        return comp
    }

    fun clearActivePine() {
        currentPineCode = null
        _activePineResult.value = null
    }

    private fun recomputeActivePine() {
        val code = currentPineCode ?: return
        val comp = pineParser.parse(code)
        if (comp.success) {
            viewModelScope.launch(Dispatchers.Default) {
                val candles = _chartPayload.value?.candles ?: emptyList()
                _activePineResult.value = pineSandbox.execute(comp, candles)
            }
        }
    }

    fun analyzeActiveChartWithAi() {
        val sym = _selectedStockSymbol.value ?: "NVDA"
        val payload = _chartPayload.value
        val candles = payload?.candles ?: emptyList()
        if (candles.isEmpty()) {
            _chartAiAnalysis.value = "Unable to perform chart analysis: No price candle telemetry available for $sym."
            return
        }

        viewModelScope.launch {
            _isAnalyzingChart.value = true
            val telemetryContext = buildChartTelemetryContext(sym, candles)
            val res = repository.analyzeChartWithIndicators(telemetryContext)
            _chartAiAnalysis.value = res.getOrElse { "Chart analysis error: ${it.localizedMessage}" }
            _isAnalyzingChart.value = false
        }
    }

    private fun buildChartTelemetryContext(sym: String, candles: List<CandleData>): String {
        val lastIdx = candles.size - 1
        if (lastIdx < 0) return "No candle data available."
        val currentCandle = candles[lastIdx]
        val prevCandle = if (lastIdx >= 1) candles[lastIdx - 1] else currentCandle
        val prev2Candle = if (lastIdx >= 2) candles[lastIdx - 2] else prevCandle

        val sb = StringBuilder()
        sb.appendLine("TICKER: $sym")
        sb.appendLine("TIMEFRAME: ${_chartTimeframe.value.label} | PERIOD: ${_chartPeriod.value.label}")
        val pctChange = if (currentCandle.open > 0) ((currentCandle.close - currentCandle.open) / currentCandle.open) * 100 else 0.0
        sb.appendLine("CURRENT PRICE: $${String.format(java.util.Locale.US, "%.2f", currentCandle.close)} (Bar Change: ${String.format(java.util.Locale.US, "%.2f", pctChange)}%)")
        sb.appendLine()
        sb.appendLine("RECENT CANDLE TELEMETRY:")
        sb.appendLine("- Bar[0] (Current Bar): Open=${String.format(java.util.Locale.US, "%.2f", currentCandle.open)}, High=${String.format(java.util.Locale.US, "%.2f", currentCandle.high)}, Low=${String.format(java.util.Locale.US, "%.2f", currentCandle.low)}, Close=${String.format(java.util.Locale.US, "%.2f", currentCandle.close)}, Vol=${String.format(java.util.Locale.US, "%.2fM", currentCandle.volume / 1_000_000.0)}")
        if (lastIdx >= 1) {
            sb.appendLine("- Bar[1] (Prior Bar): Open=${String.format(java.util.Locale.US, "%.2f", prevCandle.open)}, High=${String.format(java.util.Locale.US, "%.2f", prevCandle.high)}, Low=${String.format(java.util.Locale.US, "%.2f", prevCandle.low)}, Close=${String.format(java.util.Locale.US, "%.2f", prevCandle.close)}, Vol=${String.format(java.util.Locale.US, "%.2fM", prevCandle.volume / 1_000_000.0)}")
        }
        if (lastIdx >= 2) {
            sb.appendLine("- Bar[2] (2 Bars Ago): Open=${String.format(java.util.Locale.US, "%.2f", prev2Candle.open)}, High=${String.format(java.util.Locale.US, "%.2f", prev2Candle.high)}, Low=${String.format(java.util.Locale.US, "%.2f", prev2Candle.low)}, Close=${String.format(java.util.Locale.US, "%.2f", prev2Candle.close)}, Vol=${String.format(java.util.Locale.US, "%.2fM", prev2Candle.volume / 1_000_000.0)}")
        }

        // Active Standard Technical Indicators
        val activeInds = _activeIndicators.value
        sb.appendLine()
        sb.appendLine("ACTIVE TECHNICAL INDICATORS (Computed on live bars):")
        if (activeInds.isEmpty()) {
            sb.appendLine("- No standard technical indicators currently active on chart.")
        } else {
            for (ind in activeInds) {
                if (!ind.isVisible) continue
                val res = com.example.engine.technical.TechnicalIndicatorEngine.calculate(ind, candles)
                val p0 = res.primaryValues.getOrNull(lastIdx)?.let { String.format(java.util.Locale.US, "%.2f", it) } ?: "N/A"
                val p1 = res.primaryValues.getOrNull(lastIdx - 1)?.let { String.format(java.util.Locale.US, "%.2f", it) } ?: "N/A"
                val p2 = res.primaryValues.getOrNull(lastIdx - 2)?.let { String.format(java.util.Locale.US, "%.2f", it) } ?: "N/A"

                val sec0 = res.secondaryValues?.getOrNull(lastIdx)?.let { String.format(java.util.Locale.US, "%.2f", it) }
                val tert0 = res.tertiaryValues?.getOrNull(lastIdx)?.let { String.format(java.util.Locale.US, "%.2f", it) }

                when (ind.type) {
                    com.example.engine.technical.BuiltInIndicatorType.BOLLINGER_BANDS -> {
                        sb.appendLine("- ${ind.title}: Upper=$p0, Middle=$sec0, Lower=$tert0 | Prior Upper=$p1")
                    }
                    com.example.engine.technical.BuiltInIndicatorType.MACD -> {
                        sb.appendLine("- ${ind.title}: MACD Line=$p0, Signal Line=$sec0, Histogram=$tert0 | Prior MACD=$p1")
                    }
                    com.example.engine.technical.BuiltInIndicatorType.STOCHASTIC -> {
                        sb.appendLine("- ${ind.title}: %K=$p0, %D=$sec0 | Prior %K=$p1")
                    }
                    com.example.engine.technical.BuiltInIndicatorType.RSI -> {
                        sb.appendLine("- ${ind.title}: Current [0]=$p0 | Bar[1]=$p1 | Bar[2]=$p2 (Overbought: 70, Oversold: 30)")
                    }
                    else -> {
                        sb.appendLine("- ${ind.title}: Current [0]=$p0 | Bar[1]=$p1 | Bar[2]=$p2")
                    }
                }
            }
        }

        // Custom Pine Script Indicator Engine Outputs
        val pineResult = _activePineResult.value
        sb.appendLine()
        sb.appendLine("PINE SCRIPT INDICATOR ENGINE OUTPUT:")
        if (pineResult != null) {
            sb.appendLine("- Script Title: '${pineResult.title}' (Overlay: ${pineResult.isOverlay})")
            for (plot in pineResult.plots) {
                val v0 = plot.series.getOrNull(lastIdx)?.let { String.format(java.util.Locale.US, "%.2f", it) } ?: "N/A"
                val v1 = plot.series.getOrNull(lastIdx - 1)?.let { String.format(java.util.Locale.US, "%.2f", it) } ?: "N/A"
                val v2 = plot.series.getOrNull(lastIdx - 2)?.let { String.format(java.util.Locale.US, "%.2f", it) } ?: "N/A"
                sb.appendLine("  * Plot '${plot.title}': Current [0]=$v0, Bar[1]=$v1, Bar[2]=$v2")
            }
            if (pineResult.shapes.isNotEmpty()) {
                sb.appendLine("  * Script Signals:")
                for (s in pineResult.shapes) {
                    val activeRecent = (maxOf(0, lastIdx - 10)..lastIdx).filter { idx ->
                        s.condition.getOrNull(idx) == true
                    }
                    if (activeRecent.isNotEmpty()) {
                        for (idx in activeRecent) {
                            val barsAgo = lastIdx - idx
                            val price = candles.getOrNull(idx)?.close ?: 0.0
                            sb.appendLine("    - Signal [-$barsAgo bars]: ${s.title} (${if (s.isBuy) "BUY" else "SELL"}) at $${String.format(java.util.Locale.US, "%.2f", price)}")
                        }
                    }
                }
            }
        } else {
            sb.appendLine("- No custom Pine Script indicator currently applied to chart.")
        }

        // User Chart Drawings
        val drawings = _chartDrawings.value.filter { it.symbol == sym }
        if (drawings.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("USER CHART DRAWINGS & ANNOTATED LEVELS:")
            for (d in drawings) {
                sb.appendLine("- ${d.type.label}: Level 1 = $${String.format(java.util.Locale.US, "%.2f", d.price1)}${if (d.price2 != d.price1) ", Level 2 = $${String.format(java.util.Locale.US, "%.2f", d.price2)}" else ""} (${d.label.ifBlank { "User Annotation" }})")
            }
        }

        return sb.toString()
    }

    fun saveCustomIndicator(name: String, code: String, isOverlay: Boolean, desc: String) {
        viewModelScope.launch {
            val id = name.lowercase().replace(" ", "_").filter { it.isLetterOrDigit() || it == '_' }
            val entity = CustomIndicatorEntity(
                id = id,
                name = name,
                description = desc,
                code = code,
                version = "//@version=6",
                isOverlay = isOverlay
            )
            repository.saveCustomIndicator(entity)
        }
    }

    fun deleteCustomIndicator(id: String) {
        viewModelScope.launch {
            repository.deleteCustomIndicator(id)
        }
    }

    fun addChartDrawing(type: DrawingToolType, p1: Double, p2: Double = p1, label: String = "") {
        val sym = _selectedStockSymbol.value ?: "NVDA"
        val drawing = ChartDrawing(
            id = "draw_${System.currentTimeMillis()}",
            symbol = sym,
            type = type,
            price1 = p1,
            price2 = p2,
            label = label
        )
        _chartDrawings.value = _chartDrawings.value + drawing
    }

    fun removeDrawing(id: String) {
        _chartDrawings.value = _chartDrawings.value.filter { it.id != id }
    }

    fun clearDrawings() {
        _chartDrawings.value = emptyList()
    }

    fun generatePineIndicatorWithAi(userPrompt: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val prompt = """
                You are a quantitative Pine Script engineer.
                Write valid, sandboxed Pine Script (version 6 compatible) for the following request:
                "$userPrompt"
                
                Rules:
                - Start with //@version=6
                - indicator("Title", overlay = true/false)
                - Use valid functions: ta.sma, ta.ema, ta.wma, ta.rsi, ta.macd, ta.atr, plot, hline, plotshape
                - Output ONLY the Pine Script code, no extra commentary or markdown formatting.
            """.trimIndent()
            val aiResp = repository.askAiAssistant(prompt).getOrNull()
            if (aiResp != null) {
                val clean = aiResp.replace("```pine", "").replace("```pinescript", "").replace("```", "").trim()
                onResult(clean)
            } else {
                // High-quality fallback template based on prompt keywords
                val fallback = if (userPrompt.contains("rsi", ignoreCase = true)) {
                    com.example.engine.pine.PrebuiltPineScripts.RSI_EXTREMES
                } else if (userPrompt.contains("squeeze", ignoreCase = true) || userPrompt.contains("bollinger", ignoreCase = true)) {
                    com.example.engine.pine.PrebuiltPineScripts.BOLLINGER_SQUEEZE
                } else {
                    com.example.engine.pine.PrebuiltPineScripts.TRIPLE_EMA
                }
                onResult(fallback)
            }
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            _isRefreshing.value = true
            repository.refreshLiveQuotes()
            refreshSignals()
            _isRefreshing.value = false
        }
    }

    private suspend fun refreshSignals() {
        _opportunities.value = repository.getOpportunities()
        _deteriorating.value = repository.getDeteriorationSignals()
    }

    fun switchModel(model: InvestmentModel) {
        viewModelScope.launch {
            repository.setInvestmentModel(model)
            repository.recalculateAndSaveAllScores()
            refreshSignals()
        }
    }

    fun updateWeights(weights: ScoringWeights) {
        viewModelScope.launch {
            repository.updateCustomWeights(weights)
            repository.recalculateAndSaveAllScores()
            refreshSignals()
        }
    }

    fun toggleWatchlist(symbol: String) {
        viewModelScope.launch {
            repository.toggleWatchlist(symbol)
        }
    }

    fun removeFromWatchlist(symbol: String) {
        viewModelScope.launch {
            repository.removeFromWatchlist(symbol)
        }
    }

    fun addPortfolioPosition(symbol: String, shares: Double, buyPrice: Double, targetPrice: Double? = null) {
        viewModelScope.launch {
            repository.upsertPortfolioPosition(symbol, shares, buyPrice, targetPrice)
        }
    }

    fun removePortfolioPosition(symbol: String) {
        viewModelScope.launch {
            repository.removePortfolioPosition(symbol)
        }
    }

    fun markAlertRead(id: Int) {
        viewModelScope.launch {
            repository.markAlertRead(id)
        }
    }

    fun dismissAlert(id: Int) {
        viewModelScope.launch {
            repository.dismissAlert(id)
        }
    }

    fun clearAllAlerts() {
        viewModelScope.launch {
            repository.clearAllAlerts()
        }
    }

    fun generateAiReportForSelected() {
        val sym = _selectedStockSymbol.value ?: return
        viewModelScope.launch {
            _isGeneratingReport.value = true
            val res = repository.generateAiReport(sym)
            _aiReport.value = res.getOrElse { "Report generation error: ${it.localizedMessage}" }
            _isGeneratingReport.value = false
        }
    }

    fun sendChatMessage(text: String) {
        if (text.isBlank()) return
        val userMsg = ChatMessage("USER", text)
        val historyPairs = _chatMessages.value.map { Pair(it.sender, it.message) }
        _chatMessages.value = _chatMessages.value + userMsg

        viewModelScope.launch {
            _isChatLoading.value = true
            try {
                val res = repository.askAiAssistant(text, historyPairs)
                val aiMsg = ChatMessage("AI", res.getOrElse { "AI response error: ${it.localizedMessage}" })
                _chatMessages.value = _chatMessages.value + aiMsg
            } finally {
                _isChatLoading.value = false
            }
        }
    }

    fun clearChatHistory() {
        _chatMessages.value = listOf(
            ChatMessage(
                sender = "AI",
                message = "Chat history cleared. How can I assist your quantitative analysis today?"
            )
        )
    }
}
