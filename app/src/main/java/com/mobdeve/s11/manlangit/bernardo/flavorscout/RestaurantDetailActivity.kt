package com.mobdeve.s11.manlangit.bernardo.flavorscout

import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatRatingBar
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.launch

class RestaurantDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.restaurant_view_layout)

        val restaurantId = intent.getStringExtra("RESTAURANT_ID")
        val location = intent.getParcelableExtra("LOCATION", LatLng::class.java) ?: LatLng(0.0, 0.0)

        Log.d("RestaurantDetailActivity", "Restaurant ID: $restaurantId")
        Log.d("RestaurantDetailActivity", "Location: $location")

        if (restaurantId != null) {
            lifecycleScope.launch {
                GetPlaces.initialize(this@RestaurantDetailActivity)
                val restaurant = GetPlaces.getNearbyRestaurantDetails(restaurantId, location)
                if (restaurant != null) {
                    findViewById<TextView>(R.id.restaurantNameViewTv).text = restaurant.name
                    findViewById<TextView>(R.id.addressTv).text = restaurant.address
                    findViewById<AppCompatRatingBar>(R.id.restoPriceBar).rating = restaurant.priceRange.toFloat()
                    findViewById<TextView>(R.id.restaurantTypeTv).text = restaurant.restaurantType
                    findViewById<TextView>(R.id.storeHoursTv).text = restaurant.storeHours
                    val imageView = findViewById<ImageView>(R.id.restaurantImageView)
                    Glide.with(this@RestaurantDetailActivity)
                        .load(restaurant.imageURI)
                        .into(imageView)
                } else {
                    Log.e("RestaurantDetailActivity", "Failed to fetch restaurant details")
                }
            }
        } else {
            finish()
        }
    }
}