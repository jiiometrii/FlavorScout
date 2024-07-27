package com.mobdeve.s11.manlangit.bernardo.flavorscout

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

class MainActivity : AppCompatActivity(), RestaurantPreviewAdapter.OnItemClickListener {

    private lateinit var openFilterBtn: Button
    private lateinit var restaurantPreviewAdapter: RestaurantPreviewAdapter
    private lateinit var recyclerView: RecyclerView
    private var locationLatLng = LatLng(0.0, 0.0)
    private val allRestaurantPreviews = mutableListOf<Restaurant>()
    private var currentFilters = RestaurantFilters()
    private var isLoading = false
    private var searchOffset = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        requestLocationPermissions()
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e("MainActivity", "Location permissions not granted")
            return
        } else {
            Log.d("MainActivity", "Location permissions granted")
        }

        this.openFilterBtn = findViewById(R.id.openFilterBtn)
        openFilterBtn.setOnClickListener {
            openFilter()
        }

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                if (!isLoading && (visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                    && firstVisibleItemPosition >= 0) {
                    fetchRestaurantPreviews(currentFilters)
                }
            }
        })

        restaurantPreviewAdapter = RestaurantPreviewAdapter(emptyList(), this)
        recyclerView.adapter = restaurantPreviewAdapter

        lifecycleScope.launch{
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this@MainActivity)
            val location = fusedLocationClient.lastLocation.await()
            locationLatLng = LatLng(location.latitude + searchOffset, location.longitude + searchOffset)
            GetPlaces.initialize(this@MainActivity)
            fetchRestaurantPreviews(currentFilters)
        }
    }

    private fun openFilter() {
        var filters = RestaurantFilters()
        val bottomSheetDialog = BottomSheetDialog(this)
        val bottomSheetView: View = LayoutInflater.from(this).inflate(R.layout.filter_bottomsheet_layout, null)
        val distanceSeekBar: SeekBar = bottomSheetView.findViewById(R.id.distanceSeekBar)
        val distanceTv: TextView = bottomSheetView.findViewById(R.id.distanceNumTv)

        val star5: RadioButton = bottomSheetView.findViewById(R.id.star5)
        val star4: RadioButton = bottomSheetView.findViewById(R.id.star4)
        val star3: RadioButton = bottomSheetView.findViewById(R.id.star3)
        val star2: RadioButton = bottomSheetView.findViewById(R.id.star2)
        val star1: RadioButton = bottomSheetView.findViewById(R.id.star1)

        val radioButtons = listOf(star5, star4, star3, star2, star1)
        var selectedRatingButtonId: Int = -1

        for (radioButton in radioButtons) {
            radioButton.setOnClickListener {
                selectedRatingButtonId = radioButton.id
                for (rb in radioButtons) {
                    if (rb.id != selectedRatingButtonId) {
                        rb.isChecked = false
                    }
                }
            }
        }

        val priceRadioGroup = bottomSheetView.findViewById<RadioGroup>(R.id.priceRadioGroup)

        distanceSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                distanceTv.text = progress.toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        bottomSheetDialog.setContentView(bottomSheetView)
        bottomSheetDialog.show()

        bottomSheetView.findViewById<Button>(R.id.filterBtn).setOnClickListener {
            // Get the checked radio button ID here, inside the click listener
            val priceRangeButton = priceRadioGroup.checkedRadioButtonId

            val selectedPriceRange = when (priceRangeButton) {
                R.id.price1 -> 1
                R.id.price2 -> 2
                R.id.price3 -> 3
                else -> 1 // Default or handle as needed
            }
            val selectedDistance = distanceSeekBar.progress
            val selectedMinRating = when (selectedRatingButtonId) {
                R.id.star5 -> 5
                R.id.star4 -> 4
                R.id.star3 -> 3
                R.id.star2 -> 2
                R.id.star1 -> 1
                else -> 0
            }

            filters = RestaurantFilters(selectedPriceRange, selectedDistance, selectedMinRating.toDouble())
            bottomSheetDialog.dismiss()
            applyFilters(filters)
        }
    }

    private fun applyFilters(filters: RestaurantFilters) {
        currentFilters = filters
        searchOffset = 0.0
        restaurantPreviewAdapter.updateList(emptyList())
        allRestaurantPreviews.clear()
        fetchRestaurantPreviews(currentFilters)
    }

    private fun requestLocationPermissions()
    {
        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 1)
    }

    private fun fetchRestaurantPreviews(filters: RestaurantFilters) {
        locationLatLng = LatLng(locationLatLng.latitude + searchOffset, locationLatLng.longitude + searchOffset)

        if (!isLoading) {
            isLoading = true

            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val restaurantPreviews = GetPlaces.getNearbyRestaurantPreviews(
                        filters.distance.toDouble(),
                        locationLatLng,
                        filters.priceRange,
                        filters.minRating
                    )

                    Log.d("MainActivity", "Fetched ${restaurantPreviews.size} restaurants")

                    if (restaurantPreviews.isNotEmpty()) {
                        if (allRestaurantPreviews.isEmpty()) {
                            allRestaurantPreviews.clear()
                        }
                        allRestaurantPreviews.addAll(restaurantPreviews)
                        withContext(Dispatchers.Main) {
                            restaurantPreviewAdapter.updateList(allRestaurantPreviews)
                            recyclerView.post { recyclerView.requestLayout() }
                        }
                    } else {
                        isLoading = false
                    }

                    searchOffset += 0.009
                    delay(3000)
                    isLoading = false

                } catch (e: Exception) {
                    isLoading = false
                    Log.e("MainActivity", "Error fetching restaurants: ${e.message}")
                }
            }
        }
    }

    private fun getDistanceFromFilter(): Int {
        return 2
    }

    override fun onItemClick(position: Int) {
        val selectedRestaurant = allRestaurantPreviews[position]
        val intent = Intent(this, RestaurantDetailActivity::class.java)
        intent.putExtra("RESTAURANT_ID", selectedRestaurant.id)
        intent.putExtra("LOCATION", locationLatLng)
        startActivity(intent)
    }
}