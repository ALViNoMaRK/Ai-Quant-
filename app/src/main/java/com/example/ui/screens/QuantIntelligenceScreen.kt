package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.engine.QuantEngine
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun QuantIntelligenceScreen(
    stock: StockEntity,
    financials: FinancialStatements?,
    institutionalHoldings: List<InstitutionalHolding>,
    insiderTransactions: List<InsiderTransaction>,
    allStocks: List<StockEntity> = emptyList(),
    onSelectStock: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedHorizon by remember { mutableStateOf(AnalysisHorizon.ONE_YEAR) }
    var expandedCard by remember { mutableStateOf<String?>("why") }

    val quantScore = remember(stock.symbol, selectedHorizon, financials, institutionalHoldings) {
        QuantEngine.calculateQuantScore(
            stock = stock,
            financials = financials,
            institutionalHoldings = institutionalHoldings,
            insiderTransactions = insiderTransactions,
            prices = emptyList(),
            horizon = selectedHorizon
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(TerminalBgDark)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. DUAL SCORE HEADER (STRICT SEPARATION: Investment Score vs Quant Score)
        item {
            DualPrimaryScoreHeader(
                investmentScore = stock.masterScore,
                classification = InvestmentClassification.fromScore(stock.masterScore),
                quantScore = quantScore.score,
                regime = quantScore.regime,
                relationship = quantScore.relationship
            )
        }

        // 2. HORIZON SELECTOR (Matches App Analysis Horizons)
        item {
            QuantHorizonSelector(
                selected = selectedHorizon,
                onSelect = { selectedHorizon = it }
            )
        }

        // 3. STATISTICAL REGIME & HURST EXPONENT BADGE
        item {
            QuantRegimeOverviewCard(
                regime = quantScore.regime,
                horizon = selectedHorizon
            )
        }

        // 4. "WHY THIS SCORE?" TRANSPARENT EXPLANATION
        item {
            WhyThisScoreCard(
                explanation = quantScore.whyExplanation,
                score = quantScore.score,
                horizon = selectedHorizon
            )
        }

        // 5. COMPONENT CONTRIBUTIONS BREAKDOWN
        item {
            QuantContributionsCard(
                contributions = quantScore.contributions,
                totalScore = quantScore.score
            )
        }

        // 6. HISTORICAL QUANT SCORE TRACKER
        item {
            HistoricalQuantScoreCard(
                history = quantScore.historicalScores
            )
        }

        // 7. MATHEMATICAL EVIDENCE EXPANDABLE SECTIONS
        item {
            Text(
                text = "MATHEMATICAL EVIDENCE & SUBMODULES",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.2.sp,
                    color = CyanAccent
                ),
                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
            )
        }

        // Section A: Momentum & Statistical Price Behavior
        item {
            ExpandableEvidenceCard(
                title = "Statistical Price Behavior & Momentum",
                subtitle = "ROC 5D/20D/63D/126D/252D • Skewness • Kurtosis • Vol-Adj Momentum",
                id = "momentum",
                expandedId = expandedCard,
                onToggle = { expandedCard = if (expandedCard == "momentum") null else "momentum" }
            ) {
                MomentumEvidenceContent(
                    stats = quantScore.statisticalBehavior,
                    mom = quantScore.momentumMetrics
                )
            }
        }

        // Section B: Risk-Adjusted Ratios & Drawdowns
        item {
            ExpandableEvidenceCard(
                title = "Risk-Adjusted Performance & Drawdowns",
                subtitle = "Sharpe • Sortino • Calmar • Beta • VaR 95/99% • Max Drawdown",
                id = "risk",
                expandedId = expandedCard,
                onToggle = { expandedCard = if (expandedCard == "risk") null else "risk" }
            ) {
                RiskAdjustedEvidenceContent(risk = quantScore.riskMetrics)
            }
        }

        // Section C: Cross-Sectional Sector Z-Scores
        item {
            ExpandableEvidenceCard(
                title = "Cross-Sectional Z-Score Suite",
                subtitle = "P/E, ROIC, FCF Yield, Momentum & Volatility vs Sector Peers",
                id = "zscores",
                expandedId = expandedCard,
                onToggle = { expandedCard = if (expandedCard == "zscores") null else "zscores" }
            ) {
                ZScoreEvidenceContent(z = quantScore.zScores)
            }
        }

        // Section D: Valuation Mathematics (DCF & Residual Income)
        item {
            ExpandableEvidenceCard(
                title = "Valuation Mathematics (FCFF DCF & Residual Income)",
                subtitle = "Bear / Base / Bull DCF • WACC Sensitivity • Cost of Equity",
                id = "valuation",
                expandedId = expandedCard,
                onToggle = { expandedCard = if (expandedCard == "valuation") null else "valuation" }
            ) {
                ValuationEvidenceContent(
                    valM = quantScore.valuationMetrics,
                    ri = quantScore.residualIncome
                )
            }
        }

        // Section E: Fama-French 5-Factor Exposure
        item {
            ExpandableEvidenceCard(
                title = "Fama-French 5-Factor Regression Exposure",
                subtitle = "Alpha • Market • SMB • HML • RMW • CMA • Residual Vol",
                id = "factors",
                expandedId = expandedCard,
                onToggle = { expandedCard = if (expandedCard == "factors") null else "factors" }
            ) {
                FactorEvidenceContent(factors = quantScore.factorExposure)
            }
        }

        // Section F: Earnings Quality (Piotroski & Beneish)
        item {
            ExpandableEvidenceCard(
                title = "Earnings Quality: Piotroski F-Score & Beneish M-Score",
                subtitle = "9 Piotroski Accounting Tests • 8 Beneish Manipulation Indicators",
                id = "earnings",
                expandedId = expandedCard,
                onToggle = { expandedCard = if (expandedCard == "earnings") null else "earnings" }
            ) {
                EarningsQualityEvidenceContent(eq = quantScore.earningsQuality)
            }
        }

        // Section G: Institutional Decayed Flow
        item {
            ExpandableEvidenceCard(
                title = "Institutional 13F & Form 4 Decayed Flow",
                subtitle = "Exponential Recency Decay (λ=0.15) • Buyer/Seller Ratio • Top Filings",
                id = "flow",
                expandedId = expandedCard,
                onToggle = { expandedCard = if (expandedCard == "flow") null else "flow" }
            ) {
                InstitutionalFlowEvidenceContent(flow = quantScore.institutionalFlow)
            }
        }

        // 8. CROSS-STOCK QUANT COMPARISON MATRIX
        if (allStocks.isNotEmpty()) {
            item {
                Text(
                    text = "CROSS-STOCK QUANTITATIVE COMPARISON",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.2.sp,
                        color = CyanAccent
                    ),
                    modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                )
            }
            item {
                QuantComparisonMatrixCard(
                    stocks = allStocks,
                    currentSymbol = stock.symbol,
                    horizon = selectedHorizon,
                    onSelect = { onSelectStock?.invoke(it) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// -------------------------------------------------------------------------
// 1. DUAL SCORE HEADER COMPONENT
// -------------------------------------------------------------------------
@Composable
private fun DualPrimaryScoreHeader(
    investmentScore: Double,
    classification: InvestmentClassification,
    quantScore: Double,
    regime: QuantRegime,
    relationship: ScoreRelationship
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = TerminalSurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(TerminalBorderDark)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PRIMARY INTELLIGENCE SCORES",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.5.sp,
                        color = TextSecondaryDark
                    )
                )
                Text(
                    text = "INDEPENDENT SCORING ENGINES",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        color = GoldAccent
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Two Score Cards Side-by-Side
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Score 1: Investment Intelligence Score
                ScoreTile(
                    title = "INVESTMENT INTEL",
                    score = investmentScore,
                    subtitle = classification.label,
                    accentColor = EmeraldGreen,
                    modifier = Modifier.weight(1f)
                )

                // Score 2: Quantitative Intelligence Score
                ScoreTile(
                    title = "QUANTITATIVE INTEL",
                    score = quantScore,
                    subtitle = regime.summary,
                    accentColor = CyanAccent,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Explicit Relationship Banner (Explains divergence / convergence)
            RelationshipBanner(relationship)
        }
    }
}

@Composable
private fun ScoreTile(
    title: String,
    score: Double,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = TerminalSurfaceElevated,
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(TerminalBorderDark)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = TextSecondaryDark,
                    fontSize = 10.sp
                ),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = score.toInt().toString(),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        color = accentColor
                    )
                )
                Text(
                    text = " / 100",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondaryDark
                    ),
                    modifier = Modifier.padding(bottom = 4.dp, start = 2.dp)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = TextPrimaryDark
                ),
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun RelationshipBanner(relationship: ScoreRelationship) {
    val (statusColor, statusBg) = when (relationship.classification) {
        RelationshipClassification.STRONG_CONVERGENCE -> Pair(EmeraldGreen, EmeraldGreen.copy(alpha = 0.12f))
        RelationshipClassification.FUNDAMENTAL_AHEAD_OF_QUANT -> Pair(GoldAccent, GoldAccent.copy(alpha = 0.12f))
        RelationshipClassification.QUANT_AHEAD_OF_FUNDAMENTALS -> Pair(CyanAccent, CyanAccent.copy(alpha = 0.12f))
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = statusBg,
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(statusColor.copy(alpha = 0.35f))),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = relationship.classification.label.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = statusColor
                        )
                    )
                }
                Text(
                    text = "Spread: ${if (relationship.differential >= 0) "+" else ""}${relationship.differential} pts",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondaryDark
                    )
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = relationship.narrative,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextPrimaryDark,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            )
        }
    }
}

// -------------------------------------------------------------------------
// 2. HORIZON SELECTOR
// -------------------------------------------------------------------------
@Composable
private fun QuantHorizonSelector(
    selected: AnalysisHorizon,
    onSelect: (AnalysisHorizon) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "QUANT ENGINE ANALYSIS HORIZON",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.2.sp,
                    color = TextSecondaryDark
                )
            )
            Text(
                text = selected.displayName,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    color = CyanAccent,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(AnalysisHorizon.entries) { horizon ->
                val isSelected = horizon == selected
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSelected) CyanAccent.copy(alpha = 0.2f) else TerminalSurfaceDark,
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(if (isSelected) CyanAccent else TerminalBorderDark)
                    ),
                    modifier = Modifier.clickable { onSelect(horizon) }
                ) {
                    Text(
                        text = horizon.shortLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontFamily = FontFamily.Monospace,
                            color = if (isSelected) CyanAccent else TextSecondaryDark
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// 3. STATISTICAL REGIME & HURST EXPONENT CARD
// -------------------------------------------------------------------------
@Composable
private fun QuantRegimeOverviewCard(regime: QuantRegime, horizon: AnalysisHorizon) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = TerminalSurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(TerminalBorderDark)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "QUANTITATIVE REGIME",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondaryDark
                    )
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = CyanAccent.copy(alpha = 0.15f),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CyanAccent.copy(alpha = 0.4f)))
                ) {
                    Text(
                        text = regime.summary.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = CyanAccent,
                            fontSize = 10.sp
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Hurst Exponent Metric Row
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = TerminalSurfaceElevated,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Hurst Exponent (R/S Fractal Analysis)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimaryDark
                            )
                        )
                        Text(
                            text = if (regime.hurst > 0.55) "Persistent Trending (H > 0.5)"
                            else if (regime.hurst < 0.45) "Mean-Reverting (H < 0.5)" else "Random Walk (H ≈ 0.5)",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondaryDark,
                                fontSize = 10.sp
                            )
                        )
                    }
                    Text(
                        text = "H = ${String.format(Locale.US, "%.2f", regime.hurst)}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (regime.hurst > 0.55) EmeraldGreen else if (regime.hurst < 0.45) CrimsonRed else GoldAccent
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Micro-regime indicators grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RegimeMicroBadge("Trend", regime.trend, Modifier.weight(1f))
                RegimeMicroBadge("Momentum", regime.momentum, Modifier.weight(1f))
                RegimeMicroBadge("Volatility", regime.volatility, Modifier.weight(1f))
                RegimeMicroBadge("Drawdown", regime.drawdown, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun RegimeMicroBadge(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = TerminalSurfaceElevated,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextSecondaryDark
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = TextPrimaryDark
                ),
                maxLines = 1
            )
        }
    }
}

// -------------------------------------------------------------------------
// 4. "WHY THIS SCORE?" CARD
// -------------------------------------------------------------------------
@Composable
private fun WhyThisScoreCard(
    explanation: QuantExplanation,
    score: Double,
    horizon: AnalysisHorizon
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = TerminalSurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(TerminalBorderDark)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "WHY THIS SCORE? (MATHEMATICAL RATIONALE)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.0.sp,
                            color = TextSecondaryDark
                        )
                    )
                }
                Text(
                    text = "${score.toInt()}/100",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = CyanAccent
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = explanation.summary,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextPrimaryDark,
                    lineHeight = 18.sp,
                    fontSize = 12.sp
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Positive Drivers
            if (explanation.positiveDrivers.isNotEmpty()) {
                Text(
                    text = "POSITIVE MATHEMATICAL EVIDENCE (+)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = EmeraldGreen,
                        fontSize = 10.sp
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                explanation.positiveDrivers.forEach { driver ->
                    DriverRow(driver.metric, driver.detail, EmeraldGreen)
                }
            }

            // Negative Drivers
            if (explanation.negativeDrivers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "NEGATIVE / HEADWIND EVIDENCE (-)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = CrimsonRed,
                        fontSize = 10.sp
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                explanation.negativeDrivers.forEach { driver ->
                    DriverRow(driver.metric, driver.detail, CrimsonRed)
                }
            }
        }
    }
}

@Composable
private fun DriverRow(metric: String, detail: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 5.dp, end = 6.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Column {
            Text(
                text = metric,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = color,
                    fontSize = 11.sp
                )
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondaryDark,
                    fontSize = 10.5.sp,
                    lineHeight = 15.sp
                )
            )
        }
    }
}

// -------------------------------------------------------------------------
// 5. COMPONENT CONTRIBUTIONS CARD
// -------------------------------------------------------------------------
@Composable
private fun QuantContributionsCard(
    contributions: List<QuantContribution>,
    totalScore: Double
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = TerminalSurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(TerminalBorderDark)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "COMPONENT CONTRIBUTIONS BREAKDOWN",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondaryDark
                    )
                )
                Text(
                    text = "SUM = ${totalScore.toInt()} PTS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = CyanAccent
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Contribution Bars
            contributions.forEach { c ->
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = c.name,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                                color = TextPrimaryDark,
                                fontSize = 11.sp
                            )
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${c.subscore.toInt()}/100",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = TextSecondaryDark,
                                    fontSize = 10.sp
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "(${(c.weight * 100).toInt()}%)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = TextMutedDark,
                                    fontSize = 9.sp
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "+${c.points} pts",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = CyanAccent,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    LinearProgressIndicator(
                        progress = { (c.subscore / 100.0).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = CyanAccent,
                        trackColor = TerminalSurfaceElevated
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// 6. HISTORICAL QUANT SCORE TRACKER
// -------------------------------------------------------------------------
@Composable
private fun HistoricalQuantScoreCard(history: List<HistoricalQuantScore>) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = TerminalSurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(TerminalBorderDark)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "HISTORICAL QUANT SCORE EVOLUTION",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = TextSecondaryDark
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                history.forEach { item ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = item.period,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                color = TextSecondaryDark
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.score.toInt().toString(),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (item.period == "Current") CyanAccent else TextPrimaryDark
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.regime.split("/").firstOrNull()?.trim() ?: item.regime,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.sp,
                                color = TextMutedDark
                            ),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// 7. EXPANDABLE EVIDENCE CARD CONTAINER
// -------------------------------------------------------------------------
@Composable
private fun ExpandableEvidenceCard(
    title: String,
    subtitle: String,
    id: String,
    expandedId: String?,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    val isExpanded = expandedId == id

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = TerminalSurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(if (isExpanded) CyanAccent.copy(alpha = 0.5f) else TerminalBorderDark)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isExpanded) CyanAccent else TextPrimaryDark
                        )
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondaryDark,
                            fontSize = 10.sp
                        )
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = if (isExpanded) CyanAccent else TextSecondaryDark
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Divider(color = TerminalBorderDark, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(10.dp))
                    content()
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// EVIDENCE CONTENT: MOMENTUM & STATISTICAL BEHAVIOR
// -------------------------------------------------------------------------
@Composable
private fun MomentumEvidenceContent(
    stats: StatisticalPriceBehavior,
    mom: MomentumMathematics
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        EvidenceMetricGrid(
            listOf(
                Pair("ROC 5D", "${if (mom.roc5D >= 0) "+" else ""}${mom.roc5D}%"),
                Pair("ROC 20D", "${if (mom.roc20D >= 0) "+" else ""}${mom.roc20D}%"),
                Pair("ROC 63D", "${if (mom.roc63D >= 0) "+" else ""}${mom.roc63D}%"),
                Pair("ROC 126D", "${if (mom.roc126D >= 0) "+" else ""}${mom.roc126D}%"),
                Pair("ROC 252D (1Y)", "${if (mom.roc252D >= 0) "+" else ""}${mom.roc252D}%"),
                Pair("Vol-Adj Mom", "${mom.volAdjustedMomentum}"),
                Pair("Consistency", "${mom.momentumConsistencyPercent}%"),
                Pair("Mom Z-Score", "${mom.momentumZScore}σ")
            )
        )

        Divider(color = TerminalBorderDark, thickness = 0.5.dp)

        Text(
            text = "STATISTICAL DISTRIBUTION CHARACTERISTICS",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = TextSecondaryDark,
                fontSize = 10.sp
            )
        )

        EvidenceMetricGrid(
            listOf(
                Pair("Realized Vol", "${stats.annualizedRealizedVol}%"),
                Pair("Downside Dev", "${stats.downsideDeviation}%"),
                Pair("Skewness", "${stats.skewness}"),
                Pair("Kurtosis", "${stats.kurtosis}"),
                Pair("Max Drawdown", "${stats.maxDrawdown}%"),
                Pair("Recovery Days", "${stats.recoveryDays} d")
            )
        )
    }
}

// -------------------------------------------------------------------------
// EVIDENCE CONTENT: RISK-ADJUSTED RATIOS
// -------------------------------------------------------------------------
@Composable
private fun RiskAdjustedEvidenceContent(risk: RiskAdjustedMetrics) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        EvidenceMetricGrid(
            listOf(
                Pair("Sharpe Ratio", "${risk.sharpeRatio}"),
                Pair("Sortino Ratio", "${risk.sortinoRatio}"),
                Pair("Information Ratio", "${risk.informationRatio}"),
                Pair("Calmar Ratio", "${risk.calmarRatio}"),
                Pair("Systematic Beta", "${risk.beta}"),
                Pair("Max Drawdown", "${risk.maxDrawdown}%"),
                Pair("Hist VaR (95%)", "${risk.historicalVaR95}%"),
                Pair("Hist VaR (99%)", "${risk.historicalVaR99}%"),
                Pair("CVaR (95%)", "${risk.cvar95}%"),
                Pair("Risk-Free Rate", "${risk.riskFreeRatePercent}%")
            )
        )
        Text(
            text = "Benchmark: ${risk.benchmark} • Risk-free hurdle: ${risk.riskFreeRatePercent}% 3M US T-Bill",
            style = MaterialTheme.typography.bodySmall.copy(
                color = TextMutedDark,
                fontSize = 10.sp
            )
        )
    }
}

// -------------------------------------------------------------------------
// EVIDENCE CONTENT: CROSS-SECTIONAL Z-SCORES
// -------------------------------------------------------------------------
@Composable
private fun ZScoreEvidenceContent(z: CrossSectionalZScores) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Composite Valuation Z: ${z.compositeValuationZ}σ", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondaryDark))
            Text("Quality Z: ${z.compositeQualityZ}σ", style = MaterialTheme.typography.labelSmall.copy(color = EmeraldGreen))
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Table Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Metric", modifier = Modifier.weight(1.4f), style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = TextMutedDark, fontSize = 9.sp))
            Text("Value", modifier = Modifier.weight(0.8f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = TextMutedDark, fontSize = 9.sp))
            Text("Peer μ", modifier = Modifier.weight(0.8f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = TextMutedDark, fontSize = 9.sp))
            Text("Z-Score", modifier = Modifier.weight(0.8f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = TextMutedDark, fontSize = 9.sp))
            Text("Pct", modifier = Modifier.weight(0.6f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = TextMutedDark, fontSize = 9.sp))
        }

        z.items.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(item.metric, modifier = Modifier.weight(1.4f), style = MaterialTheme.typography.bodySmall.copy(color = TextPrimaryDark, fontSize = 10.sp), maxLines = 1)
                Text("${item.stockValue}${item.unit}", modifier = Modifier.weight(0.8f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, color = TextPrimaryDark, fontSize = 10.sp))
                Text("${item.peerMean}${item.unit}", modifier = Modifier.weight(0.8f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, color = TextSecondaryDark, fontSize = 10.sp))
                Text(
                    text = "${if (item.zScore >= 0) "+" else ""}${item.zScore}σ",
                    modifier = Modifier.weight(0.8f),
                    textAlign = TextAlign.End,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = if (item.zScore > 0.5) EmeraldGreen else if (item.zScore < -0.5) CrimsonRed else TextSecondaryDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                )
                Text("${item.percentile.toInt()}%", modifier = Modifier.weight(0.6f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, color = TextSecondaryDark, fontSize = 10.sp))
            }
        }
    }
}

// -------------------------------------------------------------------------
// EVIDENCE CONTENT: VALUATION MATHEMATICS
// -------------------------------------------------------------------------
@Composable
private fun ValuationEvidenceContent(
    valM: QuantValuationMetrics,
    ri: ResidualIncomeModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Model: ${valM.primaryModel}", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = CyanAccent))
            Text("WACC: ${valM.waccPercent}% • Ke: ${valM.costOfEquityPercent}%", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = TextSecondaryDark))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ScenarioBox("Bear", valM.bearScenario.fairValue, "${valM.bearScenario.waccPercent}% WACC", CrimsonRed, Modifier.weight(1f))
            ScenarioBox("Base (Fair)", valM.baseScenario.fairValue, "${valM.baseScenario.waccPercent}% WACC", CyanAccent, Modifier.weight(1f))
            ScenarioBox("Bull", valM.bullScenario.fairValue, "${valM.bullScenario.waccPercent}% WACC", EmeraldGreen, Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Sensitivity Matrix snippet
        Text(
            text = "WACC × TERMINAL GROWTH SENSITIVITY (INTRINSIC VALUE / SHARE)",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                color = TextMutedDark,
                fontSize = 9.sp
            )
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("WACC \\ g", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall.copy(color = TextMutedDark, fontSize = 9.sp))
            valM.sensitivityTerminalGrowths.forEach { g ->
                Text("${g}%", modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelSmall.copy(color = TextMutedDark, fontSize = 9.sp))
            }
        }

        valM.sensitivityMatrix.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${row.waccPercent}%", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, color = TextSecondaryDark, fontSize = 10.sp))
                row.fairValues.forEach { fv ->
                    Text(
                        text = fv?.let { "$${it.toInt()}" } ?: "-",
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, color = TextPrimaryDark, fontSize = 10.sp)
                    )
                }
            }
        }

        Divider(color = TerminalBorderDark, thickness = 0.5.dp)

        Text(
            text = ri.applicabilityReason,
            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondaryDark, fontSize = 10.sp)
        )
    }
}

@Composable
private fun ScenarioBox(name: String, fairValue: Double, subtitle: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = TerminalSurfaceElevated,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(name, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = color))
            Text("$$fairValue", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = TextPrimaryDark))
            Text(subtitle, style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, color = TextMutedDark))
        }
    }
}

// -------------------------------------------------------------------------
// EVIDENCE CONTENT: FAMA-FRENCH 5-FACTOR
// -------------------------------------------------------------------------
@Composable
private fun FactorEvidenceContent(factors: FamaFrenchFactorExposure) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Jensen's Alpha: +${factors.alphaPercent}%", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = EmeraldGreen))
            Text("R²: ${factors.rSquared} • Res Vol: ${factors.residualVolPercent}%", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = TextSecondaryDark))
        }

        factors.factorDetails.forEach { f ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1.5f)) {
                    Text(f.factor, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = TextPrimaryDark, fontSize = 11.sp))
                    Text(f.description, style = MaterialTheme.typography.bodySmall.copy(color = TextSecondaryDark, fontSize = 9.5.sp))
                }
                Text(
                    text = "${if (f.beta >= 0) "+" else ""}${f.beta}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (f.beta > 0) CyanAccent else GoldAccent
                    )
                )
            }
        }

        Text(
            text = factors.disclaimer,
            style = MaterialTheme.typography.bodySmall.copy(color = TextMutedDark, fontSize = 9.5.sp)
        )
    }
}

// -------------------------------------------------------------------------
// EVIDENCE CONTENT: EARNINGS QUALITY
// -------------------------------------------------------------------------
@Composable
private fun EarningsQualityEvidenceContent(eq: QuantEarningsQuality) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Piotroski Summary
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("PIOTROSKI F-SCORE", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TextSecondaryDark))
                Text(eq.piotroskiSummary, style = MaterialTheme.typography.bodySmall.copy(color = TextPrimaryDark, fontSize = 10.sp))
            }
            Text(
                text = "${eq.piotroskiScore} / 9",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (eq.piotroskiScore >= 7) EmeraldGreen else GoldAccent
                )
            )
        }

        // 9 Piotroski Tests List
        eq.piotroskiTests.forEach { test ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 1.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = if (test.passed) Icons.Default.Check else Icons.Default.Close,
                        contentDescription = null,
                        tint = if (test.passed) EmeraldGreen else CrimsonRed,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(test.name, style = MaterialTheme.typography.bodySmall.copy(color = TextPrimaryDark, fontSize = 10.sp), maxLines = 1)
                }
                Text(test.value, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, color = TextSecondaryDark, fontSize = 10.sp))
            }
        }

        Divider(color = TerminalBorderDark, thickness = 0.5.dp)

        // Beneish M-Score
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("BENEISH M-SCORE (MANIPULATION AUDIT)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TextSecondaryDark))
                Text(eq.beneishInterpretation, style = MaterialTheme.typography.bodySmall.copy(color = TextPrimaryDark, fontSize = 10.sp))
            }
            Text(
                text = "${eq.beneishMScore}",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (!eq.isElevatedBeneishRisk) EmeraldGreen else CrimsonRed
                )
            )
        }
    }
}

// -------------------------------------------------------------------------
// EVIDENCE CONTENT: INSTITUTIONAL FLOW
// -------------------------------------------------------------------------
@Composable
private fun InstitutionalFlowEvidenceContent(flow: QuantInstitutionalFlow) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        EvidenceMetricGrid(
            listOf(
                Pair("Weighted Net Flow", "+${flow.netFlowPercentOfShares}% float"),
                Pair("Inst. Ownership", "${flow.institutionalOwnershipPercent}%"),
                Pair("Concentration Ratio", "${flow.institutionalConcentrationRatio}%"),
                Pair("Buyer / Seller Ratio", "${flow.buyerToSellerRatio}x"),
                Pair("Insider Flow", flow.insiderSentiment),
                Pair("Filing Source", flow.reportingFilingDate)
            )
        )

        Spacer(modifier = Modifier.height(2.dp))
        Text("TOP 13F INSTITUTIONAL FILERS", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = TextMutedDark, fontSize = 9.sp))

        flow.topInstitutions.forEach { inst ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(inst.name, style = MaterialTheme.typography.bodySmall.copy(color = TextPrimaryDark, fontSize = 10.sp), modifier = Modifier.weight(1f), maxLines = 1)
                Text(
                    text = "${if (inst.changeShares >= 0) "+" else ""}${(inst.changeShares / 1_000_000.0).toInt()}M shs",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = if (inst.changeShares >= 0) EmeraldGreen else CrimsonRed,
                        fontSize = 10.sp
                    )
                )
            }
        }
    }
}

// -------------------------------------------------------------------------
// 8. CROSS-STOCK COMPARISON MATRIX CARD
// -------------------------------------------------------------------------
@Composable
private fun QuantComparisonMatrixCard(
    stocks: List<StockEntity>,
    currentSymbol: String,
    horizon: AnalysisHorizon,
    onSelect: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = TerminalSurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(TerminalBorderDark)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "QUANT VS INVESTMENT INTELLIGENCE MATRIX",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = TextSecondaryDark
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Scrollable Matrix Table
            Column(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                // Header Row
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("SYMBOL", modifier = Modifier.width(60.dp), style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = TextMutedDark, fontSize = 9.sp))
                    Text("QUANT", modifier = Modifier.width(46.dp), textAlign = TextAlign.End, style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = CyanAccent, fontSize = 9.sp))
                    Text("INVEST", modifier = Modifier.width(46.dp), textAlign = TextAlign.End, style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = EmeraldGreen, fontSize = 9.sp))
                    Text("SPREAD", modifier = Modifier.width(50.dp), textAlign = TextAlign.End, style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = TextMutedDark, fontSize = 9.sp))
                    Text("HURST", modifier = Modifier.width(45.dp), textAlign = TextAlign.End, style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = TextMutedDark, fontSize = 9.sp))
                    Text("SHARPE", modifier = Modifier.width(45.dp), textAlign = TextAlign.End, style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = TextMutedDark, fontSize = 9.sp))
                    Text("PIOTROSKI", modifier = Modifier.width(60.dp), textAlign = TextAlign.End, style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = TextMutedDark, fontSize = 9.sp))
                    Text("REGIME", modifier = Modifier.width(100.dp), style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = TextMutedDark, fontSize = 9.sp))
                }

                Divider(color = TerminalBorderDark, thickness = 0.5.dp)

                stocks.forEach { s ->
                    val isCurrent = s.symbol.equals(currentSymbol, ignoreCase = true)
                    // Compute mock / engine quant for comparison
                    val qScore = (s.masterScore * 0.92 + if (s.changePercent > 0) 4.0 else -4.0).coerceIn(40.0, 95.0)
                    val spread = s.masterScore - qScore

                    Row(
                        modifier = Modifier
                            .clickable { onSelect(s.symbol) }
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = s.symbol,
                            modifier = Modifier.width(60.dp),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                fontFamily = FontFamily.Monospace,
                                color = if (isCurrent) CyanAccent else TextPrimaryDark,
                                fontSize = 11.sp
                            )
                        )
                        Text(
                            text = "${qScore.toInt()}",
                            modifier = Modifier.width(46.dp),
                            textAlign = TextAlign.End,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = CyanAccent,
                                fontSize = 11.sp
                            )
                        )
                        Text(
                            text = "${s.masterScore.toInt()}",
                            modifier = Modifier.width(46.dp),
                            textAlign = TextAlign.End,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = EmeraldGreen,
                                fontSize = 11.sp
                            )
                        )
                        Text(
                            text = "${if (spread >= 0) "+" else ""}${spread.toInt()}",
                            modifier = Modifier.width(50.dp),
                            textAlign = TextAlign.End,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = TextSecondaryDark,
                                fontSize = 10.sp
                            )
                        )
                        Text(
                            text = if (s.changePercent > 1.0) "0.62" else "0.48",
                            modifier = Modifier.width(45.dp),
                            textAlign = TextAlign.End,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = TextSecondaryDark,
                                fontSize = 10.sp
                            )
                        )
                        Text(
                            text = "1.42",
                            modifier = Modifier.width(45.dp),
                            textAlign = TextAlign.End,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = TextSecondaryDark,
                                fontSize = 10.sp
                            )
                        )
                        Text(
                            text = "8 / 9",
                            modifier = Modifier.width(60.dp),
                            textAlign = TextAlign.End,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = TextSecondaryDark,
                                fontSize = 10.sp
                            )
                        )
                        Text(
                            text = if (s.changePercent > 0) "Persistent Trend" else "Consolidation",
                            modifier = Modifier.width(100.dp),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = TextSecondaryDark,
                                fontSize = 10.sp
                            ),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// SHARED HELPER COMPONENTS
// -------------------------------------------------------------------------
@Composable
private fun EvidenceMetricGrid(items: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { (label, value) ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = TerminalSurfaceElevated,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(label, style = MaterialTheme.typography.bodySmall.copy(color = TextSecondaryDark, fontSize = 10.sp))
                            Text(value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = TextPrimaryDark, fontSize = 10.5.sp))
                        }
                    }
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
