package com.example.engine.events

import com.example.data.model.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

object MarketEventEngine {

    // Configurable ceiling caps so event adjustments never overwhelm fundamental base scores
    const val MAX_POSITIVE_EVENT_ADJUSTMENT = 8.0
    const val MAX_NEGATIVE_EVENT_ADJUSTMENT = -10.0

    // Half-life in days for event decay
    private const val EVENT_DECAY_HALF_LIFE_DAYS = 14.0

    /**
     * Calculates the exponential time decay factor for an event.
     * Fresh event (<1 day): 1.0 (100% impact)
     * 7 days: ~70% impact
     * 14 days: 50% impact
     * 30+ days: ~20% impact
     */
    fun calculateDecayFactor(publishedTimestamp: Long, currentTimestamp: Long = System.currentTimeMillis()): Double {
        val ageMillis = max(0L, currentTimestamp - publishedTimestamp)
        val ageDays = ageMillis.toDouble() / (1000.0 * 60.0 * 60.0 * 24.0)
        // Exponential decay: e^(-lambda * t) where lambda = ln(2) / half_life
        val lambda = 0.693147 / EVENT_DECAY_HALF_LIFE_DAYS
        return exp(-lambda * ageDays).coerceIn(0.05, 1.0)
    }

    /**
     * Multiplies the raw impact by verification confidence (0.0 - 1.0) and time decay.
     */
    fun calculateEffectiveEventDelta(
        event: MarketEventEntity,
        targetSymbol: String,
        currentTimestamp: Long = System.currentTimeMillis()
    ): Double {
        val isDirect = event.primaryTicker.equals(targetSymbol, ignoreCase = true)
        val relationshipMultiplier = if (isDirect) 1.0 else 0.60 // Second-order affected companies receive 60% of base impact

        val status = try {
            EventVerificationStatus.valueOf(event.verificationStatus)
        } catch (_: Exception) {
            EventVerificationStatus.VERIFIED
        }

        // Unverified rumors have 0 impact until verified
        if (status == EventVerificationStatus.UNVERIFIED) return 0.0

        val verificationWeight = status.weight
        val decay = calculateDecayFactor(event.publishedTimestamp, currentTimestamp)
        val rawBase = event.baseScoreAdjustment

        // Return signed effective delta
        return rawBase * verificationWeight * decay * relationshipMultiplier
    }

    /**
     * Computes the transparent score breakdown:
     * BASE SCORE + EVENT IMPACT ADJUSTMENT = CURRENT INTELLIGENCE SCORE
     */
    fun calculateStockScoreBreakdown(
        symbol: String,
        baseFundamentalScore: Double,
        events: List<MarketEventEntity>,
        currentTimestamp: Long = System.currentTimeMillis()
    ): StockEventScoreBreakdown {
        val explanationItems = mutableListOf<EventScoreExplanationItem>()
        var cumulativeAdjustment = 0.0

        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.US)

        events.forEach { event ->
            val delta = calculateEffectiveEventDelta(event, symbol, currentTimestamp)
            if (kotlin.math.abs(delta) >= 0.1) {
                cumulativeAdjustment += delta

                val dir = if (delta > 0) EventImpactDirection.POSITIVE else if (delta < 0) EventImpactDirection.NEGATIVE else EventImpactDirection.NEUTRAL
                val status = try { EventVerificationStatus.valueOf(event.verificationStatus) } catch (_: Exception) { EventVerificationStatus.VERIFIED }

                val ageHours = (currentTimestamp - event.publishedTimestamp) / (1000 * 60 * 60)
                val ageStr = if (ageHours < 24) "${ageHours}h ago" else sdf.format(Date(event.publishedTimestamp))

                explanationItems.add(
                    EventScoreExplanationItem(
                        eventId = event.eventId,
                        headline = event.headline,
                        delta = delta,
                        direction = dir,
                        reason = event.aiReasoning.ifBlank { event.summary },
                        verificationStatus = status,
                        publishedAgo = ageStr,
                        source = event.sourceName
                    )
                )
            }
        }

        // Cap event impact so it cannot overwhelm base fundamentals
        val boundedAdjustment = cumulativeAdjustment.coerceIn(MAX_NEGATIVE_EVENT_ADJUSTMENT, MAX_POSITIVE_EVENT_ADJUSTMENT)
        val currentScore = (baseFundamentalScore + boundedAdjustment).coerceIn(5.0, 99.0)

        return StockEventScoreBreakdown(
            symbol = symbol,
            baseFundamentalScore = baseFundamentalScore,
            eventImpactAdjustment = boundedAdjustment,
            currentIntelligenceScore = currentScore,
            activeEvents = events,
            explanationItems = explanationItems
        )
    }

    /**
     * Map relationships & second-order propagation
     */
    fun getSecondOrderAffectedSecurities(
        primaryTicker: String,
        eventType: MarketEventType
    ): List<AffectedSecurity> {
        return when (primaryTicker.uppercase()) {
            "NVDA" -> listOf(
                AffectedSecurity("NVDA", "NVIDIA Corporation", RelationshipType.DIRECT, EventImpactDirection.POSITIVE, 4.0, "Direct beneficiary of hyperscale AI accelerator demand", listOf("growth", "catalysts", "earnings")),
                AffectedSecurity("TSM", "Taiwan Semiconductor", RelationshipType.SUPPLIER, EventImpactDirection.POSITIVE, 2.5, "Exclusive advanced foundry fabrication partner (CoWoS packaging)", listOf("growth", "catalysts")),
                AffectedSecurity("AMD", "Advanced Micro Devices", RelationshipType.COMPETITOR, EventImpactDirection.POSITIVE, 1.8, "Expanding industry total addressable market for data center accelerators", listOf("market", "growth")),
                AffectedSecurity("AVGO", "Broadcom Inc.", RelationshipType.SECTOR_PEER, EventImpactDirection.POSITIVE, 2.0, "Custom ASIC compute and high-speed PCIe/Ethernet optical fabric demand", listOf("growth", "earnings")),
                AffectedSecurity("ASML", "ASML Holding", RelationshipType.SUPPLIER, EventImpactDirection.POSITIVE, 1.5, "Extreme Ultraviolet (EUV) lithography equipment supplier to foundries", listOf("growth", "quality"))
            )
            "AAPL" -> listOf(
                AffectedSecurity("AAPL", "Apple Inc.", RelationshipType.DIRECT, EventImpactDirection.POSITIVE, 3.0, "Consumer hardware installed base and high-margin services ecosystem", listOf("catalysts", "earnings")),
                AffectedSecurity("TSM", "Taiwan Semiconductor", RelationshipType.SUPPLIER, EventImpactDirection.POSITIVE, 1.5, "Sole supplier of A-series and M-series Silicon wafer production", listOf("growth")),
                AffectedSecurity("QCOM", "Qualcomm Inc.", RelationshipType.SUPPLIER, EventImpactDirection.POSITIVE, 1.0, "5G modem supplier and on-device neural processing competitor", listOf("catalysts")),
                AffectedSecurity("GOOGL", "Alphabet Inc.", RelationshipType.COMPETITOR, EventImpactDirection.NEUTRAL, 0.0, "Default search engine distribution agreement balance", listOf("risk"))
            )
            "MSFT" -> listOf(
                AffectedSecurity("MSFT", "Microsoft Corporation", RelationshipType.DIRECT, EventImpactDirection.POSITIVE, 3.5, "Enterprise cloud Azure AI infrastructure scaling and software copilot subscriptions", listOf("growth", "catalysts")),
                AffectedSecurity("NVDA", "NVIDIA Corporation", RelationshipType.SUPPLIER, EventImpactDirection.POSITIVE, 2.0, "Primary supplier of GPUs for Azure supercomputing clusters", listOf("growth")),
                AffectedSecurity("AMZN", "Amazon.com Inc.", RelationshipType.COMPETITOR, EventImpactDirection.POSITIVE, 1.0, "Hyperscale cloud market validation across enterprise clients", listOf("market"))
            )
            "TSLA" -> listOf(
                AffectedSecurity("TSLA", "Tesla, Inc.", RelationshipType.DIRECT, EventImpactDirection.POSITIVE, 3.5, "Autonomous FSD neural network deployments and next-gen vehicle platform", listOf("catalysts", "growth")),
                AffectedSecurity("NVDA", "NVIDIA Corporation", RelationshipType.SUPPLIER, EventImpactDirection.POSITIVE, 1.5, "Compute supplier for AI training clusters", listOf("growth")),
                AffectedSecurity("F", "Ford Motor Company", RelationshipType.COMPETITOR, EventImpactDirection.NEUTRAL, -0.5, "EV price elasticity pressure in passenger vehicle segment", listOf("risk"))
            )
            else -> listOf(
                AffectedSecurity(primaryTicker, primaryTicker, RelationshipType.DIRECT, EventImpactDirection.POSITIVE, 3.0, "Material verified market catalyst for core operational outlook", listOf("growth", "catalysts"))
            )
        }
    }
}
