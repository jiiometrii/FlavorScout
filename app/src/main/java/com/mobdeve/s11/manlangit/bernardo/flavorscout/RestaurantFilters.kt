package com.mobdeve.s11.manlangit.bernardo.flavorscout

data class RestaurantFilters(
    val priceRange: Int = 1,
    val distance: Int = 2,
    val minRating: Double = 1.0
)