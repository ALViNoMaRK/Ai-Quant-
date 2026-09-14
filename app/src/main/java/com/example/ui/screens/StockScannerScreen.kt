package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import com.example.data.model.InvestmentClassification
import com.example.data.model.StockEntity
import com.example.ui.components.StockCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.StockIntelViewModel

enum class ScannerFilter(val label: String) {
    ALL("All Equities"),
    ELITE("Elite Candidates"),
    STRONG("Strong Candidates"),
    GROWTH("Growth"),
    VALUE("Deep Value"),
    DEFENSIVE("Defensive")
}

enum class ScannerSort(val label: String) {
    MASTER_SCORE("Master Score"),
    GROWTH_SCORE("Growth Score"),
    QUALITY_SCORE("Quality Score"),
    VALUATION_SCORE("Valuation Score"),
    INSTITUTIONAL_SCORE("Institutional Score"),
    PRICE("Price")
}

@Composable
fun StockScannerScreen(
    viewModel: StockIntelViewModel,
    onNavigateToResearch: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val stocks by viewModel.allStocks.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isResolvingStock by viewModel.isResolvingStock.collectAsState()
    val resolveStockError by viewModel.resolveStockError.collectAsState()
    var selectedFilter by remember { mutableStateOf(ScannerFilter.ALL) }
    var selectedSort by remember { mutableStateOf(ScannerSort.MASTER_SCORE) }

    val trimmedQuery = searchQuery.trim().uppercase()
    val filteredStocks = remember(stocks, searchQuery, selectedFilter, selectedSort) {
        stocks.filter { stock ->
            val matchesQuery = stock.symbol.contains(searchQuery.trim(), ignoreCase = true) ||
                    stock.companyName.contains(searchQuery.trim(), ignoreCase = true) ||
                    stock.sector.contains(searchQuery.trim(), ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                ScannerFilter.ALL -> true
                ScannerFilter.ELITE -> stock.classification == InvestmentClassification.ELITE_CANDIDATE.label
                ScannerFilter.STRONG -> stock.classification == InvestmentClassification.STRONG_CANDIDATE.label
                ScannerFilter.GROWTH -> stock.growthScore >= 80
                ScannerFilter.VALUE -> stock.valuationScore >= 70
                ScannerFilter.DEFENSIVE -> stock.financialHealthScore >= 80 && (stock.beta ?: 1.0) < 1.1
            }
            matchesQuery && matchesFilter
        }.let { list ->
            when (selectedSort) {
                ScannerSort.MASTER_SCORE -> list.sortedByDescending { it.masterScore }
                ScannerSort.GROWTH_SCORE -> list.sortedByDescending { it.growthScore }
                ScannerSort.QUALITY_SCORE -> list.sortedByDescending { it.businessQualityScore }
                ScannerSort.VALUATION_SCORE -> list.sortedByDescending { it.valuationScore }
                ScannerSort.INSTITUTIONAL_SCORE -> list.sortedByDescending { it.institutionalScore }
                ScannerSort.PRICE -> list.sortedByDescending { it.price }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TerminalBgDark)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.onSearchQueryChanged(it) },
            placeholder = { Text("Search or enter ticker (e.g. HPE, NVDA)...", fontSize = 12.sp, color = TextMutedDark) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CyanAccent) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondaryDark)
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    if (searchQuery.isNotBlank()) {
                        viewModel.resolveStock(searchQuery) { onNavigateToResearch(it) }
                    }
                }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("scanner_search_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = TerminalSurfaceDark,
                unfocusedContainerColor = TerminalSurfaceDark,
                focusedBorderColor = CyanAccent,
                unfocusedBorderColor = TerminalBorderDark,
                focusedTextColor = TextPrimaryDark,
                unfocusedTextColor = TextPrimaryDark
            ),
            shape = RoundedCornerShape(10.dp),
            singleLine = true
        )

        // Resolving State or Error Notification
        if (isResolvingStock) {
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(3.dp),
                color = CyanAccent,
                trackColor = TerminalBorderDark
            )
            Text(
                "Resolving live market feeds and financial metrics for '$trimmedQuery'...",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = CyanAccent,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (resolveStockError != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = CrimsonRed.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonRed.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = resolveStockError ?: "",
                    fontSize = 11.sp,
                    color = CrimsonRed,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Filter chips horizontal scroll
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ScannerFilter.entries.forEach { filter ->
                val isSelected = filter == selectedFilter
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedFilter = filter },
                    label = {
                        Text(
                            text = filter.label,
                            fontSize = 11.sp,
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

        Spacer(modifier = Modifier.height(8.dp))

        // Sort Options & Results Count
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${filteredStocks.size} EQUITIES MATCHED",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = TextSecondaryDark
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ScannerSort.entries.take(3).forEach { sort ->
                    TextButton(
                        onClick = { selectedSort = sort },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = sort.label,
                            fontSize = 10.sp,
                            color = if (selectedSort == sort) GoldAccent else TextMutedDark,
                            fontWeight = if (selectedSort == sort) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Stock Cards List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // If user searched for a symbol not in current filter/cache
            if (trimmedQuery.isNotBlank() && filteredStocks.none { it.symbol == trimmedQuery }) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = TerminalSurfaceDark),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "LOOKUP '$trimmedQuery' VIA LIVE FINANCIAL APIS",
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        color = CyanAccent
                                    )
                                    Text(
                                        text = "Pull real-time quote, financial statements & candles",
                                        fontSize = 10.sp,
                                        color = TextSecondaryDark
                                    )
                                }
                                Button(
                                    onClick = { viewModel.resolveStock(trimmedQuery) { onNavigateToResearch(it) } },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp).testTag("resolve_symbol_button")
                                ) {
                                    Text("RESOLVE", color = TerminalBgDark, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            if (filteredStocks.isEmpty() && trimmedQuery.isBlank()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No stocks match current filter criteria.", color = TextMutedDark, fontSize = 12.sp)
                    }
                }
            }

            items(filteredStocks) { stock ->
                StockCard(
                    stock = stock,
                    onClick = { onNavigateToResearch(stock.symbol) }
                )
            }
        }
    }
}
