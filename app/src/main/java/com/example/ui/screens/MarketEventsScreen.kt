package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import com.example.ui.components.MarketEventCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.StockIntelViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketEventsScreen(
    viewModel: StockIntelViewModel,
    onNavigateToResearch: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val events by viewModel.allMarketEvents.collectAsState()
    var selectedCategory by remember { mutableStateOf("ALL") }
    var selectedPriority by remember { mutableStateOf("ALL") }
    var selectedEventDetail by remember { mutableStateOf<MarketEventEntity?>(null) }
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    val categories = listOf("ALL", "GOVERNMENT", "CORPORATE", "REGULATION", "M&A", "POLICY", "MACRO")

    val filteredEvents = remember(events, selectedCategory, selectedPriority) {
        events.filter { event ->
            val matchesCategory = if (selectedCategory == "ALL") true else {
                val type = try { MarketEventType.valueOf(event.eventType) } catch (_: Exception) { null }
                type?.category.equals(selectedCategory, ignoreCase = true)
            }
            val matchesPriority = if (selectedPriority == "ALL") true else {
                event.priority.equals(selectedPriority, ignoreCase = true)
            }
            matchesCategory && matchesPriority
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = TerminalBgDark,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "EVENT INTELLIGENCE ENGINE",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = CyanAccent,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Verified Policy, Gov & Market Disclosures",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextSecondaryDark
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimaryDark
                        )
                    }
                },
                actions = {
                    FilledTonalIconButton(
                        onClick = { viewModel.refreshData() },
                        modifier = Modifier.testTag("refresh_events_button")
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = CyanAccent,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Market Events",
                                tint = CyanAccent
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TerminalSurfaceDark,
                    titleContentColor = TextPrimaryDark
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Engine Architecture Banner
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = TerminalSurfaceDark),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = CyanAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "VERIFIED IMPACT PIPELINE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = CyanAccent
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = EmeraldGreen.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "REAL-TIME AUDIT",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = EmeraldGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = "Detects government funding, trade decisions, M&A, and policy directives from official government registers and SEC filings. Unverified rumors receive 0% score weight until corroborated.",
                            fontSize = 11.sp,
                            color = TextSecondaryDark,
                            lineHeight = 16.sp
                        )

                        // Three pillars formula
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            PillarBadge("Base Score", "Audited Fundamentals")
                            PillarBadge("Event Impact", "Decayed Multipliers")
                            PillarBadge("Second-Order", "Supply Chain Propagation")
                        }
                    }
                }
            }

            // Category Filter Chips
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "EVENT CATEGORY",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondaryDark
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { cat ->
                            val isSelected = selectedCategory == cat
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategory = cat },
                                label = {
                                    Text(
                                        text = cat,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) TextPrimaryDark else TextSecondaryDark
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyanAccent.copy(alpha = 0.25f),
                                    containerColor = TerminalSurfaceDark
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = if (isSelected) CyanAccent else TerminalBorderDark
                                )
                            )
                        }
                    }
                }
            }

            // Events List Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DETECTED MARKET EVENTS (${filteredEvents.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = TextPrimaryDark
                    )
                }
            }

            // Events List
            items(filteredEvents) { event ->
                MarketEventCard(
                    event = event,
                    onClick = { selectedEventDetail = event }
                )
            }
        }
    }

    // Event Detail & Impact Propagation BottomSheet / Dialog
    selectedEventDetail?.let { event ->
        EventDetailDialog(
            event = event,
            onDismiss = { selectedEventDetail = null },
            onSelectStock = { ticker ->
                selectedEventDetail = null
                onNavigateToResearch(ticker)
            }
        )
    }
}

@Composable
private fun PillarBadge(title: String, subtitle: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = TerminalSurfaceElevated,
        border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorderDark)
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = TextPrimaryDark
            )
            Text(
                text = subtitle,
                fontSize = 8.sp,
                color = TextSecondaryDark
            )
        }
    }
}

@Composable
fun EventDetailDialog(
    event: MarketEventEntity,
    onDismiss: () -> Unit,
    onSelectStock: (String) -> Unit
) {
    val decay = remember(event.publishedTimestamp) {
        MarketEventEngine.calculateDecayFactor(event.publishedTimestamp)
    }
    val affectedSecurities = remember(event.primaryTicker, event.eventType) {
        val eventType = try { MarketEventType.valueOf(event.eventType) } catch (_: Exception) { MarketEventType.OTHER_MATERIAL_EVENT }
        MarketEventEngine.getSecondOrderAffectedSecurities(event.primaryTicker, eventType)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = CyanAccent, fontFamily = FontFamily.Monospace)
            }
        },
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = CyanAccent.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "${event.priority} • ${event.verificationStatus}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = CyanAccent,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = event.headline,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark,
                    lineHeight = 19.sp
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = "EXECUTIVE SUMMARY",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondaryDark
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = event.summary,
                        fontSize = 12.sp,
                        color = TextPrimaryDark,
                        lineHeight = 17.sp
                    )
                }

                item {
                    Text(
                        text = "QUANT & FINANCIAL REASONING",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondaryDark
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = event.aiReasoning,
                        fontSize = 11.sp,
                        color = TextSecondaryDark,
                        lineHeight = 16.sp
                    )
                }

                // Decay & Calibration Metrics
                item {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = TerminalBgDark,
                        border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorderDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Decay Factor (Half-life 14d):", fontSize = 10.sp, color = TextSecondaryDark)
                                Text("${(decay * 100).toInt()}% active", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyanAccent)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Source Hierarchy Level:", fontSize = 10.sp, color = TextSecondaryDark)
                                Text("Level ${event.sourceHierarchyLevel} (Official/Direct)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Verified Capital Allocation:", fontSize = 10.sp, color = TextSecondaryDark)
                                Text(event.dollarAmount ?: "Non-cash policy directive", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GoldAccent)
                            }
                        }
                    }
                }

                // Second-Order Impact Propagation List
                item {
                    Text(
                        text = "AFFECTED SECURITIES & 2ND-ORDER BENEFICIARIES",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondaryDark
                    )
                }

                items(affectedSecurities) { sec ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = TerminalSurfaceElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorderDark),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectStock(sec.ticker) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = sec.ticker,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = CyanAccent
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(3.dp),
                                        color = TerminalBgDark
                                    ) {
                                        Text(
                                            text = sec.relationship.name,
                                            fontSize = 8.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = TextSecondaryDark,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = sec.scoreContributionExplanation,
                                    fontSize = 10.sp,
                                    color = TextSecondaryDark,
                                    maxLines = 2
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            val deltaSign = if (sec.estimatedScoreDelta >= 0) "+" else ""
                            Text(
                                text = "$deltaSign${"%.1f".format(sec.estimatedScoreDelta)} pts",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (sec.estimatedScoreDelta >= 0) EmeraldGreen else CrimsonRed
                            )
                        }
                    }
                }
            }
        },
        containerColor = TerminalSurfaceDark
    )
}
