package com.example.pogodynka.model

import com.google.gson.annotations.SerializedName


data class Rain (

    @SerializedName("1h" ) var oneHour : Double? = null

)