package com.example.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.*
import com.example.engine.pine.PineExecutionResult
import com.example.engine.pine.PinePlot
import com.example.engine.technical.ActiveIndicator
import com.example.engine.technical.CalculatedSeriesResult
import com.example.engine.technical.TechnicalIndicatorEngine
import com.example.ui.theme.*
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

// =========================================================================
// HIGH-PERFORMANCE VIEWPORT STATE HOLDER
// Isolates fast touch updates from parent recomposition cycles (60-120 FPS)
// =========================================================================
@Stable
class ChartViewportState(
    initialVisibleCount: Float = 45f,
    initialScrollOffset: Float = 0f
) {
    var visibleCount by mutableFloatStateOf(initialVisibleCount)
    var scrollOffset by mutableFloatStateOf(initialScrollOffset)
    var crosshairIndex by mutableIntStateOf(-1)
    var crosshairOffset by mutableStateOf<Offset?>(null)

    fun resetToLive(defaultCount: Float = 45f) {
        visibleCount = defaultCount
        scrollOffset = 0f
        crosshairIndex = -1
        crosshairOffset = null
    }

    fun clearCrosshair() {
        crosshairIndex = -1
        crosshairOffset = null
    }
}

@Composable
fun rememberChartViewportState(
    initialVisibleCount: Float = 45f,
    initialScrollOffset: Float = 0f
): ChartViewportState {
    return remember { ChartViewportState(initialVisibleCount, initialScrollOffset) }
}

// =========================================================================
// ZERO-ALLOCATION CANVAS RENDER BUFFERS
// Pre-allocated paths to eliminate Garbage Collector stutter during panning/zooming
// =========================================================================
class ChartRenderBuffers {
    val linePath = Path()
    val areaPath = Path()
    val overlayPaths = Array(16) { Path() }
    val secondaryOverlayPaths = Array(16) { Path() }
    val tertiaryOverlayPaths = Array(16) { Path() }
    val oscillatorPrimaryPath = Path()
    val oscillatorSecondaryPath = Path()
    val pinePlotPath = Path()
}

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
    onTriggerAiAnalysis: () -> Unit = {},
    isAnalyzingChart: Boolean = false,
    chartAiAnalysis: String? = null,
    onClearChartAiAnalysis: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isFullscreen by remember { mutableStateOf(false) }

    if (isFullscreen) {
        Dialog(
            onDismissRequest = { isFullscreen = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF080D18))
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
                    onTriggerAiAnalysis = onTriggerAiAnalysis,
                    isAnalyzingChart = isAnalyzingChart,
                    chartAiAnalysis = chartAiAnalysis,
                    onClearChartAiAnalysis = onClearChartAiAnalysis,
                    modifier = Modifier.fillMaxSize()
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
            isAnalyzingChart = isAnalyzingChart,
            chartAiAnalysis = chartAiAnalysis,
            onClearChartAiAnalysis = onClearChartAiAnalysis,
            modifier = modifier
        )
    }
}

@Composable
fun ChartWorkspaceContent(
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
    onTriggerAiAnalysis: () -> Unit,
    isAnalyzingChart: Boolean = false,
    chartAiAnalysis: String? = null,
    onClearChartAiAnalysis: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val candles = payload?.candles ?: emptyList()
    val totalCandles = candles.size

    // High-performance isolated viewport state
    val viewportState = rememberChartViewportState(initialVisibleCount = 45f, initialScrollOffset = 0f)
    val renderBuffers = remember { ChartRenderBuffers() }

    var selectedEvent by remember { mutableStateOf<ChartEventMarker?>(null) }
    var showChartTypeMenu by remember { mutableStateOf(false) }
    var showAiReasonerSheet by remember { mutableStateOf(false) }

    // Auto-open AI reasoner panel when active analysis finishes or is running
    LaunchedEffect(isAnalyzingChart, chartAiAnalysis) {
        if (isAnalyzingChart || chartAiAnalysis != null) {
            showAiReasonerSheet = true
        }
    }

    // Keep viewport safely within bounds when dataset updates
    LaunchedEffect(totalCandles) {
        if (totalCandles > 0) {
            val maxVisible = 250f.coerceAtMost(totalCandles.toFloat()).coerceAtLeast(10f)
            viewportState.visibleCount = viewportState.visibleCount.coerceIn(10f, maxVisible)
            val maxScroll = max(0f, (totalCandles - viewportState.visibleCount).toFloat())
            viewportState.scrollOffset = viewportState.scrollOffset.coerceIn(0f, maxScroll)
        }
    }

    // Cached indicator overlays & sub-panes (Zero recalculation during pan & zoom)
    val calculatedOverlays = remember(candles, activeIndicators) {
        activeIndicators.filter { it.isVisible && it.isOverlay }.map { ind ->
            TechnicalIndicatorEngine.calculate(ind, candles)
        }
    }

    val calculatedPanes = remember(candles, activeIndicators) {
        activeIndicators.filter { it.isVisible && !it.isOverlay }.map { ind ->
            TechnicalIndicatorEngine.calculate(ind, candles)
        }
    }

    // Active candle for HUD display: crosshair candle if active, else latest live candle
    val activeCandle = remember(viewportState.crosshairIndex, candles) {
        if (viewportState.crosshairIndex in candles.indices) {
            candles[viewportState.crosshairIndex]
        } else {
            candles.lastOrNull()
        }
    }

    // Cached Android TextPaints for 60-120 FPS Canvas rendering
    val axisTextPaint = remember {
        Paint().apply {
            color = android.graphics.Color.parseColor("#94A3B8")
            textSize = 21f
            isAntiAlias = true
            typeface = Typeface.create("monospace", Typeface.NORMAL)
        }
    }
    val badgeTextPaint = remember {
        Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 20f
            isFakeBoldText = true
            isAntiAlias = true
            typeface = Typeface.create("monospace", Typeface.BOLD)
        }
    }
    val hudDatePaint = remember {
        Paint().apply {
            color = android.graphics.Color.parseColor("#CBD5E1")
            textSize = 18f
            isAntiAlias = true
            typeface = Typeface.create("monospace", Typeface.NORMAL)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF080D1A))
    ) {
        // =========================================================================
        // 1. UNIFIED SLEEK TRADING HEADER BAR
        // High-density single-row header consolidating Timeframes, Status, and Live Quote
        // Reclaims ~50dp of vertical space for the actual candles!
        // =========================================================================
        Surface(
            color = Color(0xFF0C1322),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = 0.5.dp, color = Color(0xFF1E293B))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Timeframe & Period Selector Chips
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    ChartTimeframe.entries.forEach { tf ->
                        val isSelected = tf == currentTimeframe
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 1.5.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSelected) CyanAccent.copy(alpha = 0.22f) else Color.Transparent)
                                .clickable { onTimeframeSelected(tf) }
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = tf.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) CyanAccent else TextSecondaryDark,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(Modifier.width(6.dp))
                    Box(Modifier.width(1.dp).height(12.dp).background(Color(0xFF2E3D52)))
                    Spacer(Modifier.width(6.dp))

                    ChartPeriod.entries.forEach { pr ->
                        val isSelected = pr == currentPeriod
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 1.5.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSelected) TerminalGreen.copy(alpha = 0.22f) else Color.Transparent)
                                .clickable { onPeriodSelected(pr) }
                                .padding(horizontal = 5.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = pr.label,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) TerminalGreen else TextMutedDark,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Right: Provider Freshness Badge & Source
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val isLive = payload?.freshness == DataFreshness.LIVE
                    val isStale = payload?.isCached == true || payload?.freshness == DataFreshness.STALE
                    val badgeBg = if (isLive) EmeraldGreen.copy(alpha = 0.15f) else if (isStale) AmberWarning.copy(alpha = 0.15f) else CrimsonRed.copy(alpha = 0.15f)
                    val badgeBorder = if (isLive) EmeraldGreen.copy(alpha = 0.5f) else if (isStale) AmberWarning.copy(alpha = 0.5f) else CrimsonRed.copy(alpha = 0.5f)
                    val badgeTextColor = if (isLive) EmeraldGreen else if (isStale) AmberWarning else CrimsonRed
                    val badgeLabel = if (isLive) "● LIVE" else if (isStale) "▲ CACHE" else "✖ OFFLINE"

                    Surface(
                        color = badgeBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, badgeBorder),
                        shape = RoundedCornerShape(3.dp)
                    ) {
                        Text(
                            text = badgeLabel,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = badgeTextColor,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }

                    Text(
                        text = payload?.source ?: "Alpha Vantage",
                        fontSize = 8.5.sp,
                        color = TextMutedDark,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )
                }
            }
        }

        // =========================================================================
        // 2. SLIM HIGH-DENSITY QUOTE TELEMETRY STRIP (OHLCV)
        // Shows real-time candle metrics or crosshair-hovered candle metrics
        // =========================================================================
        if (activeCandle != null) {
            val diff = activeCandle.close - activeCandle.open
            val pct = if (activeCandle.open > 0) (diff / activeCandle.open) * 100.0 else 0.0
            val isBullish = diff >= 0
            val clr = if (isBullish) Color(0xFF10B981) else Color(0xFFEF4444)
            val sign = if (isBullish) "+" else ""

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF090E1A))
                    .border(width = 0.5.dp, color = Color(0xFF162032))
                    .padding(horizontal = 8.dp, vertical = 2.5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = activeCandle.dateStr,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextMutedDark
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("O:${formatFastPrice(activeCandle.open)}", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = TextSecondaryDark)
                    Text("H:${formatFastPrice(activeCandle.high)}", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = TextSecondaryDark)
                    Text("L:${formatFastPrice(activeCandle.low)}", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = TextSecondaryDark)
                    Text("C:${formatFastPrice(activeCandle.close)}", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = clr, fontWeight = FontWeight.Bold)
                    Text("$sign${String.format(Locale.US, "%.2f", pct)}%", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = clr, fontWeight = FontWeight.Bold)
                }
            }
        }

        // =========================================================================
        // 3. COMPACT ACTIVE INDICATOR CHIPS (ONLY WHEN INDICATORS ACTIVE)
        // High-density 20dp strip to maximize canvas vertical space
        // =========================================================================
        if (activeIndicators.isNotEmpty() || activePineResult != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF070B14))
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                activeIndicators.forEach { ind ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF101726))
                            .border(0.5.dp, Color(ind.color).copy(alpha = 0.45f), RoundedCornerShape(3.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.5.dp)
                    ) {
                        Box(Modifier.size(5.dp).clip(CircleShape).background(Color(ind.color)))
                        Text(ind.title, fontSize = 8.5.sp, color = TextPrimaryDark, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace)

                        Icon(
                            if (ind.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle Visibility",
                            modifier = Modifier
                                .size(11.dp)
                                .clickable { onToggleIndicatorVisibility(ind.id) },
                            tint = if (ind.isVisible) TextPrimaryDark else TextSecondaryDark
                        )

                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            modifier = Modifier
                                .size(11.dp)
                                .clickable { onOpenIndicatorSettings(ind) },
                            tint = TextSecondaryDark
                        )

                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove Indicator",
                            modifier = Modifier
                                .size(11.dp)
                                .clickable { onRemoveIndicator(ind.id) },
                            tint = TerminalRed.copy(alpha = 0.8f)
                        )
                    }
                }

                if (activePineResult != null) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF101726))
                            .border(0.5.dp, TerminalGreen.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Box(Modifier.size(5.dp).clip(CircleShape).background(TerminalGreen))
                        Text(
                            "PINE: ${activePineResult.title}",
                            fontSize = 8.5.sp,
                            color = TerminalGreen,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // =========================================================================
        // 4. MAIN CHART: CANDLES + INDICATORS + INTEGRATED VOLUME
        // Occupies maximum available vertical space (weight = 1f)
        // With tight 3.5% top/bottom margins so candles fill the screen beautifully!
        // =========================================================================
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF070B14))
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(color = CyanAccent, modifier = Modifier.size(32.dp), strokeWidth = 2.5.dp)
                        Text("Streaming market depth...", color = TextSecondaryDark, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            } else if (candles.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.SignalCellularNoSim, contentDescription = null, tint = TextMutedDark, modifier = Modifier.size(36.dp))
                        Text("No market candles available for this interval", color = TextSecondaryDark, fontSize = 12.sp)
                    }
                }
            } else {
                // =========================================================================
                // HIGH-PERFORMANCE UNIFIED TOUCH GESTURE CANVAS
                // Single pointer input loop: Smooth pan, centroid pinch zoom, tap, double tap
                // Eliminates gesture conflicts, delays, and frame drops
                // =========================================================================
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(totalCandles) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                var isDragging = false
                                val touchSlop = viewConfiguration.touchSlop
                                val initialX = down.position.x
                                val initialY = down.position.y
                                var prevDistance = 0f
                                var prevCentroidX = down.position.x
                                val startTime = System.currentTimeMillis()
                                var lastTapTime = 0L

                                do {
                                    val event = awaitPointerEvent()
                                    val pressedPointers = event.changes.filter { it.pressed }
                                    val pointerCount = pressedPointers.size

                                    if (pointerCount == 1) {
                                        val p = pressedPointers[0]
                                        val dx = p.position.x - p.previousPosition.x
                                        val totalDx = p.position.x - initialX
                                        val totalDy = p.position.y - initialY

                                        if (!isDragging && (abs(totalDx) > touchSlop || abs(totalDy) > touchSlop)) {
                                            isDragging = true
                                        }

                                        if (isDragging) {
                                            p.consume()
                                            val chartW = (size.width - 60f).coerceAtLeast(10f)
                                            val candleW = chartW / viewportState.visibleCount
                                            if (candleW > 0f) {
                                                val deltaCandles = dx / candleW
                                                val maxScroll = max(0f, (totalCandles - viewportState.visibleCount).toFloat())
                                                viewportState.scrollOffset = (viewportState.scrollOffset + deltaCandles).coerceIn(0f, maxScroll)
                                                if (viewportState.crosshairIndex != -1) {
                                                    viewportState.clearCrosshair()
                                                }
                                            }
                                        }
                                    } else if (pointerCount >= 2) {
                                        isDragging = true
                                        val p1 = pressedPointers[0]
                                        val p2 = pressedPointers[1]
                                        p1.consume()
                                        p2.consume()

                                        val curDistance = (p1.position - p2.position).getDistance()
                                        val curCentroidX = (p1.position.x + p2.position.x) / 2f

                                        if (prevDistance > 0f && curDistance > 0f) {
                                            val zoomRatio = curDistance / prevDistance
                                            val chartW = (size.width - 60f).coerceAtLeast(10f)
                                            val focalRatio = (curCentroidX / chartW).coerceIn(0f, 1f)

                                            val focalCandle = viewportState.scrollOffset + (1f - focalRatio) * viewportState.visibleCount
                                            val targetCount = (viewportState.visibleCount / zoomRatio)
                                                .coerceIn(10f, 300f.coerceAtMost(max(15f, totalCandles.toFloat())))
                                            val maxScroll = max(0f, (totalCandles - targetCount).toFloat())
                                            val newScroll = (focalCandle - (1f - focalRatio) * targetCount).coerceIn(0f, maxScroll)

                                            viewportState.visibleCount = targetCount
                                            viewportState.scrollOffset = newScroll
                                            viewportState.clearCrosshair()
                                        }
                                        prevDistance = curDistance
                                        prevCentroidX = curCentroidX
                                    }
                                } while (event.changes.any { it.pressed })

                                // Tap & Double Tap Handling
                                val duration = System.currentTimeMillis() - startTime
                                if (!isDragging && duration < 350) {
                                    val tapOffset = down.position
                                    val chartW = (size.width - 60f).coerceAtLeast(10f)

                                    if (tapOffset.x in 0f..chartW) {
                                        val candleW = chartW / viewportState.visibleCount
                                        val candlesFromRight = (chartW - tapOffset.x) / candleW
                                        val targetIdx = (totalCandles - 1 - viewportState.scrollOffset - candlesFromRight).roundToInt()

                                        // Check for event marker hit
                                        val markerHit = if (showEventMarkers) {
                                            eventMarkers.firstOrNull { marker ->
                                                val markerIdx = candles.indices.minByOrNull { abs(candles[it].timestamp - marker.timestamp) } ?: -1
                                                abs(markerIdx - targetIdx) <= 1
                                            }
                                        } else null

                                        if (markerHit != null) {
                                            selectedEvent = markerHit
                                        } else {
                                            // Toggle crosshair
                                            if (viewportState.crosshairIndex == targetIdx) {
                                                viewportState.clearCrosshair()
                                            } else if (targetIdx in 0 until totalCandles) {
                                                viewportState.crosshairIndex = targetIdx
                                                viewportState.crosshairOffset = tapOffset
                                            }
                                        }
                                    } else {
                                        // Tap on price axis clears crosshair
                                        viewportState.clearCrosshair()
                                    }
                                }
                            }
                        }
                ) {
                    val w = size.width - 60f
                    val h = size.height
                    // Price candles utilize 88% of vertical height with volume subtly overlaid in bottom 22%
                    val priceAreaHeight = h * 0.88f
                    val volumeAreaHeight = h * 0.22f
                    val volumeTop = h - volumeAreaHeight

                    val candleWidth = w / viewportState.visibleCount

                    // Instantaneous visible indices slice
                    val startIdx = (totalCandles - 1 - viewportState.scrollOffset - viewportState.visibleCount - 1).toInt().coerceIn(0, totalCandles - 1)
                    val endIdx = (totalCandles - 1 - viewportState.scrollOffset + 2).toInt().coerceIn(startIdx + 1, totalCandles)

                    // Compute visible candle min/max & volume strictly from valid visible data
                    var minLow = Double.MAX_VALUE
                    var maxHigh = Double.MIN_VALUE
                    var maxVolume = 0L

                    for (i in startIdx until endIdx) {
                        val c = candles[i]
                        if (c.low > 0.0 && c.low < minLow) minLow = c.low
                        if (c.high > 0.0 && c.high > maxHigh) maxHigh = c.high
                        if (c.volume > maxVolume) maxVolume = c.volume
                    }

                    if (minLow == Double.MAX_VALUE || maxHigh <= minLow) {
                        minLow = 100.0
                        maxHigh = 200.0
                    }

                    // Eliminate massive empty space: Overlays ONLY expand scale if they are proximate to candle prices
                    val candleSpan = max(0.01, maxHigh - minLow)
                    val validMinBound = minLow - (candleSpan * 0.25)
                    val validMaxBound = maxHigh + (candleSpan * 0.25)

                    calculatedOverlays.forEach { ov ->
                        for (i in startIdx until endIdx) {
                            ov.primaryValues.getOrNull(i)?.let { v ->
                                if (v.isFinite() && v in validMinBound..validMaxBound) {
                                    if (v < minLow) minLow = v
                                    if (v > maxHigh) maxHigh = v
                                }
                            }
                            ov.secondaryValues?.getOrNull(i)?.let { v ->
                                if (v != null && v.isFinite() && v in validMinBound..validMaxBound) {
                                    if (v < minLow) minLow = v
                                    if (v > maxHigh) maxHigh = v
                                }
                            }
                            ov.tertiaryValues?.getOrNull(i)?.let { v ->
                                if (v != null && v.isFinite() && v in validMinBound..validMaxBound) {
                                    if (v < minLow) minLow = v
                                    if (v > maxHigh) maxHigh = v
                                }
                            }
                        }
                    }

                    // Drawings bounds safety
                    drawings.forEach { d ->
                        if (d.price1.isFinite() && d.price1 in validMinBound..validMaxBound) {
                            if (d.price1 < minLow) minLow = d.price1
                            if (d.price1 > maxHigh) maxHigh = d.price1
                        }
                    }

                    // Tight professional padding: 3.5% top & bottom gives expansive candle heights!
                    val finalSpan = max(0.01, maxHigh - minLow)
                    val verticalPadding = finalSpan * 0.035
                    val minPrice = minLow - verticalPadding
                    val maxPrice = maxHigh + verticalPadding

                    // 1. Draw Crisp Grid
                    drawFastGrid(w, priceAreaHeight, minPrice, maxPrice)

                    // 2. Draw Price Chart & Volume with clipping
                    clipRect(left = 0f, top = 0f, right = w, bottom = h) {
                        // A. Semi-transparent modern volume histogram in bottom 22%
                        drawFastVolume(
                            candles = candles,
                            startIdx = startIdx,
                            endIdx = endIdx,
                            totalCandles = totalCandles,
                            scrollOffset = viewportState.scrollOffset,
                            candleWidth = candleWidth,
                            chartWidth = w,
                            top = volumeTop,
                            height = volumeAreaHeight,
                            maxVolume = maxVolume
                        )

                        // B. Price Candles / Line / Area / Bar
                        when (chartType) {
                            ChartType.CANDLESTICK -> drawFastCandlesticks(
                                candles = candles,
                                startIdx = startIdx,
                                endIdx = endIdx,
                                totalCandles = totalCandles,
                                scrollOffset = viewportState.scrollOffset,
                                candleWidth = candleWidth,
                                chartWidth = w,
                                height = priceAreaHeight,
                                minPrice = minPrice,
                                maxPrice = maxPrice
                            )
                            ChartType.LINE -> drawFastLineChart(
                                candles = candles,
                                startIdx = startIdx,
                                endIdx = endIdx,
                                totalCandles = totalCandles,
                                scrollOffset = viewportState.scrollOffset,
                                candleWidth = candleWidth,
                                chartWidth = w,
                                height = priceAreaHeight,
                                minPrice = minPrice,
                                maxPrice = maxPrice,
                                isArea = false,
                                buffers = renderBuffers
                            )
                            ChartType.AREA -> drawFastLineChart(
                                candles = candles,
                                startIdx = startIdx,
                                endIdx = endIdx,
                                totalCandles = totalCandles,
                                scrollOffset = viewportState.scrollOffset,
                                candleWidth = candleWidth,
                                chartWidth = w,
                                height = priceAreaHeight,
                                minPrice = minPrice,
                                maxPrice = maxPrice,
                                isArea = true,
                                buffers = renderBuffers
                            )
                            ChartType.BAR -> drawFastBarChart(
                                candles = candles,
                                startIdx = startIdx,
                                endIdx = endIdx,
                                totalCandles = totalCandles,
                                scrollOffset = viewportState.scrollOffset,
                                candleWidth = candleWidth,
                                chartWidth = w,
                                height = priceAreaHeight,
                                minPrice = minPrice,
                                maxPrice = maxPrice
                            )
                        }

                        // C. Draw Indicator Overlays with zero GC churn
                        calculatedOverlays.forEachIndexed { idx, overlay ->
                            drawFastIndicatorOverlay(
                                overlay = overlay,
                                startIdx = startIdx,
                                endIdx = endIdx,
                                totalCandles = totalCandles,
                                scrollOffset = viewportState.scrollOffset,
                                candleWidth = candleWidth,
                                chartWidth = w,
                                height = priceAreaHeight,
                                minPrice = minPrice,
                                maxPrice = maxPrice,
                                bufferIdx = idx % renderBuffers.overlayPaths.size,
                                buffers = renderBuffers
                            )
                        }

                        // D. Draw Pine Script Overlays
                        if (activePineResult != null && activePineResult.isOverlay) {
                            activePineResult.plots.forEach { plot ->
                                drawFastPinePlot(
                                    plot = plot,
                                    startIdx = startIdx,
                                    endIdx = endIdx,
                                    totalCandles = totalCandles,
                                    scrollOffset = viewportState.scrollOffset,
                                    candleWidth = candleWidth,
                                    chartWidth = w,
                                    height = priceAreaHeight,
                                    minPrice = minPrice,
                                    maxPrice = maxPrice,
                                    buffers = renderBuffers
                                )
                            }
                        }

                        // E. Draw User Drawings (Horizontal Levels)
                        drawings.forEach { drawing ->
                            drawFastUserDrawing(drawing, w, priceAreaHeight, minPrice, maxPrice)
                        }

                        // F. Draw Event Markers (Earnings, 13F, Insider, Analysts)
                        if (showEventMarkers && eventMarkers.isNotEmpty()) {
                            drawFastEventMarkers(
                                markers = eventMarkers,
                                candles = candles,
                                startIdx = startIdx,
                                endIdx = endIdx,
                                totalCandles = totalCandles,
                                scrollOffset = viewportState.scrollOffset,
                                candleWidth = candleWidth,
                                chartWidth = w,
                                height = priceAreaHeight,
                                minPrice = minPrice,
                                maxPrice = maxPrice
                            )
                        }
                    }

                    // 3. Draw Right Price Axis
                    val latestClose = candles.lastOrNull()?.close ?: 0.0
                    drawFastPriceAxis(
                        axisLeft = w,
                        width = 60f,
                        height = priceAreaHeight,
                        minPrice = minPrice,
                        maxPrice = maxPrice,
                        currentPrice = latestClose,
                        axisPaint = axisTextPaint,
                        badgePaint = badgeTextPaint
                    )

                    // 4. Draw Crosshair if active
                    if (viewportState.crosshairIndex in candles.indices && viewportState.crosshairOffset != null) {
                        val c = candles[viewportState.crosshairIndex]
                        val candlesFromRight = (totalCandles - 1 - viewportState.crosshairIndex) - viewportState.scrollOffset
                        val crossX = w - (candlesFromRight + 0.5f) * candleWidth
                        val crossY = viewportState.crosshairOffset!!.y.coerceIn(0f, priceAreaHeight)

                        drawFastCrosshair(
                            x = crossX,
                            y = crossY,
                            width = w,
                            height = priceAreaHeight,
                            minPrice = minPrice,
                            maxPrice = maxPrice,
                            dateStr = c.dateStr,
                            axisPaint = axisTextPaint,
                            badgePaint = badgeTextPaint,
                            datePaint = hudDatePaint
                        )
                    }
                }

                // TradingView HUD Crosshair Floating Pill
                if (viewportState.crosshairIndex in candles.indices) {
                    val c = candles[viewportState.crosshairIndex]
                    val diff = c.close - c.open
                    val pct = if (c.open > 0) (diff / c.open) * 100 else 0.0
                    val isPos = diff >= 0
                    val clr = if (isPos) Color(0xFF10B981) else Color(0xFFEF4444)
                    val sign = if (isPos) "+" else ""

                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 8.dp, top = 6.dp),
                        color = Color(0xEE0B1220),
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(c.dateStr, fontSize = 8.5.sp, fontFamily = FontFamily.Monospace, color = TextMutedDark)
                            Text("O:${formatFastPrice(c.open)}", fontSize = 8.5.sp, fontFamily = FontFamily.Monospace, color = TextSecondaryDark)
                            Text("H:${formatFastPrice(c.high)}", fontSize = 8.5.sp, fontFamily = FontFamily.Monospace, color = TextSecondaryDark)
                            Text("L:${formatFastPrice(c.low)}", fontSize = 8.5.sp, fontFamily = FontFamily.Monospace, color = TextSecondaryDark)
                            Text("C:${formatFastPrice(c.close)}", fontSize = 8.5.sp, fontFamily = FontFamily.Monospace, color = clr, fontWeight = FontWeight.Bold)
                            Text("$sign${String.format(Locale.US, "%.2f", pct)}%", fontSize = 8.5.sp, fontFamily = FontFamily.Monospace, color = clr, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Floating Jump to Live Pill (Appears when scrolled into historical candles)
                if (viewportState.scrollOffset > 0.5f) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 66.dp, bottom = 8.dp)
                            .clickable {
                                viewportState.resetToLive(viewportState.visibleCount)
                            },
                        color = Color(0xF00B1322),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.8f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.FastForward, contentDescription = "Jump to Live", tint = CyanAccent, modifier = Modifier.size(11.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("LIVE", color = CyanAccent, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Spacer(Modifier.width(2.dp))
                            Text("(-${viewportState.scrollOffset.toInt()})", color = TextMutedDark, fontSize = 8.5.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                // Quick Zoom Controls (+ / -) in Top Right corner
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 65.dp, top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .size(22.dp)
                            .clickable {
                                viewportState.visibleCount = (viewportState.visibleCount - 10f).coerceAtLeast(10f)
                            },
                        color = Color(0xDD0D1322),
                        shape = RoundedCornerShape(3.dp),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF1E293B))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("+", color = TextPrimaryDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Surface(
                        modifier = Modifier
                            .size(22.dp)
                            .clickable {
                                val maxVis = 300f.coerceAtMost(totalCandles.toFloat())
                                viewportState.visibleCount = (viewportState.visibleCount + 10f).coerceAtMost(maxVis)
                            },
                        color = Color(0xDD0D1322),
                        shape = RoundedCornerShape(3.dp),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF1E293B))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("-", color = TextPrimaryDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 5. SUB-PANE: OSCILLATORS (RSI, MACD, STOCHASTIC, PINE)
        // Compact 68dp pane with reference bounds
        // =========================================================================
        if (calculatedPanes.isNotEmpty() || (activePineResult != null && !activePineResult.isOverlay)) {
            val paneHeight = 68.dp
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(paneHeight)
                    .background(Color(0xFF080D18))
                    .border(width = 0.5.dp, color = Color(0xFF1E293B))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width - 60f
                    val h = size.height
                    val candleWidth = w / viewportState.visibleCount

                    val startIdx = (totalCandles - 1 - viewportState.scrollOffset - viewportState.visibleCount - 1).toInt().coerceIn(0, totalCandles - 1)
                    val endIdx = (totalCandles - 1 - viewportState.scrollOffset + 2).toInt().coerceIn(startIdx + 1, totalCandles)

                    calculatedPanes.forEach { pane ->
                        drawFastOscillatorPane(
                            pane = pane,
                            startIdx = startIdx,
                            endIdx = endIdx,
                            totalCandles = totalCandles,
                            scrollOffset = viewportState.scrollOffset,
                            candleWidth = candleWidth,
                            chartWidth = w,
                            height = h,
                            buffers = renderBuffers
                        )
                    }

                    if (activePineResult != null && !activePineResult.isOverlay) {
                        drawFastPineOscillatorPane(
                            exec = activePineResult,
                            startIdx = startIdx,
                            endIdx = endIdx,
                            totalCandles = totalCandles,
                            scrollOffset = viewportState.scrollOffset,
                            candleWidth = candleWidth,
                            chartWidth = w,
                            height = h,
                            buffers = renderBuffers
                        )
                    }
                }
            }
        }

        // =========================================================================
        // 6. CHART CONTROLS BOTTOM TOOLBAR
        // Indicators, Pine, AI Reason, Chart Type, Level, Events, Fullscreen
        // =========================================================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0C1322))
                .border(width = 0.5.dp, color = Color(0xFF1E293B))
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            // Add Indicators Button
            FilledTonalButton(
                onClick = onOpenAddIndicator,
                modifier = Modifier.height(28.dp).testTag("add_indicator_button"),
                contentPadding = PaddingValues(horizontal = 7.dp, vertical = 0.dp),
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF162032))
            ) {
                Icon(Icons.Default.AddChart, contentDescription = null, modifier = Modifier.size(13.dp), tint = CyanAccent)
                Spacer(Modifier.width(3.dp))
                Text("INDICATORS", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark, fontFamily = FontFamily.Monospace)
            }

            // Pine Script Editor Button
            FilledTonalButton(
                onClick = onOpenPineEditor,
                modifier = Modifier.height(28.dp).testTag("pine_editor_button"),
                contentPadding = PaddingValues(horizontal = 7.dp, vertical = 0.dp),
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF162032))
            ) {
                Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(13.dp), tint = TerminalGreen)
                Spacer(Modifier.width(3.dp))
                Text("{ } PINE", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = TerminalGreen, fontFamily = FontFamily.Monospace)
            }

            // AI Indicator Confluence Reasoner Button
            FilledTonalButton(
                onClick = {
                    onTriggerAiAnalysis()
                    showAiReasonerSheet = true
                },
                modifier = Modifier.height(28.dp).testTag("ai_chart_analysis_button"),
                contentPadding = PaddingValues(horizontal = 7.dp, vertical = 0.dp),
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = CyanAccent.copy(alpha = 0.18f))
            ) {
                if (isAnalyzingChart) {
                    CircularProgressIndicator(modifier = Modifier.size(11.dp), strokeWidth = 1.5.dp, color = CyanAccent)
                } else {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(13.dp), tint = CyanAccent)
                }
                Spacer(Modifier.width(3.dp))
                Text("AI REASON", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = CyanAccent, fontFamily = FontFamily.Monospace)
            }

            // Chart Type Selector
            Box {
                OutlinedButton(
                    onClick = { showChartTypeMenu = true },
                    modifier = Modifier.height(28.dp),
                    contentPadding = PaddingValues(horizontal = 7.dp, vertical = 0.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF1E293B))
                ) {
                    val icon = when (chartType) {
                        ChartType.CANDLESTICK -> Icons.Default.CandlestickChart
                        ChartType.LINE -> Icons.Default.ShowChart
                        ChartType.AREA -> Icons.Default.AreaChart
                        ChartType.BAR -> Icons.Default.BarChart
                    }
                    Icon(icon, contentDescription = null, modifier = Modifier.size(13.dp), tint = TextSecondaryDark)
                    Spacer(Modifier.width(3.dp))
                    Text(chartType.label, fontSize = 9.5.sp, color = TextPrimaryDark, fontFamily = FontFamily.Monospace)
                }

                DropdownMenu(
                    expanded = showChartTypeMenu,
                    onDismissRequest = { showChartTypeMenu = false },
                    modifier = Modifier.background(Color(0xFF0F172A))
                ) {
                    ChartType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.label, color = if (type == chartType) CyanAccent else TextPrimaryDark, fontSize = 11.sp) },
                            onClick = {
                                onChartTypeSelected(type)
                                showChartTypeMenu = false
                            }
                        )
                    }
                }
            }

            // Drawings: Horizontal Support/Resistance Level
            OutlinedButton(
                onClick = {
                    val p = candles.lastOrNull()?.close ?: 100.0
                    onAddDrawing(DrawingToolType.HORIZONTAL_LINE, p)
                },
                modifier = Modifier.height(28.dp),
                contentPadding = PaddingValues(horizontal = 7.dp, vertical = 0.dp),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF1E293B))
            ) {
                Icon(Icons.Default.HorizontalRule, contentDescription = null, modifier = Modifier.size(13.dp), tint = GoldAccent)
                Spacer(Modifier.width(3.dp))
                Text("LEVEL", fontSize = 9.5.sp, color = GoldAccent, fontFamily = FontFamily.Monospace)
            }

            if (drawings.isNotEmpty()) {
                IconButton(
                    onClick = onClearDrawings,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Clear Drawings", tint = TerminalRed, modifier = Modifier.size(13.dp))
                }
            }

            // Event Markers Toggle Button
            FilledIconToggleButton(
                checked = showEventMarkers,
                onCheckedChange = { onToggleEventMarkers() },
                modifier = Modifier.size(28.dp).testTag("events_toggle_button"),
                colors = IconButtonDefaults.filledIconToggleButtonColors(
                    containerColor = Color(0xFF162032),
                    checkedContainerColor = TerminalAccent.copy(alpha = 0.25f)
                )
            ) {
                Icon(
                    Icons.Default.Flag,
                    contentDescription = "Toggle Event Markers",
                    modifier = Modifier.size(13.dp),
                    tint = if (showEventMarkers) TerminalAccent else TextSecondaryDark
                )
            }

            // Fullscreen Toggle Button
            IconButton(
                onClick = onToggleFullscreen,
                modifier = Modifier.size(28.dp).testTag("fullscreen_toggle_button")
            ) {
                Icon(
                    if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                    contentDescription = "Toggle Fullscreen",
                    tint = TextSecondaryDark,
                    modifier = Modifier.size(15.dp)
                )
            }
        }

        // =========================================================================
        // 7. EVENT DETAILS POPUP CARD (WHEN TAPPED)
        // =========================================================================
        if (selectedEvent != null) {
            val ev = selectedEvent!!
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF10192A)),
                border = androidx.compose.foundation.BorderStroke(1.dp, TerminalAccent.copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(getMarkerColor(ev.type))
                            )
                            Text(ev.title, fontWeight = FontWeight.Bold, color = TextPrimaryDark, fontSize = 12.sp)
                        }
                        IconButton(onClick = { selectedEvent = null }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondaryDark, modifier = Modifier.size(14.dp))
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(ev.subtitle, color = TerminalAccent, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(2.dp))
                    Text(ev.details, color = TextSecondaryDark, fontSize = 10.sp, lineHeight = 14.sp)
                }
            }
        }

        // =========================================================================
        // 8. AI REASONER CONFLUENCE DOCKED CARD
        // =========================================================================
        val clipboardManager = LocalClipboardManager.current
        AnimatedVisibility(
            visible = showAiReasonerSheet && (isAnalyzingChart || chartAiAnalysis != null),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1424)),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("ai_indicator_reasoner_card")
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                            Text("AI CONFLUENCE ENGINE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyanAccent, fontFamily = FontFamily.Monospace)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (chartAiAnalysis != null) {
                                IconButton(
                                    onClick = { clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(chartAiAnalysis)) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Analysis", tint = TextSecondaryDark, modifier = Modifier.size(14.dp))
                                }
                            }
                            IconButton(
                                onClick = {
                                    showAiReasonerSheet = false
                                    onClearChartAiAnalysis()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = TextSecondaryDark, modifier = Modifier.size(14.dp))
                            }
                        }
                    }

                    if (isAnalyzingChart) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)), color = CyanAccent, trackColor = Color(0xFF1E293B))
                        Text("Synthesizing price action, ${activeIndicators.size} indicators, and Pine Script outputs...", fontSize = 10.sp, color = TextSecondaryDark, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                    } else if (chartAiAnalysis != null) {
                        Surface(
                            color = Color(0xFF131C31),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = chartAiAnalysis,
                                fontSize = 11.sp,
                                color = TextPrimaryDark,
                                lineHeight = 16.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }

                        Button(
                            onClick = onTriggerAiAnalysis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("run_indicator_ai_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("RE-EVALUATE CONFLUENCE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// ZERO-ALLOCATION FAST CANVAS DRAWING IMPLEMENTATIONS (60-120 FPS TARGET)
// Reuses cached Path, Rect, and TextPaint objects to prevent GC frame pauses
// =========================================================================

private fun DrawScope.drawFastGrid(
    width: Float,
    height: Float,
    minPrice: Double,
    maxPrice: Double
) {
    val steps = 5
    for (i in 0..steps) {
        val y = height * (i / steps.toFloat())
        drawLine(
            color = Color(0xFF131D31),
            start = Offset(0f, y),
            end = Offset(width, y),
            strokeWidth = 0.8f
        )
    }
}

private fun DrawScope.drawFastCandlesticks(
    candles: List<CandleData>,
    startIdx: Int,
    endIdx: Int,
    totalCandles: Int,
    scrollOffset: Float,
    candleWidth: Float,
    chartWidth: Float,
    height: Float,
    minPrice: Double,
    maxPrice: Double
) {
    val priceRange = (maxPrice - minPrice).toFloat()
    if (priceRange <= 0f) return

    val bodyWidth = (candleWidth * 0.76f).coerceAtLeast(1.5f)

    for (i in startIdx until endIdx) {
        val c = candles[i]
        val candlesFromRight = (totalCandles - 1 - i) - scrollOffset
        val xCenter = chartWidth - (candlesFromRight + 0.5f) * candleWidth

        val isBullish = c.close >= c.open
        val color = if (isBullish) Color(0xFF10B981) else Color(0xFFEF4444)

        val highY = height - (((c.high - minPrice) / priceRange).toFloat() * height)
        val lowY = height - (((c.low - minPrice) / priceRange).toFloat() * height)
        val openY = height - (((c.open - minPrice) / priceRange).toFloat() * height)
        val closeY = height - (((c.close - minPrice) / priceRange).toFloat() * height)

        // Upper & Lower Wicks
        drawLine(
            color = color,
            start = Offset(xCenter, highY),
            end = Offset(xCenter, lowY),
            strokeWidth = 1.2f
        )

        // Candle Body
        val bodyTop = min(openY, closeY)
        val bodyHeight = max(1.5f, abs(closeY - openY))
        drawRect(
            color = color,
            topLeft = Offset(xCenter - (bodyWidth / 2f), bodyTop),
            size = Size(bodyWidth, bodyHeight)
        )
    }
}

private fun DrawScope.drawFastLineChart(
    candles: List<CandleData>,
    startIdx: Int,
    endIdx: Int,
    totalCandles: Int,
    scrollOffset: Float,
    candleWidth: Float,
    chartWidth: Float,
    height: Float,
    minPrice: Double,
    maxPrice: Double,
    isArea: Boolean,
    buffers: ChartRenderBuffers
) {
    val priceRange = (maxPrice - minPrice).toFloat()
    if (priceRange <= 0f || endIdx - startIdx < 2) return

    val path = buffers.linePath
    path.reset()
    var firstX = 0f
    var lastX = 0f

    for (i in startIdx until endIdx) {
        val c = candles[i]
        val candlesFromRight = (totalCandles - 1 - i) - scrollOffset
        val x = chartWidth - (candlesFromRight + 0.5f) * candleWidth
        val y = height - (((c.close - minPrice) / priceRange).toFloat() * height)

        if (i == startIdx) {
            path.moveTo(x, y)
            firstX = x
        } else {
            path.lineTo(x, y)
        }
        lastX = x
    }

    if (isArea) {
        val areaPath = buffers.areaPath
        areaPath.reset()
        areaPath.addPath(path)
        areaPath.lineTo(lastX, height)
        areaPath.lineTo(firstX, height)
        areaPath.close()

        drawPath(
            path = areaPath,
            brush = Brush.verticalGradient(
                colors = listOf(CyanAccent.copy(alpha = 0.35f), CyanAccent.copy(alpha = 0.02f)),
                startY = 0f,
                endY = height
            )
        )
    }

    drawPath(
        path = path,
        color = CyanAccent,
        style = Stroke(width = 2f, cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawFastBarChart(
    candles: List<CandleData>,
    startIdx: Int,
    endIdx: Int,
    totalCandles: Int,
    scrollOffset: Float,
    candleWidth: Float,
    chartWidth: Float,
    height: Float,
    minPrice: Double,
    maxPrice: Double
) {
    val priceRange = (maxPrice - minPrice).toFloat()
    if (priceRange <= 0f) return

    val tickWidth = (candleWidth * 0.4f).coerceAtLeast(2f)

    for (i in startIdx until endIdx) {
        val c = candles[i]
        val candlesFromRight = (totalCandles - 1 - i) - scrollOffset
        val xCenter = chartWidth - (candlesFromRight + 0.5f) * candleWidth

        val isBullish = c.close >= c.open
        val color = if (isBullish) Color(0xFF10B981) else Color(0xFFEF4444)

        val highY = height - (((c.high - minPrice) / priceRange).toFloat() * height)
        val lowY = height - (((c.low - minPrice) / priceRange).toFloat() * height)
        val openY = height - (((c.open - minPrice) / priceRange).toFloat() * height)
        val closeY = height - (((c.close - minPrice) / priceRange).toFloat() * height)

        drawLine(color, Offset(xCenter, highY), Offset(xCenter, lowY), strokeWidth = 1.4f)
        drawLine(color, Offset(xCenter - tickWidth, openY), Offset(xCenter, openY), strokeWidth = 1.4f)
        drawLine(color, Offset(xCenter, closeY), Offset(xCenter + tickWidth, closeY), strokeWidth = 1.4f)
    }
}

private fun DrawScope.drawFastVolume(
    candles: List<CandleData>,
    startIdx: Int,
    endIdx: Int,
    totalCandles: Int,
    scrollOffset: Float,
    candleWidth: Float,
    chartWidth: Float,
    top: Float,
    height: Float,
    maxVolume: Long
) {
    if (maxVolume <= 0) return
    val barWidth = (candleWidth * 0.72f).coerceAtLeast(1.5f)

    for (i in startIdx until endIdx) {
        val c = candles[i]
        val candlesFromRight = (totalCandles - 1 - i) - scrollOffset
        val xCenter = chartWidth - (candlesFromRight + 0.5f) * candleWidth

        val barHeight = (c.volume.toFloat() / maxVolume.toFloat()) * height
        val isBullish = c.close >= c.open
        val color = if (isBullish) Color(0xFF10B981).copy(alpha = 0.30f) else Color(0xFFEF4444).copy(alpha = 0.30f)

        drawRect(
            color = color,
            topLeft = Offset(xCenter - (barWidth / 2f), top + (height - barHeight)),
            size = Size(barWidth, barHeight)
        )
    }
}

private fun DrawScope.drawFastIndicatorOverlay(
    overlay: CalculatedSeriesResult,
    startIdx: Int,
    endIdx: Int,
    totalCandles: Int,
    scrollOffset: Float,
    candleWidth: Float,
    chartWidth: Float,
    height: Float,
    minPrice: Double,
    maxPrice: Double,
    bufferIdx: Int,
    buffers: ChartRenderBuffers
) {
    val priceRange = (maxPrice - minPrice).toFloat()
    if (priceRange <= 0f) return
    val color = Color(overlay.color)

    // 1. Primary Line (EMA, SMA, BB Basis)
    val path = buffers.overlayPaths[bufferIdx]
    path.reset()
    var started = false

    for (i in startIdx until endIdx) {
        val v = overlay.primaryValues.getOrNull(i)
        if (v != null && v.isFinite()) {
            val candlesFromRight = (totalCandles - 1 - i) - scrollOffset
            val x = chartWidth - (candlesFromRight + 0.5f) * candleWidth
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

    // 2. Secondary Line (e.g. BB Upper)
    if (overlay.secondaryValues != null) {
        val upperPath = buffers.secondaryOverlayPaths[bufferIdx]
        upperPath.reset()
        var upperStarted = false
        for (i in startIdx until endIdx) {
            val v = overlay.secondaryValues[i]
            if (v != null && v.isFinite()) {
                val candlesFromRight = (totalCandles - 1 - i) - scrollOffset
                val x = chartWidth - (candlesFromRight + 0.5f) * candleWidth
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

    // 3. Tertiary Line (e.g. BB Lower)
    if (overlay.tertiaryValues != null) {
        val lowerPath = buffers.tertiaryOverlayPaths[bufferIdx]
        lowerPath.reset()
        var lowerStarted = false
        for (i in startIdx until endIdx) {
            val v = overlay.tertiaryValues[i]
            if (v != null && v.isFinite()) {
                val candlesFromRight = (totalCandles - 1 - i) - scrollOffset
                val x = chartWidth - (candlesFromRight + 0.5f) * candleWidth
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

    // 4. SMC Labels (HH, HL, LH, LL)
    overlay.labels?.let { labels ->
        for (i in startIdx until endIdx) {
            val label = labels.getOrNull(i)
            val v = overlay.primaryValues.getOrNull(i)
            if (label != null && v != null && v.isFinite()) {
                val candlesFromRight = (totalCandles - 1 - i) - scrollOffset
                val x = chartWidth - (candlesFromRight + 0.5f) * candleWidth
                val y = height - (((v - minPrice) / priceRange).toFloat() * height)
                drawCircle(color, radius = 3.5f, center = Offset(x, y))
            }
        }
    }
}

private fun DrawScope.drawFastPinePlot(
    plot: PinePlot,
    startIdx: Int,
    endIdx: Int,
    totalCandles: Int,
    scrollOffset: Float,
    candleWidth: Float,
    chartWidth: Float,
    height: Float,
    minPrice: Double,
    maxPrice: Double,
    buffers: ChartRenderBuffers
) {
    val priceRange = (maxPrice - minPrice).toFloat()
    if (priceRange <= 0f) return

    val path = buffers.pinePlotPath
    path.reset()
    var started = false

    for (i in startIdx until endIdx) {
        val v = plot.series.getOrNull(i)
        if (v != null && v.isFinite()) {
            val candlesFromRight = (totalCandles - 1 - i) - scrollOffset
            val x = chartWidth - (candlesFromRight + 0.5f) * candleWidth
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

private fun DrawScope.drawFastUserDrawing(
    drawing: ChartDrawing,
    width: Float,
    height: Float,
    minPrice: Double,
    maxPrice: Double
) {
    val priceRange = (maxPrice - minPrice).toFloat()
    if (priceRange <= 0f) return
    val y = height - (((drawing.price1 - minPrice) / priceRange).toFloat() * height)
    val color = Color(drawing.color)

    drawLine(
        color = color,
        start = Offset(0f, y),
        end = Offset(width, y),
        strokeWidth = 1.5f
    )
}

private fun DrawScope.drawFastEventMarkers(
    markers: List<ChartEventMarker>,
    candles: List<CandleData>,
    startIdx: Int,
    endIdx: Int,
    totalCandles: Int,
    scrollOffset: Float,
    candleWidth: Float,
    chartWidth: Float,
    height: Float,
    minPrice: Double,
    maxPrice: Double
) {
    val priceRange = (maxPrice - minPrice).toFloat()
    if (priceRange <= 0f) return

    markers.forEach { marker ->
        val closestIdx = candles.indices.minByOrNull { abs(candles[it].timestamp - marker.timestamp) } ?: -1
        if (closestIdx in startIdx until endIdx) {
            val candlesFromRight = (totalCandles - 1 - closestIdx) - scrollOffset
            val x = chartWidth - (candlesFromRight + 0.5f) * candleWidth
            val y = (height - (((marker.price - minPrice) / priceRange).toFloat() * height)).coerceIn(16f, height - 16f)
            val markerColor = getMarkerColor(marker.type)

            drawCircle(color = markerColor.copy(alpha = 0.35f), radius = 8f, center = Offset(x, y))
            drawCircle(color = markerColor, radius = 5f, center = Offset(x, y))
            drawCircle(color = Color.White, radius = 2f, center = Offset(x, y))
        }
    }
}

private fun DrawScope.drawFastPriceAxis(
    axisLeft: Float,
    width: Float,
    height: Float,
    minPrice: Double,
    maxPrice: Double,
    currentPrice: Double,
    axisPaint: Paint,
    badgePaint: Paint
) {
    drawLine(Color(0xFF1E293B), Offset(axisLeft, 0f), Offset(axisLeft, height), strokeWidth = 1f)

    val priceRange = (maxPrice - minPrice).toFloat()
    if (priceRange <= 0f) return

    // 4 horizontal price ticks
    val steps = 4
    for (i in 0..steps) {
        val frac = i / steps.toFloat()
        val y = height - (frac * height)
        val priceVal = minPrice + (frac * priceRange)
        val priceStr = formatFastPrice(priceVal)
        drawContext.canvas.nativeCanvas.drawText(priceStr, axisLeft + 6f, (y + 6f).coerceIn(16f, height - 4f), axisPaint)
    }

    // Current price highlight badge
    val currY = (height - (((currentPrice - minPrice) / priceRange).toFloat() * height)).coerceIn(10f, height - 10f)
    val badgeColor = if (currentPrice >= minPrice + priceRange * 0.5) Color(0xFF10B981) else Color(0xFFEF4444)
    drawRect(
        color = badgeColor,
        topLeft = Offset(axisLeft, currY - 9f),
        size = Size(width, 18f)
    )
    drawContext.canvas.nativeCanvas.drawText(formatFastPrice(currentPrice), axisLeft + 4f, currY + 5f, badgePaint)
}

private fun DrawScope.drawFastCrosshair(
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    minPrice: Double,
    maxPrice: Double,
    dateStr: String,
    axisPaint: Paint,
    badgePaint: Paint,
    datePaint: Paint
) {
    // Vertical dotted/translucent line
    drawLine(
        color = Color.White.copy(alpha = 0.45f),
        start = Offset(x, 0f),
        end = Offset(x, height),
        strokeWidth = 1f
    )
    // Horizontal dotted/translucent line
    drawLine(
        color = Color.White.copy(alpha = 0.45f),
        start = Offset(0f, y),
        end = Offset(width, y),
        strokeWidth = 1f
    )
    drawCircle(Color.White, radius = 3f, center = Offset(x, y))

    // Price badge at right axis for crosshair
    val priceRange = (maxPrice - minPrice).toFloat()
    if (priceRange > 0f) {
        val priceAtY = maxPrice - ((y / height) * priceRange)
        drawRect(
            color = Color(0xFF334155),
            topLeft = Offset(width, y - 9f),
            size = Size(60f, 18f)
        )
        drawContext.canvas.nativeCanvas.drawText(formatFastPrice(priceAtY), width + 4f, y + 5f, badgePaint)
    }

    // Date badge at bottom of crosshair
    val dateBadgeWidth = 80f
    val badgeLeft = (x - dateBadgeWidth / 2f).coerceIn(4f, width - dateBadgeWidth - 4f)
    drawRect(
        color = Color(0xFF1E293B),
        topLeft = Offset(badgeLeft, height - 16f),
        size = Size(dateBadgeWidth, 16f)
    )
    drawContext.canvas.nativeCanvas.drawText(dateStr, badgeLeft + 4f, height - 4f, datePaint)
}

private fun DrawScope.drawFastOscillatorPane(
    pane: CalculatedSeriesResult,
    startIdx: Int,
    endIdx: Int,
    totalCandles: Int,
    scrollOffset: Float,
    candleWidth: Float,
    chartWidth: Float,
    height: Float,
    buffers: ChartRenderBuffers
) {
    val isBounded0to100 = pane.hlines.any { it == 70.0 || it == 80.0 }
    val minVal = if (isBounded0to100) 0.0 else -50.0
    val maxVal = if (isBounded0to100) 100.0 else 50.0
    val range = max(0.0001, maxVal - minVal).toFloat()

    // Reference lines
    pane.hlines.forEach { level ->
        if (level in minVal..maxVal) {
            val y = height - (((level - minVal) / range).toFloat() * height)
            drawLine(Color(0xFF223048), Offset(0f, y), Offset(chartWidth, y), strokeWidth = 0.8f)
        }
    }

    // Tertiary histogram (e.g. MACD histogram)
    pane.tertiaryValues?.let { tert ->
        val zeroY = height / 2f
        val barW = (candleWidth * 0.65f).coerceAtLeast(1.5f)
        for (i in startIdx until endIdx) {
            val v = tert.getOrNull(i)
            if (v != null && v.isFinite()) {
                val candlesFromRight = (totalCandles - 1 - i) - scrollOffset
                val x = chartWidth - (candlesFromRight + 0.5f) * candleWidth
                val y = height - (((v - minVal) / range).toFloat() * height)
                val barTop = min(y, zeroY)
                val barH = max(1f, abs(y - zeroY))
                val barColor = if (v >= 0) Color(0xFF10B981).copy(alpha = 0.7f) else Color(0xFFEF4444).copy(alpha = 0.7f)
                drawRect(color = barColor, topLeft = Offset(x - barW / 2f, barTop), size = Size(barW, barH))
            }
        }
    }

    // Secondary line (e.g. MACD Signal, Stoch %D)
    pane.secondaryValues?.let { sec ->
        val secPath = buffers.oscillatorSecondaryPath
        secPath.reset()
        var started = false
        for (i in startIdx until endIdx) {
            val v = sec.getOrNull(i)
            if (v != null && v.isFinite()) {
                val candlesFromRight = (totalCandles - 1 - i) - scrollOffset
                val x = chartWidth - (candlesFromRight + 0.5f) * candleWidth
                val y = height - (((v - minVal) / range).toFloat() * height)
                if (!started) {
                    secPath.moveTo(x, y)
                    started = true
                } else {
                    secPath.lineTo(x, y)
                }
            }
        }
        if (started) {
            drawPath(secPath, Color(0xFFF59E0B), style = Stroke(width = 1.4f))
        }
    }

    // Primary oscillator curve
    val path = buffers.oscillatorPrimaryPath
    path.reset()
    var started = false
    for (i in startIdx until endIdx) {
        val v = pane.primaryValues.getOrNull(i)
        if (v != null && v.isFinite()) {
            val candlesFromRight = (totalCandles - 1 - i) - scrollOffset
            val x = chartWidth - (candlesFromRight + 0.5f) * candleWidth
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

private fun DrawScope.drawFastPineOscillatorPane(
    exec: PineExecutionResult,
    startIdx: Int,
    endIdx: Int,
    totalCandles: Int,
    scrollOffset: Float,
    candleWidth: Float,
    chartWidth: Float,
    height: Float,
    buffers: ChartRenderBuffers
) {
    val plot = exec.plots.firstOrNull() ?: return
    val minV = 0.0
    val maxV = 100.0
    val range = (maxV - minV).toFloat()

    val path = buffers.pinePlotPath
    path.reset()
    var started = false

    for (i in startIdx until endIdx) {
        val v = plot.series.getOrNull(i)
        if (v != null && v.isFinite()) {
            val candlesFromRight = (totalCandles - 1 - i) - scrollOffset
            val x = chartWidth - (candlesFromRight + 0.5f) * candleWidth
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

private fun formatFastPrice(price: Double): String {
    if (price.isNaN() || price.isInfinite()) return "--"
    return String.format(Locale.US, "%.2f", price)
}

private fun getMarkerColor(type: ChartMarkerType): Color {
    return when (type) {
        ChartMarkerType.EARNINGS_BEAT -> Color(0xFF10B981)
        ChartMarkerType.EARNINGS_MISS -> Color(0xFFEF4444)
        ChartMarkerType.INSTITUTIONAL_13F -> Color(0xFFF59E0B)
        ChartMarkerType.INSIDER_BUY -> Color(0xFF38BDF8)
        ChartMarkerType.INSIDER_SELL -> Color(0xFFF97316)
        ChartMarkerType.ANALYST_UPGRADE -> Color(0xFFA855F7)
        ChartMarkerType.ANALYST_DOWNGRADE -> Color(0xFF94A3B8)
        ChartMarkerType.DIVIDEND -> Color(0xFF10B981)
    }
}
