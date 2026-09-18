package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StockEntity
import com.example.ui.components.StockCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.StockIntelViewModel

enum class DiscoveryCategory(val label: String) {
    DISCOVER("Discover All"),
    POPULAR("Popular Today"),
    QUALITY("High Quality"),
    PREFERRED("Most Preferred"),
    MOMENTUM("Top Momentum"),
    QUANT("Top Quantitative"),
    ALL("All Equities")
}

enum class ScannerSort(val label: String) {
    MASTER_SCORE("Investment Score"),
    QUANT_SCORE("Quant Score"),
    GROWTH_SCORE("Growth Score"),
    QUALITY_SCORE("Quality Score"),
    VALUATION_SCORE("Valuation Score"),
    PRICE("Price")
}

data class PreferredCandidate(
    val stock: StockEntity,
    val score: Double,
    val reason: String
)

@Composable
fun StockScannerScreen(
    viewModel: StockIntelViewModel,
    onNavigateToResearch: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val stocks by viewModel.allStocks.collectAsState()
    val watchlist by viewModel.watchlist.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isResolvingStock by viewModel.isResolvingStock.collectAsState()
    val resolveStockError by viewModel.resolveStockError.collectAsState()

    var selectedCategory by remember { mutableStateOf(DiscoveryCategory.DISCOVER) }
    var selectedSort by remember { mutableStateOf(ScannerSort.MASTER_SCORE) }

    val recentSearches = listOf("IBM", "NVDA", "MSFT", "AAPL", "GOOGL", "INTC")
    val watchlistSymbols = remember(watchlist) { watchlist.map { it.symbol }.toSet() }

    // Precalculate Quant Scores for each stock using the QuantEngine
    val quantScores: Map<String, Double> = remember(stocks) {
        stocks.associate { stock: StockEntity ->
            stock.symbol to viewModel.repository.getQuantScore(stock).score
        }
    }

    // Dynamic categorizations
    val popularTodayStocks: List<StockEntity> = remember(stocks) {
        stocks.sortedByDescending { stock: StockEntity ->
            stock.volume.toDouble() * kotlin.math.abs(stock.changePercent)
        }
    }

    val highQualityStocks: List<StockEntity> = remember(stocks) {
        stocks.filter { it.financialHealthScore >= 75 && it.businessQualityScore >= 75 }
            .sortedByDescending { (it.financialHealthScore + it.businessQualityScore) / 2.0 }
    }

    val mostPreferredStocks: List<PreferredCandidate> = remember(stocks, quantScores) {
        stocks.map { stock: StockEntity ->
            val qScore = quantScores[stock.symbol] ?: 70.0
            val combined = (stock.masterScore * 0.6) + (qScore * 0.4)
            val reason = when (stock.symbol) {
                "NVDA" -> "86.4% Rev Growth + $57.8B FCF + F-Score 9/9"
                "MSFT" -> "Cloud Acceleration (+33%) + Durable Moat + $74B FCF"
                "IBM" -> "Hybrid Cloud & AI Backlog >$2.5B + 51% FCF Conversion"
                "AAPL" -> "Expanding Services Gross Margin + Massive $100B Buyback"
                "GOOGL" -> "Sub-20x Forward P/E + Accelerating Cloud + $100B Net Cash"
                "META" -> "High ROIC (31%) + Family of Apps Monetization + FCF Rebound"
                else -> "Strong Capital Efficiency & Institutional Backing"
            }
            PreferredCandidate(stock, combined, reason)
        }.sortedByDescending { it.score }
    }

    val topMomentumStocks: List<StockEntity> = remember(stocks) {
        stocks.sortedByDescending { it.changePercent + (it.marketScore * 0.2) }
    }

    val topQuantStocks: List<StockEntity> = remember(stocks, quantScores) {
        stocks.sortedByDescending { quantScores[it.symbol] ?: 0.0 }
    }

    val trimmedQuery = searchQuery.trim().uppercase()

    // Filtered stocks when searching or when a category tab is selected
    val filteredStocks: List<StockEntity> = remember(stocks, searchQuery, selectedCategory, selectedSort, quantScores) {
        val baseList = when (selectedCategory) {
            DiscoveryCategory.DISCOVER -> stocks
            DiscoveryCategory.POPULAR -> popularTodayStocks
            DiscoveryCategory.QUALITY -> highQualityStocks
            DiscoveryCategory.PREFERRED -> mostPreferredStocks.map { it.stock }
            DiscoveryCategory.MOMENTUM -> topMomentumStocks
            DiscoveryCategory.QUANT -> topQuantStocks
            DiscoveryCategory.ALL -> stocks
        }

        baseList.filter { stock ->
            if (trimmedQuery.isBlank()) true
            else {
                stock.symbol.contains(trimmedQuery, ignoreCase = true) ||
                        stock.companyName.contains(trimmedQuery, ignoreCase = true) ||
                        stock.sector.contains(trimmedQuery, ignoreCase = true) ||
                        stock.industry.contains(trimmedQuery, ignoreCase = true)
            }
        }.let { list ->
            when (selectedSort) {
                ScannerSort.MASTER_SCORE -> list.sortedByDescending { it.masterScore }
                ScannerSort.QUANT_SCORE -> list.sortedByDescending { quantScores[it.symbol] ?: 0.0 }
                ScannerSort.GROWTH_SCORE -> list.sortedByDescending { it.growthScore }
                ScannerSort.QUALITY_SCORE -> list.sortedByDescending { it.businessQualityScore }
                ScannerSort.VALUATION_SCORE -> list.sortedByDescending { it.valuationScore }
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

        // Header Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "STOCK DISCOVERY CENTER",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = CyanAccent,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Search & Institutional Screening",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                )
            }
            Surface(
                color = TerminalSurfaceDark,
                shape = RoundedCornerShape(6.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorderDark)
            ) {
                Text(
                    text = "${stocks.size} Equities Live",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = EmeraldGreen,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.onSearchQueryChanged(it) },
            placeholder = { Text("Search ticker, company, sector (e.g. IBM, NVDA, IT Services)...", fontSize = 11.sp, color = TextMutedDark) },
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

        // Recent Searches Chips
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent:",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = TextMutedDark,
                modifier = Modifier.padding(end = 6.dp)
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                recentSearches.forEach { ticker ->
                    Surface(
                        color = TerminalSurfaceDark,
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, TerminalBorderDark),
                        modifier = Modifier.clickable {
                            viewModel.onSearchQueryChanged(ticker)
                        }
                    ) {
                        Text(
                            text = ticker,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            color = CyanAccent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

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

        Spacer(modifier = Modifier.height(8.dp))

        // Discovery Category Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            DiscoveryCategory.entries.forEach { category ->
                val isSelected = category == selectedCategory
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategory = category },
                    label = {
                        Text(
                            text = category.label,
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

        // Content Area: Either Sectioned Discovery View or Filtered List
        if (selectedCategory == DiscoveryCategory.DISCOVER && trimmedQuery.isBlank()) {
            // Comprehensive Categorized Discovery Center
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: POPULAR TODAY
                item {
                    Column {
                        DiscoverySectionHeader(
                            title = "POPULAR TODAY",
                            subtitle = "Highest trading volume & daily market activity",
                            icon = Icons.Default.TrendingUp,
                            tint = CyanAccent
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            items(popularTodayStocks.take(6)) { stock ->
                                PopularStockMiniCard(
                                    stock = stock,
                                    isWatchlist = watchlistSymbols.contains(stock.symbol),
                                    onToggleWatchlist = { viewModel.toggleWatchlist(stock.symbol) },
                                    onClick = { onNavigateToResearch(stock.symbol) }
                                )
                            }
                        }
                    }
                }

                // Section 2: MOST PREFERRED (INSTITUTIONAL & QUANT RESEARCH)
                item {
                    Column {
                        DiscoverySectionHeader(
                            title = "MOST PREFERRED (RESEARCH CONVICTION)",
                            subtitle = "Highest dual score convergence with verified catalysts",
                            icon = Icons.Default.Verified,
                            tint = GoldAccent
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (candidate in mostPreferredStocks.take(4)) {
                                PreferredStockRowCard(
                                    stock = candidate.stock,
                                    quantScore = quantScores[candidate.stock.symbol] ?: 70.0,
                                    reason = candidate.reason,
                                    isWatchlist = watchlistSymbols.contains(candidate.stock.symbol),
                                    onToggleWatchlist = { viewModel.toggleWatchlist(candidate.stock.symbol) },
                                    onClick = { onNavigateToResearch(candidate.stock.symbol) }
                                )
                            }
                        }
                    }
                }

                // Section 3: HIGH QUALITY
                item {
                    Column {
                        DiscoverySectionHeader(
                            title = "HIGH QUALITY / BALANCE SHEET RESILIENCE",
                            subtitle = "Exceptional FCF conversion, ROIC, and low debt leverage",
                            icon = Icons.Default.Analytics,
                            tint = EmeraldGreen
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (stock in highQualityStocks.take(3)) {
                                StockCard(
                                    stock = stock,
                                    onClick = { onNavigateToResearch(stock.symbol) },
                                    isWatchlist = watchlistSymbols.contains(stock.symbol),
                                    onToggleWatchlist = { viewModel.toggleWatchlist(stock.symbol) },
                                    quantScore = quantScores[stock.symbol]
                                )
                            }
                        }
                    }
                }

                // Section 4: TOP QUANTITATIVE INTELLIGENCE
                item {
                    Column {
                        DiscoverySectionHeader(
                            title = "TOP QUANTITATIVE ENGINE SCORES",
                            subtitle = "Multi-factor statistical score, volatility-adjusted return & regime model",
                            icon = Icons.Default.Speed,
                            tint = CyanAccent
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (stock in topQuantStocks.take(3)) {
                                StockCard(
                                    stock = stock,
                                    onClick = { onNavigateToResearch(stock.symbol) },
                                    isWatchlist = watchlistSymbols.contains(stock.symbol),
                                    onToggleWatchlist = { viewModel.toggleWatchlist(stock.symbol) },
                                    quantScore = quantScores[stock.symbol]
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Search / Filtered Equities List with Sort Controls
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
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "LOOKUP '$trimmedQuery' VIA LIVE FINANCIAL APIS",
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp,
                                            color = CyanAccent
                                        )
                                        Text(
                                            text = "Pull live quote, statements, 13F holdings & quant model",
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
                        onClick = { onNavigateToResearch(stock.symbol) },
                        isWatchlist = watchlistSymbols.contains(stock.symbol),
                        onToggleWatchlist = { viewModel.toggleWatchlist(stock.symbol) },
                        quantScore = quantScores[stock.symbol]
                    )
                }
            }
        }
    }
}

@Composable
private fun DiscoverySectionHeader(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = tint
            )
            Text(
                text = subtitle,
                fontSize = 9.sp,
                color = TextMutedDark
            )
        }
    }
}

@Composable
private fun PopularStockMiniCard(
    stock: StockEntity,
    isWatchlist: Boolean,
    onToggleWatchlist: () -> Unit,
    onClick: () -> Unit
) {
    val isPos = stock.changePercent >= 0
    val changeColor = if (isPos) EmeraldGreen else CrimsonRed
    val prefix = if (isPos) "+" else ""

    Card(
        colors = CardDefaults.cardColors(containerColor = TerminalSurfaceDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorderDark),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stock.symbol,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = TextPrimaryDark
                )
                IconButton(
                    onClick = onToggleWatchlist,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (isWatchlist) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Watchlist",
                        tint = if (isWatchlist) GoldAccent else TextMutedDark,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                text = "$${"%.2f".format(stock.price)}",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = TextPrimaryDark
            )
            Text(
                text = "$prefix${"%.2f".format(stock.changePercent)}%",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                color = changeColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Vol: ${stock.volume / 1000000}M",
                fontSize = 9.sp,
                color = TextMutedDark,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun PreferredStockRowCard(
    stock: StockEntity,
    quantScore: Double,
    reason: String,
    isWatchlist: Boolean,
    onToggleWatchlist: () -> Unit,
    onClick: () -> Unit
) {
    val isPos = stock.changePercent >= 0
    val changeColor = if (isPos) EmeraldGreen else CrimsonRed
    val prefix = if (isPos) "+" else ""

    Card(
        colors = CardDefaults.cardColors(containerColor = TerminalSurfaceDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorderDark),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stock.symbol,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        color = TextPrimaryDark
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stock.companyName,
                        fontSize = 11.sp,
                        color = TextSecondaryDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleWatchlist,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isWatchlist) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Watchlist",
                            tint = if (isWatchlist) GoldAccent else TextMutedDark,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "$${"%.2f".format(stock.price)}",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = TextPrimaryDark
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$prefix${"%.2f".format(stock.changePercent)}%",
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = changeColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Dual Scores
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = GoldAccent.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(0.6.dp, GoldAccent.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "INVEST: ${stock.masterScore.toInt()}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = GoldAccent,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                    Surface(
                        color = CyanAccent.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(0.6.dp, CyanAccent.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "QUANT: ${quantScore.toInt()}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = CyanAccent,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Rationale
            Surface(
                color = Color.Black.copy(alpha = 0.3f),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Rationale: $reason",
                    fontSize = 10.sp,
                    color = TextSecondaryDark,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
