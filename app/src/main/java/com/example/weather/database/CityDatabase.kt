package com.example.weather.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.weather.DAO.CityDAO
import com.example.weather.entity.City

@Database(
    entities = [City::class],
    version = 1,
    exportSchema = false
)

abstract class CityDatabase : RoomDatabase() {
    abstract fun cityDAO(): CityDAO

    companion object {
        @Volatile
        private var INSTANCE: CityDatabase? = null

        fun getDatabase(context: Context): CityDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }

            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CityDatabase::class.java,
                    "city_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                return instance
            }
        }
    }
}