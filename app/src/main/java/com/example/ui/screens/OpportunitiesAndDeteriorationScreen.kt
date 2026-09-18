package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.DeteriorationSignal
import com.example.engine.OpportunitySignal
import com.example.ui.theme.*
import com.example.ui.viewmodel.StockIntelViewModel

@Composable
fun OpportunitiesAndDeteriorationScreen(
    viewModel: StockIntelViewModel,
    initialTab: Int = 0,
    onNavigateToResearch: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(initialTab) }
    val opportunities by viewModel.opportunities.collectAsState()
    val deteriorating by viewModel.deteriorating.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TerminalBgDark)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = TerminalSurfaceDark,
            contentColor = CyanAccent,
            divider = {}
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("OPPORTUNITIES (${opportunities.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = CrimsonRed, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("DETERIORATING (${deteriorating.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedTab == 0) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(opportunities) { opp ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = TerminalSurfaceDark),
                        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToResearch(opp.symbol) }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = opp.symbol,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 16.sp,
                                        color = TextPrimaryDark
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(opp.companyName, fontSize = 12.sp, color = TextSecondaryDark)
                                }
                                Surface(
                                    color = EmeraldGreen.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "${opp.opportunityScore} / 100",
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = EmeraldGreen,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(opp.primaryTrigger, fontWeight = FontWeight.Bold, color = CyanAccent, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(6.dp))

                            opp.evidenceList.forEach { ev ->
                                Row(modifier = Modifier.padding(vertical = 2.dp)) {
                                    Text("✓ ", color = EmeraldGreen, fontSize = 11.sp)
                                    Text(ev, fontSize = 11.sp, color = TextPrimaryDark)
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${opp.growthRate} • ${opp.valuationMultiple}",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextMutedDark
                            )
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(deteriorating) { det ->
                    val severityColor = if (det.severity == "CRITICAL") CrimsonRed else AmberWarning
                    Card(
                        colors = CardDefaults.cardColors(containerColor = TerminalSurfaceDark),
                        border = androidx.compose.foundation.BorderStroke(1.dp, severityColor.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToResearch(det.symbol) }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = det.symbol,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 16.sp,
                                        color = TextPrimaryDark
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(det.companyName, fontSize = 12.sp, color = TextSecondaryDark)
                                }
                                Surface(
                                    color = severityColor.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = det.severity,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = severityColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(det.headline, fontWeight = FontWeight.Bold, color = CrimsonRed, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(6.dp))

                            det.rootCauses.forEach { cause ->
                                Row(modifier = Modifier.padding(vertical = 2.dp)) {
                                    Text("⚠ ", color = CrimsonRed, fontSize = 11.sp)
                                    Text(cause, fontSize = 11.sp, color = TextPrimaryDark)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
