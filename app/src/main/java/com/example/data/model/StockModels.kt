package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class InvestmentClassification(val label: String, val minScore: Int, val maxScore: Int) {
    ELITE_CANDIDATE("ELITE CANDIDATE", 90, 100),
    STRONG_CANDIDATE("STRONG CANDIDATE", 80, 89),
    WATCHLIST_ATTRACTIVE("WATCHLIST / ATTRACTIVE", 70, 79),
    NEUTRAL("NEUTRAL", 60, 69),
    SPECULATIVE_CAUTION("SPECULATIVE / CAUTION", 50, 59),
    WEAKENING_AVOID("WEAKENING / AVOID", 0, 49);

    companion object {
        fun fromScore(score: Double): InvestmentClassification {
            val rounded = score.toInt().coerceIn(0, 100)
            return entries.firstOrNull { rounded in it.minScore..it.maxScore } ?: WEAKENING_AVOID
        }
    }
}

enum class ConfidenceLevel {
    HIGH,
    MEDIUM,
    LOW
}

data class ConfidenceScore(
    val percentage: Int = 80,
    val level: ConfidenceLevel = ConfidenceLevel.HIGH,
    val completenessFactors: List<Pair<String, Boolean>> = emptyList()
)

enum class AnalysisHorizon(val displayName: String, val shortLabel: String, val description: String) {
    DAILY("Daily", "1D", "Tactical 1D: Short-term momentum, volume & near-term volatility"),
    WEEKLY("Weekly", "1W", "Swing 1W: Multi-day trend, momentum, recent volume & news catalysts"),
    MONTHLY("Monthly", "1M", "Positional 1M: Earnings trajectory, growth, valuation & institutional flow"),
    THREE_MONTHS("3 Months", "3M", "Quarterly 3M: Fundamental revisions, earnings momentum & sector strength"),
    SIX_MONTHS("6 Months", "6M", "Medium-Term 6M: Business quality, FCF durability & institutional accumulation"),
    ONE_YEAR("1 Year", "1Y", "Long-Term 1Y: Economic moat, ROIC, balance sheet resilience & fair value"),
    THREE_YEARS("3 Years", "3Y", "Cycle 3Y: Capital compounding, multi-year factor regression & persistence"),
    FIVE_YEARS("5 Years", "5Y", "Strategic 5Y: Full economic cycle resilience, intrinsic DCF realization & moat durability")
}

enum class DataFreshness(val label: String) {
    LIVE("LIVE"),
    RECENT("RECENT"),
    DELAYED("DELAYED"),
    STALE("STALE"),
    VERY_STALE("VERY STALE"),
    UNAVAILABLE("UNAVAILABLE")
}

enum class GrowthPace {
    ACCELERATING,
    STABLE,
    DECELERATING,
    CONTRACTING
}

enum class BalanceSheetHealth {
    IMPROVING,
    STABLE,
    WEAKENING
}

enum class BusinessQualityGrade {
    EXCEPTIONAL,
    STRONG,
    GOOD,
    AVERAGE,
    WEAK
}

enum class ValuationGrade {
    UNDERVALUED,
    FAIRLY_VALUED,
    PREMIUM,
    EXPENSIVE,
    EXTREMELY_EXPENSIVE
}

enum class InvestmentModel(val displayName: String, val description: String) {
    BALANCED_MASTER("Institutional Balanced", "Base 9-factor model with weighted balance"),
    LONG_TERM_QUALITY("Long-Term Quality", "Heavy weight on moat, ROIC, balance sheet & FCF"),
    GROWTH_MOMENTUM("Growth Momentum", "Emphasis on accelerating revenue, EPS revisions & catalysts"),
    DEEP_VALUE("Value & Cash Flow", "Discounts to EV/FCF, normalized earnings & safety margin"),
    INSTITUTIONAL_FLOW("Institutional Flow", "Concentrates on 13F accumulation, insider conviction & fund positioning"),
    DEFENSIVE("Defensive & Stability", "Low debt, resilient margins, dividend safety & low beta")
}

@Entity(tableName = "stocks")
data class StockEntity(
    @PrimaryKey val symbol: String,
    val companyName: String,
    val exchange: String,
    val sector: String,
    val industry: String,
    val price: Double,
    val changeAmount: Double,
    val changePercent: Double,
    val marketCap: Double,
    val volume: Long,
    val avgVolume: Long,
    val high52: Double,
    val low52: Double,
    val peRatio: Double?,
    val forwardPe: Double?,
    val pegRatio: Double?,
    val psRatio: Double?,
    val pbRatio: Double?,
    val evToEbitda: Double?,
    val fcfYield: Double?,
    val dividendYield: Double?,
    val beta: Double?,
    val currency: String = "USD",
    val masterScore: Double = 0.0,
    val confidence: String = ConfidenceLevel.MEDIUM.name,
    val classification: String = InvestmentClassification.NEUTRAL.name,
    val financialHealthScore: Double = 0.0,
    val businessQualityScore: Double = 0.0,
    val growthScore: Double = 0.0,
    val valuationScore: Double = 0.0,
    val institutionalScore: Double = 0.0,
    val earningsScore: Double = 0.0,
    val marketScore: Double = 0.0,
    val catalystScore: Double = 0.0,
    val riskScore: Double = 0.0,
    val lastUpdated: Long = System.currentTimeMillis(),
    val freshness: String = DataFreshness.RECENT.name,
    val dataSource: String = "Yahoo Finance + SEC EDGAR"
)

data class ScoringWeights(
    val financialHealth: Double = 0.20,
    val businessQuality: Double = 0.15,
    val growth: Double = 0.15,
    val valuation: Double = 0.15,
    val institutionalCapital: Double = 0.10,
    val earningsExpectations: Double = 0.10,
    val marketStrength: Double = 0.05,
    val catalysts: Double = 0.05,
    val risk: Double = 0.05
) {
    fun normalized(): ScoringWeights {
        val total = financialHealth + businessQuality + growth + valuation +
                institutionalCapital + earningsExpectations + marketStrength + catalysts + risk
        if (total <= 0.0) return ScoringWeights()
        return ScoringWeights(
            financialHealth / total,
            businessQuality / total,
            growth / total,
            valuation / total,
            institutionalCapital / total,
            earningsExpectations / total,
            marketStrength / total,
            catalysts / total,
            risk / total
        )
    }

    companion object {
        fun forModel(model: InvestmentModel): ScoringWeights {
            return when (model) {
                InvestmentModel.BALANCED_MASTER -> ScoringWeights(0.20, 0.15, 0.15, 0.15, 0.10, 0.10, 0.05, 0.05, 0.05)
                InvestmentModel.LONG_TERM_QUALITY -> ScoringWeights(0.25, 0.25, 0.10, 0.15, 0.05, 0.05, 0.05, 0.05, 0.05)
                InvestmentModel.GROWTH_MOMENTUM -> ScoringWeights(0.10, 0.10, 0.30, 0.05, 0.10, 0.20, 0.05, 0.05, 0.05)
                InvestmentModel.DEEP_VALUE -> ScoringWeights(0.20, 0.10, 0.05, 0.35, 0.05, 0.05, 0.05, 0.05, 0.10)
                InvestmentModel.INSTITUTIONAL_FLOW -> ScoringWeights(0.10, 0.10, 0.10, 0.10, 0.35, 0.10, 0.05, 0.05, 0.05)
                InvestmentModel.DEFENSIVE -> ScoringWeights(0.30, 0.20, 0.05, 0.15, 0.05, 0.05, 0.05, 0.05, 0.10)
            }
        }

        fun forHorizon(horizon: AnalysisHorizon): ScoringWeights {
            return when (horizon) {
                AnalysisHorizon.DAILY -> ScoringWeights(
                    financialHealth = 0.05,
                    businessQuality = 0.05,
                    growth = 0.05,
                    valuation = 0.05,
                    institutionalCapital = 0.10,
                    earningsExpectations = 0.15,
                    marketStrength = 0.35,
                    catalysts = 0.10,
                    risk = 0.10
                )
                AnalysisHorizon.WEEKLY -> ScoringWeights(
                    financialHealth = 0.10,
                    businessQuality = 0.10,
                    growth = 0.10,
                    valuation = 0.10,
                    institutionalCapital = 0.15,
                    earningsExpectations = 0.15,
                    marketStrength = 0.20,
                    catalysts = 0.10,
                    risk = 0.00
                )
                AnalysisHorizon.MONTHLY -> ScoringWeights(
                    financialHealth = 0.15,
                    businessQuality = 0.15,
                    growth = 0.20,
                    valuation = 0.15,
                    institutionalCapital = 0.15,
                    earningsExpectations = 0.10,
                    marketStrength = 0.05,
                    catalysts = 0.05,
                    risk = 0.00
                )
                AnalysisHorizon.THREE_MONTHS -> ScoringWeights(
                    financialHealth = 0.15,
                    businessQuality = 0.20,
                    growth = 0.20,
                    valuation = 0.15,
                    institutionalCapital = 0.15,
                    earningsExpectations = 0.10,
                    marketStrength = 0.00,
                    catalysts = 0.05,
                    risk = 0.00
                )
                AnalysisHorizon.SIX_MONTHS -> ScoringWeights(
                    financialHealth = 0.20,
                    businessQuality = 0.25,
                    growth = 0.20,
                    valuation = 0.15,
                    institutionalCapital = 0.10,
                    earningsExpectations = 0.05,
                    marketStrength = 0.00,
                    catalysts = 0.05,
                    risk = 0.00
                )
                AnalysisHorizon.ONE_YEAR -> ScoringWeights(
                    financialHealth = 0.25,
                    businessQuality = 0.30,
                    growth = 0.15,
                    valuation = 0.15,
                    institutionalCapital = 0.10,
                    earningsExpectations = 0.00,
                    marketStrength = 0.00,
                    catalysts = 0.00,
                    risk = 0.05
                )
                AnalysisHorizon.THREE_YEARS, AnalysisHorizon.FIVE_YEARS -> ScoringWeights(
                    financialHealth = 0.25,
                    businessQuality = 0.35,
                    growth = 0.15,
                    valuation = 0.15,
                    institutionalCapital = 0.05,
                    earningsExpectations = 0.00,
                    marketStrength = 0.00,
                    catalysts = 0.00,
                    risk = 0.05
                )
            }
        }
    }
}
