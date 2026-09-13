package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MacroRegime {
    SUPPORTIVE,
    NEUTRAL,
    CAUTIOUS,
    HOSTILE
}

enum class MarketRegime {
    RISK_ON,
    NEUTRAL,
    RISK_OFF
}

data class MacroData(
    val fedFundsRate: Double,
    val yield10Y: Double,
    val yield2Y: Double,
    val cpiInflation: Double,
    val unemploymentRate: Double,
    val dxyDollarIndex: Double,
    val gdpGrowth: Double,
    val creditSpreadBaa: Double,
    val macroEnvironment: MacroRegime,
    val marketRegime: MarketRegime,
    val lastUpdated: String,
    val source: String = "FRED (Federal Reserve Bank of St. Louis)"
)

data class TechnicalIndicators(
    val ma20: Double,
    val ma50: Double,
    val ma100: Double,
    val ma200: Double,
    val rsi14: Double,
    val relativeVolume: Double,
    val position52wPercent: Double, // 0 to 100%
    val relativeStrengthVsSp500: Double,
    val trendClassification: String // Bullish, Neutral, Bearish
)

@Entity(tableName = "watchlist")
data class WatchlistEntity(
    @PrimaryKey val symbol: String,
    val addedAt: Long = System.currentTimeMillis(),
    val notes: String = "",
    val alertPriceUpper: Double? = null,
    val alertPriceLower: Double? = null
)

@Entity(tableName = "portfolio")
data class PortfolioEntity(
    @PrimaryKey val symbol: String,
    val shares: Double,
    val averageBuyPrice: Double,
    val targetPrice: Double? = null,
    val notes: String = "",
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "alerts")
data class AlertEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val symbol: String,
    val alertType: String, // Institutional, Insider, Earnings, Downgrade, ScoreShift, Deterioration
    val title: String,
    val reason: String,
    val importance: String, // HIGH, MEDIUM, LOW
    val source: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

@Entity(tableName = "score_history")
data class ScoreHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val symbol: String,
    val score: Double,
    val previousScore: Double?,
    val changeExplanation: String,
    val date: String,
    val timestamp: Long = System.currentTimeMillis()
)
