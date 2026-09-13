package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ScoringWeights
import com.example.data.remote.ProviderStatus
import com.example.ui.theme.*
import com.example.ui.viewmodel.StockIntelViewModel

@Composable
fun AlertsAndHealthScreen(
    viewModel: StockIntelViewModel,
    onNavigateToResearch: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val alerts by viewModel.alerts.collectAsState()
    val providerHealths by viewModel.providerHealths.collectAsState()
    val customWeights by viewModel.customWeights.collectAsState()

    var showWeightsDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(TerminalBgDark)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // API Providers Status Section
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = TerminalSurfaceDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorderDark),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DATA PROVIDER CONNECTIVITY & HEALTH",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = CyanAccent
                            )
                        )
                        IconButton(onClick = { viewModel.refreshData() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = CyanAccent, modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (providerHealths.isEmpty()) {
                        Text("Querying data sources...", fontSize = 11.sp, color = TextMutedDark)
                    }

                    providerHealths.forEach { health ->
                        val (statusColor, statusLabel) = when (health.status) {
                            ProviderStatus.ONLINE -> Pair(EmeraldGreen, "ONLINE")
                            ProviderStatus.DEGRADED -> Pair(AmberWarning, "DEGRADED")
                            ProviderStatus.OFFLINE -> Pair(CrimsonRed, "OFFLINE")
                            ProviderStatus.KEY_REQUIRED -> Pair(Color(0xFF38BDF8), "KEY REQUIRED")
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(statusColor)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(health.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                                    Text(health.details, fontSize = 10.sp, color = TextMutedDark)
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${health.latencyMs}ms",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextSecondaryDark
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = statusColor.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = statusLabel,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = statusColor,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = TerminalBorderDark.copy(alpha = 0.3f), thickness = 0.5.dp)
                    }
                }
            }
        }

        // Factor Weights Customization Trigger
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = TerminalSurfaceDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorderDark),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showWeightsDialog = true }
                    .testTag("configure_weights_card")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "CUSTOMIZE QUANT FACTOR WEIGHTS",
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = GoldAccent
                        )
                        Text(
                            text = "Adjust the 9 fundamental and institutional factor weights.",
                            fontSize = 11.sp,
                            color = TextSecondaryDark
                        )
                    }
                    Icon(Icons.Default.Tune, contentDescription = "Configure", tint = GoldAccent)
                }
            }
        }

        // Active Alerts Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SYSTEM & MATERIAL ALERTS (${alerts.size})",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
            }
        }

        if (alerts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No active alerts at this time.", color = TextSecondaryDark)
                }
            }
        }

        items(alerts) { alert ->
            Card(
                colors = CardDefaults.cardColors(containerColor = TerminalSurfaceDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorderDark),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = alert.symbol,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = CyanAccent,
                                fontSize = 13.sp,
                                modifier = Modifier.clickable { onNavigateToResearch(alert.symbol) }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = if (alert.importance == "HIGH") CrimsonRed.copy(alpha = 0.2f) else AmberWarning.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = alert.alertType.uppercase(),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (alert.importance == "HIGH") CrimsonRed else AmberWarning,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }

                        IconButton(onClick = { viewModel.dismissAlert(alert.id) }) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = TextMutedDark, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(alert.title, fontWeight = FontWeight.Bold, color = TextPrimaryDark, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(alert.reason, fontSize = 11.sp, color = TextSecondaryDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Source: ${alert.source}", fontSize = 9.sp, color = TextMutedDark)
                }
            }
        }
    }

    if (showWeightsDialog) {
        WeightsConfigDialog(
            initialWeights = customWeights,
            onDismiss = { showWeightsDialog = false },
            onApply = { newWeights ->
                viewModel.updateWeights(newWeights)
                showWeightsDialog = false
            }
        )
    }
}

@Composable
fun WeightsConfigDialog(
    initialWeights: ScoringWeights,
    onDismiss: () -> Unit,
    onApply: (ScoringWeights) -> Unit
) {
    var health by remember { mutableStateOf((initialWeights.financialHealth * 100).toFloat()) }
    var quality by remember { mutableStateOf((initialWeights.businessQuality * 100).toFloat()) }
    var growth by remember { mutableStateOf((initialWeights.growth * 100).toFloat()) }
    var valuation by remember { mutableStateOf((initialWeights.valuation * 100).toFloat()) }
    var inst by remember { mutableStateOf((initialWeights.institutionalCapital * 100).toFloat()) }
    var earnings by remember { mutableStateOf((initialWeights.earningsExpectations * 100).toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom Quant Weights", color = TextPrimaryDark) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Adjust relative factor emphasis (auto-normalized to 100%):", fontSize = 11.sp, color = TextSecondaryDark)
                WeightSlider("Financial Health", health) { health = it }
                WeightSlider("Business Quality", quality) { quality = it }
                WeightSlider("Growth Momentum", growth) { growth = it }
                WeightSlider("Valuation Multiple", valuation) { valuation = it }
                WeightSlider("Institutional 13F", inst) { inst = it }
                WeightSlider("Earnings & Revisions", earnings) { earnings = it }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updated = ScoringWeights(
                        financialHealth = health.toDouble(),
                        businessQuality = quality.toDouble(),
                        growth = growth.toDouble(),
                        valuation = valuation.toDouble(),
                        institutionalCapital = inst.toDouble(),
                        earningsExpectations = earnings.toDouble()
                    ).normalized()
                    onApply(updated)
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
            ) {
                Text("Recalculate All Scores", color = TerminalBgDark, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondaryDark) }
        },
        containerColor = TerminalSurfaceDark
    )
}

@Composable
fun WeightSlider(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 11.sp, color = TextPrimaryDark)
            Text("${value.toInt()}%", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = CyanAccent)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..50f,
            colors = SliderDefaults.colors(
                thumbColor = CyanAccent,
                activeTrackColor = CyanAccent,
                inactiveTrackColor = TerminalBorderDark
            )
        )
    }
}
