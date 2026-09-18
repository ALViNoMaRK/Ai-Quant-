package com.example.data.remote

import com.example.data.model.*
import com.squareup.moshi.Json
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

// Retrofit API interfaces
interface YahooFinanceApi {
    @GET("v8/finance/chart/{symbol}")
    suspend fun getChart(
        @Path("symbol") symbol: String,
        @Query("interval") interval: String = "1d",
        @Query("range") range: String = "1mo"
    ): YahooChartResponse
}

interface SecEdgarApi {
    @GET("submissions/CIK{cik}.json")
    suspend fun getSubmissions(
        @Path("cik") cik: String
    ): SecSubmissionsResponse
}

interface FredApi {
    @GET("fred/series/observations")
    suspend fun getObservations(
        @Query("series_id") seriesId: String,
        @Query("api_key") apiKey: String,
        @Query("file_type") fileType: String = "json"
    ): FredObservationsResponse
}

interface FinnhubApi {
    @GET("api/v1/quote")
    suspend fun getQuote(
        @Query("symbol") symbol: String,
        @Query("token") token: String
    ): FinnhubQuoteResponse

    @GET("api/v1/stock/candle")
    suspend fun getCandles(
        @Query("symbol") symbol: String,
        @Query("resolution") resolution: String,
        @Query("from") from: Long,
        @Query("to") to: Long,
        @Query("token") token: String
    ): FinnhubCandleResponse

    @GET("api/v1/stock/recommendation")
    suspend fun getRecommendations(
        @Query("symbol") symbol: String,
        @Query("token") token: String
    ): List<FinnhubRecommendationItem>
}

interface PolygonApi {
    @GET("v2/aggs/ticker/{ticker}/prev")
    suspend fun getPreviousClose(
        @Path("ticker") ticker: String,
        @Query("apiKey") apiKey: String
    ): PolygonPrevResponse

    @GET("v2/aggs/ticker/{ticker}/range/{multiplier}/{timespan}/{from}/{to}")
    suspend fun getAggregates(
        @Path("ticker") ticker: String,
        @Path("multiplier") multiplier: Int,
        @Path("timespan") timespan: String,
        @Path("from") from: String,
        @Path("to") to: String,
        @Query("apiKey") apiKey: String
    ): PolygonAggResponse
}

interface FmpApi {
    @GET("api/v3/quote/{symbol}")
    suspend fun getQuote(
        @Path("symbol") symbol: String,
        @Query("apikey") apikey: String
    ): List<FmpQuoteItem>

    @GET("api/v3/historical-price-full/{symbol}")
    suspend fun getHistoricalPrice(
        @Path("symbol") symbol: String,
        @Query("apikey") apikey: String
    ): FmpHistoricalResponse

    @GET("stable/price-target-consensus")
    suspend fun getPriceTargetConsensus(
        @Query("symbol") symbol: String,
        @Query("apikey") apikey: String
    ): List<FmpPriceTargetConsensusItem>

    @GET("stable/grades")
    suspend fun getGrades(
        @Query("symbol") symbol: String,
        @Query("apikey") apikey: String
    ): List<FmpGradeItem>
}

// FRED DTOs
data class FredObservationsResponse(
    val observations: List<FredObservation>?
)

data class FredObservation(
    val date: String?,
    val value: String?
)

// Finnhub DTOs
data class FinnhubQuoteResponse(
    val c: Double?, // Current price
    val d: Double?, // Change
    val dp: Double?, // Percent change
    val h: Double?, // High
    val l: Double?, // Low
    val o: Double?, // Open
    val pc: Double? // Previous close
)

data class FinnhubCandleResponse(
    val s: String?,
    val t: List<Long>?,
    val o: List<Double>?,
    val h: List<Double>?,
    val l: List<Double>?,
    val c: List<Double>?,
    val v: List<Long>?
)

data class FinnhubRecommendationItem(
    val symbol: String?,
    val period: String?,
    val strongBuy: Int?,
    val buy: Int?,
    val hold: Int?,
    val sell: Int?,
    val strongSell: Int?
)

// Polygon DTOs
data class PolygonPrevResponse(
    val status: String?,
    val results: List<PolygonBar>?
)

data class PolygonAggResponse(
    val status: String?,
    val results: List<PolygonBar>?
)

data class PolygonBar(
    val o: Double?,
    val h: Double?,
    val l: Double?,
    val c: Double?,
    val v: Long?,
    val t: Long?
)

// FMP DTOs
data class FmpQuoteItem(
    val symbol: String?,
    val price: Double?,
    val changesPercentage: Double?,
    val change: Double?,
    val dayLow: Double?,
    val dayHigh: Double?,
    val yearHigh: Double?,
    val yearLow: Double?,
    val marketCap: Double?,
    val volume: Long?
)

data class FmpHistoricalResponse(
    val symbol: String?,
    val historical: List<FmpHistoricalBar>?
)

data class FmpHistoricalBar(
    val date: String?,
    val open: Double?,
    val high: Double?,
    val low: Double?,
    val close: Double?,
    val volume: Long?
)

data class FmpPriceTargetConsensusItem(
    val symbol: String?,
    val targetHigh: Double?,
    val targetLow: Double?,
    val targetConsensus: Double?,
    val targetMedian: Double?
)

data class FmpGradeItem(
    val symbol: String?,
    val date: String?,
    val gradingCompany: String?,
    val previousGrade: String?,
    val newGrade: String?,
    val action: String?
)

// Minimal Moshi DTOs for parsing live responses
data class YahooChartResponse(
    val chart: YahooChartData?
)

data class YahooChartData(
    val result: List<YahooChartResult>?,
    val error: YahooError?
)

data class YahooError(
    val code: String?,
    val description: String?
)

data class YahooChartResult(
    val meta: YahooMeta?,
    val timestamp: List<Long>?,
    val indicators: YahooIndicators?
)

data class YahooMeta(
    val currency: String?,
    val symbol: String?,
    val regularMarketPrice: Double?,
    val chartPreviousClose: Double?,
    val previousClose: Double?,
    val fiftyTwoWeekHigh: Double?,
    val fiftyTwoWeekLow: Double?,
    val regularMarketDayHigh: Double?,
    val regularMarketDayLow: Double?,
    val regularMarketVolume: Double?
)

data class YahooIndicators(
    val quote: List<YahooQuoteValues>?
)

data class YahooQuoteValues(
    val open: List<Double?>?,
    val high: List<Double?>?,
    val low: List<Double?>?,
    val close: List<Double?>?,
    val volume: List<Double?>?
)

data class SecSubmissionsResponse(
    val cik: String?,
    val entityType: String?,
    val name: String?,
    val filings: SecFilingsList?
)

data class SecFilingsList(
    val recent: SecRecentFilings?
)

data class SecRecentFilings(
    val form: List<String>?,
    val filingDate: List<String>?,
    val reportDate: List<String>?,
    val accessionNumber: List<String>?
)

// Gemini API DTOs
data class GeminiRequestBody(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig? = null
)

data class GeminiContent(
    val role: String? = null,
    val parts: List<GeminiPart>
)

data class GeminiPart(
    val text: String? = null
)

data class GeminiGenerationConfig(
    val temperature: Float = 0.3f,
    val maxOutputTokens: Int = 2048
)

data class GeminiResponseBody(
    val candidates: List<GeminiCandidate>?
)

data class GeminiCandidate(
    val content: GeminiContent?
)

interface GeminiApi {
    @retrofit2.http.POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @retrofit2.http.Path("model") model: String,
        @Query("key") apiKey: String,
        @retrofit2.http.Body request: GeminiRequestBody
    ): GeminiResponseBody
}

// Alpha Vantage DTOs & API Interface
interface AlphaVantageApi {
    @GET("query")
    suspend fun getGlobalQuote(
        @Query("function") function: String = "GLOBAL_QUOTE",
        @Query("symbol") symbol: String,
        @Query("apikey") apiKey: String,
        @Query("source") source: String = "alphavantagemcp",
        @Query("datatype") datatype: String = "json"
    ): AlphaVantageGlobalQuoteWrapper

    @GET("query")
    suspend fun getTimeSeriesDaily(
        @Query("function") function: String = "TIME_SERIES_DAILY",
        @Query("symbol") symbol: String,
        @Query("outputsize") outputsize: String = "compact",
        @Query("apikey") apiKey: String,
        @Query("source") source: String = "alphavantagemcp",
        @Query("datatype") datatype: String = "json"
    ): AlphaVantageTimeSeriesDailyWrapper

    @GET("query")
    suspend fun getTimeSeriesIntraday(
        @Query("function") function: String = "TIME_SERIES_INTRADAY",
        @Query("symbol") symbol: String,
        @Query("interval") interval: String = "5min",
        @Query("outputsize") outputsize: String = "compact",
        @Query("apikey") apiKey: String,
        @Query("source") source: String = "alphavantagemcp",
        @Query("datatype") datatype: String = "json"
    ): AlphaVantageTimeSeriesIntradayWrapper

    @GET("query")
    suspend fun getCompanyOverview(
        @Query("function") function: String = "OVERVIEW",
        @Query("symbol") symbol: String,
        @Query("apikey") apiKey: String,
        @Query("source") source: String = "alphavantagemcp"
    ): AlphaVantageOverview

    @GET("query")
    suspend fun getNewsSentiment(
        @Query("function") function: String = "NEWS_SENTIMENT",
        @Query("tickers") tickers: String,
        @Query("limit") limit: Int = 30,
        @Query("apikey") apiKey: String,
        @Query("source") source: String = "alphavantagemcp"
    ): AlphaVantageNewsResponse
}

data class AlphaVantageGlobalQuoteWrapper(
    @Json(name = "Global Quote") val globalQuote: AlphaVantageGlobalQuote? = null,
    @Json(name = "Information") val information: String? = null,
    @Json(name = "Note") val note: String? = null,
    @Json(name = "Error Message") val errorMessage: String? = null
)

data class AlphaVantageGlobalQuote(
    @Json(name = "01. symbol") val symbol: String? = null,
    @Json(name = "02. open") val open: String? = null,
    @Json(name = "03. high") val high: String? = null,
    @Json(name = "04. low") val low: String? = null,
    @Json(name = "05. price") val price: String? = null,
    @Json(name = "06. volume") val volume: String? = null,
    @Json(name = "07. latest trading day") val latestTradingDay: String? = null,
    @Json(name = "08. previous close") val previousClose: String? = null,
    @Json(name = "09. change") val change: String? = null,
    @Json(name = "10. change percent") val changePercent: String? = null
)

data class AlphaVantageTimeSeriesDailyWrapper(
    @Json(name = "Meta Data") val metaData: Map<String, String>? = null,
    @Json(name = "Time Series (Daily)") val timeSeriesDaily: Map<String, AlphaVantageDailyBar>? = null,
    @Json(name = "Information") val information: String? = null,
    @Json(name = "Note") val note: String? = null,
    @Json(name = "Error Message") val errorMessage: String? = null
)

data class AlphaVantageDailyBar(
    @Json(name = "1. open") val open: String? = null,
    @Json(name = "2. high") val high: String? = null,
    @Json(name = "3. low") val low: String? = null,
    @Json(name = "4. close") val close: String? = null,
    @Json(name = "5. volume") val volume: String? = null
)

data class AlphaVantageTimeSeriesIntradayWrapper(
    @Json(name = "Meta Data") val metaData: Map<String, String>? = null,
    @Json(name = "Time Series (5min)") val timeSeries5min: Map<String, AlphaVantageDailyBar>? = null,
    @Json(name = "Time Series (1min)") val timeSeries1min: Map<String, AlphaVantageDailyBar>? = null,
    @Json(name = "Time Series (15min)") val timeSeries15min: Map<String, AlphaVantageDailyBar>? = null,
    @Json(name = "Time Series (30min)") val timeSeries30min: Map<String, AlphaVantageDailyBar>? = null,
    @Json(name = "Time Series (60min)") val timeSeries60min: Map<String, AlphaVantageDailyBar>? = null,
    @Json(name = "Information") val information: String? = null,
    @Json(name = "Note") val note: String? = null,
    @Json(name = "Error Message") val errorMessage: String? = null
)

data class AlphaVantageOverview(
    @Json(name = "Symbol") val symbol: String? = null,
    @Json(name = "AssetType") val assetType: String? = null,
    @Json(name = "Name") val name: String? = null,
    @Json(name = "Description") val description: String? = null,
    @Json(name = "CIK") val cik: String? = null,
    @Json(name = "Exchange") val exchange: String? = null,
    @Json(name = "Currency") val currency: String? = null,
    @Json(name = "Country") val country: String? = null,
    @Json(name = "Sector") val sector: String? = null,
    @Json(name = "Industry") val industry: String? = null,
    @Json(name = "MarketCapitalization") val marketCap: String? = null,
    @Json(name = "EBITDA") val ebitda: String? = null,
    @Json(name = "PERatio") val peRatio: String? = null,
    @Json(name = "PEGRatio") val pegRatio: String? = null,
    @Json(name = "BookValue") val bookValue: String? = null,
    @Json(name = "DividendPerShare") val dividendPerShare: String? = null,
    @Json(name = "DividendYield") val dividendYield: String? = null,
    @Json(name = "EPS") val eps: String? = null,
    @Json(name = "RevenuePerShareTTM") val revenuePerShareTTM: String? = null,
    @Json(name = "ProfitMargin") val profitMargin: String? = null,
    @Json(name = "OperatingMarginTTM") val operatingMarginTTM: String? = null,
    @Json(name = "ReturnOnAssetsTTM") val returnOnAssetsTTM: String? = null,
    @Json(name = "ReturnOnEquityTTM") val returnOnEquityTTM: String? = null,
    @Json(name = "RevenueTTM") val revenueTTM: String? = null,
    @Json(name = "GrossProfitTTM") val grossProfitTTM: String? = null,
    @Json(name = "QuarterlyEarningsGrowthYOY") val quarterlyEarningsGrowthYOY: String? = null,
    @Json(name = "QuarterlyRevenueGrowthYOY") val quarterlyRevenueGrowthYOY: String? = null,
    @Json(name = "AnalystTargetPrice") val analystTargetPrice: String? = null,
    @Json(name = "TrailingPE") val trailingPE: String? = null,
    @Json(name = "ForwardPE") val forwardPE: String? = null,
    @Json(name = "PriceToSalesRatioTTM") val priceToSalesRatioTTM: String? = null,
    @Json(name = "PriceToBookRatio") val priceToBookRatio: String? = null,
    @Json(name = "EVToRevenue") val evToRevenue: String? = null,
    @Json(name = "EVToEBITDA") val evToEBITDA: String? = null,
    @Json(name = "Beta") val beta: String? = null,
    @Json(name = "52WeekHigh") val high52: String? = null,
    @Json(name = "52WeekLow") val low52: String? = null,
    @Json(name = "SharesOutstanding") val sharesOutstanding: String? = null,
    @Json(name = "Information") val information: String? = null,
    @Json(name = "Note") val note: String? = null,
    @Json(name = "Error Message") val errorMessage: String? = null
)

data class AlphaVantageNewsResponse(
    @Json(name = "items") val items: String? = null,
    @Json(name = "feed") val feed: List<AlphaVantageNewsArticle>? = null,
    @Json(name = "Information") val information: String? = null,
    @Json(name = "Note") val note: String? = null,
    @Json(name = "Error Message") val errorMessage: String? = null
)

data class AlphaVantageNewsArticle(
    @Json(name = "title") val title: String? = null,
    @Json(name = "url") val url: String? = null,
    @Json(name = "time_published") val timePublished: String? = null,
    @Json(name = "summary") val summary: String? = null,
    @Json(name = "source") val source: String? = null,
    @Json(name = "overall_sentiment_label") val sentimentLabel: String? = null,
    @Json(name = "overall_sentiment_score") val sentimentScore: Double? = null
)

// Normalized Quote Model for Multi-Provider Architecture
data class NormalizedQuote(
    val symbol: String,
    val price: Double,
    val changeAmount: Double,
    val changePercent: Double,
    val high: Double = 0.0,
    val low: Double = 0.0,
    val open: Double = 0.0,
    val previousClose: Double = 0.0,
    val volume: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val sourceProvider: String,
    val sourceType: String = "REST_API", // "MCP_TOOL", "REST_API", "LOCAL_CACHE"
    val isLive: Boolean = true,
    val isCached: Boolean = false,
    val freshness: DataFreshness = DataFreshness.LIVE,
    val fallbackChain: List<String> = emptyList(),
    val crossCheckDiscrepancy: String? = null
)
