package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.engine.events.MarketEventEngine
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun EventScoreDecompositionCard(
    symbol: String,
    baseScore: Double,
    events: List<MarketEventEntity>,
    onOpenEventIntelligence: () -> Unit,
    modifier: Modifier = Modifier
) {
    val breakdown = remember(symbol, baseScore, events) {
        MarketEventEngine.calculateStockScoreBreakdown(
            symbol = symbol,
            baseFundamentalScore = baseScore,
            events = events
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = TerminalSurfaceDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorderDark),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = GoldAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "INTELLIGENCE SCORE DECOMPOSITION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = GoldAccent
                    )
                }

                TextButton(
                    onClick = onOpenEventIntelligence,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "Event Engine →",
                        fontSize = 11.sp,
                        color = CyanAccent,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Transparent Formula display: BASE + EVENT = CURRENT
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = TerminalBgDark,
                border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorderDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Base Fundamental Score
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "BASE SCORE",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextSecondaryDark
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "${"%.1f".format(breakdown.baseFundamentalScore)}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = "Fundamentals",
                            fontSize = 8.sp,
                            color = TextSecondaryDark
                        )
                    }

                    Text(
                        text = "+",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondaryDark
                    )

                    // Event Impact Adjustment
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "EVENT IMPACT",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (breakdown.eventImpactAdjustment >= 0) EmeraldGreen else CrimsonRed
                        )
                        Spacer(Modifier.height(2.dp))
                        val sign = if (breakdown.eventImpactAdjustment >= 0) "+" else ""
                        Text(
                            text = "$sign${"%.1f".format(breakdown.eventImpactAdjustment)}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (breakdown.eventImpactAdjustment >= 0) EmeraldGreen else CrimsonRed
                        )
                        Text(
                            text = "${breakdown.explanationItems.size} verified events",
                            fontSize = 8.sp,
                            color = TextSecondaryDark
                        )
                    }

                    Text(
                        text = "=",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondaryDark
                    )

                    // Current Intelligence Score
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "CURRENT SCORE",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = CyanAccent,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "${"%.1f".format(breakdown.currentIntelligenceScore)}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = CyanAccent
                        )
                        Text(
                            text = "Live Active Intel",
                            fontSize = 8.sp,
                            color = CyanAccent
                        )
                    }
                }
            }

            // Impacted Event Explanations (Evidence-based)
            if (breakdown.explanationItems.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    breakdown.explanationItems.take(2).forEach { item ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = TerminalSurfaceElevated,
                            border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorderDark),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item.headline,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimaryDark,
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    val deltaSign = if (item.delta >= 0) "+" else ""
                                    Text(
                                        text = "$deltaSign${"%.1f".format(item.delta)} pts",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (item.delta >= 0) EmeraldGreen else CrimsonRed
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = item.reason,
                                    fontSize = 10.sp,
                                    color = TextSecondaryDark,
                                    maxLines = 2
                                )
                                Spacer(Modifier.height(2.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Source: ${item.source}",
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = CyanAccent.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = "${item.verificationStatus.label} • ${item.publishedAgo}",
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = TextSecondaryDark
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = "No pending high-impact market event adjustments active. Base fundamental score represents 100% of the rating.",
                    fontSize = 10.sp,
                    color = TextSecondaryDark
                )
            }
        }
    }
}

@Composable
fun MarketEventCard(
    event: MarketEventEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCritical = event.priority == EventPriority.CRITICAL.name
    val borderColor = if (isCritical) CrimsonRed.copy(alpha = 0.6f) else TerminalBorderDark
    val sdf = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US) }
    val formattedDate = remember(event.publishedTimestamp) { sdf.format(Date(event.publishedTimestamp)) }

    Card(
        colors = CardDefaults.cardColors(containerColor = TerminalSurfaceDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Tags
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Priority Badge
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isCritical) CrimsonRed.copy(alpha = 0.2f) else GoldAccent.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = event.priority,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (isCritical) CrimsonRed else GoldAccent,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Verification Badge
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = TerminalSurfaceElevated
                    ) {
                        Text(
                            text = event.verificationStatus,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = CyanAccent,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Score Adjustment Pill
                val deltaSign = if (event.baseScoreAdjustment >= 0) "+" else ""
                val deltaColor = if (event.baseScoreAdjustment >= 0) EmeraldGreen else CrimsonRed
                Text(
                    text = "$deltaSign${"%.1f".format(event.baseScoreAdjustment)} Score Delta",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = deltaColor
                )
            }

            // Headline
            Text(
                text = event.headline,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark,
                lineHeight = 18.sp
            )

            // Summary
            Text(
                text = event.summary,
                fontSize = 11.sp,
                color = TextSecondaryDark,
                lineHeight = 15.sp,
                maxLines = 3
            )

            // Entities and Sources
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tickers
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "AFFECTED: ",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondaryDark
                    )
                    Text(
                        text = event.affectedTickers,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = CyanAccent
                    )
                }

                // Date
                Text(
                    text = formattedDate,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextSecondaryDark
                )
            }

            // Official Source Citation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TerminalBgDark, RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = "Verified Source",
                    tint = CyanAccent,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Source: ${event.sourceName} (${event.sourceCount} verified filings)",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextSecondaryDark
                )
            }
        }
    }
}
