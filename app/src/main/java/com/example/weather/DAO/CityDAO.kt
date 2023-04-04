package com.example.weather.DAO

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.weather.entity.City

@Dao

interface CityDAO {
    @Query("SELECT name FROM city")
    fun getCity(): String

    @Insert
    suspend fun insert(city: City)

    @Update
    suspend fun update(city: City)

    @Query("DELETE FROM city")
    suspend fun deleteAll()
}