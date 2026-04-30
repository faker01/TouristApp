package com.example.touristapp.models

import java.io.Serializable

data class Attraction(
    val title: String,
    val lat: Double,
    val lon: Double,
    var isSelected: Boolean = false
) : Serializable
