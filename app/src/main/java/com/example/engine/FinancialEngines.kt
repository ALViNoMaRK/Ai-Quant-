package com.example.engine

import com.example.data.model.*

object FinancialEngines {

    fun evaluateFundamentalHealth(st: FinancialStatements): Pair<Double, List<String>> {
        var score = 50.0
        val reasons = mutableListOf<String>()

        // 1. Current Ratio (Liquidity)
        when {
            st.currentRatio >= 2.0 -> { score += 15; reasons.add("Superior liquidity (Current Ratio: ${"%.2f".format(st.currentRatio)})") }
            st.currentRatio >= 1.3 -> { score += 10; reasons.add("Healthy liquidity (Current Ratio: ${"%.2f".format(st.currentRatio)})") }
            st.currentRatio < 1.0 -> { score -= 15; reasons.add("Working capital deficit (Current Ratio: ${"%.2f".format(st.currentRatio)})") }
        }

        // 2. Debt to Equity
        when {
            st.debtToEquity <= 0.5 -> { score += 15; reasons.add("Fortress balance sheet with low leverage (D/E: ${"%.2f".format(st.debtToEquity)})") }
            st.debtToEquity <= 1.2 -> { score += 8; reasons.add("Manageable financial leverage (D/E: ${"%.2f".format(st.debtToEquity)})") }
            st.debtToEquity > 2.5 -> { score -= 15; reasons.add("Elevated debt load (D/E: ${"%.2f".format(st.debtToEquity)})") }
        }

        // 3. Interest Coverage
        st.interestCoverage?.let { cov ->
            when {
                cov >= 10.0 -> { score += 10; reasons.add("Robust interest coverage (${"%.1f".format(cov)}x)") }
                cov >= 4.0 -> { score += 5 }
                cov < 2.0 -> { score -= 15; reasons.add("Tight interest coverage risk (${"%.1f".format(cov)}x)") }
            }
        }

        // 4. Net Debt to EBITDA
        when {
            st.netDebt <= 0 -> { score += 10; reasons.add("Negative net debt (Net cash position: $${"%.2f".format(-st.netDebt / 1e9)}B)") }
            st.netDebtToEbitda <= 1.5 -> { score += 5 }
            st.netDebtToEbitda > 4.0 -> { score -= 10; reasons.add("High net debt/EBITDA multiple (${"%.2f".format(st.netDebtToEbitda)}x)") }
        }

        return Pair(score.coerceIn(0.0, 100.0), reasons)
    }

    fun evaluateBusinessQuality(st: FinancialStatements): Triple<Double, BusinessQualityGrade, List<String>> {
        var score = 40.0
        val reasons = mutableListOf<String>()

        // ROIC (Invested capital efficiency)
        when {
            st.roic >= 25.0 -> { score += 25; reasons.add("Exceptional capital efficiency (ROIC: ${"%.1f".format(st.roic)}%)") }
            st.roic >= 15.0 -> { score += 18; reasons.add("Strong economic moat (ROIC: ${"%.1f".format(st.roic)}%)") }
            st.roic >= 9.0 -> { score += 10 }
            st.roic < 5.0 -> { score -= 10; reasons.add("Sub-par return on invested capital (ROIC: ${"%.1f".format(st.roic)}%)") }
        }

        // ROE
        when {
            st.roe >= 20.0 -> { score += 15; reasons.add("High return on equity (ROE: ${"%.1f".format(st.roe)}%)") }
            st.roe >= 12.0 -> { score += 10 }
            st.roe < 5.0 -> { score -= 8 }
        }

        // Operating Margin
        when {
            st.operatingMargin >= 30.0 -> { score += 15; reasons.add("Tier-1 operating margin (${"%.1f".format(st.operatingMargin)}%)") }
            st.operatingMargin >= 18.0 -> { score += 10 }
            st.operatingMargin < 8.0 -> { score -= 10; reasons.add("Thin operating margin (${"%.1f".format(st.operatingMargin)}%)") }
        }

        // Cash Conversion (FCF vs Net Income)
        if (st.netIncome > 0 && st.freeCashFlow > 0) {
            val conversion = st.freeCashFlow / st.netIncome
            if (conversion >= 0.9) {
                score += 10
                reasons.add("High quality cash conversion (${"%.0f".format(conversion * 100)}% FCF/NI)")
            } else if (conversion < 0.5) {
                score -= 8
                reasons.add("Earnings quality divergence (low cash conversion)")
            }
        }

        val clamped = score.coerceIn(0.0, 100.0)
        val grade = when {
            clamped >= 85 -> BusinessQualityGrade.EXCEPTIONAL
            clamped >= 75 -> BusinessQualityGrade.STRONG
            clamped >= 60 -> BusinessQualityGrade.GOOD
            clamped >= 45 -> BusinessQualityGrade.AVERAGE
            else -> BusinessQualityGrade.WEAK
        }
        return Triple(clamped, grade, reasons)
    }

    fun evaluateGrowth(st: FinancialStatements): Triple<Double, GrowthPace, List<String>> {
        var score = 45.0
        val reasons = mutableListOf<String>()

        // Revenue YoY
        when {
            st.revenueYoY >= 30.0 -> { score += 25; reasons.add("Hyper revenue growth (+${"%.1f".format(st.revenueYoY)}% YoY)") }
            st.revenueYoY >= 15.0 -> { score += 18; reasons.add("Robust top-line expansion (+${"%.1f".format(st.revenueYoY)}% YoY)") }
            st.revenueYoY >= 7.0 -> { score += 10 }
            st.revenueYoY <= 0.0 -> { score -= 20; reasons.add("Top-line contraction (${"%.1f".format(st.revenueYoY)}% YoY)") }
        }

        // EPS YoY
        when {
            st.epsYoY >= 25.0 -> { score += 20; reasons.add("Accelerating earnings per share (+${"%.1f".format(st.epsYoY)}% YoY)") }
            st.epsYoY >= 12.0 -> { score += 12 }
            st.epsYoY <= 0.0 -> { score -= 15; reasons.add("Earnings decline (${"%.1f".format(st.epsYoY)}% YoY)") }
        }

        // FCF Growth
        when {
            st.fcfYoY >= 20.0 -> { score += 15; reasons.add("Strong FCF generation growth (+${"%.1f".format(st.fcfYoY)}% YoY)") }
            st.fcfYoY > 0 -> { score += 8 }
            st.fcfYoY < -15.0 -> { score -= 10; reasons.add("Cash flow contraction") }
        }

        val clamped = score.coerceIn(0.0, 100.0)
        val pace = when {
            st.revenueYoY > 20.0 && st.epsYoY > 15.0 -> GrowthPace.ACCELERATING
            st.revenueYoY >= 5.0 -> GrowthPace.STABLE
            st.revenueYoY >= 0.0 -> GrowthPace.DECELERATING
            else -> GrowthPace.CONTRACTING
        }
        return Triple(clamped, pace, reasons)
    }

    fun evaluateValuation(stock: StockEntity, st: FinancialStatements): Triple<Double, ValuationGrade, List<String>> {
        var score = 50.0
        val reasons = mutableListOf<String>()

        val pe = stock.peRatio
        val fwdPe = stock.forwardPe
        val peg = stock.pegRatio
        val fcfYield = stock.fcfYield

        // P/E and Forward P/E relative to growth
        if (pe != null && pe > 0) {
            when {
                pe <= 18.0 -> { score += 15; reasons.add("Attractive trailing P/E multiple (${"%.1f".format(pe)}x)") }
                pe <= 28.0 -> { score += 8 }
                pe > 60.0 -> { score -= 15; reasons.add("High valuation multiple (P/E: ${"%.1f".format(pe)}x)") }
            }
        }

        // PEG ratio
        if (peg != null && peg > 0) {
            when {
                peg <= 1.0 -> { score += 20; reasons.add("Undervalued relative to growth (PEG: ${"%.2f".format(peg)})") }
                peg <= 1.8 -> { score += 10; reasons.add("Reasonable growth-adjusted multiple (PEG: ${"%.2f".format(peg)})") }
                peg > 2.8 -> { score -= 12; reasons.add("Premium growth pricing (PEG: ${"%.2f".format(peg)})") }
            }
        }

        // FCF Yield
        if (fcfYield != null) {
            when {
                fcfYield >= 5.5 -> { score += 15; reasons.add("High Free Cash Flow yield (${"%.1f".format(fcfYield)}%)") }
                fcfYield >= 3.0 -> { score += 8 }
                fcfYield < 1.0 -> { score -= 8; reasons.add("Low FCF yield (${"%.1f".format(fcfYield)}%)") }
            }
        }

        val clamped = score.coerceIn(0.0, 100.0)
        val grade = when {
            clamped >= 80 -> ValuationGrade.UNDERVALUED
            clamped >= 65 -> ValuationGrade.FAIRLY_VALUED
            clamped >= 50 -> ValuationGrade.PREMIUM
            clamped >= 35 -> ValuationGrade.EXPENSIVE
            else -> ValuationGrade.EXTREMELY_EXPENSIVE
        }
        return Triple(clamped, grade, reasons)
    }

    fun evaluateInstitutionalActivity(holdings: List<InstitutionalHolding>): Double {
        if (holdings.isEmpty()) return 60.0
        var score = 55.0
        val netAccumulation = holdings.count { it.changeType == PositionChangeType.INCREASED || it.changeType == PositionChangeType.NEW_POSITION }
        val netReduction = holdings.count { it.changeType == PositionChangeType.REDUCED || it.changeType == PositionChangeType.EXITED }

        score += (netAccumulation - netReduction) * 7.5
        val topTierStake = holdings.filter { it.portfolioWeight > 3.0 }.size
        score += topTierStake * 4.0

        return score.coerceIn(0.0, 100.0)
    }

    fun evaluateRisk(stock: StockEntity, st: FinancialStatements): Pair<Double, List<String>> {
        // Lower risk score = lower danger (higher quality/safety)
        var riskLevel = 30.0
        val riskFactors = mutableListOf<String>()

        if (st.debtToEquity > 2.0) {
            riskLevel += 20
            riskFactors.add("High debt leverage ratio (${"%.2f".format(st.debtToEquity)}x equity)")
        }
        if ((stock.peRatio ?: 0.0) > 55.0) {
            riskLevel += 18
            riskFactors.add("Multiple compression vulnerability (P/E > 55x)")
        }
        if (st.operatingMargin < 10.0) {
            riskLevel += 12
            riskFactors.add("Supply/wage inflation vulnerability from compressed margins")
        }
        if ((stock.beta ?: 1.0) > 1.4) {
            riskLevel += 10
            riskFactors.add("Elevated equity beta volatility (${"%.2f".format(stock.beta)})")
        }
        if (st.growthPace == GrowthPace.DECELERATING || st.growthPace == GrowthPace.CONTRACTING) {
            riskLevel += 15
            riskFactors.add("Decelerating growth momentum")
        }

        return Pair(riskLevel.coerceIn(0.0, 100.0), riskFactors)
    }
}
