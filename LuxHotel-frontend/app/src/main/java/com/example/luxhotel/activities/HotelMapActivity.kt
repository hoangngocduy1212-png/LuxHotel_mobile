package com.example.luxhotel.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.example.luxhotel.R
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class HotelMapActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private val hotelLocation = GeoPoint(10.7769, 106.7009)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // OSMDroid configuration
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        
        setContentView(R.layout.activity_hotel_map)

        mapView = findViewById(R.id.mapView)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)

        val mapController = mapView.controller
        mapController.setZoom(16.0)
        mapController.setCenter(hotelLocation)

        // Add Marker
        val marker = Marker(mapView)
        marker.position = hotelLocation
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.title = "LuxHotel – Khách sạn 5 sao"
        mapView.overlays.add(marker)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        
        findViewById<Button>(R.id.btnDirections).setOnClickListener {
            val gmmIntentUri = Uri.parse("google.navigation:q=10.7769,106.7009")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            startActivity(mapIntent)
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }
}
