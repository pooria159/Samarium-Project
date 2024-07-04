package com.example.samarium

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.os.Bundle
import com.google.android.gms.maps.model.Marker
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@SuppressLint("MissingPermission")
class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        dbHelper = DatabaseHelper(this)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        Log.d("MapActivity", "No data found in the database.")
        mMap = googleMap

        val data = dbHelper.getAllData()
        Log.d("MapActivity", "Data retrieved: ${data.size} entries")

        if (data.isEmpty()) {
            Log.d("MapActivity", "No data found in the database.")
            return
        }

        Log.d("MapActivity", " here u must see entry.latitude: ${data} entries")
        for (entry in data) {
            val location = LatLng(entry.latitude, entry.longitude)
            // Determine the signal quality and color based on the technology
            val signalQuality = when(entry.technology) {
                "GSM" -> getGsmSignalQuality(entry.signalStrength)
                "CDMA" -> getCdmaSignalQuality(entry.signalStrength)
                "WCDMA" -> getWcdmaSignalQuality(entry.signalStrength)
                else -> -1
            }
            val color = getColorForSignalQuality(signalQuality)

            // Format snippet with metrics
            val markerOptions = MarkerOptions()
                .position(location)
                .title("Signal Quality: ${entry.signalQuality}")
                .snippet("""
            Location: (${entry.latitude}, ${entry.longitude})
            PLMN ID: ${entry.plmnId}
            LAC: ${entry.lac}
            RAC: ${entry.rac}
            TAC: ${entry.tac}
            Cell ID: ${entry.cellId}
            Band: ${entry.band}
            ARFCAN: ${entry.arfcan}
            Signal Strength: ${entry.signalStrength} [dBm] (${getSignalQualityText(signalQuality)})
            Technology: ${entry.technology}
            Node ID: ${entry.nodeId}
            Scan Tech: ${entry.scanTech}
            Scan Serving Signal Power: ${entry.scanServingSigPow} [dBm] (${getSignalQualityText(signalQuality)})
            Distance Walked: ${entry.distanceWalked} m
            Timestamp: ${formatTimestamp(entry.timestamp)}
        """.trimIndent())
                .icon(BitmapDescriptorFactory.defaultMarker(color))

            mMap.addMarker(markerOptions)

            // Drawing a path
            val currentIndex = data.indexOf(entry)
            if (currentIndex > 0) {
                val previousLocation = LatLng(data[currentIndex - 1].latitude, data[currentIndex - 1].longitude)
                mMap.addPolyline(PolylineOptions().add(previousLocation, location).color(color.toInt()))
            }
        }

        // Move camera to the first location
        if (data.isNotEmpty()) {
            val firstLocation = LatLng(data[0].latitude, data[0].longitude)
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(firstLocation, 15f))
        }

        // Custom info window adapter
        mMap.setInfoWindowAdapter(object : GoogleMap.InfoWindowAdapter {
            override fun getInfoWindow(marker: Marker): View? {
                return null // Use default frame
            }

            override fun getInfoContents(marker: Marker): View? {
                val infoView = layoutInflater.inflate(R.layout.custom_info_window, null)
                val title = infoView.findViewById<TextView>(R.id.title)
                val snippet = infoView.findViewById<TextView>(R.id.snippet)

                title.text = marker.title
                snippet.text = marker.snippet

                return infoView
            }
        })

        mMap.setOnMarkerClickListener { marker ->
            marker.showInfoWindow()
            true
        }
    }

    fun formatTimestamp(timestamp: String): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val date = Date(timestamp.toLong())
        return sdf.format(date)
    }
    // Define signal quality ranges and corresponding colors
    fun getColorForSignalQuality(signalQuality: Int): Float {
        return when (signalQuality) {
            4 -> BitmapDescriptorFactory.HUE_BLUE   // Very good
            3 -> BitmapDescriptorFactory.HUE_GREEN  // Good
            2 -> BitmapDescriptorFactory.HUE_YELLOW // Fair
            1 -> BitmapDescriptorFactory.HUE_ORANGE // Poor
            else -> BitmapDescriptorFactory.HUE_RED // Very poor or unknown
        }
    }

    fun getGsmSignalQuality(signalStrength: Int): Int {
        return when {
            signalStrength >= -75 -> 4 // Very good
            signalStrength >= -80 -> 3  // Good
            signalStrength >= -90 -> 2  // Fair
            signalStrength > -100 -> 1   // Poor
            else -> -1 // Unknown or no signal
        }
    }

    fun getCdmaSignalQuality(signalStrength: Int): Int {
        return when {
            signalStrength >= -70 -> 4 // Very good
            signalStrength >= -80 -> 3 // Good
            signalStrength >= -90 -> 2 // Fair
            signalStrength > -100 -> 1  // Poor
            else -> -1 // Unknown or no signal
        }
    }

    fun getSignalQualityText(signalQuality: Int): String {
        return when (signalQuality) {
            4 -> "Very good"
            3 -> "Good"
            2 -> "Fair"
            1 -> "Poor"
            else -> "Very poor or unknown"
        }
    }
    fun getWcdmaSignalQuality(signalStrength: Int): Int {
        return when {
            signalStrength >= -70 -> 4 // Very good
            signalStrength >= -80 -> 3 // Good
            signalStrength >= -90 -> 2 // Fair
            signalStrength > -100 -> 1  // Poor
            else -> -1 // Unknown or no signal
        }
    }

//    private fun getColorForSignalQuality(signalQuality: Int): Float {
//        return when {
//            signalQuality <= -18 -> BitmapDescriptorFactory.HUE_RED // Very Poor
//            signalQuality <= -15 -> BitmapDescriptorFactory.HUE_ORANGE // Poor
//            signalQuality <= -12 -> BitmapDescriptorFactory.HUE_YELLOW // Fair
//            signalQuality <= 0 -> BitmapDescriptorFactory.HUE_GREEN // Good
//            else -> BitmapDescriptorFactory.HUE_BLUE // Excellent
//        }
//    }
}
