package com.example.data.remote

import com.example.data.model.*
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
