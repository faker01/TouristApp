package com.example.touristapp.models

import java.io.Serializable

data class Attraction(
    val id: Int = 0,
    val title: String,
    val lat: Double,
    val lon: Double,
    val description: String = "",
    var isSelected: Boolean = false
) : Serializable