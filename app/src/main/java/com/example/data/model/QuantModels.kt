package com.example.data.model

data class QuantScore(
    val symbol: String,
    val score: Double, // 0 - 100
    val rawScore: Double,
    val horizon: AnalysisHorizon = AnalysisHorizon.ONE_YEAR,
    val regime: QuantRegime,
    val relationship: ScoreRelationship,
    val whyExplanation: QuantExplanation,
    val contributions: List<QuantContribution>,
    val subscores: QuantSubscores,
    val statisticalBehavior: StatisticalPriceBehavior,
    val momentumMetrics: MomentumMathematics,
    val zScores: CrossSectionalZScores,
    val valuationMetrics: QuantValuationMetrics,
    val residualIncome: ResidualIncomeModel,
    val riskMetrics: RiskAdjustedMetrics,
    val factorExposure: FamaFrenchFactorExposure,
    val earningsQuality: QuantEarningsQuality,
    val institutionalFlow: QuantInstitutionalFlow,
    val historicalScores: List<HistoricalQuantScore>,
    val metadata: QuantCalculationMetadata = QuantCalculationMetadata()
)

data class ScoreRelationship(
    val investmentScore: Double,
    val quantScore: Double,
    val differential: Double,
    val classification: RelationshipClassification,
    val narrative: String
)

enum class RelationshipClassification(val label: String) {
    STRONG_CONVERGENCE("Strong Alignment"),
    FUNDAMENTAL_AHEAD_OF_QUANT("Fundamentals Ahead of Market Quant"),
    QUANT_AHEAD_OF_FUNDAMENTALS("Quant Momentum Ahead of Valuation")
}

data class QuantRegime(
    val hurst: Double,
    val hurstClassification: String,
    val hurstInterpretation: String,
    val trend: String,
    val momentum: String,
    val volatility: String,
    val relativeStrength: String,
    val drawdown: String,
    val statisticalPersistence: String,
    val liquidity: String,
    val summary: String
)

data class QuantContribution(
    val name: String,
    val subscore: Double,
    val weight: Double,
    val points: Double
)

data class QuantSubscores(
    val momentum: Double,
    val riskAdjusted: Double,
    val volatility: Double,
    val relativeStrength: Double,
    val regime: Double,
    val valuation: Double,
    val earningsQuality: Double,
    val institutionalFlow: Double
)

data class QuantExplanation(
    val summary: String,
    val positiveDrivers: List<DriverDetail>,
    val negativeDrivers: List<DriverDetail>
)

data class DriverDetail(
    val metric: String,
    val detail: String
)

data class StatisticalPriceBehavior(
    val meanDailyReturn: Double,
    val annualizedRealizedVol: Double,
    val rollingVol20D: Double,
    val volPercentile: Double,
    val skewness: Double,
    val kurtosis: Double,
    val downsideDeviation: Double,
    val priceZScore: Double,
    val maxDrawdown: Double,
    val recoveryDays: Int
)

data class MomentumMathematics(
    val roc5D: Double,
    val roc20D: Double,
    val roc63D: Double,
    val roc126D: Double,
    val roc252D: Double,
    val volAdjustedMomentum: Double,
    val momentumConsistencyPercent: Int,
    val relativeMomentumBenchmark: Double,
    val momentumZScore: Double
)

data class ZScoreItem(
    val metric: String,
    val stockValue: Double,
    val unit: String,
    val peerMean: Double,
    val peerMedian: Double,
    val peerStdDev: Double,
    val zScore: Double,
    val percentile: Double
)

data class CrossSectionalZScores(
    val items: List<ZScoreItem>,
    val compositeValuationZ: Double,
    val compositeQualityZ: Double,
    val compositeGrowthZ: Double,
    val peerUniverse: String,
    val timestamp: String
)

data class DcfScenario(
    val name: String,
    val fairValue: Double,
    val waccPercent: Double,
    val terminalGrowthPercent: Double,
    val revGrowthPercent: Double,
    val operatingMarginPercent: Double
)

data class QuantValuationMetrics(
    val currentPrice: Double,
    val primaryModel: String,
    val primaryFairValue: Double,
    val intrinsicDiscountPercent: Double,
    val waccPercent: Double,
    val costOfEquityPercent: Double,
    val bearScenario: DcfScenario,
    val baseScenario: DcfScenario,
    val bullScenario: DcfScenario,
    val sensitivityTerminalGrowths: List<Double>,
    val sensitivityMatrix: List<SensitivityRow>
)

data class SensitivityRow(
    val waccPercent: Double,
    val fairValues: List<Double?>
)

data class ResidualIncomeModel(
    val isApplicable: Boolean,
    val applicabilityReason: String,
    val intrinsicPerShare: Double,
    val bookValuePerShare: Double,
    val costOfEquityPercent: Double,
    val roePercent: Double,
    val pvResidualIncome5Y: Double
)

data class RiskAdjustedMetrics(
    val sharpeRatio: Double,
    val sortinoRatio: Double,
    val informationRatio: Double,
    val calmarRatio: Double,
    val beta: Double,
    val maxDrawdown: Double,
    val recoveryDays: Int,
    val historicalVaR95: Double,
    val historicalVaR99: Double,
    val cvar95: Double,
    val cvar99: Double,
    val annualizedReturn: Double,
    val annualizedVol: Double,
    val benchmark: String = "S&P 500 Index (SPY)",
    val riskFreeRatePercent: Double = 4.28
)

data class FactorDetail(
    val factor: String,
    val beta: Double,
    val description: String
)

data class FamaFrenchFactorExposure(
    val alphaPercent: Double,
    val marketBeta: Double,
    val smbBeta: Double,
    val hmlBeta: Double,
    val rmwBeta: Double,
    val cmaBeta: Double,
    val residualVolPercent: Double,
    val rSquared: Double,
    val observationPeriod: String,
    val factorDetails: List<FactorDetail>,
    val disclaimer: String = "Factor exposures quantify historical regression characteristics and do not forecast future return trajectories."
)

data class PiotroskiTest(
    val id: String,
    val category: String,
    val name: String,
    val condition: String,
    val value: String,
    val passed: Boolean,
    val reason: String
)

data class QuantEarningsQuality(
    val piotroskiScore: Int, // 0 - 9
    val piotroskiRating: String,
    val piotroskiSummary: String,
    val piotroskiTests: List<PiotroskiTest>,
    val beneishMScore: Double,
    val beneishThreshold: Double = -1.78,
    val isElevatedBeneishRisk: Boolean,
    val beneishRating: String,
    val beneishInterpretation: String,
    val beneishVariables: List<BeneishVariable>
)

data class BeneishVariable(
    val code: String,
    val name: String,
    val value: Double,
    val benchmark: String,
    val description: String
)

data class TopInstitution(
    val name: String,
    val shares: Long,
    val changeShares: Long,
    val filingDate: String
)

data class QuantInstitutionalFlow(
    val weightedNetFlowShares: Long,
    val rawNetFlowShares: Long,
    val netFlowPercentOfShares: Double,
    val institutionalOwnershipPercent: Double,
    val institutionalConcentrationRatio: Double,
    val buyerToSellerRatio: Double,
    val topInstitutions: List<TopInstitution>,
    val insiderSentiment: String,
    val insiderNetValueUSD: Double,
    val reportingFilingDate: String
)

data class HistoricalQuantScore(
    val period: String,
    val score: Double,
    val regime: String,
    val date: String
)

data class QuantCalculationMetadata(
    val timestamp: String = "",
    val lookbackDays: Int = 252,
    val benchmark: String = "SPY",
    val riskFreeRate: Double = 4.28,
    val confidence: String = "HIGH (Full SEC & Market Depth)"
)
