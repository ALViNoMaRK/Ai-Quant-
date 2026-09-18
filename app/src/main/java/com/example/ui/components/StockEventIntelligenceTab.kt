package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.engine.events.MarketEventEngine
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StockEventIntelligenceTab(
    stock: StockEntity,
    events: List<MarketEventEntity>,
    onSelectStock: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val stockEvents = remember(events, stock.symbol) {
        events.filter { it.primaryTicker == stock.symbol || it.affectedTickers.contains(stock.symbol) }
    }

    val breakdown = remember(stock.symbol, stock.masterScore, stockEvents) {
        MarketEventEngine.calculateStockScoreBreakdown(
            symbol = stock.symbol,
            baseFundamentalScore = stock.masterScore,
            events = stockEvents
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 60.dp)
    ) {
        // Event Decomposition Banner
        item {
            EventScoreDecompositionCard(
                symbol = stock.symbol,
                baseScore = stock.masterScore,
                events = stockEvents,
                onOpenEventIntelligence = {}
            )
        }

        // Live Event Adjustments Summary
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = TerminalSurfaceDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorderDark),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "EVENT ENGINE AUDIT & METHODOLOGY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = CyanAccent
                    )

                    Text(
                        text = "The Market Event Intelligence Engine ingests official federal notifications, presidential/administrative directives, antitrust filings, major corporate investments, and supply chain allocations. Base fundamental metrics remain isolated from speculative volatility, with event impact calibrated via exponential time decay and source hierarchy weights.",
                        fontSize = 11.sp,
                        color = TextSecondaryDark,
                        lineHeight = 16.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = TerminalBgDark,
                            border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorderDark),
                            modifier = Modifier.weight(1f).padding(end = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("DECAY MODEL", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = TextSecondaryDark)
                                Text("14-Day Half-Life", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = TerminalBgDark,
                            border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorderDark),
                            modifier = Modifier.weight(1f).padding(start = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("ADJUSTMENT BOUNDS", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = TextSecondaryDark)
                                Text("-10.0 to +8.0 pts max", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmeraldGreen)
                            }
                        }
                    }
                }
            }
        }

        // List of Active Events for this Ticker
        item {
            Text(
                text = "ACTIVE EVENTS AFFECTING ${stock.symbol} (${stockEvents.size})",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = TextPrimaryDark
            )
        }

        if (stockEvents.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = TerminalSurfaceDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorderDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = TextSecondaryDark,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "No material verified government or market events currently active for ${stock.symbol}.",
                            fontSize = 11.sp,
                            color = TextSecondaryDark,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        } else {
            items(stockEvents) { event ->
                MarketEventCard(
                    event = event,
                    onClick = {}
                )
            }
        }
    }
}
