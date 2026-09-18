package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
    var selectedCategoryFilter by remember { mutableStateOf("ALL") }
    var selectedSeverityFilter by remember { mutableStateOf("ALL") }

    val categories = listOf("ALL", "MARKET", "INTELLIGENCE", "FUNDAMENTAL", "INSTITUTIONAL", "NEWS")
    val severities = listOf("ALL", "HIGH", "MEDIUM")

    val filteredAlerts = alerts.filter { alert ->
        val matchesCat = selectedCategoryFilter == "ALL" || alert.alertType.equals(selectedCategoryFilter, ignoreCase = true)
        val matchesSev = selectedSeverityFilter == "ALL" || alert.importance.equals(selectedSeverityFilter, ignoreCase = true)
        matchesCat && matchesSev
    }

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
                            ProviderStatus.RATE_LIMITED -> Pair(Color(0xFFFB923C), "RATE LIMITED")
                            ProviderStatus.AUTHENTICATION_ERROR -> Pair(Color(0xFFE879F9), "AUTH ERROR")
                            ProviderStatus.TIMEOUT -> Pair(Color(0xFFF43F5E), "TIMEOUT")
                            ProviderStatus.KEY_REQUIRED -> Pair(Color(0xFF38BDF8), "KEY REQUIRED")
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(statusColor)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f, fill = false)) {
                                    Text(
                                        health.name,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryDark,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    Text(
                                        health.details,
                                        fontSize = 10.sp,
                                        color = TextMutedDark,
                                        maxLines = 2,
                                        lineHeight = 13.sp,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End
                            ) {
                                Text(
                                    text = "${health.latencyMs}ms",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextSecondaryDark,
                                    maxLines = 1,
                                    softWrap = false
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
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
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
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SYSTEM & MATERIAL ALERTS (${filteredAlerts.size})",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    if (alerts.isNotEmpty()) {
                        Text(
                            text = "CLEAR ALL",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = TextMutedDark,
                            modifier = Modifier
                                .clickable { viewModel.clearAllAlerts() }
                                .padding(4.dp)
                        )
                    }
                }

                // Category Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = selectedCategoryFilter == cat
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) CyanAccent.copy(alpha = 0.2f) else CardBgDark,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) CyanAccent else TerminalBorderDark
                            ),
                            modifier = Modifier.clickable { selectedCategoryFilter = cat }
                        ) {
                            Text(
                                text = cat,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (isSelected) CyanAccent else TextSecondaryDark,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Severity Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    severities.forEach { sev ->
                        val isSelected = selectedSeverityFilter == sev
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) GoldAccent.copy(alpha = 0.2f) else CardBgDark,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) GoldAccent else TerminalBorderDark
                            ),
                            modifier = Modifier.clickable { selectedSeverityFilter = sev }
                        ) {
                            Text(
                                text = if (sev == "ALL") "ALL SEVERITIES" else "$sev SEVERITY",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (isSelected) GoldAccent else TextSecondaryDark,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }

        if (filteredAlerts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No alerts match active filters.", color = TextSecondaryDark, fontSize = 12.sp)
                }
            }
        }

        items(filteredAlerts) { alert ->
            val typeColor = when (alert.alertType.uppercase()) {
                "MARKET" -> CyanAccent
                "INTELLIGENCE" -> Color(0xFFBA68C8) // Violet
                "FUNDAMENTAL" -> EmeraldGreen
                "INSTITUTIONAL" -> GoldAccent
                "NEWS" -> Color(0xFFFF9800) // Orange
                else -> CyanAccent
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = TerminalSurfaceDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorderDark),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                color = CyanAccent.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, CyanAccent.copy(alpha = 0.4f)),
                                modifier = Modifier.clickable { onNavigateToResearch(alert.symbol) }
                            ) {
                                Text(
                                    text = alert.symbol,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = CyanAccent,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            Surface(
                                color = typeColor.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = alert.alertType.uppercase(),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = typeColor,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }

                            Surface(
                                color = if (alert.importance == "HIGH") CrimsonRed.copy(alpha = 0.2f) else AmberWarning.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = alert.importance,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (alert.importance == "HIGH") CrimsonRed else AmberWarning,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = { viewModel.dismissAlert(alert.id) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = TextMutedDark, modifier = Modifier.size(14.dp))
                        }
                    }

                    Text(alert.title, fontWeight = FontWeight.Bold, color = TextPrimaryDark, fontSize = 12.sp)

                    // Evidence / "Why" callout box
                    Surface(
                        color = TerminalBgDark,
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, TerminalBorderDark)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = "EVIDENCE (WHY TRIGGERED):",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = TextMutedDark
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(alert.reason, fontSize = 10.sp, color = TextSecondaryDark, lineHeight = 14.sp)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Source: ${alert.source}", fontSize = 8.sp, color = TextMutedDark, fontFamily = FontFamily.Monospace)
                        Text(
                            text = "Inspect in Research →",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent,
                            modifier = Modifier.clickable { onNavigateToResearch(alert.symbol) }
                        )
                    }
                }
            }
        }

        // GitHub & APK Distribution Hub Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = TerminalSurfaceDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("github_apk_hub_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "GITHUB APK DISTRIBUTION",
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = CyanAccent
                            )
                        }
                        Surface(
                            color = EmeraldGreen.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "CI/CD ACTIVE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = EmeraldGreen,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Automated GitHub Actions workflow '.github/workflows/build-apk.yml' compiles fresh APK packages automatically upon code push.",
                        fontSize = 11.sp,
                        color = TextPrimaryDark,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        color = TerminalBgDark,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "HOW TO DOWNLOAD FROM GITHUB:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = GoldAccent
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "1. Open your repository on GitHub\n2. Tap the 'Actions' tab\n3. Select the latest build workflow\n4. Scroll to 'Artifacts' and download 'stock-intel-debug-apk'",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextSecondaryDark,
                                lineHeight = 15.sp
                            )
                        }
                    }
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
