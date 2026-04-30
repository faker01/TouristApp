package com.example.touristapp.models

data class Route(
    val name: String,
    val duration: String,
    val distance: String,
    val count: String,
    val attractions: List<String>
)