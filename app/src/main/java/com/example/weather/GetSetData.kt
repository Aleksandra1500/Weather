package com.example.weather

import com.example.pogodynka.model.OpenWeatherMapData

class GetSetData {
    companion object{
        private  var weatherData: OpenWeatherMapData? = null

        fun getData(): OpenWeatherMapData? {
            return weatherData
        }

        fun setData(newData: OpenWeatherMapData) {
            weatherData = newData
        }
    }
}