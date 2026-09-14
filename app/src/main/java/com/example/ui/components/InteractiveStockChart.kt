package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.*
import com.example.engine.pine.PineExecutionResult
import com.example.engine.technical.ActiveIndicator
import com.example.engine.technical.CalculatedSeriesResult
import com.example.engine.technical.TechnicalIndicatorEngine
import com.example.ui.theme.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.abs

@Composable
fun InteractiveStockChart(
    payload: ChartPayload?,
    isLoading: Boolean,
    currentTimeframe: ChartTimeframe,
    currentPeriod: ChartPeriod,
    chartType: ChartType,
    activeIndicators: List<ActiveIndicator>,
    activePineResult: PineExecutionResult?,
    eventMarkers: List<ChartEventMarker>,
    showEventMarkers: Boolean,
    drawings: List<ChartDrawing>,
    onTimeframeSelected: (ChartTimeframe) -> Unit,
    onPeriodSelected: (ChartPeriod) -> Unit,
    onChartTypeSelected: (ChartType) -> Unit,
    onToggleIndicatorVisibility: (String) -> Unit,
    onRemoveIndicator: (String) -> Unit,
    onOpenIndicatorSettings: (ActiveIndicator) -> Unit,
    onOpenAddIndicator: () -> Unit,
    onOpenPineEditor: () -> Unit,
    onAddDrawing: (DrawingToolType, Double) -> Unit,
    onClearDrawings: () -> Unit,
    onToggleEventMarkers: () -> Unit,
    onTriggerAiAnalysis: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isFullscreen by remember { mutableStateOf(false) }

    if (isFullscreen) {
        Dialog(
            onDismissRequest = { isFullscreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(TerminalBgDark)
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                ChartWorkspaceContent(
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
                    isFullscreen = true,
                    onToggleFullscreen = { isFullscreen = false },
                    onTriggerAiAnalysis = onTriggerAiAnalysis
                )
            }
        }
    } else {
        ChartWorkspaceContent(
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
            isFullscreen = false,
            onToggleFullscreen = { isFullscreen = true },
            onTriggerAiAnalysis = onTriggerAiAnalysis,
            modifier = modifier
        )
    }
}

@Composable
private fun ChartWorkspaceContent(
    payload: ChartPayload?,
    isLoading: Boolean,
    currentTimeframe: ChartTimeframe,
    currentPeriod: ChartPeriod,
    chartType: ChartType,
    activeIndicators: List<ActiveIndicator>,
    activePineResult: PineExecutionResult?,
    eventMarkers: List<ChartEventMarker>,
    showEventMarkers: Boolean,
    drawings: List<ChartDrawing>,
    onTimeframeSelected: (ChartTimeframe) -> Unit,
    onPeriodSelected: (ChartPeriod) -> Unit,
    onChartTypeSelected: (ChartType) -> Unit,
    onToggleIndicatorVisibility: (String) -> Unit,
    onRemoveIndicator: (String) -> Unit,
    onOpenIndicatorSettings: (ActiveIndicator) -> Unit,
    onOpenAddIndicator: () -> Unit,
    onOpenPineEditor: () -> Unit,
    onAddDrawing: (DrawingToolType, Double) -> Unit,
    onClearDrawings: () -> Unit,
    onToggleEventMarkers: () -> Unit,
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    onTriggerAiAnalysis: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedEvent by remember { mutableStateOf<ChartEventMarker?>(null) }
    var crosshairCandle by remember { mutableStateOf<CandleData?>(null) }
    var candleVisibleCount by remember { mutableStateOf(50) }
    var scrollOffset by remember { mutableStateOf(0) }
    var panSubCandleAccumulator by remember { mutableFloatStateOf(0f) }
    var selectedDrawingTool by remember { mutableStateOf(DrawingToolType.NONE) }

    LaunchedEffect(payload?.symbol, currentTimeframe, currentPeriod) {
        scrollOffset = 0
        panSubCandleAccumulator = 0f
    }

    val candles = payload?.candles ?: emptyList()

    // Compute active indicators data
    val calculatedOverlays = remember(candles, activeIndicators) {
        activeIndicators.filter { it.isVisible && it.isOverlay }.map {
            TechnicalIndicatorEngine.calculate(it, candles)
        }
    }

    val calculatedPanes = remember(candles, activeIndicators) {
        activeIndicators.filter { it.isVisible && !it.isOverlay }.map {
            TechnicalIndicatorEngine.calculate(it, candles)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(TerminalBgDark)
    ) {
        // 1. Top Workspace Header Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceDark)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left controls: Indicators, Pine Editor, AI Reason, Drawings, Events
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Indicators Button
                FilledTonalButton(
                    onClick = onOpenAddIndicator,
                    modifier = Modifier.height(32.dp).testTag("add_indicator_button"),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = CardBgDark)
                ) {
                    Icon(Icons.Default.AddChart, contentDescription = null, modifier = Modifier.size(16.dp), tint = TerminalAccent)
                    Spacer(Modifier.width(4.dp))
                    Text("INDICATORS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                }

                // Pine Editor Button
                OutlinedButton(
                    onClick = onOpenPineEditor,
                    modifier = Modifier.height(32.dp).testTag("pine_editor_button"),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (activePineResult != null) TerminalGreen else TerminalBorder)
                ) {
                    Text(
                        if (activePineResult != null) "PINE: ${activePineResult.title.take(8)}●" else "{ } PINE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (activePineResult != null) TerminalGreen else TextSecondaryDark,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // AI Chart Reasoner Button
                if (onTriggerAiAnalysis != null) {
                    FilledTonalButton(
                        onClick = onTriggerAiAnalysis,
                        modifier = Modifier.height(32.dp).testTag("ai_chart_analysis_button"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = CyanAccent.copy(alpha = 0.2f))
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp), tint = CyanAccent)
                        Spacer(Modifier.width(4.dp))
                        Text("AI REASON", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyanAccent)
                    }
                }

                // Event markers toggle
                IconButton(
                    onClick = onToggleEventMarkers,
                    modifier = Modifier.size(32.dp).testTag("events_toggle_button")
                ) {
                    Icon(
                        Icons.Default.Event,
                        contentDescription = "Toggle Events",
                        tint = if (showEventMarkers) TerminalGreen else TextSecondaryDark,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Horizontal line drawing tool
                IconButton(
                    onClick = {
                        val lastClose = candles.lastOrNull()?.close ?: 150.0
                        onAddDrawing(DrawingToolType.HORIZONTAL_LINE, lastClose)
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.HorizontalRule, contentDescription = "Draw Level", tint = TextSecondaryDark, modifier = Modifier.size(18.dp))
                }

                if (drawings.isNotEmpty()) {
                    IconButton(onClick = onClearDrawings, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Clear Drawings", tint = TerminalRed, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Right controls: Chart Type, Reset Zoom, Fullscreen
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Chart Type Selector
                var showTypeMenu by remember { mutableStateOf(false) }
                Box {
                    TextButton(
                        onClick = { showTypeMenu = true },
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                    ) {
                        Text(chartType.label, fontSize = 11.sp, color = TerminalAccent, fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TerminalAccent, modifier = Modifier.size(16.dp))
                    }
                    DropdownMenu(
                        expanded = showTypeMenu,
                        onDismissRequest = { showTypeMenu = false },
                        modifier = Modifier.background(SurfaceDark)
                    ) {
                        ChartType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.label, color = TextPrimaryDark, fontSize = 12.sp) },
                                onClick = {
                                    onChartTypeSelected(type)
                                    showTypeMenu = false
                                }
                            )
                        }
                    }
                }

                // Reset Zoom button
                IconButton(
                    onClick = {
                        candleVisibleCount = 50
                        scrollOffset = 0
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = "Reset Chart", tint = TextSecondaryDark, modifier = Modifier.size(18.dp))
                }

                // Fullscreen toggle
                IconButton(
                    onClick = onToggleFullscreen,
                    modifier = Modifier.size(32.dp).testTag("fullscreen_toggle_button")
                ) {
                    Icon(
                        if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = "Fullscreen",
                        tint = TextPrimaryDark,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 2. Timeframe & Period Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBgDark)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("TF: ", fontSize = 10.sp, color = TextSecondaryDark, fontWeight = FontWeight.Bold)
            ChartTimeframe.entries.forEach { tf ->
                val isSelected = tf == currentTimeframe
                Box(
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isSelected) TerminalAccent.copy(alpha = 0.25f) else Color.Transparent)
                        .clickable { onTimeframeSelected(tf) }
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = tf.label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) TerminalAccent else TextSecondaryDark
                    )
                }
            }

            Spacer(Modifier.width(12.dp))
            Box(Modifier.width(1.dp).height(14.dp).background(TerminalBorder))
            Spacer(Modifier.width(12.dp))

            Text("PERIOD: ", fontSize = 10.sp, color = TextSecondaryDark, fontWeight = FontWeight.Bold)
            ChartPeriod.entries.forEach { pr ->
                val isSelected = pr == currentPeriod
                Box(
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isSelected) TerminalGreen.copy(alpha = 0.25f) else Color.Transparent)
                        .clickable { onPeriodSelected(pr) }
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = pr.label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) TerminalGreen else TextSecondaryDark
                    )
                }
            }
        }

        // 2b. Data Source & Freshness Status Bar
        if (payload != null) {
            val isLive = payload.freshness == DataFreshness.LIVE
            val isStale = payload.isCached || payload.freshness == DataFreshness.STALE
            val badgeBg = if (isLive) EmeraldGreen.copy(alpha = 0.15f) else if (isStale) AmberWarning.copy(alpha = 0.15f) else CrimsonRed.copy(alpha = 0.15f)
            val badgeBorder = if (isLive) EmeraldGreen.copy(alpha = 0.4f) else if (isStale) AmberWarning.copy(alpha = 0.4f) else CrimsonRed.copy(alpha = 0.4f)
            val badgeTextColor = if (isLive) EmeraldGreen else if (isStale) AmberWarning else CrimsonRed
            val badgeLabel = if (isLive) "● LIVE" else if (isStale) "▲ CACHED" else "✖ OFFLINE"

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0D1322))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = badgeBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, badgeBorder),
                        shape = RoundedCornerShape(3.dp)
                    ) {
                        Text(
                            text = badgeLabel,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = badgeTextColor,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = payload.source,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimaryDark,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Text(
                    text = if (payload.isCached) "Cached: ${payload.lastUpdated}" else "Updated: ${payload.lastUpdated}",
                    fontSize = 9.sp,
                    color = TextMutedDark,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // 3. Active Indicator Layer Pills
        if (activeIndicators.isNotEmpty() || activePineResult != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TerminalBgDark)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                activeIndicators.forEach { ind ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(SurfaceDark)
                            .border(1.dp, Color(ind.color).copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(Color(ind.color)))
                        Text(ind.title, fontSize = 10.sp, color = TextPrimaryDark, fontWeight = FontWeight.Medium)

                        // Eye toggle
                        Icon(
                            if (ind.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle",
                            modifier = Modifier
                                .size(14.dp)
                                .clickable { onToggleIndicatorVisibility(ind.id) },
                            tint = if (ind.isVisible) TextPrimaryDark else TextSecondaryDark
                        )

                        // Settings gear
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            modifier = Modifier
                                .size(14.dp)
                                .clickable { onOpenIndicatorSettings(ind) },
                            tint = TextSecondaryDark
                        )

                        // Delete
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove",
                            modifier = Modifier
                                .size(14.dp)
                                .clickable { onRemoveIndicator(ind.id) },
                            tint = TerminalRed.copy(alpha = 0.8f)
                        )
                    }
                }

                if (activePineResult != null) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(SurfaceDark)
                            .border(1.dp, TerminalGreen.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(TerminalGreen))
                        Text(
                            "PINE: ${activePineResult.title}",
                            fontSize = 10.sp,
                            color = TerminalGreen,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // 4. Live Quote & Crosshair Data HUD Bar
        val activeCandle = crosshairCandle ?: candles.lastOrNull()
        if (activeCandle != null) {
            val candleChange = activeCandle.close - activeCandle.open
            val candleChangePct = if (activeCandle.open > 0) (candleChange / activeCandle.open) * 100.0 else 0.0
            val isBullish = candleChange >= 0

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        activeCandle.dateStr,
                        fontSize = 11.sp,
                        color = TextSecondaryDark,
                        fontFamily = FontFamily.Monospace
                    )
                    Text("O: ${String.format("%.2f", activeCandle.open)}", fontSize = 11.sp, color = TextPrimaryDark, fontFamily = FontFamily.Monospace)
                    Text("H: ${String.format("%.2f", activeCandle.high)}", fontSize = 11.sp, color = TextPrimaryDark, fontFamily = FontFamily.Monospace)
                    Text("L: ${String.format("%.2f", activeCandle.low)}", fontSize = 11.sp, color = TextPrimaryDark, fontFamily = FontFamily.Monospace)
                    Text(
                        "C: ${String.format("%.2f", activeCandle.close)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isBullish) TerminalGreen else TerminalRed,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${if (isBullish) "+" else ""}${String.format("%.2f", candleChangePct)}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isBullish) TerminalGreen else TerminalRed
                    )
                    Text(
                        "Vol: ${(activeCandle.volume / 1_000_000.0).let { String.format("%.2fM", it) }}",
                        fontSize = 10.sp,
                        color = TextSecondaryDark,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // 5. Main Canvas: Candlesticks, Overlays, Drawings, Event Markers & Volume
        val chartHeight = if (isFullscreen) 420.dp else 280.dp
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight)
                .background(Color(0xFF070B14))
        ) {
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TerminalAccent, modifier = Modifier.size(36.dp))
                }
            } else if (candles.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.CloudOff,
                        contentDescription = "Data Offline",
                        tint = CrimsonRed,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Market Data Unavailable",
                        fontWeight = FontWeight.Bold,
                        color = CrimsonRed,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        payload?.statusMessage ?: "Live provider offline or rate-limited. No cached real candles exist for this symbol and timeframe.",
                        color = TextSecondaryDark,
                        fontSize = 11.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { onTimeframeSelected(currentTimeframe) },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp), tint = TerminalBgDark)
                        Spacer(Modifier.width(6.dp))
                        Text("Retry Feed", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TerminalBgDark)
                    }
                }
            } else {
                // Determine visible slice
                val maxVisible = candleVisibleCount.coerceIn(15, 250)
                val totalCandles = candles.size
                val startIdx = (totalCandles - maxVisible - scrollOffset).coerceIn(0, max(0, totalCandles - 15))
                val endIdx = (startIdx + maxVisible).coerceAtMost(totalCandles)
                val visibleCandles = candles.subList(startIdx, endIdx)

                // Visible Overlays slice
                val visibleOverlays = calculatedOverlays.map { res ->
                    res.copy(
                        primaryValues = res.primaryValues.subList(startIdx, endIdx),
                        secondaryValues = res.secondaryValues?.subList(startIdx, endIdx),
                        tertiaryValues = res.tertiaryValues?.subList(startIdx, endIdx),
                        labels = res.labels?.subList(startIdx, endIdx)
                    )
                }

                // Pine visible slice
                val visiblePinePlots = activePineResult?.plots?.filter { activePineResult.isOverlay }?.map { plot ->
                    plot.copy(series = plot.series.subList(startIdx.coerceAtMost(plot.series.size), endIdx.coerceAtMost(plot.series.size)))
                } ?: emptyList()

                // Calculate Price scale bounds
                val minLow = visibleCandles.minOfOrNull { it.low } ?: 100.0
                val maxHigh = visibleCandles.maxOfOrNull { it.high } ?: 200.0
                val pricePadding = max(0.5, (maxHigh - minLow) * 0.08)
                val minPrice = minLow - pricePadding
                val maxPrice = maxHigh + pricePadding

                // Calculate Volume bounds
                val maxVolume = visibleCandles.maxOfOrNull { it.volume } ?: 1000L

                var touchX by remember { mutableStateOf<Float?>(null) }
                var touchY by remember { mutableStateOf<Float?>(null) }

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(totalCandles) {
                            detectTransformGestures(panZoomLock = false) { _, pan, zoom, _ ->
                                // Smooth Zoom
                                if (zoom != 1f) {
                                    val targetCount = (candleVisibleCount / zoom).toInt().coerceIn(15, 250)
                                    if (targetCount != candleVisibleCount) {
                                        candleVisibleCount = targetCount
                                    }
                                }
                                // Smooth Pan (dragging right moves into past history; dragging left moves toward live)
                                if (pan.x != 0f) {
                                    panSubCandleAccumulator += pan.x
                                    val candleW = (size.width - 60f) / candleVisibleCount.toFloat()
                                    if (candleW > 0f) {
                                        val shift = (panSubCandleAccumulator / candleW).toInt()
                                        if (shift != 0) {
                                            val maxScroll = max(0, totalCandles - candleVisibleCount)
                                            scrollOffset = (scrollOffset + shift).coerceIn(0, maxScroll)
                                            panSubCandleAccumulator -= shift * candleW
                                        }
                                    }
                                }
                            }
                        }
                        .pointerInput(visibleCandles.size) {
                            detectTapGestures(
                                onPress = { offset ->
                                    if (offset.x < size.width - 60f) {
                                        touchX = offset.x
                                        touchY = offset.y
                                        val candleW = (size.width - 60f) / visibleCandles.size.toFloat()
                                        val idx = (offset.x / candleW).toInt().coerceIn(0, visibleCandles.size - 1)
                                        crosshairCandle = visibleCandles[idx]
                                        tryAwaitRelease()
                                        touchX = null
                                        touchY = null
                                        crosshairCandle = null
                                    }
                                }
                            )
                        }
                ) {
                    val w = size.width - 60f // reserve 60px for right price scale
                    val h = size.height
                    val priceAreaHeight = h * 0.78f
                    val volumeAreaHeight = h * 0.22f
                    val volumeTop = priceAreaHeight

                    // 1. Grid lines (horizontal price & vertical time)
                    drawChartGrid(w, priceAreaHeight, minPrice, maxPrice)

                    // 2. Render Volume histogram
                    drawVolumeHistogram(
                        candles = visibleCandles,
                        width = w,
                        top = volumeTop,
                        height = volumeAreaHeight,
                        maxVolume = maxVolume
                    )

                    // 3. Render Price series (Candles / Line / Area / Bar)
                    when (chartType) {
                        ChartType.CANDLESTICK -> drawCandlesticks(visibleCandles, w, priceAreaHeight, minPrice, maxPrice)
                        ChartType.LINE -> drawLineChart(visibleCandles, w, priceAreaHeight, minPrice, maxPrice, isArea = false)
                        ChartType.AREA -> drawLineChart(visibleCandles, w, priceAreaHeight, minPrice, maxPrice, isArea = true)
                        ChartType.BAR -> drawBarChart(visibleCandles, w, priceAreaHeight, minPrice, maxPrice)
                    }

                    // 4. Render Indicator Overlays (EMA, SMA, Bollinger Bands, Swing Structure)
                    visibleOverlays.forEach { overlay ->
                        drawIndicatorOverlay(overlay, visibleCandles, w, priceAreaHeight, minPrice, maxPrice)
                    }

                    // 5. Render Pine Script Plots
                    visiblePinePlots.forEach { plot ->
                        drawPinePlot(plot, visibleCandles.size, w, priceAreaHeight, minPrice, maxPrice)
                    }

                    // 6. Render Drawings (Horizontal Levels, Trendlines)
                    drawings.forEach { d ->
                        drawUserDrawing(d, w, priceAreaHeight, minPrice, maxPrice)
                    }

                    // 7. Render Event Markers (Earnings, 13F, Insider, Analysts)
                    if (showEventMarkers) {
                        drawChartMarkers(
                            markers = eventMarkers,
                            candles = visibleCandles,
                            width = w,
                            height = priceAreaHeight,
                            minPrice = minPrice,
                            maxPrice = maxPrice,
                            onMarkerTapped = { marker -> selectedEvent = marker }
                        )
                    }

                    // 8. Right Price Axis
                    drawPriceAxis(
                        axisLeft = w,
                        width = 60f,
                        height = priceAreaHeight,
                        minPrice = minPrice,
                        maxPrice = maxPrice,
                        currentPrice = visibleCandles.lastOrNull()?.close ?: 0.0
                    )

                    // 9. Crosshair if touching
                    if (touchX != null && touchY != null && touchX!! < w) {
                        drawCrosshair(
                            x = touchX!!,
                            y = touchY!!,
                            width = w,
                            height = priceAreaHeight,
                            minPrice = minPrice,
                            maxPrice = maxPrice
                        )
                    }
                }

                // TradingView HUD Pill on Crosshair
                if (crosshairCandle != null) {
                    val c = crosshairCandle!!
                    val diff = c.close - c.open
                    val pct = if (c.open > 0) (diff / c.open) * 100 else 0.0
                    val isPos = diff >= 0
                    val clr = if (isPos) Color(0xFF10B981) else Color(0xFFEF4444)
                    val sign = if (isPos) "+" else ""

                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 8.dp, top = 6.dp),
                        color = Color(0xEE0D1322),
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(c.dateStr, fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = TextMutedDark)
                            Text("O:${String.format("%.2f", c.open)}", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = TextSecondaryDark)
                            Text("H:${String.format("%.2f", c.high)}", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = TextSecondaryDark)
                            Text("L:${String.format("%.2f", c.low)}", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = TextSecondaryDark)
                            Text("C:${String.format("%.2f", c.close)}", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = clr, fontWeight = FontWeight.Bold)
                            Text("$sign${String.format("%.2f", pct)}%", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = clr, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Jump to Live Pill (Appears when scrolled into history)
                if (scrollOffset > 0) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 68.dp, bottom = 8.dp)
                            .clickable {
                                scrollOffset = 0
                                panSubCandleAccumulator = 0f
                            },
                        color = Color(0xF00D1322),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.7f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.FastForward, contentDescription = "Jump to Live", tint = CyanAccent, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("LIVE", color = CyanAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Spacer(Modifier.width(3.dp))
                            Text("(-$scrollOffset)", color = TextMutedDark, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                // Quick Zoom Controls (+ / -)
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 66.dp, top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .size(24.dp)
                            .clickable {
                                candleVisibleCount = (candleVisibleCount - 10).coerceAtLeast(15)
                            },
                        color = Color(0xDD0D1322),
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("+", color = TextPrimaryDark, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Surface(
                        modifier = Modifier
                            .size(24.dp)
                            .clickable {
                                candleVisibleCount = (candleVisibleCount + 10).coerceAtMost(250)
                            },
                        color = Color(0xDD0D1322),
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("-", color = TextPrimaryDark, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 6. Sub-Pane: Oscillators (RSI, MACD, Stochastic)
        if (calculatedPanes.isNotEmpty() || (activePineResult != null && !activePineResult.isOverlay)) {
            val paneHeight = 100.dp
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(paneHeight)
                    .background(Color(0xFF0A0F1D))
                    .border(androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width - 60f
                    val h = size.height

                    val startIdx = (candles.size - candleVisibleCount - scrollOffset).coerceIn(0, max(0, candles.size - 15))
                    val endIdx = (startIdx + candleVisibleCount).coerceAtMost(candles.size)

                    // Draw oscillator pane indicators
                    calculatedPanes.forEach { pane ->
                        val subSlice = pane.copy(
                            primaryValues = pane.primaryValues.subList(startIdx.coerceAtMost(pane.primaryValues.size), endIdx.coerceAtMost(pane.primaryValues.size)),
                            secondaryValues = pane.secondaryValues?.subList(startIdx.coerceAtMost(pane.secondaryValues.size), endIdx.coerceAtMost(pane.secondaryValues.size)),
                            tertiaryValues = pane.tertiaryValues?.subList(startIdx.coerceAtMost(pane.tertiaryValues.size), endIdx.coerceAtMost(pane.tertiaryValues.size))
                        )
                        drawOscillatorPane(subSlice, w, h)
                    }

                    // Draw Pine oscillator if non-overlay
                    if (activePineResult != null && !activePineResult.isOverlay) {
                        drawPineOscillatorPane(activePineResult, startIdx, endIdx, w, h)
                    }
                }
            }
        }

        // 7. Event Details Card Modal/Sheet
        if (selectedEvent != null) {
            val ev = selectedEvent!!
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                colors = CardDefaults.cardColors(containerColor = CardBgDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, TerminalAccent.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(getMarkerColor(ev.type))
                            )
                            Text(ev.title, fontWeight = FontWeight.Bold, color = TextPrimaryDark, fontSize = 13.sp)
                        }
                        IconButton(onClick = { selectedEvent = null }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondaryDark, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(ev.subtitle, color = TerminalAccent, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    Text(ev.details, color = TextSecondaryDark, fontSize = 11.sp, lineHeight = 16.sp)
                }
            }
        }

        // 8. Data Freshness Status Footer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (payload?.freshness == DataFreshness.LIVE) TerminalGreen else TerminalYellow)
                )
                Text(
                    text = "${payload?.freshness?.name ?: "RECENT"} · ${payload?.source ?: "Yahoo Finance Market Data"} · Refreshed: ${payload?.lastUpdated ?: "Just now"}",
                    fontSize = 9.sp,
                    color = TextSecondaryDark,
                    fontFamily = FontFamily.Monospace
                )
            }
            Text("PINE COMPATIBLE RUNTIME v6", fontSize = 9.sp, color = TerminalAccent, fontFamily = FontFamily.Monospace)
        }
    }
}

// Drawing Helper Implementations for Custom Canvas

private fun DrawScope.drawChartGrid(width: Float, height: Float, minPrice: Double, maxPrice: Double) {
    val steps = 5
    for (i in 0..steps) {
        val y = height * (i / steps.toFloat())
        drawLine(
            color = Color(0xFF1E293B),
            start = Offset(0f, y),
            end = Offset(width, y),
            strokeWidth = 1f
        )
    }
}

private fun DrawScope.drawCandlesticks(
    candles: List<CandleData>,
    width: Float,
    height: Float,
    minPrice: Double,
    maxPrice: Double
) {
    if (candles.isEmpty()) return
    val candleWidth = width / candles.size.toFloat()
    val bodyWidth = (candleWidth * 0.75f).coerceAtLeast(1.5f)
    val priceRange = (maxPrice - minPrice).toFloat()

    candles.forEachIndexed { i, c ->
        val xCenter = i * candleWidth + (candleWidth / 2f)
        val isBullish = c.close >= c.open
        val color = if (isBullish) Color(0xFF10B981) else Color(0xFFEF4444)

        val highY = height - (((c.high - minPrice) / priceRange).toFloat() * height)
        val lowY = height - (((c.low - minPrice) / priceRange).toFloat() * height)
        val openY = height - (((c.open - minPrice) / priceRange).toFloat() * height)
        val closeY = height - (((c.close - minPrice) / priceRange).toFloat() * height)

        // Wick
        drawLine(
            color = color,
            start = Offset(xCenter, highY),
            end = Offset(xCenter, lowY),
            strokeWidth = 1.2f
        )

        // Candle Body
        val bodyTop = min(openY, closeY)
        val bodyHeight = max(1.5f, kotlin.math.abs(closeY - openY))
        drawRect(
            color = color,
            topLeft = Offset(xCenter - (bodyWidth / 2f), bodyTop),
            size = Size(bodyWidth, bodyHeight)
        )
    }
}

private fun DrawScope.drawLineChart(
    candles: List<CandleData>,
    width: Float,
    height: Float,
    minPrice: Double,
    maxPrice: Double,
    isArea: Boolean
) {
    if (candles.size < 2) return
    val candleWidth = width / candles.size.toFloat()
    val priceRange = (maxPrice - minPrice).toFloat()

    val path = Path()
    candles.forEachIndexed { i, c ->
        val x = i * candleWidth + (candleWidth / 2f)
        val y = height - (((c.close - minPrice) / priceRange).toFloat() * height)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }

    if (isArea) {
        val areaPath = Path().apply {
            addPath(path)
            val lastX = (candles.size - 1) * candleWidth + (candleWidth / 2f)
            lineTo(lastX, height)
            lineTo(candleWidth / 2f, height)
            close()
        }
        drawPath(
            path = areaPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF38BDF8).copy(alpha = 0.35f), Color(0xFF38BDF8).copy(alpha = 0.02f)),
                startY = 0f,
                endY = height
            )
        )
    }

    drawPath(
        path = path,
        color = Color(0xFF38BDF8),
        style = Stroke(width = 2f, cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawBarChart(
    candles: List<CandleData>,
    width: Float,
    height: Float,
    minPrice: Double,
    maxPrice: Double
) {
    if (candles.isEmpty()) return
    val candleWidth = width / candles.size.toFloat()
    val tickWidth = (candleWidth * 0.4f).coerceAtLeast(2f)
    val priceRange = (maxPrice - minPrice).toFloat()

    candles.forEachIndexed { i, c ->
        val xCenter = i * candleWidth + (candleWidth / 2f)
        val isBullish = c.close >= c.open
        val color = if (isBullish) Color(0xFF10B981) else Color(0xFFEF4444)

        val highY = height - (((c.high - minPrice) / priceRange).toFloat() * height)
        val lowY = height - (((c.low - minPrice) / priceRange).toFloat() * height)
        val openY = height - (((c.open - minPrice) / priceRange).toFloat() * height)
        val closeY = height - (((c.close - minPrice) / priceRange).toFloat() * height)

        // Main Bar
        drawLine(color, Offset(xCenter, highY), Offset(xCenter, lowY), strokeWidth = 1.5f)
        // Open Tick (left)
        drawLine(color, Offset(xCenter - tickWidth, openY), Offset(xCenter, openY), strokeWidth = 1.5f)
        // Close Tick (right)
        drawLine(color, Offset(xCenter, closeY), Offset(xCenter + tickWidth, closeY), strokeWidth = 1.5f)
    }
}

private fun DrawScope.drawVolumeHistogram(
    candles: List<CandleData>,
    width: Float,
    top: Float,
    height: Float,
    maxVolume: Long
) {
    if (candles.isEmpty() || maxVolume <= 0) return
    val candleWidth = width / candles.size.toFloat()
    val barWidth = (candleWidth * 0.7f).coerceAtLeast(1.5f)

    candles.forEachIndexed { i, c ->
        val xCenter = i * candleWidth + (candleWidth / 2f)
        val barHeight = (c.volume.toFloat() / maxVolume.toFloat()) * height
        val isBullish = c.close >= c.open
        val color = if (isBullish) Color(0xFF10B981).copy(alpha = 0.35f) else Color(0xFFEF4444).copy(alpha = 0.35f)

        drawRect(
            color = color,
            topLeft = Offset(xCenter - (barWidth / 2f), top + (height - barHeight)),
            size = Size(barWidth, barHeight)
        )
    }
}

private fun DrawScope.drawIndicatorOverlay(
    overlay: CalculatedSeriesResult,
    candles: List<CandleData>,
    width: Float,
    height: Float,
    minPrice: Double,
    maxPrice: Double
) {
    val count = candles.size
    if (count < 2) return
    val candleWidth = width / count.toFloat()
    val priceRange = (maxPrice - minPrice).toFloat()
    val color = Color(overlay.color)

    // Draw primary series line (e.g. EMA, SMA, BB Basis)
    val path = Path()
    var started = false
    overlay.primaryValues.forEachIndexed { i, v ->
        if (v != null) {
            val x = i * candleWidth + (candleWidth / 2f)
            val y = height - (((v - minPrice) / priceRange).toFloat() * height)
            if (!started) {
                path.moveTo(x, y)
                started = true
            } else {
                path.lineTo(x, y)
            }
        }
    }
    if (started) {
        drawPath(path, color, style = Stroke(width = 1.8f))
    }

    // Draw secondary series if present (e.g. BB Upper)
    if (overlay.secondaryValues != null) {
        val upperPath = Path()
        var upperStarted = false
        overlay.secondaryValues.forEachIndexed { i, v ->
            if (v != null) {
                val x = i * candleWidth + (candleWidth / 2f)
                val y = height - (((v - minPrice) / priceRange).toFloat() * height)
                if (!upperStarted) {
                    upperPath.moveTo(x, y)
                    upperStarted = true
                } else {
                    upperPath.lineTo(x, y)
                }
            }
        }
        if (upperStarted) {
            drawPath(upperPath, color.copy(alpha = 0.7f), style = Stroke(width = 1.2f))
        }
    }

    // Draw tertiary series if present (e.g. BB Lower)
    if (overlay.tertiaryValues != null) {
        val lowerPath = Path()
        var lowerStarted = false
        overlay.tertiaryValues.forEachIndexed { i, v ->
            if (v != null) {
                val x = i * candleWidth + (candleWidth / 2f)
                val y = height - (((v - minPrice) / priceRange).toFloat() * height)
                if (!lowerStarted) {
                    lowerPath.moveTo(x, y)
                    lowerStarted = true
                } else {
                    lowerPath.lineTo(x, y)
                }
            }
        }
        if (lowerStarted) {
            drawPath(lowerPath, color.copy(alpha = 0.7f), style = Stroke(width = 1.2f))
        }
    }

    // Draw SMC labels (HH, HL, LH, LL)
    overlay.labels?.forEachIndexed { i, label ->
        if (label != null && overlay.primaryValues.getOrNull(i) != null) {
            val v = overlay.primaryValues[i]!!
            val x = i * candleWidth + (candleWidth / 2f)
            val y = height - (((v - minPrice) / priceRange).toFloat() * height)
            drawCircle(color, radius = 3.5f, center = Offset(x, y))
        }
    }
}

private fun DrawScope.drawPinePlot(
    plot: com.example.engine.pine.PinePlot,
    candleCount: Int,
    width: Float,
    height: Float,
    minPrice: Double,
    maxPrice: Double
) {
    if (candleCount < 2) return
    val candleWidth = width / candleCount.toFloat()
    val priceRange = (maxPrice - minPrice).toFloat()
    val path = Path()
    var started = false

    plot.series.forEachIndexed { i, v ->
        if (v != null) {
            val x = i * candleWidth + (candleWidth / 2f)
            val y = height - (((v - minPrice) / priceRange).toFloat() * height)
            if (!started) {
                path.moveTo(x, y)
                started = true
            } else {
                path.lineTo(x, y)
            }
        }
    }

    if (started) {
        drawPath(path, Color(plot.color), style = Stroke(width = plot.lineWidth))
    }
}

private fun DrawScope.drawUserDrawing(
    drawing: ChartDrawing,
    width: Float,
    height: Float,
    minPrice: Double,
    maxPrice: Double
) {
    val priceRange = (maxPrice - minPrice).toFloat()
    val y = height - (((drawing.price1 - minPrice) / priceRange).toFloat() * height)
    val color = Color(drawing.color)

    // Horizontal Level line
    drawLine(
        color = color,
        start = Offset(0f, y),
        end = Offset(width, y),
        strokeWidth = 1.5f
    )
}

private fun DrawScope.drawChartMarkers(
    markers: List<ChartEventMarker>,
    candles: List<CandleData>,
    width: Float,
    height: Float,
    minPrice: Double,
    maxPrice: Double,
    onMarkerTapped: (ChartEventMarker) -> Unit
) {
    if (candles.isEmpty()) return
    val candleWidth = width / candles.size.toFloat()
    val priceRange = (maxPrice - minPrice).toFloat()

    markers.forEach { marker ->
        // Find closest candle by timestamp
        val closestIdx = candles.indices.minByOrNull { kotlin.math.abs(candles[it].timestamp - marker.timestamp) } ?: 0
        val x = closestIdx * candleWidth + (candleWidth / 2f)
        val y = (height - (((marker.price - minPrice) / priceRange).toFloat() * height)).coerceIn(20f, height - 20f)
        val markerColor = getMarkerColor(marker.type)

        // Draw outer ring & dot
        drawCircle(color = markerColor.copy(alpha = 0.3f), radius = 9f, center = Offset(x, y))
        drawCircle(color = markerColor, radius = 5.5f, center = Offset(x, y))
        drawCircle(color = Color.White, radius = 2f, center = Offset(x, y))
    }
}

private fun DrawScope.drawPriceAxis(
    axisLeft: Float,
    width: Float,
    height: Float,
    minPrice: Double,
    maxPrice: Double,
    currentPrice: Double
) {
    // Right axis background border
    drawLine(Color(0xFF1E293B), Offset(axisLeft, 0f), Offset(axisLeft, height), strokeWidth = 1f)

    val priceRange = (maxPrice - minPrice).toFloat()
    if (priceRange <= 0f) return

    val textPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.parseColor("#94A3B8")
        textSize = 20f
        isAntiAlias = true
    }

    // 4 evenly spaced horizontal price ticks
    val steps = 4
    for (i in 0..steps) {
        val frac = i / steps.toFloat()
        val y = height - (frac * height)
        val priceVal = minPrice + (frac * priceRange)
        val priceStr = String.format("%.2f", priceVal)
        drawContext.canvas.nativeCanvas.drawText(priceStr, axisLeft + 6f, (y + 6f).coerceIn(16f, height - 4f), textPaint)
    }

    // Current price highlight badge
    val currY = (height - (((currentPrice - minPrice) / priceRange).toFloat() * height)).coerceIn(10f, height - 10f)
    val badgeColor = if (currentPrice >= minPrice + priceRange * 0.5) Color(0xFF10B981) else Color(0xFFEF4444)
    drawRect(
        color = badgeColor,
        topLeft = Offset(axisLeft, currY - 9f),
        size = Size(width, 18f)
    )
    val badgePaint = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 18f
        isFakeBoldText = true
        isAntiAlias = true
    }
    drawContext.canvas.nativeCanvas.drawText(String.format("%.2f", currentPrice), axisLeft + 4f, currY + 5f, badgePaint)
}

private fun DrawScope.drawCrosshair(
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    minPrice: Double,
    maxPrice: Double
) {
    // Vertical crosshair line
    drawLine(
        color = Color.White.copy(alpha = 0.45f),
        start = Offset(x, 0f),
        end = Offset(x, height),
        strokeWidth = 1f
    )
    // Horizontal crosshair line
    drawLine(
        color = Color.White.copy(alpha = 0.45f),
        start = Offset(0f, y),
        end = Offset(width, y),
        strokeWidth = 1f
    )
    // Center point indicator
    drawCircle(Color.White, radius = 3f, center = Offset(x, y))

    // Price badge at right axis for crosshair position
    val priceRange = (maxPrice - minPrice).toFloat()
    if (priceRange > 0f) {
        val priceAtY = maxPrice - ((y / height) * priceRange)
        drawRect(
            color = Color(0xFF334155),
            topLeft = Offset(width, y - 9f),
            size = Size(60f, 18f)
        )
        val textPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 18f
            isFakeBoldText = true
            isAntiAlias = true
        }
        drawContext.canvas.nativeCanvas.drawText(String.format("%.2f", priceAtY), width + 4f, y + 5f, textPaint)
    }
}

private fun DrawScope.drawOscillatorPane(
    pane: CalculatedSeriesResult,
    width: Float,
    height: Float
) {
    val count = pane.primaryValues.size
    if (count < 2) return
    val candleWidth = width / count.toFloat()

    val isBounded0to100 = pane.hlines.any { it == 70.0 || it == 80.0 }
    val allValues = (pane.primaryValues + (pane.secondaryValues ?: emptyList()) + (pane.tertiaryValues ?: emptyList())).filterNotNull()
    val minVal = if (isBounded0to100) 0.0 else (allValues.minOrNull() ?: 0.0)
    val maxVal = if (isBounded0to100) 100.0 else (allValues.maxOrNull() ?: 1.0)
    val range = max(0.0001, maxVal - minVal).toFloat()

    // Reference lines
    pane.hlines.forEach { level ->
        if (level in minVal..maxVal) {
            val y = height - (((level - minVal) / range).toFloat() * height)
            drawLine(Color(0xFF334155), Offset(0f, y), Offset(width, y), strokeWidth = 1f)
        }
    }

    // Zero line if unbounded
    if (!isBounded0to100 && minVal < 0 && maxVal > 0) {
        val zeroY = height - (((0.0 - minVal) / range).toFloat() * height)
        drawLine(Color(0xFF475569), Offset(0f, zeroY), Offset(width, zeroY), strokeWidth = 1f)
    }

    // Tertiary values: Histogram bars (e.g. MACD histogram)
    pane.tertiaryValues?.let { tert ->
        val zeroY = if (minVal < 0 && maxVal > 0) height - (((0.0 - minVal) / range).toFloat() * height) else height
        val barW = (candleWidth * 0.65f).coerceAtLeast(1.5f)
        tert.forEachIndexed { i, v ->
            if (v != null) {
                val x = i * candleWidth + (candleWidth / 2f)
                val y = height - (((v - minVal) / range).toFloat() * height)
                val barTop = min(y, zeroY)
                val barH = max(1f, abs(y - zeroY))
                val barColor = if (v >= 0) Color(0xFF10B981).copy(alpha = 0.7f) else Color(0xFFEF4444).copy(alpha = 0.7f)
                drawRect(
                    color = barColor,
                    topLeft = Offset(x - (barW / 2f), barTop),
                    size = Size(barW, barH)
                )
            }
        }
    }

    // Secondary line (e.g. MACD Signal line, Stochastic %D)
    pane.secondaryValues?.let { sec ->
        val secPath = Path()
        var secStarted = false
        sec.forEachIndexed { i, v ->
            if (v != null) {
                val x = i * candleWidth + (candleWidth / 2f)
                val y = height - (((v - minVal) / range).toFloat() * height)
                if (!secStarted) {
                    secPath.moveTo(x, y)
                    secStarted = true
                } else {
                    secPath.lineTo(x, y)
                }
            }
        }
        if (secStarted) {
            drawPath(secPath, Color(0xFFF59E0B), style = Stroke(width = 1.4f))
        }
    }

    // Primary oscillator curve
    val path = Path()
    var started = false
    pane.primaryValues.forEachIndexed { i, v ->
        if (v != null) {
            val x = i * candleWidth + (candleWidth / 2f)
            val y = height - (((v - minVal) / range).toFloat() * height)
            if (!started) {
                path.moveTo(x, y)
                started = true
            } else {
                path.lineTo(x, y)
            }
        }
    }
    if (started) {
        drawPath(path, Color(pane.color), style = Stroke(width = 1.8f))
    }
}

private fun DrawScope.drawPineOscillatorPane(
    exec: PineExecutionResult,
    startIdx: Int,
    endIdx: Int,
    width: Float,
    height: Float
) {
    val plot = exec.plots.firstOrNull() ?: return
    val slice = plot.series.subList(startIdx.coerceAtMost(plot.series.size), endIdx.coerceAtMost(plot.series.size))
    if (slice.size < 2) return
    val candleWidth = width / slice.size.toFloat()

    val minV = slice.filterNotNull().minOrNull() ?: 0.0
    val maxV = slice.filterNotNull().maxOrNull() ?: 100.0
    val range = max(1.0, maxV - minV).toFloat()

    val path = Path()
    var started = false
    slice.forEachIndexed { i, v ->
        if (v != null) {
            val x = i * candleWidth + (candleWidth / 2f)
            val y = height - (((v - minV) / range).toFloat() * height)
            if (!started) {
                path.moveTo(x, y)
                started = true
            } else {
                path.lineTo(x, y)
            }
        }
    }
    if (started) {
        drawPath(path, Color(plot.color), style = Stroke(width = plot.lineWidth))
    }
}

private fun getMarkerColor(type: ChartMarkerType): Color {
    return when (type) {
        ChartMarkerType.EARNINGS_BEAT -> Color(0xFF10B981) // Green
        ChartMarkerType.EARNINGS_MISS -> Color(0xFFEF4444) // Red
        ChartMarkerType.INSTITUTIONAL_13F -> Color(0xFFF59E0B) // Gold
        ChartMarkerType.INSIDER_BUY -> Color(0xFF38BDF8) // Cyan
        ChartMarkerType.INSIDER_SELL -> Color(0xFFF97316) // Orange
        ChartMarkerType.ANALYST_UPGRADE -> Color(0xFFA855F7) // Purple
        ChartMarkerType.ANALYST_DOWNGRADE -> Color(0xFF94A3B8) // Slate
        ChartMarkerType.DIVIDEND -> Color(0xFF10B981)
    }
}
