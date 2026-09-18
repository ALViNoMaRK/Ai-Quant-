package com.example.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PortfolioEntity
import com.example.data.model.WatchlistEntity
import com.example.ui.components.ScoreRing
import com.example.ui.theme.*
import com.example.ui.viewmodel.StockIntelViewModel

@Composable
fun PortfolioAndWatchlistScreen(
    viewModel: StockIntelViewModel,
    onNavigateToResearch: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSubTab by remember { mutableStateOf(0) } // 0: Portfolio, 1: Watchlist
    val portfolio by viewModel.portfolio.collectAsState()
    val watchlist by viewModel.watchlist.collectAsState()
    val allStocks by viewModel.allStocks.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TerminalBgDark)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Toggle: Portfolio vs Watchlist
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TabRow(
                selectedTabIndex = selectedSubTab,
                modifier = Modifier.weight(1f),
                containerColor = TerminalSurfaceDark,
                contentColor = CyanAccent,
                divider = {}
            ) {
                Tab(
                    selected = selectedSubTab == 0,
                    onClick = { selectedSubTab = 0 },
                    text = {
                        Text(
                            "PORTFOLIO (${portfolio.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                )
                Tab(
                    selected = selectedSubTab == 1,
                    onClick = { selectedSubTab = 1 },
                    text = {
                        Text(
                            "WATCHLIST (${watchlist.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                )
            }

            if (selectedSubTab == 0) {
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.testTag("add_position_button")
                ) {
                    Icon(Icons.Default.AddCircle, contentDescription = "Add Position", tint = CyanAccent)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedSubTab == 0) {
            PortfolioTab(
                portfolio = portfolio,
                allStocks = allStocks,
                onNavigateToResearch = onNavigateToResearch,
                onRemove = { viewModel.removePortfolioPosition(it) }
            )
        } else {
            WatchlistTab(
                watchlist = watchlist,
                allStocks = allStocks,
                onNavigateToResearch = onNavigateToResearch,
                onRemove = { viewModel.removeFromWatchlist(it) }
            )
        }
    }

    if (showAddDialog) {
        AddPositionDialog(
            availableSymbols = allStocks.map { it.symbol },
            onDismiss = { showAddDialog = false },
            onConfirm = { symbol, shares, price, target ->
                viewModel.addPortfolioPosition(symbol, shares, price, target)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun PortfolioTab(
    portfolio: List<PortfolioEntity>,
    allStocks: List<com.example.data.model.StockEntity>,
    onNavigateToResearch: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    // Calculate total value, total cost, total gain
    var totalValue = 0.0
    var totalCost = 0.0

    portfolio.forEach { pos ->
        val stock = allStocks.firstOrNull { it.symbol == pos.symbol }
        val currentPrice = stock?.price ?: pos.averageBuyPrice
        totalValue += pos.shares * currentPrice
        totalCost += pos.shares * pos.averageBuyPrice
    }

    val totalGain = totalValue - totalCost
    val totalReturnPct = if (totalCost > 0) (totalGain / totalCost) * 100 else 0.0

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            // Portfolio Summary Card
            Card(
                colors = CardDefaults.cardColors(containerColor = TerminalSurfaceDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorderDark),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "TOTAL EQUITY VALUE",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondaryDark
                    )
                    Text(
                        text = "$${"%,.2f".format(totalValue)}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = TextPrimaryDark
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val isPos = totalGain >= 0
                        val gainColor = if (isPos) EmeraldGreen else CrimsonRed
                        val prefix = if (isPos) "+" else ""
                        Text(
                            text = "$prefix$${"%,.2f".format(totalGain)} ($prefix${"%.2f".format(totalReturnPct)}%)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = gainColor
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Unrealized P/L",
                            fontSize = 11.sp,
                            color = TextMutedDark
                        )
                    }
                }
            }
        }

        if (portfolio.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No active positions. Tap + to add holdings.", color = TextSecondaryDark)
                }
            }
        }

        items(portfolio) { pos ->
            val stock = allStocks.firstOrNull { it.symbol == pos.symbol }
            val currentPrice = stock?.price ?: pos.averageBuyPrice
            val posValue = pos.shares * currentPrice
            val posCost = pos.shares * pos.averageBuyPrice
            val posGain = posValue - posCost
            val posGainPct = if (posCost > 0) (posGain / posCost) * 100 else 0.0

            Card(
                colors = CardDefaults.cardColors(containerColor = TerminalSurfaceDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorderDark),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToResearch(pos.symbol) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = pos.symbol,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 16.sp,
                                color = TextPrimaryDark
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${pos.shares} shs",
                                fontSize = 12.sp,
                                color = TextSecondaryDark
                            )
                        }
                        Text(
                            text = "Avg: $${"%.2f".format(pos.averageBuyPrice)} • Current: $${"%.2f".format(currentPrice)}",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextMutedDark
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "$${"%,.2f".format(posValue)}",
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                color = TextPrimaryDark
                            )
                            val isPos = posGain >= 0
                            val gainColor = if (isPos) EmeraldGreen else CrimsonRed
                            val prefix = if (isPos) "+" else ""
                            Text(
                                text = "$prefix$${"%.2f".format(posGain)} ($prefix${"%.1f".format(posGainPct)}%)",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                color = gainColor
                            )
                        }

                        IconButton(onClick = { onRemove(pos.symbol) }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = TextMutedDark)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WatchlistTab(
    watchlist: List<WatchlistEntity>,
    allStocks: List<com.example.data.model.StockEntity>,
    onNavigateToResearch: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        if (watchlist.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Watchlist is empty. Tap the star icon on any stock to track it.", color = TextSecondaryDark)
                }
            }
        }

        items(watchlist) { item ->
            val stock = allStocks.firstOrNull { it.symbol == item.symbol }
            Card(
                colors = CardDefaults.cardColors(containerColor = TerminalSurfaceDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorderDark),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToResearch(item.symbol) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.symbol,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 16.sp,
                            color = TextPrimaryDark
                        )
                        stock?.let {
                            Text(it.companyName, fontSize = 11.sp, color = TextSecondaryDark)
                            if (item.notes.isNotBlank()) {
                                Text(item.notes, fontSize = 10.sp, color = CyanAccent)
                            }
                        }
                    }

                    stock?.let {
                        ScoreRing(score = it.masterScore, size = 42.dp, strokeWidth = 3.5.dp)
                    }

                    IconButton(onClick = { onRemove(item.symbol) }) {
                        Icon(Icons.Default.Star, contentDescription = "Remove", tint = GoldAccent)
                    }
                }
            }
        }
    }
}

@Composable
fun AddPositionDialog(
    availableSymbols: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (symbol: String, shares: Double, price: Double, target: Double?) -> Unit
) {
    var selectedSymbol by remember { mutableStateOf(availableSymbols.firstOrNull() ?: "NVDA") }
    var sharesText by remember { mutableStateOf("10") }
    var priceText by remember { mutableStateOf("120.0") }
    var targetText by remember { mutableStateOf("160.0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Portfolio Position", color = TextPrimaryDark) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Select Ticker: $selectedSymbol", fontWeight = FontWeight.Bold, color = CyanAccent)
                OutlinedTextField(
                    value = sharesText,
                    onValueChange = { sharesText = it },
                    label = { Text("Shares Count") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("Average Buy Price ($)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = it },
                    label = { Text("Target Price ($ Optional)") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val s = sharesText.toDoubleOrNull() ?: 1.0
                    val p = priceText.toDoubleOrNull() ?: 100.0
                    val t = targetText.toDoubleOrNull()
                    onConfirm(selectedSymbol, s, p, t)
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
            ) {
                Text("Add Position", color = TerminalBgDark, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondaryDark)
            }
        },
        containerColor = TerminalSurfaceDark
    )
}
