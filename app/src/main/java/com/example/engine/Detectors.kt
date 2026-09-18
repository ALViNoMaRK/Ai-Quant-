package com.example.engine

import com.example.data.model.FinancialStatements
import com.example.data.model.GrowthPace
import com.example.data.model.StockEntity

data class OpportunitySignal(
    val symbol: String,
    val companyName: String,
    val opportunityScore: Int, // 0-100
    val primaryTrigger: String,
    val evidenceList: List<String>,
    val valuationMultiple: String,
    val growthRate: String
)

data class DeteriorationSignal(
    val symbol: String,
    val companyName: String,
    val severity: String, // CRITICAL, ELEVATED, MODERATE
    val headline: String,
    val rootCauses: List<String>,
    val thesisStatus: String // "THESIS DETERIORATING", "MONITOR CLOSELY"
)

object OpportunityDetector {
    fun scanForOpportunities(stocks: List<Pair<StockEntity, FinancialStatements>>): List<OpportunitySignal> {
        val signals = mutableListOf<OpportunitySignal>()

        for ((stock, st) in stocks) {
            val evidence = mutableListOf<String>()
            var score = 50

            if (st.growthPace == GrowthPace.ACCELERATING) {
                score += 20
                evidence.add("Top-line revenue accelerating (+${"%.1f".format(st.revenueYoY)}% YoY)")
            }
            if (st.epsYoY > 20.0) {
                score += 15
                evidence.add("Strong EPS trajectory (+${"%.1f".format(st.epsYoY)}% YoY)")
            }
            if (st.freeCashFlow > 0 && st.fcfYoY > 15.0) {
                score += 15
                evidence.add("Free Cash Flow expanding rapidly (+${"%.1f".format(st.fcfYoY)}% YoY)")
            }
            if (st.roic >= 18.0) {
                score += 10
                evidence.add("High Return on Invested Capital (${"%.1f".format(st.roic)}%) indicating sustainable moat")
            }
            if (stock.institutionalScore >= 75.0) {
                score += 10
                evidence.add("Positive institutional 13F net accumulation detected")
            }
            if ((stock.pegRatio ?: 3.0) in 0.1..1.5) {
                score += 10
                evidence.add("Growth-adjusted valuation multiple highly favorable (PEG: ${"%.2f".format(stock.pegRatio)})")
            }

            if (score >= 70) {
                signals.add(
                    OpportunitySignal(
                        symbol = stock.symbol,
                        companyName = stock.companyName,
                        opportunityScore = score.coerceIn(0, 100),
                        primaryTrigger = if (st.growthPace == GrowthPace.ACCELERATING) "Accelerating Fundamental Compounder" else "Improving Cash Flow & Multiple Discount",
                        evidenceList = evidence,
                        valuationMultiple = "P/E ${stock.peRatio?.let { "%.1f".format(it) } ?: "N/A"}",
                        growthRate = "+${"%.1f".format(st.revenueYoY)}% Rev"
                    )
                )
            }
        }

        return signals.sortedByDescending { it.opportunityScore }
    }
}

object DeteriorationDetector {
    fun scanForDeterioration(stocks: List<Pair<StockEntity, FinancialStatements>>): List<DeteriorationSignal> {
        val signals = mutableListOf<DeteriorationSignal>()

        for ((stock, st) in stocks) {
            val causes = mutableListOf<String>()
            var severityPoints = 0

            if (st.growthPace == GrowthPace.CONTRACTING) {
                severityPoints += 30
                causes.add("Top-line revenue contraction detected (${"%.1f".format(st.revenueYoY)}% YoY)")
            } else if (st.growthPace == GrowthPace.DECELERATING && st.revenueYoY < 3.0) {
                severityPoints += 20
                causes.add("Significant revenue deceleration trend (${"%.1f".format(st.revenueYoY)}% YoY)")
            }

            if (st.operatingMargin < 8.0) {
                severityPoints += 15
                causes.add("Operating margins compressed to ${"%.1f".format(st.operatingMargin)}%")
            }

            if (st.freeCashFlow < 0) {
                severityPoints += 25
                causes.add("Negative Free Cash Flow burn of $${"%.2f".format(-st.freeCashFlow / 1e9)}B")
            }

            if (st.debtToEquity > 2.2) {
                severityPoints += 15
                causes.add("High leverage with D/E at ${"%.2f".format(st.debtToEquity)}x")
            }

            if (stock.institutionalScore < 45.0) {
                severityPoints += 15
                causes.add("Net institutional distribution and fund reductions observed in recent 13F cycle")
            }

            if (severityPoints >= 35) {
                val severity = when {
                    severityPoints >= 60 -> "CRITICAL"
                    severityPoints >= 45 -> "ELEVATED"
                    else -> "MODERATE"
                }
                signals.add(
                    DeteriorationSignal(
                        symbol = stock.symbol,
                        companyName = stock.companyName,
                        severity = severity,
                        headline = "Thesis Deteriorating: ${causes.firstOrNull() ?: "Fundamental headwind"}",
                        rootCauses = causes,
                        thesisStatus = if (severityPoints >= 55) "THESIS DETERIORATING" else "MONITOR CLOSELY"
                    )
                )
            }
        }

        return signals
    }
}
