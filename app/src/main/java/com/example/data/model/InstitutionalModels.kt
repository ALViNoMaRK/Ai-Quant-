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
    val targetMean: Double?,
    val targetHigh: Double?,
    val targetLow: Double?,
    val targetMedian: Double? = null,
    val numberOfAnalysts: Int,
    val asOfDate: String = "",
    val provider: String = "Wall Street Consensus",
    val freshness: DataFreshness = DataFreshness.RECENT,
    val retrievedAt: String = "",
    val momentumScore: Double? = null, // 0-100 or null if insufficient recent records
    val upgradesCount90d: Int = 0,
    val downgradesCount90d: Int = 0,
    val recentActions: List<AnalystAction> = emptyList(), // <= 90 days
    val historicalActions: List<AnalystAction> = emptyList(), // > 90 days (classified STALE)
    val statusMessage: String? = null
)

data class AnalystAction(
    val firm: String,
    val actionType: String, // Upgrade, Downgrade, Initiated, Reiteration, Maintain, Target Increase
    val fromRating: String?,
    val toRating: String,
    val targetPrice: Double?,
    val date: String,
    val previousTargetPrice: Double? = null,
    val provider: String = "Financial Modeling Prep",
    val retrievedAt: String = "",
    val freshness: DataFreshness = DataFreshness.RECENT,
    val isHistorical: Boolean = false
)

data class ScoreHistoryEntry(
    val date: String,
    val score: Double,
    val eventReason: String
)

data class EarningsAwarenessInfo(
    val nextEarningsDate: String,
    val daysUntilEarnings: Int,
    val isApproaching: Boolean, // e.g. within 14 days
    val statusDescription: String,
    val lastReportQuarter: String,
    val lastEpsSurprisePercent: Double
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
    val relatedTicker: String,
    val publishedTime: String = "",
    val fetchedTime: String = "17 Sep 2026, 03:20",
    val url: String = "",
    val relevance: Double = 0.95,
    val category: String = "Corporate",
    val freshness: DataFreshness = DataFreshness.LIVE,
    val scoreImpactDescription: String = "",
    val investmentScoreDelta: Int = 0,
    val quantScoreDelta: Int = 0,
    val impactDetails: List<String> = emptyList()
)

data class CatalystItem(
    val title: String,
    val expectedDate: String,
    val category: String, // Earnings, Product Launch, FDA, Regulatory, M&A, Investor Day
    val expectedImpact: EventImpact,
    val confidence: ConfidenceLevel,
    val source: String
)
