package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class MarketEventType(val displayName: String, val category: String) {
    GOVERNMENT_INVESTMENT("Government Investment", "GOVERNMENT"),
    PRESIDENTIAL_ANNOUNCEMENT("Presidential Announcement", "GOVERNMENT"),
    GOVERNMENT_CONTRACT("Government Contract", "GOVERNMENT"),
    GOVERNMENT_SUBSIDY("Government Subsidy", "GOVERNMENT"),
    TARIFF_TRADE_ACTION("Tariffs & Trade Restrictions", "GOVERNMENT"),
    EXPORT_CONTROL_SANCTION("Sanctions & Export Controls", "GOVERNMENT"),
    REGULATORY_CHANGE("Regulatory Action", "REGULATION"),
    ANTITRUST_ACTION("Antitrust Action", "REGULATION"),
    LEGISLATION_POLICY("Legislation & Policy", "GOVERNMENT"),
    DEFENSE_CONTRACT("Defense Contract", "GOVERNMENT"),
    SEMICONDUCTOR_AI_POLICY("AI & Semiconductor Policy", "POLICY"),
    HEALTHCARE_POLICY("Healthcare & Biotech Policy", "POLICY"),
    ENERGY_POLICY("Energy Policy", "POLICY"),
    CORPORATE_INVESTMENT("Major Corporate Investment", "CORPORATE"),
    STRATEGIC_PARTNERSHIP("Strategic Partnership", "CORPORATE"),
    MERGER_ACQUISITION("Merger & Acquisition", "M&A"),
    LARGE_CUSTOMER_CONTRACT("Major Customer Contract", "CORPORATE"),
    SUPPLY_CHAIN_AGREEMENT("Major Supply Agreement", "CORPORATE"),
    BANKRUPTCY_RESTRUCTURING("Bankruptcy / Restructuring", "CORPORATE"),
    MAJOR_LITIGATION("Major Lawsuit", "LEGAL"),
    PRODUCT_APPROVAL_FDA("FDA Decision / Product Approval", "PRODUCT"),
    PRODUCT_RECALL("Major Product Recall", "PRODUCT"),
    GEOPOLITICAL_EVENT("Geopolitical Event", "GEOPOLITICAL"),
    SUPPLY_CHAIN_DISRUPTION("Supply Chain Disruption", "OPERATIONS"),
    FACILITY_OPENING_CLOSURE("Factory Opening / Closure", "OPERATIONS"),
    MAJOR_LAYOFF("Major Restructuring / Layoffs", "OPERATIONS"),
    LEADERSHIP_CHANGE("C-Suite Leadership Change", "CORPORATE"),
    INSTITUTIONAL_DISCLOSURE("Major Institutional 13F Shift", "INSTITUTIONAL"),
    INSIDER_TRANSACTION("High-Conviction Insider Filing", "INSIDER"),
    CREDIT_RATING_CHANGE("Credit Rating Revision", "FINANCIAL"),
    ANALYST_REVISION("Major Analyst Revision", "ANALYST"),
    MACRO_CENTRAL_BANK("Central Bank / Rate Decision", "MACRO"),
    INFLATION_EMPLOYMENT("Macro Inflation / Jobs Surprise", "MACRO"),
    COMMODITY_SHOCK("Commodity Price Shock", "MACRO"),
    OTHER_MATERIAL_EVENT("Material Market Event", "OTHER")
}

enum class EventVerificationStatus(val label: String, val weight: Double) {
    UNVERIFIED("Unverified", 0.0),
    LOW_CONFIDENCE("Low Confidence", 0.35),
    PARTIALLY_VERIFIED("Partially Verified", 0.65),
    VERIFIED("Verified", 0.85),
    OFFICIAL("Official Government / SEC", 1.0)
}

enum class EventImpactDirection(val label: String, val sign: Int) {
    POSITIVE("Potentially Positive", 1),
    NEGATIVE("Potentially Negative", -1),
    NEUTRAL("Neutral / Mixed", 0),
    UNKNOWN("Impact Uncertain", 0)
}

enum class EventPriority(val label: String) {
    CRITICAL("CRITICAL"),
    HIGH("HIGH"),
    MEDIUM("MEDIUM"),
    LOW("LOW")
}

enum class EventAnalyticalHorizon(val label: String) {
    INTRADAY("Intraday"),
    SHORT_TERM("Short Term (1-4W)"),
    MEDIUM_TERM("Medium Term (1-6M)"),
    LONG_TERM("Long Term (6M+)")
}

enum class RelationshipType {
    DIRECT,
    SUPPLIER,
    CUSTOMER,
    COMPETITOR,
    SECTOR_PEER,
    MACRO_EXPOSURE
}

data class AffectedSecurity(
    val ticker: String,
    val companyName: String,
    val relationship: RelationshipType,
    val impactDirection: EventImpactDirection,
    val estimatedScoreDelta: Double, // e.g. +4.0, -6.0
    val scoreContributionExplanation: String,
    val affectedScoreComponents: List<String> = listOf("growth", "catalysts")
)

@Entity(
    tableName = "market_events",
    indices = [
        Index(value = ["primaryTicker"]),
        Index(value = ["eventType"]),
        Index(value = ["publishedTimestamp"]),
        Index(value = ["verificationStatus"]),
        Index(value = ["priority"])
    ]
)
data class MarketEventEntity(
    @PrimaryKey val eventId: String,
    val eventType: String,
    val eventSubtype: String = "",
    val headline: String,
    val summary: String,
    val primaryTicker: String,
    val affectedTickers: String, // Comma-separated: "NVDA,AMD,TSM"
    val sourceName: String,
    val sourceUrl: String = "",
    val sourceCount: Int = 1,
    val sourceHierarchyLevel: Int = 1, // 1: Official/Gov/SEC, 2: IR/Agency, 3: Tier-1 News, 4: Other
    val verificationStatus: String = EventVerificationStatus.VERIFIED.name,
    val confidence: Int = 85, // 0-100% confidence event occurred
    val impactCertainty: Int = 75, // 0-100% confidence in financial magnitude
    val impactDirection: String = EventImpactDirection.POSITIVE.name,
    val impactMagnitude: String = "HIGH", // HIGH, MEDIUM, LOW
    val priority: String = EventPriority.HIGH.name,
    val timeHorizon: String = EventAnalyticalHorizon.MEDIUM_TERM.name,
    val dollarAmount: String? = null,
    val governmentEntity: String? = null,
    val personEntities: String? = null,
    val sector: String = "Technology",
    val industry: String = "Semiconductors",
    val affectedComponents: String = "growth,catalysts,earnings", // CSV
    val aiReasoning: String = "",
    val evidenceSnippet: String = "",
    val publishedTimestamp: Long,
    val detectedTimestamp: Long = System.currentTimeMillis(),
    val lastVerifiedTimestamp: Long = System.currentTimeMillis(),
    val baseScoreAdjustment: Double = 0.0 // Raw calculated adjustment before decay
)

data class StockEventScoreBreakdown(
    val symbol: String,
    val baseFundamentalScore: Double,
    val eventImpactAdjustment: Double,
    val currentIntelligenceScore: Double,
    val activeEvents: List<MarketEventEntity>,
    val explanationItems: List<EventScoreExplanationItem>
)

data class EventScoreExplanationItem(
    val eventId: String,
    val headline: String,
    val delta: Double,
    val direction: EventImpactDirection,
    val reason: String,
    val verificationStatus: EventVerificationStatus,
    val publishedAgo: String,
    val source: String
)
