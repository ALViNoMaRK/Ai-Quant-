package com.example.engine.pine

import com.example.data.model.CandleData
import com.example.engine.technical.TechnicalIndicatorEngine
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sign
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
    data class Assignment(val varNames: List<String>, val expr: PineExpr, val line: Int) : PineStatement()
    data class PlotCall(val expr: PineExpr, val title: String, val colorHex: Long, val lineWidth: Float, val line: Int) : PineStatement()
    data class PlotShapeCall(val conditionExpr: PineExpr, val title: String, val colorHex: Long, val isBuy: Boolean, val line: Int) : PineStatement()
    data class HLineCall(val price: Double, val title: String, val colorHex: Long, val line: Int) : PineStatement()
}

sealed class PineExpr {
    data class LiteralNumber(val value: Double) : PineExpr()
    data class LiteralBool(val value: Boolean) : PineExpr()
    data class VariableRef(val name: String) : PineExpr()
    data class BuiltInSeries(val type: String) : PineExpr() // open, high, low, close, volume, hl2, hlc3, ohlc4, bar_index, time, tr
    data class HistoryRef(val seriesExpr: PineExpr, val offset: Int) : PineExpr()
    data class UnaryOp(val op: String, val expr: PineExpr) : PineExpr()
    data class BinaryOp(val left: PineExpr, val op: String, val right: PineExpr) : PineExpr()
    data class TernaryOp(val condition: PineExpr, val trueExpr: PineExpr, val falseExpr: PineExpr) : PineExpr()
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
            val line = stripComment(rawLine).trim()

            // Skip empty lines & full line comments
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

            // indicator("...", overlay = true/false), study(...), strategy(...)
            if (line.startsWith("indicator(") || line.startsWith("study(") || line.startsWith("strategy(")) {
                val params = extractParams(line)
                indicatorTitle = params.getOrNull(0)?.replace("\"", "")?.replace("'", "")?.trim() ?: "Custom Indicator"
                if (line.contains("overlay") && (line.contains("overlay=false") || line.contains("overlay = false"))) {
                    isOverlay = false
                }
                statements.add(PineStatement.IndicatorHeader(indicatorTitle, isOverlay, lineNum))
                continue
            }

            // Benign / styling statements that should not break compilation
            val benignPrefixes = listOf("bgcolor(", "barcolor(", "fill(", "alertcondition(", "alert(", "table.", "line.", "label.")
            if (benignPrefixes.any { line.startsWith(it) }) {
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
                val expr = parseExpressionSafe(args[0]) ?: return PineCompilationResult(
                    success = false,
                    errorMessage = "Failed to parse plot expression '${args[0]}' at line $lineNum",
                    errorLine = lineNum
                )
                val title = findNamedParam(args, "title")?.replace("\"", "")?.replace("'", "") ?: "Plot"
                val colorHex = parseColor(findNamedParam(args, "color"))
                val lw = findNamedParam(args, "linewidth")?.toFloatOrNull() ?: 2f
                statements.add(PineStatement.PlotCall(expr, title, colorHex, lw, lineNum))
                continue
            }

            // plotshape(...) or plotchar(...)
            if (line.startsWith("plotshape(") || line.startsWith("plotchar(")) {
                val prefix = if (line.startsWith("plotshape(")) "plotshape(" else "plotchar("
                val args = extractArguments(line.removePrefix(prefix).removeSuffix(")"))
                if (args.isEmpty()) {
                    return PineCompilationResult(
                        success = false,
                        errorMessage = "Syntax error in plotshape() at line $lineNum: Condition expected",
                        errorLine = lineNum
                    )
                }
                val expr = parseExpressionSafe(args[0]) ?: return PineCompilationResult(
                    success = false,
                    errorMessage = "Failed to parse plotshape condition '${args[0]}' at line $lineNum",
                    errorLine = lineNum
                )
                val title = findNamedParam(args, "title")?.replace("\"", "")?.replace("'", "") ?: "Signal"
                val colorHex = parseColor(findNamedParam(args, "color"))
                val isBuy = !line.contains("triangledown") && !line.contains("color.red")
                statements.add(PineStatement.PlotShapeCall(expr, title, colorHex, isBuy, lineNum))
                continue
            }

            // hline(...)
            if (line.startsWith("hline(")) {
                val args = extractArguments(line.removePrefix("hline(").removeSuffix(")"))
                val price = args.getOrNull(0)?.toDoubleOrNull() ?: 0.0
                val title = findNamedParam(args, "title")?.replace("\"", "")?.replace("'", "") ?: "HLine"
                val colorHex = parseColor(findNamedParam(args, "color"))
                statements.add(PineStatement.HLineCall(price, title, colorHex, lineNum))
                continue
            }

            // Variable Assignment (Single variable, tuple [a, b, c] = ..., or reassignment with :=)
            val isReassignment = line.contains(":=")
            if (line.contains("=") || isReassignment) {
                val equalsIdx = if (isReassignment) line.indexOf(":=") else line.indexOf('=')
                val lhsRaw = line.substring(0, equalsIdx).trim()
                val exprStr = if (isReassignment) line.substring(equalsIdx + 2).trim() else line.substring(equalsIdx + 1).trim()

                val varNames = extractVariableNames(lhsRaw)
                if (varNames.isEmpty()) {
                    return PineCompilationResult(
                        success = false,
                        errorMessage = "Syntax error at line $lineNum: Invalid variable target '$lhsRaw'",
                        errorLine = lineNum
                    )
                }

                // input.source(...)
                if (exprStr.startsWith("input.source(") || (exprStr.startsWith("input(") && (exprStr.contains("close") || exprStr.contains("open") || exprStr.contains("high") || exprStr.contains("low") || exprStr.contains("hlc3")))) {
                    val srcName = when {
                        exprStr.contains("open") -> "open"
                        exprStr.contains("high") -> "high"
                        exprStr.contains("low") -> "low"
                        exprStr.contains("hlc3") -> "hlc3"
                        else -> "close"
                    }
                    statements.add(PineStatement.Assignment(varNames, PineExpr.BuiltInSeries(srcName), lineNum))
                    continue
                }

                // input.int(...) / input.float(...) / input.bool(...) / input(...)
                if (exprStr.startsWith("input.") || exprStr.startsWith("input(")) {
                    val defaultVal = extractInputDefault(exprStr)
                    statements.add(PineStatement.Assignment(varNames, PineExpr.LiteralNumber(defaultVal), lineNum))
                    continue
                }

                val expr = parseExpressionSafe(exprStr)
                if (expr == null) {
                    return PineCompilationResult(
                        success = false,
                        errorMessage = "Syntax error at line $lineNum: Cannot parse expression '$exprStr'",
                        errorLine = lineNum
                    )
                }
                statements.add(PineStatement.Assignment(varNames, expr, lineNum))
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

    private fun stripComment(line: String): String {
        var inQuotes = false
        var quoteChar = ' '
        for (i in line.indices) {
            val c = line[i]
            if ((c == '"' || c == '\'') && (i == 0 || line[i - 1] != '\\')) {
                if (!inQuotes) {
                    inQuotes = true
                    quoteChar = c
                } else if (c == quoteChar) {
                    inQuotes = false
                }
            } else if (!inQuotes && c == '/' && i + 1 < line.length && line[i + 1] == '/') {
                return line.substring(0, i)
            }
        }
        return line
    }

    private fun extractVariableNames(lhsRaw: String): List<String> {
        val trimmed = lhsRaw.trim()
        // Check for tuple assignment like [macdLine, signalLine, histLine] or (a, b)
        if ((trimmed.startsWith("[") && trimmed.endsWith("]")) || (trimmed.startsWith("(") && trimmed.endsWith(")"))) {
            val inside = trimmed.substring(1, trimmed.length - 1)
            return inside.split(",").map { cleanVariableName(it) }.filter { it.isNotEmpty() }
        }
        val single = cleanVariableName(trimmed)
        return if (single.isNotEmpty()) listOf(single) else emptyList()
    }

    private fun cleanVariableName(raw: String): String {
        var s = raw.trim().removeSuffix(":")
        // Strip type qualifiers & keywords
        val prefixes = listOf(
            "varip ", "var ",
            "float ", "int ", "bool ", "string ", "color ",
            "series<float> ", "series<int> ", "series<bool> ",
            "series[float] ", "series[int] ", "series[bool] "
        )
        var changed = true
        while (changed) {
            changed = false
            for (p in prefixes) {
                if (s.startsWith(p)) {
                    s = s.substring(p.length).trim()
                    changed = true
                }
            }
        }
        return s.trim().removeSuffix(":")
    }

    private fun parseExpressionSafe(exprStr: String): PineExpr? {
        return try {
            val tokenizer = PineTokenizer(exprStr)
            val tokens = tokenizer.tokenize()
            if (tokens.isEmpty()) null else PineExprParser(tokens).parse()
        } catch (_: Exception) {
            null
        }
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
        for (arg in args) {
            if (arg.startsWith("defval=") || arg.startsWith("defval =")) {
                val v = arg.split("=", limit = 2)[1].trim()
                v.toDoubleOrNull()?.let { return it }
                if (v == "true") return 1.0
                if (v == "false") return 0.0
            }
        }
        val first = args.getOrNull(0) ?: "0"
        if (first == "true") return 1.0
        if (first == "false") return 0.0
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
            "color.gray", "color.grey" -> 0xFF64748B
            "color.lime" -> 0xFF84CC16
            else -> 0xFF38BDF8
        }
    }
}

private enum class TokenType {
    NUMBER, BOOLEAN, IDENTIFIER,
    PLUS, MINUS, STAR, SLASH, PERCENT,
    EQ_EQ, NOT_EQ, LTE, GTE, LT, GT,
    EQUAL,
    AND, OR, NOT,
    QUESTION, COLON,
    LPAREN, RPAREN, LBRACKET, RBRACKET, COMMA,
    EOF
}

private data class Token(val type: TokenType, val text: String, val numberVal: Double = 0.0)

private class PineTokenizer(val text: String) {
    private var pos = 0

    fun tokenize(): List<Token> {
        val tokens = mutableListOf<Token>()
        while (pos < text.length) {
            val c = text[pos]
            if (c.isWhitespace()) {
                pos++
                continue
            }
            when (c) {
                '?' -> { tokens.add(Token(TokenType.QUESTION, "?")); pos++ }
                ':' -> { tokens.add(Token(TokenType.COLON, ":")); pos++ }
                ',' -> { tokens.add(Token(TokenType.COMMA, ",")); pos++ }
                '(' -> { tokens.add(Token(TokenType.LPAREN, "(")); pos++ }
                ')' -> { tokens.add(Token(TokenType.RPAREN, ")")); pos++ }
                '[' -> { tokens.add(Token(TokenType.LBRACKET, "[")); pos++ }
                ']' -> { tokens.add(Token(TokenType.RBRACKET, "]")); pos++ }
                '+' -> { tokens.add(Token(TokenType.PLUS, "+")); pos++ }
                '-' -> { tokens.add(Token(TokenType.MINUS, "-")); pos++ }
                '*' -> { tokens.add(Token(TokenType.STAR, "*")); pos++ }
                '/' -> { tokens.add(Token(TokenType.SLASH, "/")); pos++ }
                '%' -> { tokens.add(Token(TokenType.PERCENT, "%")); pos++ }
                '=' -> {
                    if (pos + 1 < text.length && text[pos + 1] == '=') {
                        tokens.add(Token(TokenType.EQ_EQ, "==")); pos += 2
                    } else {
                        tokens.add(Token(TokenType.EQUAL, "=")); pos++
                    }
                }
                '!' -> {
                    if (pos + 1 < text.length && text[pos + 1] == '=') {
                        tokens.add(Token(TokenType.NOT_EQ, "!=")); pos += 2
                    } else {
                        tokens.add(Token(TokenType.NOT, "!")); pos++
                    }
                }
                '<' -> {
                    if (pos + 1 < text.length && text[pos + 1] == '=') {
                        tokens.add(Token(TokenType.LTE, "<=")); pos += 2
                    } else {
                        tokens.add(Token(TokenType.LT, "<")); pos++
                    }
                }
                '>' -> {
                    if (pos + 1 < text.length && text[pos + 1] == '=') {
                        tokens.add(Token(TokenType.GTE, ">=")); pos += 2
                    } else {
                        tokens.add(Token(TokenType.GT, ">")); pos++
                    }
                }
                else -> {
                    if (c.isDigit() || (c == '.' && pos + 1 < text.length && text[pos + 1].isDigit())) {
                        val start = pos
                        while (pos < text.length && (text[pos].isDigit() || text[pos] == '.')) {
                            pos++
                        }
                        val numStr = text.substring(start, pos)
                        tokens.add(Token(TokenType.NUMBER, numStr, numStr.toDoubleOrNull() ?: 0.0))
                    } else if (c.isLetter() || c == '_') {
                        val start = pos
                        while (pos < text.length && (text[pos].isLetterOrDigit() || text[pos] == '_' || text[pos] == '.')) {
                            pos++
                        }
                        val ident = text.substring(start, pos)
                        when (ident) {
                            "true" -> tokens.add(Token(TokenType.BOOLEAN, ident, 1.0))
                            "false" -> tokens.add(Token(TokenType.BOOLEAN, ident, 0.0))
                            "and" -> tokens.add(Token(TokenType.AND, ident))
                            "or" -> tokens.add(Token(TokenType.OR, ident))
                            "not" -> tokens.add(Token(TokenType.NOT, ident))
                            else -> tokens.add(Token(TokenType.IDENTIFIER, ident))
                        }
                    } else {
                        pos++
                    }
                }
            }
        }
        tokens.add(Token(TokenType.EOF, ""))
        return tokens
    }
}

private class PineExprParser(private val tokens: List<Token>) {
    private var current = 0

    fun parse(): PineExpr? {
        return parseTernary()
    }

    private fun parseTernary(): PineExpr? {
        var expr = parseOr() ?: return null
        if (match(TokenType.QUESTION)) {
            val trueExpr = parseTernary() ?: return null
            consume(TokenType.COLON, "Expected ':' in ternary conditional operator")
            val falseExpr = parseTernary() ?: return null
            expr = PineExpr.TernaryOp(expr, trueExpr, falseExpr)
        }
        return expr
    }

    private fun parseOr(): PineExpr? {
        var expr = parseAnd() ?: return null
        while (match(TokenType.OR)) {
            val right = parseAnd() ?: return null
            expr = PineExpr.BinaryOp(expr, "or", right)
        }
        return expr
    }

    private fun parseAnd(): PineExpr? {
        var expr = parseEquality() ?: return null
        while (match(TokenType.AND)) {
            val right = parseEquality() ?: return null
            expr = PineExpr.BinaryOp(expr, "and", right)
        }
        return expr
    }

    private fun parseEquality(): PineExpr? {
        var expr = parseRelational() ?: return null
        while (check(TokenType.EQ_EQ) || check(TokenType.NOT_EQ)) {
            val op = advance().text
            val right = parseRelational() ?: return null
            expr = PineExpr.BinaryOp(expr, op, right)
        }
        return expr
    }

    private fun parseRelational(): PineExpr? {
        var expr = parseAdditive() ?: return null
        while (check(TokenType.LT) || check(TokenType.LTE) || check(TokenType.GT) || check(TokenType.GTE)) {
            val op = advance().text
            val right = parseAdditive() ?: return null
            expr = PineExpr.BinaryOp(expr, op, right)
        }
        return expr
    }

    private fun parseAdditive(): PineExpr? {
        var expr = parseMultiplicative() ?: return null
        while (check(TokenType.PLUS) || check(TokenType.MINUS)) {
            val op = advance().text
            val right = parseMultiplicative() ?: return null
            expr = PineExpr.BinaryOp(expr, op, right)
        }
        return expr
    }

    private fun parseMultiplicative(): PineExpr? {
        var expr = parseUnary() ?: return null
        while (check(TokenType.STAR) || check(TokenType.SLASH) || check(TokenType.PERCENT)) {
            val op = advance().text
            val right = parseUnary() ?: return null
            expr = PineExpr.BinaryOp(expr, op, right)
        }
        return expr
    }

    private fun parseUnary(): PineExpr? {
        if (match(TokenType.NOT)) {
            val right = parseUnary() ?: return null
            return PineExpr.UnaryOp("not", right)
        }
        if (match(TokenType.MINUS)) {
            val right = parseUnary() ?: return null
            return PineExpr.UnaryOp("-", right)
        }
        return parsePostfix()
    }

    private fun parsePostfix(): PineExpr? {
        var expr = parsePrimary() ?: return null
        // Handle historical indexing operator: expr[offset] (e.g. close[1], high[2], emaFast[1])
        while (match(TokenType.LBRACKET)) {
            val offset = if (check(TokenType.NUMBER)) {
                advance().numberVal.toInt()
            } else {
                parseTernary()
                1
            }
            if (check(TokenType.RBRACKET)) advance()
            expr = PineExpr.HistoryRef(expr, offset)
        }
        return expr
    }

    private fun parsePrimary(): PineExpr? {
        if (match(TokenType.NUMBER)) {
            return PineExpr.LiteralNumber(previous().numberVal)
        }
        if (match(TokenType.BOOLEAN)) {
            return PineExpr.LiteralBool(previous().text == "true")
        }
        if (match(TokenType.LPAREN)) {
            val expr = parseTernary() ?: return null
            consume(TokenType.RPAREN, "Expected ')' after parenthesized expression")
            return expr
        }
        if (match(TokenType.IDENTIFIER)) {
            val name = previous().text
            // Check for function call: ta.xxx(...) or math.xxx(...) or fn(...)
            if (match(TokenType.LPAREN)) {
                val args = mutableListOf<PineExpr>()
                if (!check(TokenType.RPAREN)) {
                    do {
                        // Check if named parameter e.g. length = 20, defval = 14, source = close
                        if (check(TokenType.IDENTIFIER) && peekNext().type == TokenType.EQUAL) {
                            advance() // consume identifier
                            advance() // consume '='
                        }
                        val arg = parseTernary() ?: return null
                        args.add(arg)
                    } while (match(TokenType.COMMA))
                }
                consume(TokenType.RPAREN, "Expected ')' after function arguments")
                return if (name.startsWith("ta.")) {
                    PineExpr.TaCall(name.removePrefix("ta."), args)
                } else if (name.startsWith("math.")) {
                    PineExpr.MathCall(name.removePrefix("math."), args)
                } else {
                    PineExpr.TaCall(name, args)
                }
            }

            // Built-in price series & variables
            return when (name.lowercase()) {
                "open", "high", "low", "close", "volume",
                "hl2", "hlc3", "ohlc4", "bar_index", "time", "tr" -> PineExpr.BuiltInSeries(name.lowercase())
                else -> PineExpr.VariableRef(name)
            }
        }
        return null
    }

    private fun peekNext(): Token = if (current + 1 < tokens.size) tokens[current + 1] else tokens.last()

    private fun match(type: TokenType): Boolean {
        if (check(type)) {
            advance()
            return true
        }
        return false
    }

    private fun check(type: TokenType): Boolean {
        if (isAtEnd()) return false
        return peek().type == type
    }

    private fun advance(): Token {
        if (!isAtEnd()) current++
        return previous()
    }

    private fun isAtEnd(): Boolean = peek().type == TokenType.EOF
    private fun peek(): Token = tokens[current]
    private fun previous(): Token = tokens[current - 1]

    private fun consume(type: TokenType, message: String): Token {
        if (check(type)) return advance()
        throw RuntimeException(message)
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

        val size = candles.size
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
        env["bar_index"] = List(size) { i -> i.toDouble() }
        env["time"] = candles.map { it.timestamp.toDouble() }

        // Precompute true range
        env["tr"] = List(size) { i ->
            if (i == 0) candles[0].high - candles[0].low
            else {
                val h = candles[i].high
                val l = candles[i].low
                val pc = candles[i - 1].close
                max(h - l, max(abs(h - pc), abs(l - pc)))
            }
        }

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
                    if (stmt.varNames.size == 1) {
                        val calculatedSeries = evalExpr(stmt.expr, env, candles)
                        env[stmt.varNames[0]] = calculatedSeries
                    } else {
                        // Multi-variable assignment (e.g. [macdLine, signalLine, histLine] = ta.macd(...))
                        val multiSeries = evalMultiExpr(stmt.expr, env, candles, stmt.varNames.size)
                        for (i in stmt.varNames.indices) {
                            if (i < multiSeries.size) {
                                env[stmt.varNames[i]] = multiSeries[i]
                            }
                        }
                    }
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
            is PineExpr.LiteralBool -> List(size) { if (expr.value) 1.0 else 0.0 }
            is PineExpr.BuiltInSeries -> env[expr.type] ?: List(size) { null }
            is PineExpr.VariableRef -> env[expr.name] ?: List(size) { null }
            is PineExpr.HistoryRef -> {
                // Historical indexing: series[offset] refers strictly to previous bar i - offset
                val base = evalExpr(expr.seriesExpr, env, candles)
                val offset = expr.offset
                List(size) { i ->
                    val target = i - offset
                    if (target in 0 until size) base[target] else null
                }
            }
            is PineExpr.UnaryOp -> {
                val inner = evalExpr(expr.expr, env, candles)
                when (expr.op) {
                    "-" -> inner.map { it?.let { -it } }
                    "not" -> inner.map { if (it == null || it <= 0.0) 1.0 else 0.0 }
                    else -> inner
                }
            }
            is PineExpr.BinaryOp -> {
                val left = evalExpr(expr.left, env, candles)
                val right = evalExpr(expr.right, env, candles)
                List(size) { i ->
                    val l = left.getOrNull(i)
                    val r = right.getOrNull(i)
                    when (expr.op) {
                        "+" -> if (l != null && r != null) l + r else null
                        "-" -> if (l != null && r != null) l - r else null
                        "*" -> if (l != null && r != null) l * r else null
                        "/" -> if (l != null && r != null && r != 0.0) l / r else null
                        "%" -> if (l != null && r != null && r != 0.0) l % r else null
                        ">" -> if (l != null && r != null && l > r) 1.0 else 0.0
                        "<" -> if (l != null && r != null && l < r) 1.0 else 0.0
                        ">=" -> if (l != null && r != null && l >= r) 1.0 else 0.0
                        "<=" -> if (l != null && r != null && l <= r) 1.0 else 0.0
                        "==" -> if (l != null && r != null && abs(l - r) < 1e-6) 1.0 else 0.0
                        "!=" -> if (l != null && r != null && abs(l - r) >= 1e-6) 1.0 else 0.0
                        "and" -> if (l != null && l > 0.0 && r != null && r > 0.0) 1.0 else 0.0
                        "or" -> if ((l != null && l > 0.0) || (r != null && r > 0.0)) 1.0 else 0.0
                        else -> null
                    }
                }
            }
            is PineExpr.TernaryOp -> {
                val cond = evalExpr(expr.condition, env, candles)
                val tVal = evalExpr(expr.trueExpr, env, candles)
                val fVal = evalExpr(expr.falseExpr, env, candles)
                List(size) { i ->
                    val c = cond.getOrNull(i)
                    if (c != null && c > 0.0) tVal.getOrNull(i) else fVal.getOrNull(i)
                }
            }
            is PineExpr.TaCall -> evalTa(expr.function, expr.args, env, candles)
            is PineExpr.MathCall -> evalMath(expr.function, expr.args, env, candles)
        }
    }

    private fun resolveParamNumber(expr: PineExpr?, env: Map<String, List<Double?>>, candles: List<CandleData>, defaultVal: Double): Double {
        if (expr == null) return defaultVal
        if (expr is PineExpr.LiteralNumber) return expr.value
        val series = evalExpr(expr, env, candles)
        val v = series.lastOrNull { it != null } ?: series.firstOrNull { it != null }
        return v ?: defaultVal
    }

    private fun resolveParamInt(expr: PineExpr?, env: Map<String, List<Double?>>, candles: List<CandleData>, defaultVal: Int): Int {
        return resolveParamNumber(expr, env, candles, defaultVal.toDouble()).toInt().coerceAtLeast(1)
    }

    private fun evalMultiExpr(
        expr: PineExpr,
        env: Map<String, List<Double?>>,
        candles: List<CandleData>,
        expectedOutputs: Int
    ): List<List<Double?>> {
        val size = candles.size
        if (expr is PineExpr.TaCall) {
            when (expr.function) {
                "macd" -> {
                    val src = evalExpr(expr.args.getOrNull(0) ?: PineExpr.BuiltInSeries("close"), env, candles).map { it ?: 0.0 }
                    val fast = resolveParamInt(expr.args.getOrNull(1), env, candles, 12)
                    val slow = resolveParamInt(expr.args.getOrNull(2), env, candles, 26)
                    val signal = resolveParamInt(expr.args.getOrNull(3), env, candles, 9)
                    val (macd, sig, hist) = TechnicalIndicatorEngine.computeMACD(src, fast, slow, signal)
                    return listOf(macd, sig, hist)
                }
                "bb" -> {
                    val src = evalExpr(expr.args.getOrNull(0) ?: PineExpr.BuiltInSeries("close"), env, candles).map { it ?: 0.0 }
                    val len = resolveParamInt(expr.args.getOrNull(1), env, candles, 20)
                    val mult = resolveParamNumber(expr.args.getOrNull(2), env, candles, 2.0)
                    val (basis, upper, lower) = TechnicalIndicatorEngine.computeBollingerBands(src, len, mult)
                    return listOf(basis, upper, lower)
                }
                "kc" -> {
                    val len = resolveParamInt(expr.args.getOrNull(0), env, candles, 20)
                    val mult = resolveParamNumber(expr.args.getOrNull(1), env, candles, 1.5)
                    val (mid, upper, lower) = TechnicalIndicatorEngine.computeKeltner(candles, len, mult)
                    return listOf(mid, upper, lower)
                }
                "stoch" -> {
                    val len = resolveParamInt(expr.args.getOrNull(3) ?: expr.args.getOrNull(0), env, candles, 14)
                    val (k, d) = TechnicalIndicatorEngine.computeStochastic(candles, len, 3, 3)
                    return listOf(k, d)
                }
            }
        }
        val single = evalExpr(expr, env, candles)
        return List(expectedOutputs) { single }
    }

    private fun evalTa(fn: String, args: List<PineExpr>, env: Map<String, List<Double?>>, candles: List<CandleData>): List<Double?> {
        val size = candles.size
        return when (fn) {
            "sma" -> {
                val src = evalExpr(args.getOrNull(0) ?: PineExpr.BuiltInSeries("close"), env, candles).map { it ?: 0.0 }
                val len = resolveParamInt(args.getOrNull(1), env, candles, 14)
                TechnicalIndicatorEngine.computeSMA(src, len)
            }
            "ema" -> {
                val src = evalExpr(args.getOrNull(0) ?: PineExpr.BuiltInSeries("close"), env, candles).map { it ?: 0.0 }
                val len = resolveParamInt(args.getOrNull(1), env, candles, 14)
                TechnicalIndicatorEngine.computeEMA(src, len)
            }
            "wma" -> {
                val src = evalExpr(args.getOrNull(0) ?: PineExpr.BuiltInSeries("close"), env, candles).map { it ?: 0.0 }
                val len = resolveParamInt(args.getOrNull(1), env, candles, 14)
                TechnicalIndicatorEngine.computeWMA(src, len)
            }
            "rma" -> {
                val src = evalExpr(args.getOrNull(0) ?: PineExpr.BuiltInSeries("close"), env, candles).map { it ?: 0.0 }
                val len = resolveParamInt(args.getOrNull(1), env, candles, 14)
                TechnicalIndicatorEngine.computeRMA(src, len)
            }
            "hma" -> {
                val src = evalExpr(args.getOrNull(0) ?: PineExpr.BuiltInSeries("close"), env, candles).map { it ?: 0.0 }
                val len = resolveParamInt(args.getOrNull(1), env, candles, 14)
                TechnicalIndicatorEngine.computeHMA(src, len)
            }
            "rsi" -> {
                val src = evalExpr(args.getOrNull(0) ?: PineExpr.BuiltInSeries("close"), env, candles).map { it ?: 0.0 }
                val len = resolveParamInt(args.getOrNull(1), env, candles, 14)
                TechnicalIndicatorEngine.computeRSI(src, len)
            }
            "atr" -> {
                val len = resolveParamInt(args.getOrNull(0), env, candles, 14)
                TechnicalIndicatorEngine.computeATR(candles, len)
            }
            "tr" -> {
                env["tr"] ?: List(size) { null }
            }
            "change" -> {
                val src = evalExpr(args.getOrNull(0) ?: PineExpr.BuiltInSeries("close"), env, candles)
                val len = resolveParamInt(args.getOrNull(1), env, candles, 1)
                List(size) { i ->
                    if (i - len < 0) null
                    else {
                        val curr = src[i]
                        val prev = src[i - len]
                        if (curr != null && prev != null) curr - prev else null
                    }
                }
            }
            "mom" -> {
                val src = evalExpr(args.getOrNull(0) ?: PineExpr.BuiltInSeries("close"), env, candles)
                val len = resolveParamInt(args.getOrNull(1), env, candles, 10)
                List(size) { i ->
                    if (i - len < 0) null
                    else {
                        val curr = src[i]
                        val prev = src[i - len]
                        if (curr != null && prev != null) curr - prev else null
                    }
                }
            }
            "highest" -> {
                val (src, len) = if (args.size == 1) {
                    Pair(env["high"] ?: List(size) { null }, resolveParamInt(args.getOrNull(0), env, candles, 14))
                } else {
                    Pair(evalExpr(args[0], env, candles), resolveParamInt(args.getOrNull(1), env, candles, 14))
                }
                List(size) { i ->
                    if (i < len - 1) null
                    else src.subList(max(0, i - len + 1), i + 1).filterNotNull().maxOrNull()
                }
            }
            "lowest" -> {
                val (src, len) = if (args.size == 1) {
                    Pair(env["low"] ?: List(size) { null }, resolveParamInt(args.getOrNull(0), env, candles, 14))
                } else {
                    Pair(evalExpr(args[0], env, candles), resolveParamInt(args.getOrNull(1), env, candles, 14))
                }
                List(size) { i ->
                    if (i < len - 1) null
                    else src.subList(max(0, i - len + 1), i + 1).filterNotNull().minOrNull()
                }
            }
            "crossover" -> {
                val s1 = evalExpr(args[0], env, candles)
                val s2 = evalExpr(args[1], env, candles)
                List(size) { i ->
                    if (i == 0) 0.0
                    else {
                        val curr1 = s1[i]
                        val prev1 = s1[i - 1]
                        val curr2 = s2[i]
                        val prev2 = s2[i - 1]
                        if (curr1 != null && prev1 != null && curr2 != null && prev2 != null) {
                            if (prev1 <= prev2 && curr1 > curr2) 1.0 else 0.0
                        } else 0.0
                    }
                }
            }
            "crossunder" -> {
                val s1 = evalExpr(args[0], env, candles)
                val s2 = evalExpr(args[1], env, candles)
                List(size) { i ->
                    if (i == 0) 0.0
                    else {
                        val curr1 = s1[i]
                        val prev1 = s1[i - 1]
                        val curr2 = s2[i]
                        val prev2 = s2[i - 1]
                        if (curr1 != null && prev1 != null && curr2 != null && prev2 != null) {
                            if (prev1 >= prev2 && curr1 < curr2) 1.0 else 0.0
                        } else 0.0
                    }
                }
            }
            "cross" -> {
                val s1 = evalExpr(args[0], env, candles)
                val s2 = evalExpr(args[1], env, candles)
                List(size) { i ->
                    if (i == 0) 0.0
                    else {
                        val curr1 = s1[i]
                        val prev1 = s1[i - 1]
                        val curr2 = s2[i]
                        val prev2 = s2[i - 1]
                        if (curr1 != null && prev1 != null && curr2 != null && prev2 != null) {
                            val over = prev1 <= prev2 && curr1 > curr2
                            val under = prev1 >= prev2 && curr1 < curr2
                            if (over || under) 1.0 else 0.0
                        } else 0.0
                    }
                }
            }
            "stdev" -> {
                val src = evalExpr(args.getOrNull(0) ?: PineExpr.BuiltInSeries("close"), env, candles).map { it ?: 0.0 }
                val len = resolveParamInt(args.getOrNull(1), env, candles, 20)
                TechnicalIndicatorEngine.computeStdDev(src, len)
            }
            "cum" -> {
                val src = evalExpr(args[0], env, candles)
                var sum = 0.0
                src.map { v ->
                    if (v != null) {
                        sum += v
                        sum
                    } else null
                }
            }
            "macd" -> {
                // Single variable fallback returns MACD line
                val src = evalExpr(args.getOrNull(0) ?: PineExpr.BuiltInSeries("close"), env, candles).map { it ?: 0.0 }
                val fast = resolveParamInt(args.getOrNull(1), env, candles, 12)
                val slow = resolveParamInt(args.getOrNull(2), env, candles, 26)
                val signal = resolveParamInt(args.getOrNull(3), env, candles, 9)
                TechnicalIndicatorEngine.computeMACD(src, fast, slow, signal).first
            }
            "cci" -> {
                val len = resolveParamInt(args.getOrNull(1) ?: args.getOrNull(0), env, candles, 14)
                TechnicalIndicatorEngine.computeCCI(candles, len)
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
            "round" -> arg0.map { it?.let { v -> round(v) } }
            "floor" -> arg0.map { it?.let { v -> floor(v) } }
            "ceil" -> arg0.map { it?.let { v -> ceil(v) } }
            "sign" -> arg0.map { it?.let { v -> sign(v) } }
            "log" -> arg0.map { it?.let { v -> if (v > 0) ln(v) else null } }
            "pow" -> {
                val arg1 = evalExpr(args[1], env, candles)
                List(size) { i ->
                    val a = arg0[i]
                    val b = arg1[i]
                    if (a != null && b != null) a.pow(b) else null
                }
            }
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
        
        // Confluence signal: Fast crosses above Mid while price is above Slow
        bullCross = ta.crossover(emaFast, emaMid) and close > emaSlow
        plotshape(bullCross, title="Ribbon Buy", color=color.green)
    """.trimIndent()

    val BOLLINGER_SQUEEZE = """
        //@version=6
        indicator("Bollinger & Keltner Squeeze", overlay=true)
        
        bbLen = input.int(20, "BB Length")
        bbMult = input.float(2.0, "BB Mult")
        
        bbBasis = ta.sma(close, bbLen)
        dev = bbMult * ta.stdev(close, bbLen)
        bbUpper = bbBasis + dev
        bbLower = bbBasis - dev
        
        plot(bbBasis, title="BB Basis", color=color.blue, linewidth=2)
        plot(bbUpper, title="BB Upper", color=color.gray, linewidth=1)
        plot(bbLower, title="BB Lower", color=color.gray, linewidth=1)
        
        // Historical index verification: breakout of previous bar's upper band
        breakout = close > bbUpper[1] and close[1] <= bbUpper[2]
        plotshape(breakout, title="Band Breakout", color=color.green)
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
        
        // Reversal signal from oversold zone with previous bar confirmation
        oversoldBounce = rsiVal > 30 and rsiVal[1] <= 30
        plotshape(oversoldBounce, title="RSI Bounce", color=color.green)
    """.trimIndent()

    val SUPERTREND_CHANDELIER = """
        //@version=6
        indicator("Volatility Trailing Band", overlay=true)
        
        atrPeriod = input.int(10, "ATR Period")
        mult = input.float(3.0, "ATR Multiplier")
        
        atrVal = ta.atr(atrPeriod)
        upperBand = ta.highest(high, 10) - (atrVal * mult)
        lowerBand = ta.lowest(low, 10) + (atrVal * mult)
        
        plot(upperBand, title="Trailing Support", color=color.green, linewidth=2)
        plot(lowerBand, title="Trailing Resistance", color=color.red, linewidth=2)
    """.trimIndent()

    val MACD_HISTOGRAM = """
        //@version=6
        indicator("MACD Oscillator", overlay=false)
        
        fast = input.int(12, "Fast Length")
        slow = input.int(26, "Slow Length")
        signal = input.int(9, "Signal Length")
        
        [macdLine, signalLine, histLine] = ta.macd(close, fast, slow, signal)
        
        plot(macdLine, title="MACD", color=color.blue, linewidth=2)
        plot(signalLine, title="Signal", color=color.orange, linewidth=1)
        plot(histLine, title="Histogram", color=color.green, linewidth=2)
        hline(0, title="Zero Line", color=color.gray)
    """.trimIndent()
}
