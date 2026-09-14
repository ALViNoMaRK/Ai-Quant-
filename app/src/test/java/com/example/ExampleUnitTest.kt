package com.example

import com.example.data.model.CandleData
import com.example.engine.pine.IndicatorSandbox
import com.example.engine.pine.PineParser
import com.example.engine.pine.PrebuiltPineScripts
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testHistoricalIndexingParserAndSandbox() {
        val script = """
            //@version=6
            indicator("History Test", overlay=true)
            prevClose = close[1] // Previous bar close
            twoAgo = close[2]
            higherThanPrev = close > close[1] ? 1.0 : 0.0
            plot(prevClose, title="Prev Close")
            plot(twoAgo, title="Two Ago")
            plot(higherThanPrev, title="Higher")
        """.trimIndent()

        val parser = PineParser()
        val compileRes = parser.parse(script)
        assertTrue("Compilation should succeed: ${compileRes.errorMessage}", compileRes.success)

        val candles = listOf(
            CandleData(timestamp = 1000L, open = 100.0, high = 105.0, low = 95.0, close = 102.0, volume = 1000L),
            CandleData(timestamp = 2000L, open = 102.0, high = 108.0, low = 101.0, close = 107.0, volume = 1200L),
            CandleData(timestamp = 3000L, open = 107.0, high = 109.0, low = 103.0, close = 104.0, volume = 1100L)
        )

        val sandbox = IndicatorSandbox()
        val execRes = sandbox.execute(compileRes, candles)
        assertTrue("Execution should succeed: ${execRes.errorMessage}", execRes.success)
        assertEquals(3, execRes.plots.size)

        val prevClosePlot = execRes.plots.first { it.title == "Prev Close" }
        assertNull(prevClosePlot.series[0]) // bar 0 has no history
        assertEquals(102.0, prevClosePlot.series[1]!!, 0.001) // bar 1 sees close of bar 0 (102.0)
        assertEquals(107.0, prevClosePlot.series[2]!!, 0.001) // bar 2 sees close of bar 1 (107.0)

        val twoAgoPlot = execRes.plots.first { it.title == "Two Ago" }
        assertNull(twoAgoPlot.series[0])
        assertNull(twoAgoPlot.series[1])
        assertEquals(102.0, twoAgoPlot.series[2]!!, 0.001) // bar 2 sees close of bar 0 (102.0)

        val higherPlot = execRes.plots.first { it.title == "Higher" }
        // bar 1: close is 107.0, prev is 102.0 -> 107 > 102 -> 1.0
        assertEquals(1.0, higherPlot.series[1]!!, 0.001)
        // bar 2: close is 104.0, prev is 107.0 -> 104 > 107 is false -> 0.0
        assertEquals(0.0, higherPlot.series[2]!!, 0.001)
    }

    @Test
    fun testPrebuiltScriptsCompilation() {
        val parser = PineParser()

        val tripleEma = parser.parse(PrebuiltPineScripts.TRIPLE_EMA)
        assertTrue("Triple EMA should parse: ${tripleEma.errorMessage}", tripleEma.success)

        val squeeze = parser.parse(PrebuiltPineScripts.BOLLINGER_SQUEEZE)
        assertTrue("Bollinger Squeeze should parse: ${squeeze.errorMessage}", squeeze.success)

        val rsi = parser.parse(PrebuiltPineScripts.RSI_EXTREMES)
        assertTrue("RSI Extremes should parse: ${rsi.errorMessage}", rsi.success)

        val supertrend = parser.parse(PrebuiltPineScripts.SUPERTREND_CHANDELIER)
        assertTrue("Supertrend Chandelier should parse: ${supertrend.errorMessage}", supertrend.success)

        val macd = parser.parse(PrebuiltPineScripts.MACD_HISTOGRAM)
        assertTrue("MACD Histogram should parse: ${macd.errorMessage}", macd.success)
    }
}
