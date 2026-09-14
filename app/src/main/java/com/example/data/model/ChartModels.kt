package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

data class CandleData(
    val timestamp: Long,
    val dateStr: String = "",
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long
)

enum class ChartTimeframe(val label: String, val intervalParam: String) {
    M1("1m", "1m"),
    M5("5m", "5m"),
    M15("15m", "15m"),
    M30("30m", "30m"),
    H1("1H", "60m"),
    H4("4H", "1h"),
    D1("1D", "1d"),
    W1("1W", "1wk"),
    MO1("1M", "1mo")
}

enum class ChartPeriod(val label: String, val rangeParam: String) {
    D1("1D", "1d"),
    D5("5D", "5d"),
    M1("1M", "1mo"),
    M3("3M", "3mo"),
    M6("6M", "6mo"),
    YTD("YTD", "ytd"),
    Y1("1Y", "1y"),
    Y3("3Y", "3y"),
    Y5("5Y", "5y"),
    MAX("MAX", "max")
}

enum class ChartType(val label: String) {
    CANDLESTICK("Candlestick"),
    LINE("Line"),
    AREA("Area"),
    BAR("Bar")
}

enum class ChartMarkerType {
    EARNINGS_BEAT,
    EARNINGS_MISS,
    ANALYST_UPGRADE,
    ANALYST_DOWNGRADE,
    INSTITUTIONAL_13F,
    INSIDER_BUY,
    INSIDER_SELL,
    DIVIDEND
}

data class ChartEventMarker(
    val id: String,
    val timestamp: Long,
    val dateStr: String,
    val price: Double,
    val type: ChartMarkerType,
    val title: String,
    val subtitle: String,
    val details: String
)

enum class DrawingToolType(val label: String) {
    NONE("Cursor"),
    HORIZONTAL_LINE("Horizontal Level"),
    TREND_LINE("Trendline"),
    SUPPORT_ZONE("Support Zone"),
    RESISTANCE_ZONE("Resistance Zone")
}

data class ChartDrawing(
    val id: String,
    val symbol: String,
    val type: DrawingToolType,
    val price1: Double,
    val price2: Double = price1,
    val label: String = "",
    val color: Long = 0xFF38BDF8
)

@Entity(tableName = "custom_indicators")
data class CustomIndicatorEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val code: String,
    val version: String = "//@version=6",
    val isOverlay: Boolean = true,
    val author: String = "Quantitative Research Desk",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "chart_templates")
data class ChartTemplateEntity(
    @PrimaryKey val id: String,
    val name: String,
    val indicatorsJson: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "cached_candles",
    primaryKeys = ["symbol", "intervalParam", "timestamp"]
)
data class CachedCandleEntity(
    val symbol: String,
    val intervalParam: String,
    val timestamp: Long,
    val dateStr: String,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long,
    val source: String,
    val fetchedAt: Long
)

data class ChartPayload(
    val symbol: String,
    val interval: String,
    val range: String,
    val currency: String,
    val source: String,
    val freshness: DataFreshness,
    val lastUpdated: String,
    val candles: List<CandleData>,
    val statusMessage: String? = null,
    val isCached: Boolean = false
)
