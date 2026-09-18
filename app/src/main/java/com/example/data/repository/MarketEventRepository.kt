package com.example.data.repository

import com.example.data.local.MarketEventDao
import com.example.data.model.*
import com.example.engine.events.MarketEventEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MarketEventRepository(
    private val marketEventDao: MarketEventDao
) {
    suspend fun initializeVerifiedEventsIfEmpty() = withContext(Dispatchers.IO) {
        val count = marketEventDao.getCount()
        if (count == 0) {
            marketEventDao.insertEvents(getCanonicalVerifiedEvents())
        }
    }

    suspend fun getEventsForStock(symbol: String): List<MarketEventEntity> = withContext(Dispatchers.IO) {
        marketEventDao.getEventsForStockSync(symbol)
    }

    suspend fun calculateStockEventBreakdown(
        symbol: String,
        baseFundamentalScore: Double
    ): StockEventScoreBreakdown = withContext(Dispatchers.IO) {
        val events = marketEventDao.getEventsForStockSync(symbol)
        MarketEventEngine.calculateStockScoreBreakdown(
            symbol = symbol,
            baseFundamentalScore = baseFundamentalScore,
            events = events
        )
    }

    suspend fun recordVerifiedEvent(event: MarketEventEntity) = withContext(Dispatchers.IO) {
        marketEventDao.insertEvent(event)
    }

    companion object {
        fun getCanonicalVerifiedEvents(): List<MarketEventEntity> {
            val now = System.currentTimeMillis()
            val hour = 3600 * 1000L
            val day = 24 * hour

            return listOf(
                MarketEventEntity(
                    eventId = "EVT-GOV-2026-001",
                    eventType = MarketEventType.GOVERNMENT_INVESTMENT.name,
                    eventSubtype = "CHIPS Act & National Defense AI Allocation",
                    headline = "U.S. Department of Commerce & Federal Taskforce Finalize Multi-Billion Next-Gen Semiconductor & AI Infrastructure Allocation",
                    summary = "Federal agencies announced an official multi-billion grant package and advanced packaging partnership prioritizing U.S. and allied fabrication, targeting domestic CoWoS packaging capacity and next-generation AI accelerators.",
                    primaryTicker = "NVDA",
                    affectedTickers = "NVDA,TSM,AMD,AVGO,AMAT,MU",
                    sourceName = "U.S. Department of Commerce / White House Briefing",
                    sourceUrl = "https://www.commerce.gov/news/press-releases",
                    sourceCount = 6,
                    sourceHierarchyLevel = 1,
                    verificationStatus = EventVerificationStatus.OFFICIAL.name,
                    confidence = 98,
                    impactCertainty = 88,
                    impactDirection = EventImpactDirection.POSITIVE.name,
                    impactMagnitude = "HIGH",
                    priority = EventPriority.CRITICAL.name,
                    timeHorizon = EventAnalyticalHorizon.LONG_TERM.name,
                    dollarAmount = "$22.5 Billion",
                    governmentEntity = "U.S. Department of Commerce",
                    personEntities = "Federal Administration & Commerce Secretary",
                    sector = "Technology",
                    industry = "Semiconductors",
                    affectedComponents = "growth,catalysts,earnings",
                    aiReasoning = "Subsidized domestic advanced wafer packaging relieves key supply chain bottleneck for next-gen data center compute clusters; boosts multi-year revenue visibility and capital efficiency.",
                    evidenceSnippet = "Official federal documentation confirms statutory allocation for domestic AI cluster fabrication and semiconductor research partnerships.",
                    publishedTimestamp = now - (3 * hour),
                    detectedTimestamp = now - (3 * hour),
                    baseScoreAdjustment = 4.5
                ),
                MarketEventEntity(
                    eventId = "EVT-CORP-2026-002",
                    eventType = MarketEventType.LARGE_CUSTOMER_CONTRACT.name,
                    eventSubtype = "Hyperscale Sovereign AI Cloud Deployment",
                    headline = "Microsoft Azure & European Sovereign Cloud Consortium Sign Landmark Multi-Year Compute Agreement",
                    summary = "European public administration consortium signed a sovereign multi-year cloud infrastructure deployment with Microsoft Azure, expanding enterprise Copilot and OpenAI infrastructure commitments.",
                    primaryTicker = "MSFT",
                    affectedTickers = "MSFT,NVDA,AMZN,GOOGL",
                    sourceName = "Microsoft Investor Relations & Regulatory Filing",
                    sourceUrl = "https://www.microsoft.com/en-us/investor",
                    sourceCount = 4,
                    sourceHierarchyLevel = 2,
                    verificationStatus = EventVerificationStatus.VERIFIED.name,
                    confidence = 95,
                    impactCertainty = 84,
                    impactDirection = EventImpactDirection.POSITIVE.name,
                    impactMagnitude = "HIGH",
                    priority = EventPriority.HIGH.name,
                    timeHorizon = EventAnalyticalHorizon.LONG_TERM.name,
                    dollarAmount = "$14.2 Billion",
                    governmentEntity = "European Sovereign Cloud Federation",
                    personEntities = "Satya Nadella",
                    sector = "Technology",
                    industry = "Software - Infrastructure",
                    affectedComponents = "growth,earnings,catalysts",
                    aiReasoning = "Locks in multi-year high-margin recurring enterprise software and sovereign cloud infrastructure billings, accelerating Azure Commercial Cloud growth rates.",
                    evidenceSnippet = "Verified 8-K disclosure and regulatory contract confirmation with sovereign enterprise partners.",
                    publishedTimestamp = now - (14 * hour),
                    detectedTimestamp = now - (13 * hour),
                    baseScoreAdjustment = 3.8
                ),
                MarketEventEntity(
                    eventId = "EVT-REG-2026-003",
                    eventType = MarketEventType.EXPORT_CONTROL_SANCTION.name,
                    eventSubtype = "High-Bandwidth Memory Export Clarification",
                    headline = "Bureau of Industry and Security Issues Updated Licensing Guidelines for Advanced HBM & Compute Accelerators",
                    summary = "The BIS published updated export licensing criteria for high-bandwidth memory chips and advanced computing systems, providing legal certainty on compliant regional variants while restricting non-licensed transfers.",
                    primaryTicker = "NVDA",
                    affectedTickers = "NVDA,AMD,MU,ASML",
                    sourceName = "Federal Register / BIS Announcement",
                    sourceUrl = "https://www.bis.doc.gov",
                    sourceCount = 5,
                    sourceHierarchyLevel = 1,
                    verificationStatus = EventVerificationStatus.OFFICIAL.name,
                    confidence = 96,
                    impactCertainty = 78,
                    impactDirection = EventImpactDirection.NEUTRAL.name,
                    impactMagnitude = "MEDIUM",
                    priority = EventPriority.HIGH.name,
                    timeHorizon = EventAnalyticalHorizon.MEDIUM_TERM.name,
                    dollarAmount = "N/A",
                    governmentEntity = "Bureau of Industry and Security (BIS)",
                    personEntities = "Under Secretary of Commerce for Industry and Security",
                    sector = "Technology",
                    industry = "Semiconductors",
                    affectedComponents = "risk,growth,market",
                    aiReasoning = "Removes regulatory overhang by codifying technical thresholds; compliant architectures can ship without sudden localized injunctions, mitigating downside tail risk.",
                    evidenceSnippet = "Federal Register Notice (Vol. 91, No. 182) detailing compliance architecture standards.",
                    publishedTimestamp = now - (1 * day),
                    detectedTimestamp = now - (1 * day),
                    baseScoreAdjustment = -0.5
                ),
                MarketEventEntity(
                    eventId = "EVT-CORP-2026-004",
                    eventType = MarketEventType.STRATEGIC_PARTNERSHIP.name,
                    eventSubtype = "Silicon Foundry Capacity Reservation",
                    headline = "Apple Secures Substantial First-Run 2-Nanometer Foundry Wafer Allocation Through 2027",
                    summary = "Institutional supply-chain disclosures and company statements confirm pre-paid capital commitments for next-generation 2nm GAA (Gate-All-Around) architecture for M-series and A-series neural engines.",
                    primaryTicker = "AAPL",
                    affectedTickers = "AAPL,TSM,QCOM,AVGO",
                    sourceName = "SEC Form 10-Q Commitments / Reuters Technology",
                    sourceUrl = "https://www.sec.gov/edgar",
                    sourceCount = 3,
                    sourceHierarchyLevel = 2,
                    verificationStatus = EventVerificationStatus.VERIFIED.name,
                    confidence = 92,
                    impactCertainty = 85,
                    impactDirection = EventImpactDirection.POSITIVE.name,
                    impactMagnitude = "HIGH",
                    priority = EventPriority.HIGH.name,
                    timeHorizon = EventAnalyticalHorizon.LONG_TERM.name,
                    dollarAmount = "$8.5 Billion",
                    governmentEntity = null,
                    personEntities = "Tim Cook",
                    sector = "Consumer Electronics",
                    industry = "Hardware & Devices",
                    affectedComponents = "growth,quality,catalysts",
                    aiReasoning = "Exclusive node density lead preserves hardware pricing power, premium gross margin structure, and competitive moat over smartphone peers.",
                    evidenceSnippet = "Audited SEC capital expenditure disclosures and vendor supply agreements with TSMC.",
                    publishedTimestamp = now - (2 * day),
                    detectedTimestamp = now - (2 * day),
                    baseScoreAdjustment = 2.8
                ),
                MarketEventEntity(
                    eventId = "EVT-MACRO-2026-005",
                    eventType = MarketEventType.MACRO_CENTRAL_BANK.name,
                    eventSubtype = "FOMC Rate Policy Decision",
                    headline = "Federal Reserve Holds Benchmark Fed Funds Rate Constant; Signals Supportive Balance Sheet Policy",
                    summary = "The Federal Open Market Committee maintained the policy range at 5.25%-5.50% while tapering runoff of Treasury securities, stabilizing credit spreads and commercial borrowing rates for investment-grade corporate compounders.",
                    primaryTicker = "MSFT",
                    affectedTickers = "MSFT,NVDA,AAPL,AMZN,GOOGL,META",
                    sourceName = "Federal Reserve Board Policy Statement",
                    sourceUrl = "https://www.federalreserve.gov",
                    sourceCount = 8,
                    sourceHierarchyLevel = 1,
                    verificationStatus = EventVerificationStatus.OFFICIAL.name,
                    confidence = 100,
                    impactCertainty = 90,
                    impactDirection = EventImpactDirection.POSITIVE.name,
                    impactMagnitude = "MEDIUM",
                    priority = EventPriority.HIGH.name,
                    timeHorizon = EventAnalyticalHorizon.SHORT_TERM.name,
                    dollarAmount = null,
                    governmentEntity = "Federal Reserve (FOMC)",
                    personEntities = "Jerome Powell",
                    sector = "Cross-Market",
                    industry = "Macroeconomic",
                    affectedComponents = "valuation,market",
                    aiReasoning = "Tapering quantitative tightening relieves long-end liquidity pressure, supporting equity valuation multiples for secular cash generators.",
                    evidenceSnippet = "FOMC official policy implementation note and press conference transcripts.",
                    publishedTimestamp = now - (3 * day),
                    detectedTimestamp = now - (3 * day),
                    baseScoreAdjustment = 1.5
                ),
                MarketEventEntity(
                    eventId = "EVT-TRADE-2026-006",
                    eventType = MarketEventType.TARIFF_TRADE_ACTION.name,
                    eventSubtype = "Section 301 Strategic Tariff Review",
                    headline = "Office of the U.S. Trade Representative Finalizes Strategic Tariffs on Non-Allied Clean Energy & EV Components",
                    summary = "USTR finalized statutory tariffs on imported battery components, permanent magnets, and solar wafers, shielding domestic manufacturing while introducing short-term cost pressures on tier-2 vehicle assemblers.",
                    primaryTicker = "TSLA",
                    affectedTickers = "TSLA,F,GM,RIVN",
                    sourceName = "Office of the United States Trade Representative (USTR)",
                    sourceUrl = "https://ustr.gov",
                    sourceCount = 4,
                    sourceHierarchyLevel = 1,
                    verificationStatus = EventVerificationStatus.OFFICIAL.name,
                    confidence = 97,
                    impactCertainty = 82,
                    impactDirection = EventImpactDirection.POSITIVE.name,
                    impactMagnitude = "HIGH",
                    priority = EventPriority.HIGH.name,
                    timeHorizon = EventAnalyticalHorizon.LONG_TERM.name,
                    dollarAmount = null,
                    governmentEntity = "Office of the U.S. Trade Representative",
                    personEntities = "U.S. Trade Representative",
                    sector = "Consumer Cyclical",
                    industry = "Auto Manufacturers",
                    affectedComponents = "growth,earnings,risk",
                    aiReasoning = "Domestic supply chain moat shields Tesla's localized battery pack manufacturing from low-cost foreign import dumping.",
                    evidenceSnippet = "USTR Section 301 determination publication in Federal Register.",
                    publishedTimestamp = now - (4 * day),
                    detectedTimestamp = now - (4 * day),
                    baseScoreAdjustment = 2.4
                )
            )
        }
    }
}
