package com.example.engine.pine

import com.example.data.model.CandleData
import com.example.engine.technical.TechnicalIndicatorEngine
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class PinePlot(
    val title: String,
    val series: List<Double?>,
    val color: Long = 0xFF38BDF8,
    val lineWidth: Float = 2f
)

data class PineHLine(
    val price: Double,
    val title: String,
    val color: Long = 0xFF64748B
)

data class PineShape(
    val title: String,
    val condition: List<Boolean>,
    val color: Long = 0xFF10B981,
    val isBuy: Boolean = true
)

data class PineCompilationResult(
    val success: Boolean,
    val title: String = "Custom Indicator",
    val isOverlay: Boolean = true,
    val errorMessage: String? = null,
    val errorLine: Int? = null,
    val parsedStatements: List<PineStatement> = emptyList()
)

data class PineExecutionResult(
    val success: Boolean,
    val title: String,
    val isOverlay: Boolean,
    val plots: List<PinePlot> = emptyList(),
    val hlines: List<PineHLine> = emptyList(),
    val shapes: List<PineShape> = emptyList(),
    val errorMessage: String? = null
)

sealed class PineStatement {
    data class IndicatorHeader(val title: String, val isOverlay: Boolean, val line: Int) : PineStatement()
    data class Assignment(val varName: String, val expr: PineExpr, val line: Int) : PineStatement()
    data class PlotCall(val expr: PineExpr, val title: String, val colorHex: Long, val lineWidth: Float, val line: Int) : PineStatement()
    data class PlotShapeCall(val conditionExpr: PineExpr, val title: String, val colorHex: Long, val isBuy: Boolean, val line: Int) : PineStatement()
    data class HLineCall(val price: Double, val title: String, val colorHex: Long, val line: Int) : PineStatement()
}

sealed class PineExpr {
    data class LiteralNumber(val value: Double) : PineExpr()
    data class VariableRef(val name: String) : PineExpr()
    data class BuiltInSeries(val type: String) : PineExpr() // open, high, low, close, volume, hl2, hlc3
    data class BinaryOp(val left: PineExpr, val op: String, val right: PineExpr) : PineExpr()
    data class TaCall(val function: String, val args: List<PineExpr>) : PineExpr()
    data class MathCall(val function: String, val args: List<PineExpr>) : PineExpr()
}

class PineParser {
    companion object {
        private val UNSUPPORTED_KEYWORDS = listOf(
            "request.security", "strategy.entry", "strategy.order", "strategy.close",
            "pine.exec", "matrix.new", "table.new", "request.financial", "ticker.new"
        )
    }

    fun parse(code: String): PineCompilationResult {
        val lines = code.lines()
        var indicatorTitle = "Custom Indicator"
        var isOverlay = true
        val statements = mutableListOf<PineStatement>()

        for ((index, rawLine) in lines.withIndex()) {
            val lineNum = index + 1
            val line = rawLine.trim()

            // Skip empty lines & comments
            if (line.isEmpty() || line.startsWith("//")) {
                continue
            }

            // Check for unsupported features
            for (unsupported in UNSUPPORTED_KEYWORDS) {
                if (line.contains(unsupported)) {
                    return PineCompilationResult(
                        success = false,
                        errorMessage = "Compilation failed.\n\nUnsupported Pine feature:\n$unsupported()\n\nLine: $lineNum\n\nThis feature is currently unavailable in the sandboxed custom indicator runtime.",
                        errorLine = lineNum
                    )
                }
            }

            // indicator("...", overlay = true/false)
            if (line.startsWith("indicator(") || line.startsWith("study(")) {
                val params = extractParams(line)
                indicatorTitle = params.getOrNull(0)?.replace("\"", "")?.trim() ?: "Custom Indicator"
                if (line.contains("overlay") && (line.contains("overlay=false") || line.contains("overlay = false"))) {
                    isOverlay = false
                }
                statements.add(PineStatement.IndicatorHeader(indicatorTitle, isOverlay, lineNum))
                continue
            }

            // plot(...)
            if (line.startsWith("plot(") && !line.startsWith("plotshape") && !line.startsWith("plotchar")) {
                val args = extractArguments(line.removePrefix("plot(").removeSuffix(")"))
                if (args.isEmpty()) {
                    return PineCompilationResult(
                        success = false,
                        errorMessage = "Syntax error in plot() at line $lineNum: Expression expected",
                        errorLine = lineNum
                    )
                }
                val expr = parseExpr(args[0]) ?: return PineCompilationResult(
                    success = false,
                    errorMessage = "Failed to parse plot expression '${args[0]}' at line $lineNum",
                    errorLine = lineNum
                )
                val title = findNamedParam(args, "title")?.replace("\"", "") ?: "Plot"
                val colorHex = parseColor(findNamedParam(args, "color"))
                val lw = findNamedParam(args, "linewidth")?.toFloatOrNull() ?: 2f
                statements.add(PineStatement.PlotCall(expr, title, colorHex, lw, lineNum))
                continue
            }

            // plotshape(...)
            if (line.startsWith("plotshape(")) {
                val args = extractArguments(line.removePrefix("plotshape(").removeSuffix(")"))
                if (args.isEmpty()) {
                    return PineCompilationResult(
                        success = false,
                        errorMessage = "Syntax error in plotshape() at line $lineNum: Condition expected",
                        errorLine = lineNum
                    )
                }
                val expr = parseExpr(args[0]) ?: return PineCompilationResult(
                    success = false,
                    errorMessage = "Failed to parse plotshape condition '${args[0]}' at line $lineNum",
                    errorLine = lineNum
                )
                val title = findNamedParam(args, "title")?.replace("\"", "") ?: "Signal"
                val colorHex = parseColor(findNamedParam(args, "color"))
                val isBuy = !line.contains("triangledown") && !line.contains("color.red")
                statements.add(PineStatement.PlotShapeCall(expr, title, colorHex, isBuy, lineNum))
                continue
            }

            // hline(...)
            if (line.startsWith("hline(")) {
                val args = extractArguments(line.removePrefix("hline(").removeSuffix(")"))
                val price = args.getOrNull(0)?.toDoubleOrNull() ?: 0.0
                val title = findNamedParam(args, "title")?.replace("\"", "") ?: "HLine"
                val colorHex = parseColor(findNamedParam(args, "color"))
                statements.add(PineStatement.HLineCall(price, title, colorHex, lineNum))
                continue
            }

            // Variable Assignment: e.g. ema200 = ta.ema(close, 200)
            if (line.contains("=")) {
                val parts = line.split("=", limit = 2)
                var varName = parts[0].trim()
                if (varName.startsWith("var ")) {
                    varName = varName.removePrefix("var ").trim()
                }
                val exprStr = parts[1].trim()

                // input.int(...) / input.float(...) support
                if (exprStr.startsWith("input.") || exprStr.startsWith("input(")) {
                    val defaultVal = extractInputDefault(exprStr)
                    statements.add(PineStatement.Assignment(varName, PineExpr.LiteralNumber(defaultVal), lineNum))
                    continue
                }

                val expr = parseExpr(exprStr)
                if (expr == null) {
                    return PineCompilationResult(
                        success = false,
                        errorMessage = "Syntax error at line $lineNum: Cannot parse expression '$exprStr'",
                        errorLine = lineNum
                    )
                }
                statements.add(PineStatement.Assignment(varName, expr, lineNum))
                continue
            }

            // Unknown statement
            return PineCompilationResult(
                success = false,
                errorMessage = "Unsupported syntax or statement at line $lineNum: '$line'",
                errorLine = lineNum
            )
        }

        return PineCompilationResult(
            success = true,
            title = indicatorTitle,
            isOverlay = isOverlay,
            parsedStatements = statements
        )
    }

    private fun parseExpr(str: String): PineExpr? {
        val s = str.trim()
        if (s.isEmpty()) return null

        // Number literal
        s.toDoubleOrNull()?.let { return PineExpr.LiteralNumber(it) }

        // Built-in price series
        when (s.lowercase()) {
            "open" -> return PineExpr.BuiltInSeries("open")
            "high" -> return PineExpr.BuiltInSeries("high")
            "low" -> return PineExpr.BuiltInSeries("low")
            "close" -> return PineExpr.BuiltInSeries("close")
            "volume" -> return PineExpr.BuiltInSeries("volume")
            "hl2" -> return PineExpr.BuiltInSeries("hl2")
            "hlc3" -> return PineExpr.BuiltInSeries("hlc3")
            "ohlc4" -> return PineExpr.BuiltInSeries("ohlc4")
        }

        // ta.* function call
        if (s.startsWith("ta.")) {
            val parenIdx = s.indexOf('(')
            if (parenIdx > 3 && s.endsWith(')')) {
                val fn = s.substring(3, parenIdx).trim()
                val argsInside = s.substring(parenIdx + 1, s.length - 1)
                val argStrings = extractArguments(argsInside)
                val argExprs = argStrings.mapNotNull { parseExpr(it) }
                return PineExpr.TaCall(fn, argExprs)
            }
        }

        // math.* function call
        if (s.startsWith("math.")) {
            val parenIdx = s.indexOf('(')
            if (parenIdx > 5 && s.endsWith(')')) {
                val fn = s.substring(5, parenIdx).trim()
                val argsInside = s.substring(parenIdx + 1, s.length - 1)
                val argStrings = extractArguments(argsInside)
                val argExprs = argStrings.mapNotNull { parseExpr(it) }
                return PineExpr.MathCall(fn, argExprs)
            }
        }

        // Binary operations like s1 > s2, s1 + s2, s1 - s2
        val operators = listOf(">=", "<=", "==", "!=", ">", "<", "+", "-", "*", "/")
        for (op in operators) {
            val idx = findOperatorOutsideParens(s, op)
            if (idx != -1) {
                val left = parseExpr(s.substring(0, idx)) ?: continue
                val right = parseExpr(s.substring(idx + op.length)) ?: continue
                return PineExpr.BinaryOp(left, op, right)
            }
        }

        // Simple Variable identifier
        if (s.matches(Regex("^[a-zA-Z_][a-zA-Z0-9_]*$"))) {
            return PineExpr.VariableRef(s)
        }

        return null
    }

    private fun findOperatorOutsideParens(s: String, op: String): Int {
        var depth = 0
        var i = s.length - op.length
        while (i >= 0) {
            val c = s[i]
            if (c == ')') depth++
            else if (c == '(') depth--
            else if (depth == 0 && s.substring(i, i + op.length) == op) {
                return i
            }
            i--
        }
        return -1
    }

    private fun extractArguments(content: String): List<String> {
        val result = mutableListOf<String>()
        var depth = 0
        var current = StringBuilder()

        for (c in content) {
            when (c) {
                '(', '[' -> {
                    depth++
                    current.append(c)
                }
                ')', ']' -> {
                    depth--
                    current.append(c)
                }
                ',' -> {
                    if (depth == 0) {
                        result.add(current.toString().trim())
                        current = StringBuilder()
                    } else {
                        current.append(c)
                    }
                }
                else -> current.append(c)
            }
        }
        if (current.isNotBlank()) {
            result.add(current.toString().trim())
        }
        return result
    }

    private fun extractParams(line: String): List<String> {
        val open = line.indexOf('(')
        val close = line.lastIndexOf(')')
        if (open == -1 || close == -1 || close <= open) return emptyList()
        return extractArguments(line.substring(open + 1, close))
    }

    private fun findNamedParam(args: List<String>, name: String): String? {
        for (arg in args) {
            if (arg.startsWith("$name=") || arg.startsWith("$name =")) {
                return arg.split("=", limit = 2)[1].trim()
            }
        }
        return null
    }

    private fun extractInputDefault(expr: String): Double {
        val args = extractArguments(expr.substringAfter('(').substringBeforeLast(')'))
        val first = args.getOrNull(0) ?: "0"
        return first.toDoubleOrNull() ?: 14.0
    }

    private fun parseColor(colorStr: String?): Long {
        if (colorStr == null) return 0xFF38BDF8 // Default Cyan
        return when (colorStr.lowercase().trim()) {
            "color.blue" -> 0xFF3B82F6
            "color.red" -> 0xFFEF4444
            "color.green" -> 0xFF10B981
            "color.yellow" -> 0xFFF59E0B
            "color.orange" -> 0xFFF97316
            "color.purple" -> 0xFFA855F7
            "color.white" -> 0xFFFFFFFF
            "color.aqua" -> 0xFF06B6D4
            "color.gray" -> 0xFF64748B
            else -> 0xFF38BDF8
        }
    }
}

class IndicatorSandbox {
    companion object {
        const val MAX_EXECUTION_STEPS = 100000
        const val MAX_OUTPUT_PLOTS = 8
    }

    fun execute(compilation: PineCompilationResult, candles: List<CandleData>): PineExecutionResult {
        if (!compilation.success) {
            return PineExecutionResult(
                success = false,
                title = compilation.title,
                isOverlay = compilation.isOverlay,
                errorMessage = compilation.errorMessage
            )
        }

        if (candles.isEmpty()) {
            return PineExecutionResult(
                success = true,
                title = compilation.title,
                isOverlay = compilation.isOverlay
            )
        }

        val env = mutableMapOf<String, List<Double?>>()

        // Populate base built-in series
        env["open"] = candles.map { it.open }
        env["high"] = candles.map { it.high }
        env["low"] = candles.map { it.low }
        env["close"] = candles.map { it.close }
        env["volume"] = candles.map { it.volume.toDouble() }
        env["hl2"] = candles.map { (it.high + it.low) / 2.0 }
        env["hlc3"] = candles.map { (it.high + it.low + it.close) / 3.0 }
        env["ohlc4"] = candles.map { (it.open + it.high + it.low + it.close) / 4.0 }

        val plots = mutableListOf<PinePlot>()
        val hlines = mutableListOf<PineHLine>()
        val shapes = mutableListOf<PineShape>()

        var steps = 0

        for (stmt in compilation.parsedStatements) {
            steps++
            if (steps > MAX_EXECUTION_STEPS) {
                return PineExecutionResult(
                    success = false,
                    title = compilation.title,
                    isOverlay = compilation.isOverlay,
                    errorMessage = "Execution aborted: Maximum execution steps exceeded in sandbox."
                )
            }

            when (stmt) {
                is PineStatement.IndicatorHeader -> { /* already captured */ }
                is PineStatement.Assignment -> {
                    val calculatedSeries = evalExpr(stmt.expr, env, candles)
                    env[stmt.varName] = calculatedSeries
                }
                is PineStatement.PlotCall -> {
                    if (plots.size < MAX_OUTPUT_PLOTS) {
                        val series = evalExpr(stmt.expr, env, candles)
                        plots.add(PinePlot(stmt.title, series, stmt.colorHex, stmt.lineWidth))
                    }
                }
                is PineStatement.PlotShapeCall -> {
                    val condSeries = evalExpr(stmt.conditionExpr, env, candles)
                    val booleanCond = condSeries.map { it != null && it > 0.0 }
                    shapes.add(PineShape(stmt.title, booleanCond, stmt.colorHex, stmt.isBuy))
                }
                is PineStatement.HLineCall -> {
                    hlines.add(PineHLine(stmt.price, stmt.title, stmt.colorHex))
                }
            }
        }

        return PineExecutionResult(
            success = true,
            title = compilation.title,
            isOverlay = compilation.isOverlay,
            plots = plots,
            hlines = hlines,
            shapes = shapes
        )
    }

    private fun evalExpr(expr: PineExpr, env: Map<String, List<Double?>>, candles: List<CandleData>): List<Double?> {
        val size = candles.size
        return when (expr) {
            is PineExpr.LiteralNumber -> List(size) { expr.value }
            is PineExpr.BuiltInSeries -> env[expr.type] ?: List(size) { null }
            is PineExpr.VariableRef -> env[expr.name] ?: List(size) { null }
            is PineExpr.BinaryOp -> {
                val left = evalExpr(expr.left, env, candles)
                val right = evalExpr(expr.right, env, candles)
                List(size) { i ->
                    val l = left.getOrNull(i)
                    val r = right.getOrNull(i)
                    if (l == null || r == null) null
                    else when (expr.op) {
                        "+" -> l + r
                        "-" -> l - r
                        "*" -> l * r
                        "/" -> if (r != 0.0) l / r else null
                        ">" -> if (l > r) 1.0 else 0.0
                        "<" -> if (l < r) 1.0 else 0.0
                        ">=" -> if (l >= r) 1.0 else 0.0
                        "<=" -> if (l <= r) 1.0 else 0.0
                        "==" -> if (abs(l - r) < 1e-6) 1.0 else 0.0
                        "!=" -> if (abs(l - r) >= 1e-6) 1.0 else 0.0
                        else -> null
                    }
                }
            }
            is PineExpr.TaCall -> evalTa(expr.function, expr.args, env, candles)
            is PineExpr.MathCall -> evalMath(expr.function, expr.args, env, candles)
        }
    }

    private fun evalTa(fn: String, args: List<PineExpr>, env: Map<String, List<Double?>>, candles: List<CandleData>): List<Double?> {
        val size = candles.size
        return when (fn) {
            "sma" -> {
                val src = evalExpr(args.getOrNull(0) ?: PineExpr.BuiltInSeries("close"), env, candles).map { it ?: 0.0 }
                val len = (args.getOrNull(1) as? PineExpr.LiteralNumber)?.value?.toInt() ?: 14
                TechnicalIndicatorEngine.computeSMA(src, len)
            }
            "ema" -> {
                val src = evalExpr(args.getOrNull(0) ?: PineExpr.BuiltInSeries("close"), env, candles).map { it ?: 0.0 }
                val len = (args.getOrNull(1) as? PineExpr.LiteralNumber)?.value?.toInt() ?: 14
                TechnicalIndicatorEngine.computeEMA(src, len)
            }
            "wma" -> {
                val src = evalExpr(args.getOrNull(0) ?: PineExpr.BuiltInSeries("close"), env, candles).map { it ?: 0.0 }
                val len = (args.getOrNull(1) as? PineExpr.LiteralNumber)?.value?.toInt() ?: 14
                TechnicalIndicatorEngine.computeWMA(src, len)
            }
            "rma" -> {
                val src = evalExpr(args.getOrNull(0) ?: PineExpr.BuiltInSeries("close"), env, candles).map { it ?: 0.0 }
                val len = (args.getOrNull(1) as? PineExpr.LiteralNumber)?.value?.toInt() ?: 14
                TechnicalIndicatorEngine.computeRMA(src, len)
            }
            "rsi" -> {
                val src = evalExpr(args.getOrNull(0) ?: PineExpr.BuiltInSeries("close"), env, candles).map { it ?: 0.0 }
                val len = (args.getOrNull(1) as? PineExpr.LiteralNumber)?.value?.toInt() ?: 14
                TechnicalIndicatorEngine.computeRSI(src, len)
            }
            "atr" -> {
                val len = (args.getOrNull(0) as? PineExpr.LiteralNumber)?.value?.toInt() ?: 14
                TechnicalIndicatorEngine.computeATR(candles, len)
            }
            "highest" -> {
                val src = evalExpr(args.getOrNull(0) ?: PineExpr.BuiltInSeries("high"), env, candles)
                val len = (args.getOrNull(1) as? PineExpr.LiteralNumber)?.value?.toInt() ?: 14
                List(size) { i ->
                    if (i < len - 1) null
                    else src.subList(i - len + 1, i + 1).filterNotNull().maxOrNull()
                }
            }
            "lowest" -> {
                val src = evalExpr(args.getOrNull(0) ?: PineExpr.BuiltInSeries("low"), env, candles)
                val len = (args.getOrNull(1) as? PineExpr.LiteralNumber)?.value?.toInt() ?: 14
                List(size) { i ->
                    if (i < len - 1) null
                    else src.subList(i - len + 1, i + 1).filterNotNull().minOrNull()
                }
            }
            "crossover" -> {
                val s1 = evalExpr(args[0], env, candles)
                val s2 = evalExpr(args[1], env, candles)
                List(size) { i ->
                    if (i == 0) 0.0
                    else {
                        val curr1 = s1[i] ?: 0.0
                        val prev1 = s1[i - 1] ?: 0.0
                        val curr2 = s2[i] ?: 0.0
                        val prev2 = s2[i - 1] ?: 0.0
                        if (prev1 <= prev2 && curr1 > curr2) 1.0 else 0.0
                    }
                }
            }
            "crossunder" -> {
                val s1 = evalExpr(args[0], env, candles)
                val s2 = evalExpr(args[1], env, candles)
                List(size) { i ->
                    if (i == 0) 0.0
                    else {
                        val curr1 = s1[i] ?: 0.0
                        val prev1 = s1[i - 1] ?: 0.0
                        val curr2 = s2[i] ?: 0.0
                        val prev2 = s2[i - 1] ?: 0.0
                        if (prev1 >= prev2 && curr1 < curr2) 1.0 else 0.0
                    }
                }
            }
            else -> List(size) { null }
        }
    }

    private fun evalMath(fn: String, args: List<PineExpr>, env: Map<String, List<Double?>>, candles: List<CandleData>): List<Double?> {
        val size = candles.size
        val arg0 = evalExpr(args[0], env, candles)
        return when (fn) {
            "abs" -> arg0.map { it?.let { v -> abs(v) } }
            "sqrt" -> arg0.map { it?.let { v -> if (v >= 0) sqrt(v) else null } }
            "round" -> arg0.map { it?.let { v -> kotlin.math.round(v) } }
            "max" -> {
                val arg1 = evalExpr(args[1], env, candles)
                List(size) { i ->
                    val a = arg0[i]
                    val b = arg1[i]
                    if (a != null && b != null) max(a, b) else null
                }
            }
            "min" -> {
                val arg1 = evalExpr(args[1], env, candles)
                List(size) { i ->
                    val a = arg0[i]
                    val b = arg1[i]
                    if (a != null && b != null) min(a, b) else null
                }
            }
            else -> List(size) { null }
        }
    }
}

object PrebuiltPineScripts {
    val TRIPLE_EMA = """
        //@version=6
        indicator("Triple EMA Ribbon", overlay=true)
        
        lenFast = input.int(8, "Fast EMA")
        lenMid = input.int(50, "Mid EMA")
        lenSlow = input.int(200, "Slow EMA")
        
        emaFast = ta.ema(close, lenFast)
        emaMid = ta.ema(close, lenMid)
        emaSlow = ta.ema(close, lenSlow)
        
        plot(emaFast, title="EMA 8", color=color.yellow, linewidth=2)
        plot(emaMid, title="EMA 50", color=color.aqua, linewidth=2)
        plot(emaSlow, title="EMA 200", color=color.purple, linewidth=3)
        
        bullCross = ta.crossover(emaFast, emaMid)
        plotshape(bullCross, title="Bullish Cross", color=color.green)
    """.trimIndent()

    val BOLLINGER_SQUEEZE = """
        //@version=6
        indicator("Bollinger & Keltner Squeeze", overlay=true)
        
        bbLen = input.int(20, "BB Length")
        bbBasis = ta.sma(close, bbLen)
        
        plot(bbBasis, title="Basis", color=color.blue, linewidth=2)
    """.trimIndent()

    val RSI_EXTREMES = """
        //@version=6
        indicator("RSI Momentum Levels", overlay=false)
        
        length = input.int(14, "Length")
        rsiVal = ta.rsi(close, length)
        
        plot(rsiVal, title="RSI", color=color.purple, linewidth=2)
        hline(70, title="Overbought", color=color.red)
        hline(30, title="Oversold", color=color.green)
        hline(50, title="Midline", color=color.gray)
    """.trimIndent()
}
