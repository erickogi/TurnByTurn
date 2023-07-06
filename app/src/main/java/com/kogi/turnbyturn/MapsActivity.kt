package com.kogi.turnbyturn

import android.graphics.Color
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.kogi.turnbyturn.databinding.ActivityMapsBinding
import com.kogi.turnbyturn.networkService.ApiState
import com.kogi.turnbyturn.objects.*
import com.kogi.turnbyturn.repository.ItemRepository
import com.kogi.turnbyturn.viewModel.ItemViewModel
import com.kogi.turnbyturn.viewModel.ItemViewModelFactory
import kotlinx.coroutines.flow.collect
import java.util.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var tts: TextToSpeech
    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var itemViewModel: ItemViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val startLat = intent.getDoubleExtra("startLat", 0.0)
        val startLng = intent.getDoubleExtra("startLng", 0.0)
        val endLat = intent.getDoubleExtra("endLat", 0.0)
        val endLng = intent.getDoubleExtra("endLng", 0.0)

        binding.txtInstructions.text = ""
        binding.navBtn.text = "Start"
        binding.navBtn.visibility = View.INVISIBLE
        speakInt()

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        itemViewModel = ViewModelProvider(
            this,
            ItemViewModelFactory(ItemRepository())
        )[ItemViewModel::class.java]
        val locations = mutableListOf<Locations>()
        locations.add(Locations.LocationsBuilder().lat(startLat).lon(startLng).build())
        locations.add(Locations.LocationsBuilder().lat(endLat).lon(endLng).build())

        val auto = Auto.AutoBuilder().auto(2000).build()
        val costingOptions = CostingOptions.CostingOptionsBuilder().auto(auto).build()

        itemViewModel.getRoute(
            RequestObject.Builder().locations(locations as ArrayList<Locations>)
                .units("km")
                .id("new")
                .costing("auto")
                .costingOptions(costingOptions)
                .build()
        )
        lifecycleScope.launchWhenCreated {
            itemViewModel.wMessage.collect {
                when (it) {
                    is ApiState.Loading -> {
                        binding.srlFlowers.isRefreshing = true
                    }
                    is ApiState.Failure -> {
                        binding.srlFlowers.isRefreshing = false
                        it.e.printStackTrace()
                    }
                    is ApiState.Success -> {
                        binding.srlFlowers.isRefreshing = false
                        val myObj = it.data as ResponseObject
                        val cords = myObj.trip?.legs?.get(0)?.shape?.let { it1 -> decode(it1) }
                        val coordinates = mutableListOf<LatLng>()
                        if (cords != null && cords.isNotEmpty()) {
                            for (i in cords)
                                coordinates.add(LatLng(i[0], i[1]))
                        }
                        myObj.trip?.summary?.length?.let { it1 ->
                            myObj.trip!!.summary?.time?.let { it2 ->
                                setMapInit(
                                    coordinates,
                                    it1, it2
                                )
                            }
                        }
                        setUpNav(coordinates, myObj)

                    }
                    is ApiState.Empty -> {
                        println("Empty...")
                    }
                }
            }
        }

    }

    var currentIndex = -1;
    private fun setUpNav(coordinates: List<LatLng>, response: ResponseObject) {
        val features = mutableListOf<Feature>()
        response.trip?.legs?.forEachIndexed { index, leg ->
            val route = coordinates

            leg.maneuvers.forEach { maneuver ->
                //val maneuverx =Maneuvers(maneuver.beginShapeIndex,maneuver.endShapeIndex,index)

                maneuver.leg = index
                //val (beginShapeIndex, endShapeIndex: Int?) = maneuver
                val beginShapeIndex = maneuver.beginShapeIndex;
                val endShapeIndex = maneuver.endShapeIndex;

                val coordinates = route.subList(beginShapeIndex!!, endShapeIndex!! + 1)
                val geometry = if (coordinates.size > 1) {
                    Geometry("LineString", coordinates)
                } else {
                    Geometry("Point", coordinates)
                }

                features.add(Feature("Feature", maneuver, geometry))
            }
        }

        binding.navBtn.setOnClickListener {
            if (currentIndex == -1 && features.size > 0) {
                currentIndex = 0
                setFeature(features[currentIndex], true, currentIndex >= features.size - 1)
            } else if (currentIndex < features.size) {
                currentIndex += 1
                setFeature(features[currentIndex], false, currentIndex >= features.size - 1)
            }
        }
    }

    var featurePolyline: Polyline? = null

    private fun createFeaturePolyLine(coordinates: List<LatLng>) {
        val polylineOptions = PolylineOptions()
            .addAll(coordinates)
            .color(Color.BLUE)
            .width(15f)
        featurePolyline = mMap.addPolyline(polylineOptions)

    }

    private fun setFeature(feature: Feature, isFirst: Boolean, isLast: Boolean) {
        feature.properties.instruction?.let { binding.txtInstructions.setText(it) }

        if (featurePolyline == null) {
            createFeaturePolyLine(feature.geometry.coordinates)
        } else {
            featurePolyline!!.points = feature.geometry.coordinates
        }
        mMap.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                feature.geometry.coordinates[0],
                featureZoomLevel
            )
        )

        speak(feature.properties.instruction)

        binding.navBtn.text = "Next"
        binding.navBtn.visibility = View.VISIBLE
        if (isLast) {
            binding.navBtn.text = "Done"
            binding.navBtn.visibility = View.VISIBLE
        }
    }

    private fun setMapInit(coordinates: List<LatLng>, totalDistance: Double, totalTime: Double) {
        val polylineOptions = PolylineOptions()
            .addAll(coordinates)
            .color(Color.RED)
            .width(10f)
        val polyline: Polyline = mMap.addPolyline(polylineOptions)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(coordinates[0], zoomLevel))

        val markerstart = coordinates[0]
        mMap.addMarker(MarkerOptions().position(markerstart).title("Start"))

        val markersend = coordinates[coordinates.size - 1]
        mMap.addMarker(MarkerOptions().position(markersend).title("End"))


        currentIndex = 0;
        binding.totalDistance.text = "Kms $totalDistance"
        binding.totalTime.text = "ETA ${formatDuration(totalTime.toLong())}"
        binding.navBtn.text = "Start"
        binding.navBtn.visibility = View.VISIBLE
    }

    fun formatDuration(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val remainingSeconds = seconds % 60

        return String.format("%02d:%02d:%02d", hours, minutes, remainingSeconds)
    }

    private fun speakInt() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts.setLanguage(Locale.US)

                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(this@MapsActivity, "Language not supported", Toast.LENGTH_SHORT)
                        .show()
                }
            } else {
                Toast.makeText(this@MapsActivity, "Initialization failed", Toast.LENGTH_SHORT)
                    .show()
            }
        }
        // Add a listener to the TTS engine to handle speech completion
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onDone(utteranceId: String?) {
                // Speech completed
            }

            override fun onError(utteranceId: String?) {
                // Speech error occurred
            }

            override fun onStart(utteranceId: String?) {
                // Speech started
            }
        })
    }

    private fun speak(text: String?) {
        if (text != null) {
            val utteranceId = UUID.randomUUID().toString()
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tts.shutdown()
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    private val featureZoomLevel = 17.0f
    private val zoomLevel = 20.0f
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Add a marker in Sydney and move the camera
        val kampala = LatLng(0.34, 32.58)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(kampala, zoomLevel))

    }

    fun decode(str: String): List<DoubleArray>? {
        val precision = 6
        var index = 0
        var lat = 0.0
        var lng = 0.0
        val coordinates: MutableList<DoubleArray> = ArrayList()
        var shift = 0
        var result = 0
        var byteValue: Int
        var latitudeChange: Int
        var longitudeChange: Int
        val factor = Math.pow(10.0, precision.toDouble())
        while (index < str.length) {
            //byteValue = str.charAt(index++) - 63;
            shift = 0
            result = 0
            do {
                byteValue = str[index++].code - 63
                result = result or (byteValue and 0x1f shl shift)
                shift += 5
            } while (byteValue >= 0x20)
            latitudeChange = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            shift = 0
            result = 0
            do {
                byteValue = str[index++].code - 63
                result = result or (byteValue and 0x1f shl shift)
                shift += 5
            } while (byteValue >= 0x20)
            longitudeChange = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += latitudeChange.toDouble()
            lng += longitudeChange.toDouble()
            coordinates.add(doubleArrayOf(lat / factor, lng / factor))
        }
        return coordinates
    }

    data class Feature(
        val type: String,
        val properties: Maneuvers,
        val geometry: Geometry
    )

    data class Geometry(
        val type: String,
        val coordinates: List<LatLng>
    )

}