package com.example.data.remote

import android.os.SystemClock
import com.example.data.model.ChartPayload
import com.example.data.model.DataFreshness
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.abs

enum class CircuitState {
    CLOSED,    // Normal operation: calls allowed
    OPEN,      // Tripped due to failures or rate-limit: calls blocked during cooldown
    HALF_OPEN  // Cooldown elapsed: single probe allowed to test recovery
}

data class ProviderCircuit(
    val providerName: String,
    var state: CircuitState = CircuitState.CLOSED,
    var consecutiveFailures: Int = 0,
    var successCount: Int = 0,
    var failureCount: Int = 0,
    var lastSuccessTime: Long = 0L,
    var lastFailureTime: Long = 0L,
    var cooldownUntilMs: Long = 0L,
    var lastErrorReason: String = "",
    var totalLatencyMs: Long = 0L,
    var latencySampleCount: Int = 0,
    var rateLimitHitCount: Int = 0
) {
    val avgLatencyMs: Long
        get() = if (latencySampleCount > 0) totalLatencyMs / latencySampleCount else 0L

    val errorRatePct: Double
        get() {
            val total = successCount + failureCount
            return if (total > 0) (failureCount.toDouble() / total) * 100.0 else 0.0
        }

    fun shouldAllowCall(): Boolean {
        val now = System.currentTimeMillis()
        return when (state) {
            CircuitState.CLOSED -> true
            CircuitState.OPEN -> {
                if (now >= cooldownUntilMs) {
                    state = CircuitState.HALF_OPEN
                    true
                } else {
                    false
                }
            }
            CircuitState.HALF_OPEN -> true
        }
    }

    fun recordSuccess(latencyMs: Long) {
        state = CircuitState.CLOSED
        consecutiveFailures = 0
        successCount++
        lastSuccessTime = System.currentTimeMillis()
        totalLatencyMs += latencyMs
        latencySampleCount++
    }

    fun recordFailure(reason: String, isRateLimit: Boolean = false, isTimeout: Boolean = false) {
        failureCount++
        consecutiveFailures++
        lastFailureTime = System.currentTimeMillis()
        lastErrorReason = reason

        val now = System.currentTimeMillis()
        if (isRateLimit) {
            rateLimitHitCount++
            state = CircuitState.OPEN
            cooldownUntilMs = now + 60_000L // 60s cooldown for rate limits
        } else if (consecutiveFailures >= 3) {
            state = CircuitState.OPEN
            cooldownUntilMs = now + 45_000L // 45s cooldown for 3 consecutive errors
        }
    }
}

object MultiProviderRouter {
    val alphaVantageProvider = AlphaVantageMcpProvider()
    val yahooProvider = YahooFinanceProvider()
    val finnhubProvider = FinnhubProvider()
    val polygonProvider = PolygonProvider()
    val fmpProvider = FmpProvider()

    // Default configurable priority: Alpha Vantage (MCP) primary, Yahoo secondary failover, then Finnhub, Polygon, FMP
    private val defaultPriority = listOf(
        "Alpha Vantage (MCP)",
        "Yahoo Finance",
        "Finnhub Stock API",
        "Polygon.io Market API",
        "Financial Modeling Prep (FMP)"
    )

    private val currentPriority = CopyOnWriteArrayList(defaultPriority)
    private val circuits = ConcurrentHashMap<String, ProviderCircuit>()
    private val discrepancyLogs = CopyOnWriteArrayList<String>()
    private val failoverAuditTrail = CopyOnWriteArrayList<String>()

    init {
        defaultPriority.forEach { name ->
            circuits[name] = ProviderCircuit(providerName = name)
        }
    }

    fun getPriority(): List<String> = currentPriority.toList()

    fun setPriority(newPriority: List<String>) {
        currentPriority.clear()
        currentPriority.addAll(newPriority)
    }

    fun resetCircuits() {
        circuits.values.forEach {
            it.state = CircuitState.CLOSED
            it.consecutiveFailures = 0
            it.cooldownUntilMs = 0L
        }
    }

    fun getCircuit(providerName: String): ProviderCircuit {
        return circuits.getOrPut(providerName) { ProviderCircuit(providerName) }
    }

    fun getAllCircuits(): Map<String, ProviderCircuit> = circuits.toMap()

    fun getDiscrepancyLogs(): List<String> = discrepancyLogs.takeLast(20)

    fun getFailoverAuditTrail(): List<String> = failoverAuditTrail.takeLast(30)

    private fun getProviderByName(name: String): MarketDataProvider? {
        return when (name) {
            "Alpha Vantage (MCP)" -> alphaVantageProvider
            "Yahoo Finance" -> yahooProvider
            "Finnhub Stock API" -> finnhubProvider
            "Polygon.io Market API" -> polygonProvider
            "Financial Modeling Prep (FMP)" -> fmpProvider
            else -> null
        }
    }

    suspend fun fetchQuoteWithFailover(
        symbol: String,
        enableDiscrepancyCheck: Boolean = true
    ): Result<NormalizedQuote> = withContext(Dispatchers.IO) {
        val attempts = mutableListOf<String>()
        var primaryQuote: NormalizedQuote? = null
        var primaryProviderName: String? = null

        for (providerName in currentPriority) {
            val provider = getProviderByName(providerName) ?: continue
            val circuit = getCircuit(providerName)

            if (!circuit.shouldAllowCall()) {
                val remainingSec = ((circuit.cooldownUntilMs - System.currentTimeMillis()) / 1000).coerceAtLeast(1)
                attempts.add("$providerName: Circuit OPEN (cooldown ${remainingSec}s remaining)")
                continue
            }

            val start = SystemClock.elapsedRealtime()
            val quoteRes = provider.fetchQuote(symbol)
            val latency = SystemClock.elapsedRealtime() - start

            if (quoteRes.isSuccess) {
                val (price, changePct) = quoteRes.getOrThrow()
                circuit.recordSuccess(latency)

                val sourceType = if (providerName == "Alpha Vantage (MCP)") "MCP_TOOL" else "REST_API"
                primaryQuote = NormalizedQuote(
                    symbol = symbol.uppercase(),
                    price = price,
                    changeAmount = price * (changePct / 100.0),
                    changePercent = changePct,
                    timestamp = System.currentTimeMillis(),
                    sourceProvider = providerName,
                    sourceType = sourceType,
                    isLive = true,
                    isCached = false,
                    freshness = DataFreshness.LIVE,
                    fallbackChain = attempts.toList()
                )
                primaryProviderName = providerName

                if (attempts.isNotEmpty()) {
                    val logEntry = "[FAILOVER] $symbol successfully resolved via $providerName after: ${attempts.joinToString("; ")}"
                    failoverAuditTrail.add(logEntry)
                }
                break
            } else {
                val exception = quoteRes.exceptionOrNull()
                val isRateLimit = exception is RateLimitException || exception?.message?.contains("rate limit", ignoreCase = true) == true
                val isTimeout = exception is java.net.SocketTimeoutException
                val reason = exception?.message ?: "Unknown error"

                circuit.recordFailure(reason, isRateLimit = isRateLimit, isTimeout = isTimeout)
                val attemptNote = "$providerName failed: $reason"
                attempts.add(attemptNote)
                failoverAuditTrail.add("[FAILURE] $symbol: $attemptNote")
            }
        }

        if (primaryQuote == null) {
            return@withContext Result.failure(
                Exception("All market data providers failed for $symbol. Attempts: ${attempts.joinToString(" | ")}")
            )
        }

        // Optional cross-provider discrepancy check against secondary provider
        var validatedQuote = primaryQuote
        if (enableDiscrepancyCheck) {
            val secondaryProviderName = currentPriority.firstOrNull { it != primaryProviderName && getCircuit(it).shouldAllowCall() }
            if (secondaryProviderName != null) {
                val secondaryProvider = getProviderByName(secondaryProviderName)
                if (secondaryProvider != null) {
                    try {
                        val secRes = secondaryProvider.fetchQuote(symbol)
                        if (secRes.isSuccess) {
                            val (secPrice, _) = secRes.getOrThrow()
                            if (secPrice > 0.0 && primaryQuote.price > 0.0) {
                                val diffPct = (abs(primaryQuote.price - secPrice) / primaryQuote.price) * 100.0
                                if (diffPct > 5.0) {
                                    val warning = "Cross-provider price divergence: ${primaryQuote.sourceProvider} ($%.2f) vs $secondaryProviderName ($%.2f) differs by %.2f%%".format(
                                        primaryQuote.price, secPrice, diffPct
                                    )
                                    discrepancyLogs.add("[DISCREPANCY $symbol] $warning")
                                    validatedQuote = primaryQuote.copy(crossCheckDiscrepancy = warning)
                                }
                            }
                        }
                    } catch (_: Exception) {
                        // Discrepancy check is non-blocking
                    }
                }
            }
        }

        Result.success(validatedQuote)
    }

    suspend fun fetchCandlesWithFailover(
        symbol: String,
        interval: String,
        range: String
    ): Result<ChartPayload> = withContext(Dispatchers.IO) {
        val attempts = mutableListOf<String>()

        for (providerName in currentPriority) {
            val provider = getProviderByName(providerName) ?: continue
            val circuit = getCircuit(providerName)

            if (!circuit.shouldAllowCall()) {
                val remainingSec = ((circuit.cooldownUntilMs - System.currentTimeMillis()) / 1000).coerceAtLeast(1)
                attempts.add("$providerName: Circuit OPEN (cooldown ${remainingSec}s)")
                continue
            }

            val start = SystemClock.elapsedRealtime()
            val candleRes = provider.fetchCandles(symbol, interval, range)
            val latency = SystemClock.elapsedRealtime() - start

            if (candleRes.isSuccess) {
                val payload = candleRes.getOrThrow()
                circuit.recordSuccess(latency)

                if (attempts.isNotEmpty()) {
                    failoverAuditTrail.add("[CANDLE FAILOVER] $symbol ($interval/$range) resolved via $providerName after: ${attempts.joinToString("; ")}")
                }

                return@withContext Result.success(payload)
            } else {
                val ex = candleRes.exceptionOrNull()
                val isRateLimit = ex is RateLimitException || ex?.message?.contains("rate limit", ignoreCase = true) == true
                val reason = ex?.message ?: "Failed"
                circuit.recordFailure(reason, isRateLimit = isRateLimit)
                attempts.add("$providerName failed: $reason")
            }
        }

        Result.failure(Exception("All candle providers exhausted for $symbol ($interval/$range). Attempts: ${attempts.joinToString(" | ")}"))
    }

    suspend fun checkAllHealth(): List<ProviderHealth> = withContext(Dispatchers.IO) {
        val list = mutableListOf<ProviderHealth>()
        for (name in currentPriority) {
            val provider = getProviderByName(name) ?: continue
            val circuit = getCircuit(name)
            val baseHealth = provider.checkHealth()

            val stateStr = circuit.state.name
            val rateLimitStr = if (circuit.rateLimitHitCount > 0) "EXCEEDED (${circuit.rateLimitHitCount}x)" else "OK"

            val enriched = baseHealth.copy(
                successCount = circuit.successCount,
                failureCount = circuit.failureCount,
                circuitState = stateStr,
                lastSuccessTime = circuit.lastSuccessTime,
                lastFailureTime = circuit.lastFailureTime,
                errorRatePct = circuit.errorRatePct,
                rateLimitStatus = rateLimitStr
            )
            list.add(enriched)
        }
        list
    }
}
