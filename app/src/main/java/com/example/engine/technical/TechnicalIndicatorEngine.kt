package com.example.engine.technical

import com.example.data.model.CandleData
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

enum class IndicatorCategory(val label: String) {
    TREND("Trend"),
    OSCILLATORS("Oscillators"),
    VOLATILITY("Volatility"),
    VOLUME("Volume")
}

enum class BuiltInIndicatorType(
    val displayName: String,
    val isOverlay: Boolean,
    val category: IndicatorCategory,
    val defaultParams: Map<String, String>,
    val description: String = ""
) {
    MOVING_AVERAGE("Moving Average", true, IndicatorCategory.TREND, mapOf("period" to "20", "method" to "EMA", "source" to "close", "shift" to "0"), "Trend line with customizable period, method (SMA, EMA, SMMA, LWMA), and applied price."),
    EMA("Exponential Moving Average", true, IndicatorCategory.TREND, mapOf("period" to "200", "source" to "close", "shift" to "0"), "Exponentially weighted moving average giving higher weight to recent prices."),
    SMA("Simple Moving Average", true, IndicatorCategory.TREND, mapOf("period" to "50", "source" to "close", "shift" to "0"), "Arithmetic mean of prices over specified period."),
    SMMA("Smoothed Moving Average", true, IndicatorCategory.TREND, mapOf("period" to "20", "source" to "close", "shift" to "0"), "MetaTrader smoothed moving average for noise reduction."),
    LWMA("Linear Weighted Moving Average", true, IndicatorCategory.TREND, mapOf("period" to "20", "source" to "close", "shift" to "0"), "Linearly weighted moving average emphasizing recent bars."),
    BOLLINGER_BANDS("Bollinger Bands", true, IndicatorCategory.TREND, mapOf("period" to "20", "stdDev" to "2.0", "shift" to "0", "source" to "close"), "Volatility bands plotted around moving average based on standard deviation."),
    KELTNER_CHANNELS("Keltner Channels", true, IndicatorCategory.TREND, mapOf("period" to "20", "mult" to "1.5"), "Volatility-based envelope using ATR around EMA."),
    SWING_STRUCTURE("Smart Money Structure (HH/HL/LH/LL)", true, IndicatorCategory.TREND, mapOf("lookback" to "5"), "Identifies higher highs, higher lows, lower highs, and lower lows."),
    
    RSI("Relative Strength Index (RSI)", false, IndicatorCategory.OSCILLATORS, mapOf("period" to "14", "source" to "close", "overbought" to "70", "oversold" to "30"), "Momentum oscillator measuring speed and change of price movements."),
    MACD("MACD (Moving Average Convergence Divergence)", false, IndicatorCategory.OSCILLATORS, mapOf("fast" to "12", "slow" to "26", "signal" to "9", "source" to "close"), "Trend-following momentum indicator showing relationship between two moving averages."),
    STOCHASTIC("Stochastic Oscillator", false, IndicatorCategory.OSCILLATORS, mapOf("periodK" to "14", "smoothK" to "3", "periodD" to "3", "overbought" to "80", "oversold" to "20"), "Compares closing price to price range over given period."),
    ADX("Average Directional Index (ADX)", false, IndicatorCategory.OSCILLATORS, mapOf("period" to "14"), "Measures overall trend strength with +DI and -DI directional lines."),
    CCI("Commodity Channel Index (CCI)", false, IndicatorCategory.OSCILLATORS, mapOf("period" to "14", "source" to "typical"), "Identifies cyclical turns in price relative to moving average."),
    WILLIAMS_R("Williams %R", false, IndicatorCategory.OSCILLATORS, mapOf("period" to "14"), "Dynamic momentum indicator reflecting level of close relative to high-low range."),
    MOMENTUM("Momentum", false, IndicatorCategory.OSCILLATORS, mapOf("period" to "14", "source" to "close"), "Measures rate of change of price over given period."),
    
    ATR("Average True Range (ATR)", false, IndicatorCategory.VOLATILITY, mapOf("period" to "14"), "Measures market volatility by decomposing the range of price."),
    STANDARD_DEVIATION("Standard Deviation (StdDev)", false, IndicatorCategory.VOLATILITY, mapOf("period" to "20", "source" to "close"), "Statistical measurement of market volatility around mean."),
    
    OBV("On Balance Volume (OBV)", false, IndicatorCategory.VOLUME, mapOf(), "Cumulative volume indicator relating volume to price changes."),
    VOLUME_MA("Volume Moving Average", false, IndicatorCategory.VOLUME, mapOf("period" to "20"), "Simple moving average plotted across volume histogram.")
}

data class ActiveIndicator(
    val id: String,
    val type: BuiltInIndicatorType,
    val title: String,
    val isVisible: Boolean = true,
    val isOverlay: Boolean = type.isOverlay,
    val params: Map<String, String> = type.defaultParams,
    val color: Long = when (type) {
        BuiltInIndicatorType.MOVING_AVERAGE -> 0xFFF59E0B // Gold
        BuiltInIndicatorType.EMA -> 0xFF06B6D4 // Aqua
        BuiltInIndicatorType.SMA -> 0xFF38BDF8 // Sky Blue
        BuiltInIndicatorType.SMMA -> 0xFF10B981 // Emerald
        BuiltInIndicatorType.LWMA -> 0xFFA855F7 // Purple
        BuiltInIndicatorType.BOLLINGER_BANDS -> 0xFF818CF8 // Indigo
        BuiltInIndicatorType.KELTNER_CHANNELS -> 0xFFEC4899 // Pink
        BuiltInIndicatorType.RSI -> 0xFFA855F7 // Purple
        BuiltInIndicatorType.MACD -> 0xFF38BDF8 // Cyan
        BuiltInIndicatorType.STOCHASTIC -> 0xFFF59E0B // Gold
        BuiltInIndicatorType.ADX -> 0xFF10B981 // Emerald
        BuiltInIndicatorType.CCI -> 0xFF06B6D4 // Cyan
        BuiltInIndicatorType.WILLIAMS_R -> 0xFFEC4899 // Pink
        BuiltInIndicatorType.MOMENTUM -> 0xFF3B82F6 // Blue
        BuiltInIndicatorType.ATR -> 0xFFF59E0B // Amber
        BuiltInIndicatorType.STANDARD_DEVIATION -> 0xFF8B5CF6 // Violet
        BuiltInIndicatorType.SWING_STRUCTURE -> 0xFF10B981 // Emerald
        else -> 0xFF38BDF8
    }
)

data class CalculatedSeriesResult(
    val indicatorId: String,
    val title: String,
    val isOverlay: Boolean,
    val primaryValues: List<Double?>,
    val secondaryValues: List<Double?>? = null,
    val tertiaryValues: List<Double?>? = null,
    val labels: List<String?>? = null,
    val color: Long,
    val hlines: List<Double> = emptyList(),
    val secondaryColor: Long? = null,
    val tertiaryColor: Long? = null
)

object TechnicalIndicatorEngine {

    fun extractSource(candles: List<CandleData>, sourceParam: String?): List<Double> {
        return when (sourceParam?.lowercase()?.trim()) {
            "open" -> candles.map { it.open }
            "high" -> candles.map { it.high }
            "low" -> candles.map { it.low }
            "median", "hl2" -> candles.map { (it.high + it.low) / 2.0 }
            "typical", "hlc3" -> candles.map { (it.high + it.low + it.close) / 3.0 }
            "weighted", "hlcc4" -> candles.map { (it.high + it.low + 2.0 * it.close) / 4.0 }
            else -> candles.map { it.close } // default close
        }
    }

    fun applyShift(series: List<Double?>, shift: Int): List<Double?> {
        if (shift == 0) return series
        val result = ArrayList<Double?>(series.size)
        for (i in series.indices) {
            val srcIdx = i - shift
            if (srcIdx in series.indices) {
                result.add(series[srcIdx])
            } else {
                result.add(null)
            }
        }
        return result
    }

    fun calculate(indicator: ActiveIndicator, candles: List<CandleData>): CalculatedSeriesResult {
        if (candles.isEmpty()) {
            return CalculatedSeriesResult(indicator.id, indicator.title, indicator.isOverlay, emptyList(), color = indicator.color)
        }

        return when (indicator.type) {
            BuiltInIndicatorType.MOVING_AVERAGE -> {
                val period = indicator.params["period"]?.toIntOrNull() ?: 20
                val method = indicator.params["method"]?.uppercase() ?: "EMA"
                val source = extractSource(candles, indicator.params["source"])
                val shift = indicator.params["shift"]?.toIntOrNull() ?: 0

                val rawValues = when (method) {
                    "SMA" -> computeSMA(source, period)
                    "SMMA" -> computeSMMA(source, period)
                    "LWMA" -> computeLWMA(source, period)
                    else -> computeEMA(source, period)
                }
                val shiftedValues = applyShift(rawValues, shift)
                CalculatedSeriesResult(indicator.id, "$method $period", true, shiftedValues, color = indicator.color)
            }
            BuiltInIndicatorType.SMA -> {
                val len = indicator.params["period"]?.toIntOrNull() ?: indicator.params["length"]?.toIntOrNull() ?: 50
                val source = extractSource(candles, indicator.params["source"])
                val shift = indicator.params["shift"]?.toIntOrNull() ?: 0
                val values = applyShift(computeSMA(source, len), shift)
                CalculatedSeriesResult(indicator.id, "SMA $len", true, values, color = indicator.color)
            }
            BuiltInIndicatorType.EMA -> {
                val len = indicator.params["period"]?.toIntOrNull() ?: indicator.params["length"]?.toIntOrNull() ?: 200
                val source = extractSource(candles, indicator.params["source"])
                val shift = indicator.params["shift"]?.toIntOrNull() ?: 0
                val values = applyShift(computeEMA(source, len), shift)
                CalculatedSeriesResult(indicator.id, "EMA $len", true, values, color = indicator.color)
            }
            BuiltInIndicatorType.SMMA -> {
                val len = indicator.params["period"]?.toIntOrNull() ?: indicator.params["length"]?.toIntOrNull() ?: 20
                val source = extractSource(candles, indicator.params["source"])
                val shift = indicator.params["shift"]?.toIntOrNull() ?: 0
                val values = applyShift(computeSMMA(source, len), shift)
                CalculatedSeriesResult(indicator.id, "SMMA $len", true, values, color = indicator.color)
            }
            BuiltInIndicatorType.LWMA -> {
                val len = indicator.params["period"]?.toIntOrNull() ?: indicator.params["length"]?.toIntOrNull() ?: 20
                val source = extractSource(candles, indicator.params["source"])
                val shift = indicator.params["shift"]?.toIntOrNull() ?: 0
                val values = applyShift(computeLWMA(source, len), shift)
                CalculatedSeriesResult(indicator.id, "LWMA $len", true, values, color = indicator.color)
            }
            BuiltInIndicatorType.BOLLINGER_BANDS -> {
                val len = indicator.params["period"]?.toIntOrNull() ?: indicator.params["length"]?.toIntOrNull() ?: 20
                val stdDevMult = indicator.params["stdDev"]?.toDoubleOrNull() ?: 2.0
                val shift = indicator.params["shift"]?.toIntOrNull() ?: 0
                val source = extractSource(candles, indicator.params["source"])
                val (basis, upper, lower) = computeBollingerBands(source, len, stdDevMult)
                CalculatedSeriesResult(
                    indicator.id,
                    "BB ($len, $stdDevMult)",
                    true,
                    primaryValues = applyShift(basis, shift),
                    secondaryValues = applyShift(upper, shift),
                    tertiaryValues = applyShift(lower, shift),
                    color = indicator.color,
                    secondaryColor = 0xFF818CF8,
                    tertiaryColor = 0xFF818CF8
                )
            }
            BuiltInIndicatorType.KELTNER_CHANNELS -> {
                val len = indicator.params["period"]?.toIntOrNull() ?: indicator.params["length"]?.toIntOrNull() ?: 20
                val mult = indicator.params["mult"]?.toDoubleOrNull() ?: 1.5
                val (mid, upper, lower) = computeKeltner(candles, len, mult)
                CalculatedSeriesResult(indicator.id, "KC ($len, $mult)", true, mid, upper, lower, color = indicator.color)
            }
            BuiltInIndicatorType.SWING_STRUCTURE -> {
                val lookback = indicator.params["lookback"]?.toIntOrNull() ?: 5
                val (labels, points) = computeSwingStructure(candles, lookback)
                CalculatedSeriesResult(
                    indicator.id,
                    "SMC Structure",
                    true,
                    primaryValues = points,
                    labels = labels,
                    color = indicator.color
                )
            }
            BuiltInIndicatorType.RSI -> {
                val len = indicator.params["period"]?.toIntOrNull() ?: indicator.params["length"]?.toIntOrNull() ?: 14
                val ob = indicator.params["overbought"]?.toDoubleOrNull() ?: 70.0
                val os = indicator.params["oversold"]?.toDoubleOrNull() ?: 30.0
                val source = extractSource(candles, indicator.params["source"])
                val values = computeRSI(source, len)
                CalculatedSeriesResult(
                    indicator.id,
                    "RSI $len",
                    false,
                    values,
                    color = indicator.color,
                    hlines = listOf(ob, os, 50.0)
                )
            }
            BuiltInIndicatorType.MACD -> {
                val fast = indicator.params["fast"]?.toIntOrNull() ?: 12
                val slow = indicator.params["slow"]?.toIntOrNull() ?: 26
                val signal = indicator.params["signal"]?.toIntOrNull() ?: 9
                val source = extractSource(candles, indicator.params["source"])
                val (macdLine, signalLine, hist) = computeMACD(source, fast, slow, signal)
                CalculatedSeriesResult(
                    indicator.id,
                    "MACD ($fast, $slow, $signal)",
                    false,
                    primaryValues = macdLine,
                    secondaryValues = signalLine,
                    tertiaryValues = hist,
                    color = indicator.color,
                    secondaryColor = 0xFFF59E0B, // Signal line Orange
                    tertiaryColor = 0xFF10B981,  // Histogram Green/Red
                    hlines = listOf(0.0)
                )
            }
            BuiltInIndicatorType.STOCHASTIC -> {
                val periodK = indicator.params["periodK"]?.toIntOrNull() ?: 14
                val smoothK = indicator.params["smoothK"]?.toIntOrNull() ?: 3
                val periodD = indicator.params["periodD"]?.toIntOrNull() ?: 3
                val ob = indicator.params["overbought"]?.toDoubleOrNull() ?: 80.0
                val os = indicator.params["oversold"]?.toDoubleOrNull() ?: 20.0
                val (kValues, dValues) = computeStochastic(candles, periodK, smoothK, periodD)
                CalculatedSeriesResult(
                    indicator.id,
                    "Stoch ($periodK, $smoothK, $periodD)",
                    false,
                    primaryValues = kValues,
                    secondaryValues = dValues,
                    color = indicator.color,
                    secondaryColor = 0xFFEC4899,
                    hlines = listOf(ob, os, 50.0)
                )
            }
            BuiltInIndicatorType.ADX -> {
                val len = indicator.params["period"]?.toIntOrNull() ?: indicator.params["length"]?.toIntOrNull() ?: 14
                val (adx, plusDi, minusDi) = computeADX(candles, len)
                CalculatedSeriesResult(
                    indicator.id,
                    "ADX $len",
                    false,
                    primaryValues = adx,
                    secondaryValues = plusDi,
                    tertiaryValues = minusDi,
                    color = indicator.color,
                    secondaryColor = 0xFF10B981, // +DI Green
                    tertiaryColor = 0xFFEF4444,  // -DI Red
                    hlines = listOf(20.0, 25.0, 40.0)
                )
            }
            BuiltInIndicatorType.CCI -> {
                val len = indicator.params["period"]?.toIntOrNull() ?: indicator.params["length"]?.toIntOrNull() ?: 14
                val values = computeCCI(candles, len)
                CalculatedSeriesResult(
                    indicator.id,
                    "CCI $len",
                    false,
                    values,
                    color = indicator.color,
                    hlines = listOf(100.0, -100.0, 0.0)
                )
            }
            BuiltInIndicatorType.WILLIAMS_R -> {
                val len = indicator.params["period"]?.toIntOrNull() ?: indicator.params["length"]?.toIntOrNull() ?: 14
                val values = computeWilliamsR(candles, len)
                CalculatedSeriesResult(
                    indicator.id,
                    "Williams %R $len",
                    false,
                    values,
                    color = indicator.color,
                    hlines = listOf(-20.0, -80.0, -50.0)
                )
            }
            BuiltInIndicatorType.MOMENTUM -> {
                val len = indicator.params["period"]?.toIntOrNull() ?: indicator.params["length"]?.toIntOrNull() ?: 14
                val source = extractSource(candles, indicator.params["source"])
                val values = computeMomentum(source, len)
                CalculatedSeriesResult(
                    indicator.id,
                    "Momentum $len",
                    false,
                    values,
                    color = indicator.color,
                    hlines = listOf(100.0)
                )
            }
            BuiltInIndicatorType.STANDARD_DEVIATION -> {
                val len = indicator.params["period"]?.toIntOrNull() ?: indicator.params["length"]?.toIntOrNull() ?: 20
                val source = extractSource(candles, indicator.params["source"])
                val values = computeStdDev(source, len)
                CalculatedSeriesResult(
                    indicator.id,
                    "StdDev $len",
                    false,
                    values,
                    color = indicator.color
                )
            }
            BuiltInIndicatorType.ATR -> {
                val len = indicator.params["period"]?.toIntOrNull() ?: indicator.params["length"]?.toIntOrNull() ?: 14
                val values = computeATR(candles, len)
                CalculatedSeriesResult(indicator.id, "ATR $len", false, values, color = indicator.color)
            }
            BuiltInIndicatorType.OBV -> {
                val values = computeOBV(candles)
                CalculatedSeriesResult(indicator.id, "OBV", false, values, color = indicator.color)
            }
            BuiltInIndicatorType.VOLUME_MA -> {
                val len = indicator.params["period"]?.toIntOrNull() ?: indicator.params["length"]?.toIntOrNull() ?: 20
                val values = computeSMA(candles.map { it.volume.toDouble() }, len)
                CalculatedSeriesResult(indicator.id, "Vol MA $len", false, values, color = indicator.color)
            }
        }
    }

    fun computeSMA(data: List<Double>, length: Int): List<Double?> {
        val result = mutableListOf<Double?>()
        var sum = 0.0
        for (i in data.indices) {
            sum += data[i]
            if (i >= length) {
                sum -= data[i - length]
            }
            if (i >= length - 1) {
                result.add(sum / length)
            } else {
                result.add(null)
            }
        }
        return result
    }

    fun computeEMA(data: List<Double>, length: Int): List<Double?> {
        val result = mutableListOf<Double?>()
        val multiplier = 2.0 / (length + 1.0)
        var prevEma: Double? = null

        for (i in data.indices) {
            if (i < length - 1) {
                result.add(null)
            } else if (i == length - 1) {
                val sma = data.subList(0, length).average()
                prevEma = sma
                result.add(sma)
            } else {
                val current = (data[i] - prevEma!!) * multiplier + prevEma
                prevEma = current
                result.add(current)
            }
        }
        return result
    }

    fun computeRMA(data: List<Double>, length: Int): List<Double?> = computeSMMA(data, length)

    fun computeSMMA(data: List<Double>, length: Int): List<Double?> {
        val result = mutableListOf<Double?>()
        var prevSmma: Double? = null
        for (i in data.indices) {
            if (i < length - 1) {
                result.add(null)
            } else if (i == length - 1) {
                val sum = data.subList(0, length).sum()
                val smma = sum / length
                prevSmma = smma
                result.add(smma)
            } else {
                val current = (prevSmma!! * (length - 1) + data[i]) / length
                prevSmma = current
                result.add(current)
            }
        }
        return result
    }

    fun computeLWMA(data: List<Double>, length: Int): List<Double?> {
        val result = mutableListOf<Double?>()
        val denom = (length * (length + 1)) / 2.0
        for (i in data.indices) {
            if (i < length - 1) {
                result.add(null)
            } else {
                var sum = 0.0
                for (j in 0 until length) {
                    sum += data[i - length + 1 + j] * (j + 1)
                }
                result.add(sum / denom)
            }
        }
        return result
    }

    fun computeWMA(data: List<Double>, length: Int): List<Double?> {
        return computeLWMA(data, length)
    }

    fun computeHMA(data: List<Double>, length: Int): List<Double?> {
        val halfLen = max(1, length / 2)
        val sqrtLen = max(1, sqrt(length.toDouble()).toInt())
        val wmaHalf = computeLWMA(data, halfLen)
        val wmaFull = computeLWMA(data, length)

        val diffSeries = mutableListOf<Double>()
        for (i in data.indices) {
            val h = wmaHalf[i]
            val f = wmaFull[i]
            if (h != null && f != null) {
                diffSeries.add(2.0 * h - f)
            } else {
                diffSeries.add(0.0)
            }
        }
        val hmaRaw = computeLWMA(diffSeries, sqrtLen)
        return data.indices.map { i ->
            if (i < length + sqrtLen - 2) null else hmaRaw[i]
        }
    }

    fun computeBollingerBands(data: List<Double>, length: Int, stdDevMult: Double): Triple<List<Double?>, List<Double?>, List<Double?>> {
        val basis = computeSMA(data, length)
        val upper = mutableListOf<Double?>()
        val lower = mutableListOf<Double?>()

        for (i in data.indices) {
            val mean = basis[i]
            if (mean == null || i < length - 1) {
                upper.add(null)
                lower.add(null)
            } else {
                var sumSq = 0.0
                for (j in (i - length + 1)..i) {
                    val d = data[j] - mean
                    sumSq += d * d
                }
                val stdDev = sqrt(sumSq / length)
                upper.add(mean + stdDevMult * stdDev)
                lower.add(mean - stdDevMult * stdDev)
            }
        }
        return Triple(basis, upper, lower)
    }

    fun computeATR(candles: List<CandleData>, length: Int): List<Double?> {
        val tr = mutableListOf<Double>()
        for (i in candles.indices) {
            if (i == 0) {
                tr.add(candles[i].high - candles[i].low)
            } else {
                val h = candles[i].high
                val l = candles[i].low
                val prevC = candles[i - 1].close
                tr.add(max(h - l, max(abs(h - prevC), abs(l - prevC))))
            }
        }
        return computeWildersSmoothing(tr, length)
    }

    fun computeADX(candles: List<CandleData>, length: Int): Triple<List<Double?>, List<Double?>, List<Double?>> {
        val trList = mutableListOf<Double>()
        val plusDmList = mutableListOf<Double>()
        val minusDmList = mutableListOf<Double>()

        for (i in candles.indices) {
            if (i == 0) {
                trList.add(candles[i].high - candles[i].low)
                plusDmList.add(0.0)
                minusDmList.add(0.0)
            } else {
                val h = candles[i].high
                val l = candles[i].low
                val prevC = candles[i - 1].close
                val prevH = candles[i - 1].high
                val prevL = candles[i - 1].low

                val tr = max(h - l, max(abs(h - prevC), abs(l - prevC)))
                trList.add(tr)

                val upMove = h - prevH
                val downMove = prevL - l

                if (upMove > downMove && upMove > 0) plusDmList.add(upMove) else plusDmList.add(0.0)
                if (downMove > upMove && downMove > 0) minusDmList.add(downMove) else minusDmList.add(0.0)
            }
        }

        val smoothedTr = computeWildersSmoothing(trList, length)
        val smoothedPlusDm = computeWildersSmoothing(plusDmList, length)
        val smoothedMinusDm = computeWildersSmoothing(minusDmList, length)

        val plusDi = mutableListOf<Double?>()
        val minusDi = mutableListOf<Double?>()
        val dxList = mutableListOf<Double?>()

        for (i in candles.indices) {
            val tr = smoothedTr[i]
            val pDm = smoothedPlusDm[i]
            val mDm = smoothedMinusDm[i]

            if (tr != null && pDm != null && mDm != null && tr > 0) {
                val pDiVal = (pDm / tr) * 100.0
                val mDiVal = (mDm / tr) * 100.0
                plusDi.add(pDiVal)
                minusDi.add(mDiVal)

                val diSum = pDiVal + mDiVal
                val dx = if (diSum > 0) (abs(pDiVal - mDiVal) / diSum) * 100.0 else 0.0
                dxList.add(dx)
            } else {
                plusDi.add(null)
                minusDi.add(null)
                dxList.add(null)
            }
        }

        val validDx = dxList.map { it ?: 0.0 }
        val adx = computeWildersSmoothing(validDx, length)

        return Triple(adx, plusDi, minusDi)
    }

    fun computeCCI(candles: List<CandleData>, length: Int): List<Double?> {
        val tp = candles.map { (it.high + it.low + it.close) / 3.0 }
        val smaTp = computeSMA(tp, length)
        val result = mutableListOf<Double?>()

        for (i in candles.indices) {
            val mean = smaTp[i]
            if (mean == null || i < length - 1) {
                result.add(null)
            } else {
                var meanDevSum = 0.0
                for (j in (i - length + 1)..i) {
                    meanDevSum += abs(tp[j] - mean)
                }
                val meanDev = meanDevSum / length
                if (meanDev == 0.0) {
                    result.add(0.0)
                } else {
                    result.add((tp[i] - mean) / (0.015 * meanDev))
                }
            }
        }
        return result
    }

    fun computeWilliamsR(candles: List<CandleData>, length: Int): List<Double?> {
        val result = mutableListOf<Double?>()
        for (i in candles.indices) {
            if (i < length - 1) {
                result.add(null)
            } else {
                val slice = candles.subList(i - length + 1, i + 1)
                val highest = slice.maxOf { it.high }
                val lowest = slice.minOf { it.low }
                val denom = highest - lowest
                if (denom == 0.0) {
                    result.add(-50.0)
                } else {
                    val wr = ((highest - candles[i].close) / denom) * -100.0
                    result.add(wr)
                }
            }
        }
        return result
    }

    fun computeMomentum(data: List<Double>, length: Int): List<Double?> {
        val result = mutableListOf<Double?>()
        for (i in data.indices) {
            if (i < length) {
                result.add(null)
            } else {
                val prev = data[i - length]
                if (prev != 0.0) {
                    result.add((data[i] / prev) * 100.0)
                } else {
                    result.add(100.0)
                }
            }
        }
        return result
    }

    fun computeStdDev(data: List<Double>, length: Int): List<Double?> {
        val sma = computeSMA(data, length)
        val result = mutableListOf<Double?>()
        for (i in data.indices) {
            val mean = sma[i]
            if (mean == null || i < length - 1) {
                result.add(null)
            } else {
                var sumSq = 0.0
                for (j in (i - length + 1)..i) {
                    val d = data[j] - mean
                    sumSq += d * d
                }
                result.add(sqrt(sumSq / length))
            }
        }
        return result
    }

    private fun computeWildersSmoothing(data: List<Double>, length: Int): List<Double?> {
        val result = mutableListOf<Double?>()
        var prev: Double? = null
        for (i in data.indices) {
            if (i < length - 1) {
                result.add(null)
            } else if (i == length - 1) {
                val sum = data.subList(0, length).sum()
                val initial = sum / length
                prev = initial
                result.add(initial)
            } else {
                val current = (prev!! * (length - 1) + data[i]) / length
                prev = current
                result.add(current)
            }
        }
        return result
    }

    fun computeKeltner(candles: List<CandleData>, length: Int, multiplier: Double): Triple<List<Double?>, List<Double?>, List<Double?>> {
        val mid = computeEMA(candles.map { it.close }, length)
        val atr = computeATR(candles, length)
        val upper = mutableListOf<Double?>()
        val lower = mutableListOf<Double?>()

        for (i in candles.indices) {
            val m = mid[i]
            val a = atr[i]
            if (m != null && a != null) {
                upper.add(m + a * multiplier)
                lower.add(m - a * multiplier)
            } else {
                upper.add(null)
                lower.add(null)
            }
        }
        return Triple(mid, upper, lower)
    }

    fun computeRSI(data: List<Double>, length: Int): List<Double?> {
        val result = mutableListOf<Double?>()
        if (data.size < length + 1) {
            return List(data.size) { null }
        }

        var avgGain = 0.0
        var avgLoss = 0.0

        for (i in 1..length) {
            val diff = data[i] - data[i - 1]
            if (diff >= 0) avgGain += diff else avgLoss += -diff
        }
        avgGain /= length
        avgLoss /= length

        for (i in 0 until length) {
            result.add(null)
        }

        val rs = if (avgLoss == 0.0) 100.0 else avgGain / avgLoss
        result.add(100.0 - (100.0 / (1.0 + rs)))

        for (i in (length + 1) until data.size) {
            val diff = data[i] - data[i - 1]
            val gain = if (diff >= 0) diff else 0.0
            val loss = if (diff < 0) -diff else 0.0

            avgGain = (avgGain * (length - 1) + gain) / length
            avgLoss = (avgLoss * (length - 1) + loss) / length

            val currentRs = if (avgLoss == 0.0) 100.0 else avgGain / avgLoss
            result.add(100.0 - (100.0 / (1.0 + currentRs)))
        }
        return result
    }

    fun computeMACD(data: List<Double>, fastLen: Int, slowLen: Int, signalLen: Int): Triple<List<Double?>, List<Double?>, List<Double?>> {
        val fastEma = computeEMA(data, fastLen)
        val slowEma = computeEMA(data, slowLen)
        val macdLine = mutableListOf<Double?>()

        for (i in data.indices) {
            val f = fastEma[i]
            val s = slowEma[i]
            if (f != null && s != null) {
                macdLine.add(f - s)
            } else {
                macdLine.add(null)
            }
        }

        val validMacdValues = macdLine.filterNotNull()
        val signalEma = computeEMA(validMacdValues, signalLen)
        val signalLine = mutableListOf<Double?>()
        var validIdx = 0

        for (i in data.indices) {
            if (macdLine[i] == null) {
                signalLine.add(null)
            } else {
                signalLine.add(signalEma.getOrNull(validIdx))
                validIdx++
            }
        }

        val histogram = mutableListOf<Double?>()
        for (i in data.indices) {
            val m = macdLine[i]
            val sig = signalLine[i]
            if (m != null && sig != null) {
                histogram.add(m - sig)
            } else {
                histogram.add(null)
            }
        }

        return Triple(macdLine, signalLine, histogram)
    }

    fun computeStochastic(candles: List<CandleData>, periodK: Int, smoothK: Int, periodD: Int): Pair<List<Double?>, List<Double?>> {
        val rawK = mutableListOf<Double?>()

        for (i in candles.indices) {
            if (i < periodK - 1) {
                rawK.add(null)
            } else {
                val sub = candles.subList(i - periodK + 1, i + 1)
                val highestHigh = sub.maxOf { it.high }
                val lowestLow = sub.minOf { it.low }
                val currentClose = candles[i].close

                val k = if (highestHigh == lowestLow) 50.0 else ((currentClose - lowestLow) / (highestHigh - lowestLow)) * 100.0
                rawK.add(k)
            }
        }

        val smoothedK = mutableListOf<Double?>()
        for (i in rawK.indices) {
            if (i < periodK - 1 + smoothK - 1) {
                smoothedK.add(null)
            } else {
                val window = rawK.subList(i - smoothK + 1, i + 1).filterNotNull()
                if (window.size == smoothK) {
                    smoothedK.add(window.average())
                } else {
                    smoothedK.add(null)
                }
            }
        }

        val periodDValues = mutableListOf<Double?>()
        for (i in smoothedK.indices) {
            if (i < periodK - 1 + smoothK - 1 + periodD - 1) {
                periodDValues.add(null)
            } else {
                val window = smoothedK.subList(i - periodD + 1, i + 1).filterNotNull()
                if (window.size == periodD) {
                    periodDValues.add(window.average())
                } else {
                    periodDValues.add(null)
                }
            }
        }

        return Pair(smoothedK, periodDValues)
    }

    fun computeOBV(candles: List<CandleData>): List<Double?> {
        val result = mutableListOf<Double?>()
        var currentObv = 0.0

        for (i in candles.indices) {
            if (i == 0) {
                currentObv = candles[i].volume.toDouble()
            } else {
                val prev = candles[i - 1].close
                val curr = candles[i].close
                val vol = candles[i].volume.toDouble()
                if (curr > prev) currentObv += vol
                else if (curr < prev) currentObv -= vol
            }
            result.add(currentObv)
        }
        return result
    }

    fun computeSwingStructure(candles: List<CandleData>, lookback: Int): Pair<List<String?>, List<Double?>> {
        val labels = MutableList<String?>(candles.size) { null }
        val points = MutableList<Double?>(candles.size) { null }

        var lastHigh = Double.MIN_VALUE
        var lastLow = Double.MAX_VALUE

        for (i in lookback until (candles.size - lookback)) {
            val currentHigh = candles[i].high
            val currentLow = candles[i].low

            val isSwingHigh = (1..lookback).all { offset ->
                currentHigh >= candles[i - offset].high && currentHigh >= candles[i + offset].high
            }

            val isSwingLow = (1..lookback).all { offset ->
                currentLow <= candles[i - offset].low && currentLow <= candles[i + offset].low
            }

            if (isSwingHigh) {
                val tag = if (currentHigh > lastHigh) "HH" else "LH"
                labels[i] = tag
                points[i] = currentHigh
                lastHigh = currentHigh
            } else if (isSwingLow) {
                val tag = if (currentLow < lastLow) "LL" else "HL"
                labels[i] = tag
                points[i] = currentLow
                lastLow = currentLow
            }
        }

        return Pair(labels, points)
    }
}
