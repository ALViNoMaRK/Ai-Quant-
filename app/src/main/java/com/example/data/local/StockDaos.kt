package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AlertEntity
import com.example.data.model.PortfolioEntity
import com.example.data.model.ScoreHistoryEntity
import com.example.data.model.StockEntity
import com.example.data.model.WatchlistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StockDao {
    @Query("SELECT * FROM stocks ORDER BY masterScore DESC")
    fun getAllStocks(): Flow<List<StockEntity>>

    @Query("SELECT * FROM stocks WHERE symbol = :symbol LIMIT 1")
    suspend fun getStockBySymbol(symbol: String): StockEntity?

    @Query("SELECT * FROM stocks WHERE symbol = :symbol LIMIT 1")
    fun observeStockBySymbol(symbol: String): Flow<StockEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStocks(stocks: List<StockEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStock(stock: StockEntity)

    @Update
    suspend fun updateStock(stock: StockEntity)

    @Query("SELECT COUNT(*) FROM stocks")
    suspend fun getStockCount(): Int
}

@Dao
interface WatchlistDao {
    @Query("SELECT * FROM watchlist ORDER BY addedAt DESC")
    fun getAllWatchlist(): Flow<List<WatchlistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToWatchlist(item: WatchlistEntity)

    @Query("DELETE FROM watchlist WHERE symbol = :symbol")
    suspend fun removeFromWatchlist(symbol: String)

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE symbol = :symbol)")
    fun isInWatchlist(symbol: String): Flow<Boolean>
}

@Dao
interface PortfolioDao {
    @Query("SELECT * FROM portfolio ORDER BY addedAt DESC")
    fun getAllPortfolio(): Flow<List<PortfolioEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPosition(position: PortfolioEntity)

    @Query("DELETE FROM portfolio WHERE symbol = :symbol")
    suspend fun removePosition(symbol: String)
}

@Dao
interface AlertDao {
    @Query("SELECT * FROM alerts ORDER BY timestamp DESC")
    fun getAllAlerts(): Flow<List<AlertEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: AlertEntity)

    @Query("UPDATE alerts SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Int)

    @Query("DELETE FROM alerts WHERE id = :id")
    suspend fun deleteAlert(id: Int)

    @Query("DELETE FROM alerts")
    suspend fun clearAllAlerts()
}

@Dao
interface ScoreHistoryDao {
    @Query("SELECT * FROM score_history WHERE symbol = :symbol ORDER BY timestamp DESC")
    fun getHistoryForSymbol(symbol: String): Flow<List<ScoreHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(item: ScoreHistoryEntity)
}

@Dao
interface CustomIndicatorDao {
    @Query("SELECT * FROM custom_indicators ORDER BY updatedAt DESC")
    fun getAllCustomIndicators(): Flow<List<com.example.data.model.CustomIndicatorEntity>>

    @Query("SELECT * FROM custom_indicators WHERE id = :id LIMIT 1")
    suspend fun getIndicatorById(id: String): com.example.data.model.CustomIndicatorEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertIndicator(indicator: com.example.data.model.CustomIndicatorEntity)

    @Query("DELETE FROM custom_indicators WHERE id = :id")
    suspend fun deleteIndicator(id: String)
}

@Dao
interface ChartTemplateDao {
    @Query("SELECT * FROM chart_templates ORDER BY createdAt DESC")
    fun getAllTemplates(): Flow<List<com.example.data.model.ChartTemplateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTemplate(template: com.example.data.model.ChartTemplateEntity)

    @Query("DELETE FROM chart_templates WHERE id = :id")
    suspend fun deleteTemplate(id: String)
}

@Dao
interface CachedCandleDao {
    @Query("SELECT * FROM cached_candles WHERE symbol = :symbol AND intervalParam = :intervalParam ORDER BY timestamp ASC")
    suspend fun getCandles(symbol: String, intervalParam: String): List<com.example.data.model.CachedCandleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCandles(candles: List<com.example.data.model.CachedCandleEntity>)

    @Query("DELETE FROM cached_candles WHERE symbol = :symbol AND intervalParam = :intervalParam")
    suspend fun clearCandles(symbol: String, intervalParam: String)
}
