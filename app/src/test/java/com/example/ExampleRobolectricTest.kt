package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.*
import com.example.engine.FinancialEngines
import com.example.engine.OpportunityDetector
import com.example.engine.DeteriorationDetector
import com.example.engine.ScoringEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Stock Intel", appName)
    }

    @Test
    fun `fundamental health engine evaluates fortress balance sheet`() {
        val statements = FinancialStatements(
            symbol = "TEST",
            period = "Q3",
            filingDate = "2024-11-20",
            revenue = 100e9,
            revenueYoY = 20.0,
            grossProfit = 70e9,
            grossMargin = 70.0,
            operatingIncome = 40e9,
            operatingMargin = 40.0,
            netIncome = 35e9,
            netMargin = 35.0,
            eps = 3.5,
            epsYoY = 25.0,
            ebitda = 45e9,
            ebitdaMargin = 45.0,
            operatingCashFlow = 42e9,
            capex = 5e9,
            freeCashFlow = 37e9,
            fcfMargin = 37.0,
            fcfYoY = 30.0,
            cashAndEquivalents = 50e9,
            totalDebt = 10e9,
            netDebt = -40e9,
            currentRatio = 3.5,
            debtToEquity = 0.2,
            netDebtToEbitda = -0.88,
            interestCoverage = 50.0,
            roe = 35.0,
            roa = 22.0,
            roic = 45.0,
            sharesOutstanding = 10e9,
            growthPace = GrowthPace.ACCELERATING,
            balanceSheetHealth = BalanceSheetHealth.IMPROVING,
            qualityGrade = BusinessQualityGrade.EXCEPTIONAL,
            valuationGrade = ValuationGrade.FAIRLY_VALUED
        )

        val (healthScore, reasons) = FinancialEngines.evaluateFundamentalHealth(statements)
        assertTrue("Health score should be >= 80 for low debt and high current ratio", healthScore >= 80.0)
        assertTrue(reasons.any { it.contains("Superior liquidity") || it.contains("Fortress balance sheet") })
    }

    @Test
    fun `opportunity detector flags accelerating company`() {
        val stock = StockEntity(
            symbol = "ACCL",
            companyName = "Accelerating Tech",
            exchange = "NASDAQ",
            sector = "Technology",
            industry = "Semiconductors",
            price = 150.0,
            changeAmount = 3.0,
            changePercent = 2.0,
            marketCap = 500e9,
            volume = 10000000L,
            avgVolume = 10000000L,
            high52 = 160.0,
            low52 = 80.0,
            peRatio = 30.0,
            forwardPe = 24.0,
            pegRatio = 1.1,
            psRatio = 10.0,
            pbRatio = 8.0,
            evToEbitda = 20.0,
            fcfYield = 3.5,
            dividendYield = 0.0,
            beta = 1.2,
            institutionalScore = 85.0
        )

        val statements = FinancialStatements(
            symbol = "ACCL",
            period = "Q3",
            filingDate = "2024-11-20",
            revenue = 50e9,
            revenueYoY = 35.0,
            grossProfit = 35e9,
            grossMargin = 70.0,
            operatingIncome = 25e9,
            operatingMargin = 50.0,
            netIncome = 22e9,
            netMargin = 44.0,
            eps = 2.2,
            epsYoY = 40.0,
            ebitda = 28e9,
            ebitdaMargin = 56.0,
            operatingCashFlow = 26e9,
            capex = 3e9,
            freeCashFlow = 23e9,
            fcfMargin = 46.0,
            fcfYoY = 45.0,
            cashAndEquivalents = 30e9,
            totalDebt = 5e9,
            netDebt = -25e9,
            currentRatio = 4.0,
            debtToEquity = 0.1,
            netDebtToEbitda = -0.89,
            interestCoverage = 80.0,
            roe = 40.0,
            roa = 25.0,
            roic = 42.0,
            sharesOutstanding = 10e9,
            growthPace = GrowthPace.ACCELERATING,
            balanceSheetHealth = BalanceSheetHealth.IMPROVING,
            qualityGrade = BusinessQualityGrade.EXCEPTIONAL,
            valuationGrade = ValuationGrade.FAIRLY_VALUED
        )

        val signals = OpportunityDetector.scanForOpportunities(listOf(Pair(stock, statements)))
        assertEquals(1, signals.size)
        assertEquals("ACCL", signals.first().symbol)
        assertTrue(signals.first().opportunityScore >= 70)
    }

    @Test
    fun `deterioration detector flags revenue contraction and negative FCF`() {
        val stock = StockEntity(
            symbol = "WEAK",
            companyName = "Weak Corp",
            exchange = "NYSE",
            sector = "Industrial",
            industry = "Machinery",
            price = 20.0,
            changeAmount = -1.0,
            changePercent = -4.7,
            marketCap = 10e9,
            volume = 5000000L,
            avgVolume = 5000000L,
            high52 = 45.0,
            low52 = 18.0,
            peRatio = null,
            forwardPe = 50.0,
            pegRatio = null,
            psRatio = 1.0,
            pbRatio = 0.8,
            evToEbitda = 15.0,
            fcfYield = -8.0,
            dividendYield = 0.0,
            beta = 1.4,
            institutionalScore = 35.0
        )

        val statements = FinancialStatements(
            symbol = "WEAK",
            period = "Q3",
            filingDate = "2024-11-20",
            revenue = 10e9,
            revenueYoY = -12.0,
            grossProfit = 2e9,
            grossMargin = 20.0,
            operatingIncome = -1.5e9,
            operatingMargin = -15.0,
            netIncome = -2.0e9,
            netMargin = -20.0,
            eps = -1.0,
            epsYoY = -150.0,
            ebitda = -0.5e9,
            ebitdaMargin = -5.0,
            operatingCashFlow = -0.8e9,
            capex = 1.2e9,
            freeCashFlow = -2.0e9,
            fcfMargin = -20.0,
            fcfYoY = -200.0,
            cashAndEquivalents = 1e9,
            totalDebt = 8e9,
            netDebt = 7e9,
            currentRatio = 0.85,
            debtToEquity = 3.5,
            netDebtToEbitda = 14.0,
            interestCoverage = 0.5,
            roe = -25.0,
            roa = -12.0,
            roic = -10.0,
            sharesOutstanding = 2e9,
            growthPace = GrowthPace.CONTRACTING,
            balanceSheetHealth = BalanceSheetHealth.WEAKENING,
            qualityGrade = BusinessQualityGrade.WEAK,
            valuationGrade = ValuationGrade.EXPENSIVE
        )

        val deterioration = DeteriorationDetector.scanForDeterioration(listOf(Pair(stock, statements)))
        assertEquals(1, deterioration.size)
        assertEquals("WEAK", deterioration.first().symbol)
        assertEquals("THESIS DETERIORATING", deterioration.first().thesisStatus)
    }
}
