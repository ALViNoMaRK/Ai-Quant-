package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.*

@Composable
fun ScoreRing(
    score: Double,
    size: Dp = 56.dp,
    strokeWidth: Dp = 5.dp,
    modifier: Modifier = Modifier
) {
    val scoreColor = when {
        score >= 90 -> GoldAccent
        score >= 80 -> CyanAccent
        score >= 70 -> EmeraldGreen
        score >= 60 -> Color(0xFF60A5FA)
        score >= 50 -> AmberWarning
        else -> CrimsonRed
    }

    val sweepAngle = (score.coerceIn(0.0, 100.0) / 100.0 * 360.0).toFloat()

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Background track
            drawCircle(
                color = Color.DarkGray.copy(alpha = 0.35f),
                style = Stroke(width = strokeWidth.toPx())
            )
            // Score arc
            drawArc(
                color = scoreColor,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )
        }
        Text(
            text = "${score.toInt()}",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.36).sp,
                fontFamily = FontFamily.Monospace,
                color = scoreColor
            )
        )
    }
}

@Composable
fun ClassificationBadge(
    classification: String,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (classification) {
        InvestmentClassification.ELITE_CANDIDATE.label -> Pair(GoldAccent.copy(alpha = 0.18f), GoldAccent)
        InvestmentClassification.STRONG_CANDIDATE.label -> Pair(CyanAccent.copy(alpha = 0.18f), CyanAccent)
        InvestmentClassification.WATCHLIST_ATTRACTIVE.label -> Pair(EmeraldGreen.copy(alpha = 0.18f), EmeraldGreen)
        InvestmentClassification.NEUTRAL.label -> Pair(Color(0xFF60A5FA).copy(alpha = 0.18f), Color(0xFF93C5FD))
        InvestmentClassification.SPECULATIVE_CAUTION.label -> Pair(AmberWarning.copy(alpha = 0.18f), AmberWarning)
        else -> Pair(CrimsonRed.copy(alpha = 0.18f), CrimsonRed)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, textColor.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Text(
            text = classification,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp,
                fontSize = 10.sp,
                color = textColor
            ),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun FreshnessBadge(
    freshness: String,
    dataSource: String? = null,
    modifier: Modifier = Modifier
) {
    val (dotColor, label) = when (freshness) {
        DataFreshness.LIVE.name -> Pair(EmeraldGreen, "LIVE")
        DataFreshness.RECENT.name -> Pair(CyanAccent, "RECENT")
        DataFreshness.DELAYED.name -> Pair(AmberWarning, "DELAYED")
        DataFreshness.STALE.name -> Pair(Color.Gray, "STALE")
        else -> Pair(CrimsonRed, "OFFLINE")
    }

    // Gentle pulse for live status
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
            .border(0.8.dp, TerminalBorderDark, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(if (freshness == DataFreshness.LIVE.name) dotColor.copy(alpha = alpha) else dotColor)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = dotColor
        )
        if (!dataSource.isNullOrBlank()) {
            Text(
                text = " • $dataSource",
                fontSize = 9.sp,
                color = TextMutedDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun MetricBar(
    label: String,
    score: Double,
    modifier: Modifier = Modifier
) {
    val barColor = when {
        score >= 85 -> GoldAccent
        score >= 75 -> CyanAccent
        score >= 60 -> EmeraldGreen
        score >= 45 -> AmberWarning
        else -> CrimsonRed
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    color = TextSecondaryDark
                )
            )
            Text(
                text = "${score.toInt()}/100",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    color = barColor
                )
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.DarkGray.copy(alpha = 0.4f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth((score.coerceIn(0.0, 100.0) / 100.0).toFloat())
                    .background(barColor)
            )
        }
    }
}

@Composable
fun StockCard(
    stock: StockEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isWatchlist: Boolean = false,
    onToggleWatchlist: (() -> Unit)? = null,
    quantScore: Double? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("stock_card_${stock.symbol}"),
        colors = CardDefaults.cardColors(
            containerColor = TerminalSurfaceDark
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorderDark),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header: Symbol, Status, Watchlist, Dual Scores
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stock.symbol,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = TextPrimaryDark
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        FreshnessBadge(freshness = stock.freshness)
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = EmeraldGreen.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp),
                            border = androidx.compose.foundation.BorderStroke(0.6.dp, EmeraldGreen.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "OPEN",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldGreen,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = stock.companyName,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextSecondaryDark,
                            fontSize = 12.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${stock.sector} • ${stock.industry}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextMutedDark,
                            fontSize = 10.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (onToggleWatchlist != null) {
                        IconButton(
                            onClick = onToggleWatchlist,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isWatchlist) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = if (isWatchlist) "In Watchlist" else "Add to Watchlist",
                                tint = if (isWatchlist) GoldAccent else TextMutedDark,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("INVEST", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = TextMutedDark)
                        ScoreRing(
                            score = stock.masterScore,
                            size = 42.dp,
                            strokeWidth = 3.5.dp
                        )
                    }

                    if (quantScore != null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("QUANT", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = CyanAccent)
                            ScoreRing(
                                score = quantScore,
                                size = 42.dp,
                                strokeWidth = 3.5.dp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Price and Daily Change
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$${"%.2f".format(stock.price)}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = TextPrimaryDark
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val isPos = stock.changePercent >= 0
                    val changeColor = if (isPos) EmeraldGreen else CrimsonRed
                    val prefix = if (isPos) "+" else ""
                    val changeAmountStr = if (stock.changeAmount != 0.0) {
                        " (${if (stock.changeAmount >= 0) "+" else ""}${"%.2f".format(stock.changeAmount)})"
                    } else ""
                    Text(
                        text = "$prefix${"%.2f".format(stock.changePercent)}%$changeAmountStr",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace,
                            color = changeColor
                        )
                    )
                }

                ClassificationBadge(classification = stock.classification)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Mini component metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricMiniPill("Health", stock.financialHealthScore, Modifier.weight(1f))
                MetricMiniPill("Quality", stock.businessQualityScore, Modifier.weight(1f))
                MetricMiniPill("Growth", stock.growthScore, Modifier.weight(1f))
                MetricMiniPill("Value", stock.valuationScore, Modifier.weight(1f))
                MetricMiniPill("Inst", stock.institutionalScore, Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun MetricMiniPill(
    label: String,
    score: Double,
    modifier: Modifier = Modifier
) {
    val color = when {
        score >= 85 -> GoldAccent
        score >= 70 -> CyanAccent
        score >= 55 -> EmeraldGreen
        else -> AmberWarning
    }

    Column(
        modifier = modifier
            .background(TerminalSurfaceElevated, RoundedCornerShape(6.dp))
            .border(0.6.dp, TerminalBorderDark, RoundedCornerShape(6.dp))
            .padding(vertical = 4.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            color = TextSecondaryDark
        )
        Text(
            text = "${score.toInt()}",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = color
        )
    }
}

@Composable
fun MiniSparkline(
    points: List<Double>,
    isPositive: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (points.size < 2) return
    val strokeColor = if (isPositive) EmeraldGreen else CrimsonRed

    Canvas(modifier = modifier) {
        val min = points.minOrNull() ?: 0.0
        val max = points.maxOrNull() ?: 1.0
        val range = if (max - min > 0) (max - min) else 1.0

        val path = Path()
        val stepX = size.width / (points.size - 1)

        points.forEachIndexed { i, p ->
            val x = i * stepX
            val y = size.height - (((p - min) / range) * size.height).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = strokeColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}
