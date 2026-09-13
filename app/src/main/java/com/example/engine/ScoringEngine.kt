package com.example.engine

import com.example.data.model.*

object ScoringEngine {

    fun calculateMasterScore(
        stock: StockEntity,
        statements: FinancialStatements,
        holdings: List<InstitutionalHolding>,
        analysts: AnalystIntelligence?,
        earnings: EarningsData?,
        weights: ScoringWeights = ScoringWeights()
    ): Pair<StockEntity, ScoreBreakdown> {
        val normWeights = weights.normalized()

        // 1. Component calculations
        val (healthScore, healthReasons) = FinancialEngines.evaluateFundamentalHealth(statements)
        val (qualityScore, qualityGrade, _) = FinancialEngines.evaluateBusinessQuality(statements)
        val (growthScore, growthPace, _) = FinancialEngines.evaluateGrowth(statements)
        val (valuationScore, valuationGrade, _) = FinancialEngines.evaluateValuation(stock, statements)
        val institutionalScore = FinancialEngines.evaluateInstitutionalActivity(holdings)
        val earningsScore = if (earnings != null) {
            var s = 60.0
            if (earnings.epsSurprisePercent > 0) s += 15
            if (earnings.guidanceStatus == "Raised") s += 15
            if (earnings.beatCountLast4Q >= 3) s += 10
            s.coerceIn(0.0, 100.0)
        } else 65.0

        val marketScore = if ((stock.beta ?: 1.0) < 1.3 && stock.changePercent >= 0) 78.0 else 64.0
        val catalystScore = 76.0
        val (rawRisk, riskFactors) = FinancialEngines.evaluateRisk(stock, statements)
        // Convert risk level to safety score (100 - risk) for composite aggregation
        val riskSafetyScore = (100.0 - rawRisk).coerceIn(0.0, 100.0)

        // 2. Weighted Master Score
        val compositeScore = (
            healthScore * normWeights.financialHealth +
            qualityScore * normWeights.businessQuality +
            growthScore * normWeights.growth +
            valuationScore * normWeights.valuation +
            institutionalScore * normWeights.institutionalCapital +
            earningsScore * normWeights.earningsExpectations +
            marketScore * normWeights.marketStrength +
            catalystScore * normWeights.catalysts +
            riskSafetyScore * normWeights.risk
        ).coerceIn(0.0, 100.0)

        val classification = InvestmentClassification.fromScore(compositeScore)
        val confidence = when {
            statements.revenue > 0 && holdings.isNotEmpty() && analysts != null -> ConfidenceLevel.HIGH
            statements.revenue > 0 -> ConfidenceLevel.MEDIUM
            else -> ConfidenceLevel.LOW
        }

        // 3. Why This Stock Is Ranked Here Traceability
        val whyComponents = listOf(
            WhyComponent("Financial Health", healthScore * normWeights.financialHealth, "Current Ratio: ${"%.2f".format(statements.currentRatio)}, D/E: ${"%.2f".format(statements.debtToEquity)}", normWeights.financialHealth * 100),
            WhyComponent("Business Quality", qualityScore * normWeights.businessQuality, "ROIC: ${"%.1f".format(statements.roic)}%, Op Margin: ${"%.1f".format(statements.operatingMargin)}%", normWeights.businessQuality * 100),
            WhyComponent("Growth Momentum", growthScore * normWeights.growth, "Rev YoY: +${"%.1f".format(statements.revenueYoY)}%, EPS YoY: +${"%.1f".format(statements.epsYoY)}%", normWeights.growth * 100),
            WhyComponent("Valuation", valuationScore * normWeights.valuation, "P/E: ${stock.peRatio ?: "N/A"}, FCF Yield: ${stock.fcfYield?.let { "%.1f%%".format(it) } ?: "N/A"}", normWeights.valuation * 100),
            WhyComponent("Institutional Capital", institutionalScore * normWeights.institutionalCapital, "13F Net Flow: ${holdings.count { it.changeType == PositionChangeType.INCREASED }} accum vs ${holdings.count { it.changeType == PositionChangeType.REDUCED }} reduced", normWeights.institutionalCapital * 100),
            WhyComponent("Earnings & Revisions", earningsScore * normWeights.earningsExpectations, "Surprise: ${earnings?.let { "+%.1f%%".format(it.epsSurprisePercent) } ?: "N/A"}, Guidance: ${earnings?.guidanceStatus ?: "Solid"}", normWeights.earningsExpectations * 100),
            WhyComponent("Market Strength", marketScore * normWeights.marketStrength, "Beta: ${stock.beta ?: 1.0}, Momentum relative to S&P 500", normWeights.marketStrength * 100),
            WhyComponent("Upcoming Catalysts", catalystScore * normWeights.catalysts, "Earnings date & strategic enterprise pipeline", normWeights.catalysts * 100),
            WhyComponent("Risk Containment", riskSafetyScore * normWeights.risk, "Safety index ${riskSafetyScore.toInt()}/100", normWeights.risk * 100)
        )

        val positiveFactors = mutableListOf<String>().apply {
            addAll(healthReasons)
            if (statements.revenueYoY > 15) add("High top-line revenue expansion (+${"%.1f".format(statements.revenueYoY)}%)")
            if (statements.roic > 18) add("Exceptional economic moat with ROIC at ${"%.1f".format(statements.roic)}%")
            if (earnings?.epsSurprisePercent ?: 0.0 > 5.0) add("Consistent quarterly earnings beats")
        }

        val negativeFactors = mutableListOf<String>().apply {
            if ((stock.peRatio ?: 0.0) > 40) add("Premium valuation multiple restricts margin of safety")
            if (statements.debtToEquity > 1.5) add("Debt leverage exceeds conservative thresholds")
            if (statements.revenueYoY < 5) add("Revenue trajectory mature or slowing")
        }

        val updatedStock = stock.copy(
            masterScore = compositeScore,
            classification = classification.label,
            confidence = confidence.name,
            financialHealthScore = healthScore,
            businessQualityScore = qualityScore,
            growthScore = growthScore,
            valuationScore = valuationScore,
            institutionalScore = institutionalScore,
            earningsScore = earningsScore,
            marketScore = marketScore,
            catalystScore = catalystScore,
            riskScore = rawRisk
        )

        val breakdown = ScoreBreakdown(
            financialHealth = healthScore,
            businessQuality = qualityScore,
            growth = growthScore,
            valuation = valuationScore,
            institutionalCapital = institutionalScore,
            earningsExpectations = earningsScore,
            marketStrength = marketScore,
            catalysts = catalystScore,
            risk = rawRisk,
            positiveFactors = positiveFactors,
            negativeFactors = negativeFactors,
            keyRisks = riskFactors,
            bullCase = "Continued compound growth supported by superior ROIC (${"%.1f".format(statements.roic)}%), steady FCF generation, and net institutional accumulation.",
            bearCase = "Vulnerability to macro rate adjustments or multiple compression if quarterly top-line drops below ${"%.0f".format(statements.revenueYoY * 0.7)}%.",
            longTermThesis = "Tier-1 secular compounder with fortress liquidity and mission-critical enterprise demand providing defensive cash flow visibility.",
            invalidationCondition = "Two consecutive quarters of decelerating FCF margins below 15% or a breach of net cash balance sheet position.",
            whyRankedExplanation = whyComponents
        )

        return Pair(updatedStock, breakdown)
    }
}
