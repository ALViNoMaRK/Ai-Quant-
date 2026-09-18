package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PositionChangeType
import com.example.ui.theme.*
import com.example.ui.viewmodel.StockIntelViewModel

data class InstitutionalEntityFlow(
    val institution: String,
    val ticker: String,
    val shares: Long,
    val changePercent: Double,
    val changeType: PositionChangeType,
    val portfolioWeight: Double,
    val reportingPeriod: String,
    val filingDate: String
)

@Composable
fun BigMoneyScreen(
    viewModel: StockIntelViewModel,
    onNavigateToResearch: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val stocks by viewModel.allStocks.collectAsState()

    val institutionalFlows = remember(stocks) {
        val list = mutableListOf<InstitutionalEntityFlow>()
        stocks.forEach { stock ->
            val holdings = viewModel.repository.getInstitutionalHoldings(stock.symbol)
            holdings.forEach { h ->
                list.add(
                    InstitutionalEntityFlow(
                        institution = h.institutionName,
                        ticker = stock.symbol,
                        shares = h.shares,
                        changePercent = h.changePercent,
                        changeType = h.changeType,
                        portfolioWeight = h.portfolioWeight,
                        reportingPeriod = h.reportingPeriod,
                        filingDate = h.filingDate
                    )
                )
            }
        }
        list.sortedByDescending { it.shares }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TerminalBgDark)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = TerminalSurfaceDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorderDark),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountBalance, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "INSTITUTIONAL 13F FLOW MONITOR",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = GoldAccent,
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Tracks Berkshire Hathaway, Vanguard, BlackRock, State Street & top funds. Disclosures explicitly distinguish public filing date from historical quarter reporting period.",
                    fontSize = 11.sp,
                    color = TextSecondaryDark,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "QUARTERLY INSTITUTIONAL POSITION CHANGES (${institutionalFlows.size})",
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = TextSecondaryDark
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(institutionalFlows) { flow ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = TerminalSurfaceDark),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorderDark),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToResearch(flow.ticker) }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = flow.institution,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark,
                                fontSize = 13.sp
                            )
                            Surface(
                                color = if (flow.changeType == PositionChangeType.INCREASED) EmeraldGreen.copy(alpha = 0.2f) else AmberWarning.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = flow.changeType.name.replace("_", " "),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (flow.changeType == PositionChangeType.INCREASED) EmeraldGreen else AmberWarning,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = flow.ticker,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = CyanAccent,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${flow.shares / 1000000}M shares (${if (flow.changePercent >= 0) "+" else ""}${"%.2f".format(flow.changePercent)}%) • Weight: ${flow.portfolioWeight}%",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextSecondaryDark
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Filed: ${flow.filingDate} • Reporting Period: ${flow.reportingPeriod}",
                            fontSize = 9.sp,
                            color = TextMutedDark
                        )
                    }
                }
            }
        }
    }
}
