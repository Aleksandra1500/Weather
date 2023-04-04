package com.example.weather.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "city")
data class City(
    @PrimaryKey(autoGenerate=true)
    val id:Int,
    val name: String,
)
