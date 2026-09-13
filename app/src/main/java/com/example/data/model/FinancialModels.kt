package com.example.data.model

data class FinancialStatements(
    val symbol: String,
    val period: String, // e.g. "FY2024" or "Q3 2025"
    val filingDate: String,
    val revenue: Double, // in USD
    val revenueYoY: Double, // percentage
    val grossProfit: Double,
    val grossMargin: Double, // percentage
    val operatingIncome: Double,
    val operatingMargin: Double,
    val netIncome: Double,
    val netMargin: Double,
    val eps: Double,
    val epsYoY: Double,
    val ebitda: Double,
    val ebitdaMargin: Double,
    val operatingCashFlow: Double,
    val capex: Double,
    val freeCashFlow: Double,
    val fcfMargin: Double,
    val fcfYoY: Double,
    val cashAndEquivalents: Double,
    val totalDebt: Double,
    val netDebt: Double,
    val currentRatio: Double,
    val debtToEquity: Double,
    val netDebtToEbitda: Double,
    val interestCoverage: Double?,
    val roe: Double, // Return on Equity %
    val roa: Double, // Return on Assets %
    val roic: Double, // Return on Invested Capital %
    val sharesOutstanding: Double,
    val growthPace: GrowthPace = GrowthPace.ACCELERATING,
    val balanceSheetHealth: BalanceSheetHealth = BalanceSheetHealth.IMPROVING,
    val qualityGrade: BusinessQualityGrade = BusinessQualityGrade.STRONG,
    val valuationGrade: ValuationGrade = ValuationGrade.FAIRLY_VALUED
)

data class ScoreBreakdown(
    val financialHealth: Double,
    val businessQuality: Double,
    val growth: Double,
    val valuation: Double,
    val institutionalCapital: Double,
    val earningsExpectations: Double,
    val marketStrength: Double,
    val catalysts: Double,
    val risk: Double,
    val positiveFactors: List<String>,
    val negativeFactors: List<String>,
    val keyRisks: List<String>,
    val bullCase: String,
    val bearCase: String,
    val longTermThesis: String,
    val invalidationCondition: String,
    val whyRankedExplanation: List<WhyComponent>
)

data class WhyComponent(
    val name: String,
    val scoreContribution: Double,
    val rawMetricDescription: String,
    val weightPercent: Double
)
