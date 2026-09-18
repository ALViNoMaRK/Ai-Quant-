package com.example.data.local

import androidx.room.*
import com.example.data.model.MarketEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MarketEventDao {
    @Query("SELECT * FROM market_events ORDER BY publishedTimestamp DESC")
    fun getAllEvents(): Flow<List<MarketEventEntity>>

    @Query("SELECT * FROM market_events ORDER BY publishedTimestamp DESC LIMIT :limit")
    fun getRecentEvents(limit: Int): Flow<List<MarketEventEntity>>

    @Query("SELECT * FROM market_events WHERE priority IN ('CRITICAL', 'HIGH') ORDER BY publishedTimestamp DESC")
    fun getMarketMovingEvents(): Flow<List<MarketEventEntity>>

    @Query("SELECT * FROM market_events WHERE primaryTicker = :symbol OR affectedTickers LIKE '%' || :symbol || '%' ORDER BY publishedTimestamp DESC")
    fun getEventsForStock(symbol: String): Flow<List<MarketEventEntity>>

    @Query("SELECT * FROM market_events WHERE primaryTicker = :symbol OR affectedTickers LIKE '%' || :symbol || '%' ORDER BY publishedTimestamp DESC")
    suspend fun getEventsForStockSync(symbol: String): List<MarketEventEntity>

    @Query("SELECT * FROM market_events WHERE eventId = :id LIMIT 1")
    suspend fun getEventById(id: String): MarketEventEntity?

    @Query("SELECT COUNT(*) FROM market_events")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<MarketEventEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: MarketEventEntity)

    @Update
    suspend fun updateEvent(event: MarketEventEntity)

    @Query("DELETE FROM market_events WHERE publishedTimestamp < :cutoffTimestamp")
    suspend fun purgeOldEvents(cutoffTimestamp: Long)
}
