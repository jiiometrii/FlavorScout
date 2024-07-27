package com.mobdeve.s11.manlangit.bernardo.flavorscout

import android.content.Context
import android.icu.util.Calendar
import android.location.Location
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.CircularBounds
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FetchResolvedPhotoUriRequest
import com.google.android.libraries.places.api.net.FetchResolvedPhotoUriResponse
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.api.net.SearchNearbyRequest
import kotlinx.coroutines.tasks.*
import java.time.DayOfWeek
import java.time.LocalDateTime

object GetPlaces {
    private lateinit var placesClient: PlacesClient

    fun initialize(context: Context) {
        Places.initializeWithNewPlacesApiEnabled(context, "AIzaSyDdfCjAxF00wjZtIpN31W82NT9GDUkYHlY")
        placesClient = Places.createClient(context)
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val location1 = Location("").apply {
            latitude = lat1
            longitude = lon1
        }

        val location2 = Location("").apply {
            latitude = lat2
            longitude = lon2
        }

        return location1.distanceTo(location2) / 1000
    }

    private fun formatTime(time: String): String {
        return if (time.length == 4) {
            val hour = time.substring(0, 2).toInt()
            val minute = time.substring(2)
            val amPm = if (hour < 12) "AM" else "PM"
            val formattedHour = if (hour > 12) hour - 12 else hour
            "$formattedHour:$minute $amPm"
        } else {
            time
        }
    }

    private fun getCleanedRestaurantTypes(types: MutableList<String>?): String {
        return types?.filter { type ->
            !type.equals("point_of_interest", ignoreCase = true) && !type.equals("establishment", ignoreCase = true) && !type.equals("food", ignoreCase = true)
        }?.joinToString(", ") {
            it.replaceFirstChar { it.uppercase() }
                .replace("_", " ")
        } ?: "Unknown"
    }

    suspend fun getNearbyRestaurantDetails(restaurantId: String, location: LatLng): Restaurant? {
        val placeFields = listOf(
            Place.Field.ID,
            Place.Field.NAME,
            Place.Field.ADDRESS,
            Place.Field.TYPES,
            Place.Field.PHOTO_METADATAS,
            Place.Field.PRICE_LEVEL,
            Place.Field.OPENING_HOURS,
            Place.Field.LAT_LNG,
            Place.Field.RATING,
            Place.Field.REVIEWS
        )
        val calendar = Calendar.getInstance()
        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)


        return try {
            val request = FetchPlaceRequest.builder(restaurantId, placeFields).build()
            val placeResult = placesClient.fetchPlace(request).await()
            val place = placeResult.place
            Log.d("GetPlaces", "Photo Metadata: ${place.photoMetadatas}")
            val latLng = place.latLng
            val distance = calculateDistance(
                location.latitude,
                location.longitude,
                latLng.latitude,
                latLng.longitude
            )
            val metadata = place.photoMetadatas
            val photoMetadata = metadata?.firstOrNull()
            val photoResponse: FetchResolvedPhotoUriResponse
            if (photoMetadata != null) {
                val photoRequest = FetchResolvedPhotoUriRequest.builder(photoMetadata).build()
                photoResponse = placesClient.fetchResolvedPhotoUri(photoRequest).await()
            } else {
                photoResponse = FetchResolvedPhotoUriResponse.newInstance(null)
            }

            val todaysHours = if (place.openingHours != null) {
                val currentDayOfWeek = DayOfWeek.from(LocalDateTime.now())
                Log.d("GetPlaces", "Opening Hours: ${place.openingHours}")

                // Check if weekdayText indicates 24-hour opening
                val is24HoursOpen = place.openingHours.weekdayText?.any { it.contains("Open 24 hours", ignoreCase = true) } ?: false

                if (is24HoursOpen) {
                    "Open 24 hours"
                } else {
                    val todaysPeriod = place.openingHours.periods.find { period ->
                        period.open?.day == currentDayOfWeek
                    }
                    Log.d("GetPlaces", "currentDayOfWeek: $currentDayOfWeek")
                    Log.d("GetPlaces", "Todays Period: $todaysPeriod")

                    if (todaysPeriod != null) {
                        val openTime = todaysPeriod.open?.time?.let { formatTime(it.toString()) } ?: "Closed"
                        val closeTime = todaysPeriod.close?.time?.let { formatTime(it.toString()) } ?: "Closed"
                        "Today: $openTime - $closeTime"
                    } else {
                        "Closed today"
                    }
                }
            } else {
                "Store hours not available"
            }

            val restaurantType = getCleanedRestaurantTypes(place.placeTypes)

            val restaurant = Restaurant(
                id = place.id ?: "",
                name = place.name ?: "",
                address = place.address ?: "",
                restaurantType = restaurantType,
                priceRange = place.priceLevel ?: 0,
                storeHours = todaysHours,
                distance = distance,
                imageURI = photoResponse.uri,
                rating = place.rating ?: 0.0,
                reviews = (place.reviews ?: emptyList()).map { googleReview ->
                    Review(
                        name = googleReview.authorAttribution.name,
                        rating = googleReview.rating,
                        text = googleReview.text ?: ""
                    )
                }
            )
            restaurant
        } catch (e: Exception) {
            Log.e("GetPlaces", "Error fetching restaurant details", e) // Log exceptions
            null
        }
    }

    suspend fun getNearbyRestaurantPreviews(distanceKm: Double, location: LatLng, priceRange: Int?,  minRating: Double?) : List<Restaurant> {
        val placeFields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.TYPES, Place.Field.PHOTO_METADATAS, Place.Field.PRICE_LEVEL, Place.Field.LAT_LNG, Place.Field.RATING)
        val circle = CircularBounds.newInstance(location, (distanceKm * 1000).toDouble())
        val includedTypes = listOf(
            "restaurant", "cafe", "american_restaurant", "bakery",
            "barbecue_restaurant", "brazilian_restaurant", "breakfast_restaurant",
            "brunch_restaurant", "chinese_restaurant", "coffee_shop",
            "fast_food_restaurant", "french_restaurant", "greek_restaurant",
            "hamburger_restaurant", "ice_cream_shop", "indian_restaurant",
            "indonesian_restaurant", "italian_restaurant", "japanese_restaurant",
            "korean_restaurant", "lebanese_restaurant", "meal_delivery",
            "meal_takeaway", "mediterranean_restaurant", "mexican_restaurant",
            "middle_eastern_restaurant", "pizza_restaurant", "ramen_restaurant",
            "sandwich_shop", "seafood_restaurant", "spanish_restaurant",
            "steak_house", "sushi_restaurant", "thai_restaurant",
            "turkish_restaurant", "vegan_restaurant", "vegetarian_restaurant",
            "vietnamese_restaurant"
        )

        return try {
            val restaurantList = mutableListOf<Restaurant>()

            val searchNearbyRequest = SearchNearbyRequest.builder(circle, placeFields)
                .setIncludedTypes(includedTypes)
                .setMaxResultCount(20)
                .build()

            val nearbyResponse = placesClient.searchNearby(searchNearbyRequest).await()
            val places = nearbyResponse.places

            Log.d("GetPlaces", "Price Range: $priceRange")
            Log.d("GetPlaces", "Min Rating: $minRating")
            for (place in places) {
                val latLng = place.latLng
                Log.d("GetPlaces", "Place Price Level: ${place.priceLevel}")
                Log.d("GetPlaces", "Place Rating: ${place.rating}")

                    if (latLng != null) {
                        val distance = calculateDistance(
                            location.latitude,
                            location.longitude,
                            latLng.latitude,
                            latLng.longitude
                        )

                        val metadata = place.photoMetadatas
                        val photoMetadata = metadata?.firstOrNull()
                        val photoResponse: FetchResolvedPhotoUriResponse
                        if (photoMetadata != null) {
                            val photoRequest = FetchResolvedPhotoUriRequest.builder(photoMetadata).build()
                            photoResponse = placesClient.fetchResolvedPhotoUri(photoRequest).await()
                        } else {
                            photoResponse = FetchResolvedPhotoUriResponse.newInstance(null)
                        }

                        val restaurant = Restaurant(
                            id = place.id ?: "",
                            name = place.name ?: "",
                            address = place.address ?: "",
                            restaurantType = place.placeTypes?.joinToString(", ") ?: "Unknown",
                            priceRange = place.priceLevel ?: 0,
                            distance = distance,
                            imageURI = photoResponse.uri,
                            rating = place.rating ?: 0.0,
                        )
                        restaurantList.add(restaurant)
                    }
                }
            val filteredRestaurants = restaurantList.filter { restaurant ->
                (priceRange == null || restaurant.priceRange == priceRange || restaurant.priceRange == 0) &&
                        (minRating == null || restaurant.rating >= minRating)
            }
            filteredRestaurants
        } catch (e: Exception) {
            Log.e("GetPlaces", "Error fetching restaurant previews", e) // Log exceptions
            emptyList()
        }
    }
}