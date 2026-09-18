package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.AlertEntity
import com.example.data.model.CachedCandleEntity
import com.example.data.model.ChartTemplateEntity
import com.example.data.model.CustomIndicatorEntity
import com.example.data.model.MarketEventEntity
import com.example.data.model.PortfolioEntity
import com.example.data.model.ScoreHistoryEntity
import com.example.data.model.StockEntity
import com.example.data.model.WatchlistEntity

@Database(
    entities = [
        StockEntity::class,
        WatchlistEntity::class,
        PortfolioEntity::class,
        AlertEntity::class,
        ScoreHistoryEntity::class,
        CustomIndicatorEntity::class,
        ChartTemplateEntity::class,
        CachedCandleEntity::class,
        MarketEventEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun stockDao(): StockDao
    abstract fun watchlistDao(): WatchlistDao
    abstract fun portfolioDao(): PortfolioDao
    abstract fun alertDao(): AlertDao
    abstract fun scoreHistoryDao(): ScoreHistoryDao
    abstract fun customIndicatorDao(): CustomIndicatorDao
    abstract fun chartTemplateDao(): ChartTemplateDao
    abstract fun cachedCandleDao(): CachedCandleDao
    abstract fun marketEventDao(): MarketEventDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "institutional_stock_intel.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
