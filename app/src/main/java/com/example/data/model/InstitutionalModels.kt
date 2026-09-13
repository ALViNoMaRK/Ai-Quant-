package com.example.data.model

enum class PositionChangeType {
    NEW_POSITION,
    INCREASED,
    MAINTAINED,
    REDUCED,
    EXITED
}

data class InstitutionalHolding(
    val institutionName: String,
    val shares: Long,
    val previousShares: Long,
    val changeShares: Long,
    val changePercent: Double,
    val portfolioWeight: Double,
    val changeType: PositionChangeType,
    val reportingPeriod: String,
    val filingDate: String,
    val source: String = "SEC Form 13F"
)

enum class InsiderTradeType {
    BUY,
    SELL,
    OPTION_EXERCISE,
    GRANT,
    OTHER
}

data class InsiderTransaction(
    val insiderName: String,
    val title: String,
    val tradeType: InsiderTradeType,
    val shares: Long,
    val price: Double,
    val totalValue: Double,
    val filingDate: String,
    val transactionDate: String,
    val isClusterBuying: Boolean = false,
    val source: String = "SEC Form 4"
)

data class AnalystIntelligence(
    val consensusRating: String, // Strong Buy, Buy, Hold, Underperform, Sell
    val targetMean: Double,
    val targetHigh: Double,
    val targetLow: Double,
    val numberOfAnalysts: Int,
    val momentumScore: Double, // 0-100
    val upgradesCount90d: Int,
    val downgradesCount90d: Int,
    val recentActions: List<AnalystAction>
)

data class AnalystAction(
    val firm: String,
    val actionType: String, // Upgrade, Downgrade, Initiated, Reiteration
    val fromRating: String?,
    val toRating: String,
    val targetPrice: Double?,
    val date: String
)

data class EarningsData(
    val lastQuarter: String,
    val actualEps: Double,
    val expectedEps: Double,
    val epsSurprisePercent: Double,
    val actualRevenue: Double,
    val expectedRevenue: Double,
    val revenueSurprisePercent: Double,
    val nextEarningsDate: String,
    val guidanceStatus: String, // Raised, Reaffirmed, Lowered
    val beatCountLast4Q: Int
)

enum class EventImpact {
    VERY_POSITIVE,
    POSITIVE,
    NEUTRAL,
    NEGATIVE,
    VERY_NEGATIVE
}

data class NewsEvent(
    val headline: String,
    val source: String,
    val date: String,
    val impact: EventImpact,
    val summary: String,
    val relatedTicker: String
)

data class CatalystItem(
    val title: String,
    val expectedDate: String,
    val category: String, // Earnings, Product Launch, FDA, Regulatory, M&A, Investor Day
    val expectedImpact: EventImpact,
    val confidence: ConfidenceLevel,
    val source: String
)
