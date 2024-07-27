package com.mobdeve.s11.manlangit.bernardo.flavorscout

import android.net.Uri

data class Restaurant(
    val id: String,
    val name: String,
    val address: String,
    val restaurantType: String,
    val priceRange: Int,
    val storeHours: String = null.toString(),
    val distance: Float,
    val imageURI: Uri?,
    val rating: Double,
    val reviews: List<Review> = emptyList()
)

data class Review(
    val name: String? = null,
    val rating: Double,
    val text: String,
)