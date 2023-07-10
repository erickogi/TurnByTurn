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
import com.google.android.gms.maps.model.CustomCap
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.RoundCap
import com.kogi.turnbyturn.databinding.ActivityMapsBinding
import com.kogi.turnbyturn.networkService.ApiState
import com.kogi.turnbyturn.objects.Auto
import com.kogi.turnbyturn.objects.CostingOptions
import com.kogi.turnbyturn.objects.Locations
import com.kogi.turnbyturn.objects.RequestObject
import com.kogi.turnbyturn.objects.ResponseObject
import com.kogi.turnbyturn.repository.ItemRepository
import com.kogi.turnbyturn.repository.LocationMockRepository
import com.kogi.turnbyturn.util.RouteUtils
import com.kogi.turnbyturn.viewModel.ItemViewModel
import com.kogi.turnbyturn.viewModel.ItemViewModelFactory
import kotlinx.coroutines.flow.collect
import java.util.Locale
import java.util.UUID


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var tts: TextToSpeech
    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var itemViewModel: ItemViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        itemViewModel = ViewModelProvider(
            this,
            ItemViewModelFactory(ItemRepository(), LocationMockRepository())
        )[ItemViewModel::class.java]

        val startLat = intent.getDoubleExtra("startLat", 0.0)
        val startLng = intent.getDoubleExtra("startLng", 0.0)
        val endLat = intent.getDoubleExtra("endLat", 0.0)
        val endLng = intent.getDoubleExtra("endLng", 0.0)

        binding.layoutMapData.txtInstructions.text = ""
        binding.navBtn.text = "Start"
        binding.navBtn.visibility = View.INVISIBLE
        speakInt()

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        makeAPICall(startLat,startLng,endLat,endLng)
        listenForApiCall()
    }

    var currentIndex = -1
    private fun setUpNav(coordinates: List<LatLng>, response: ResponseObject) {
        val features = RouteUtils.createFeatures(coordinates,response)
        setInitFeatureState(features)
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

    private fun setInitFeatureState(features: MutableList<RouteUtils.Feature>) {
        if(features[0].properties.instruction != null && features[0].properties.verbalPostTransitionInstruction != null) {
            binding.layoutMapData.txtInstructions.text =
                "${features[0].properties.instruction}\n${features[0].properties.verbalPostTransitionInstruction}"
        }else {
            features[0].properties.verbalPreTransitionInstruction?.let {
                binding.layoutMapData.txtInstructions.text = it
            }
        }
        if (featurePolyline == null) {
            createFeaturePolyLine(features[0].geometry.coordinates)
        } else {
            featurePolyline!!.points = features[0].geometry.coordinates
        }
        mMap.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                features[0].geometry.coordinates[0],
                featureZoomLevel
            )
        )
    }

    private var featurePolyline: Polyline? = null

    private fun createFeaturePolyLine(coordinates: List<LatLng>) {
        val polylineOptions = PolylineOptions()
            .addAll(coordinates)
            .color(Color.BLUE)
            .width(25f)
            .startCap( RoundCap())
            .endCap(RouteUtils().getEndCapIcon(this,Color.BLUE)?.let { CustomCap(it, 80F) }!!)
        .jointType(JointType.ROUND)
           // .add(fromLatLng, toLatLng);
        featurePolyline = mMap.addPolyline(polylineOptions)
        featurePolyline!!.isGeodesic = true

    }

    private fun setFeature(feature: RouteUtils.Feature, isFirst: Boolean, isLast: Boolean) {
        if(feature.properties.instruction != null && feature.properties.verbalPostTransitionInstruction != null) {
            binding.layoutMapData.txtInstructions.text =
                "${feature.properties.instruction}\n${feature.properties.verbalPostTransitionInstruction}"
        }else {
            feature.properties.verbalPreTransitionInstruction?.let {
                binding.layoutMapData.txtInstructions.text = it
            }
        }

        if (featurePolyline == null) {
            createFeaturePolyLine(feature.geometry.coordinates)
        } else {
            featurePolyline!!.points = feature.geometry.coordinates
            featurePolyline!!.startCap = RoundCap()
            featurePolyline!!.endCap = (RouteUtils().getEndCapIcon(this,Color.BLUE)?.let { CustomCap(it, 80F) }!!)
        }

        mMap.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                feature.geometry.coordinates[0],
                featureZoomLevel
            )
        )

        val textsToSpeak = java.util.ArrayList<String?>()
        textsToSpeak.add(feature.properties.instruction)
        textsToSpeak.add(feature.properties.verbalPostTransitionInstruction)
        speak(textsToSpeak)

        binding.navBtn.text = getString(R.string.next_btn_text)
        binding.navBtn.visibility = View.VISIBLE
        if (isLast) {
            binding.navBtn.text = getString(R.string.done_btn_txt)
            binding.navBtn.visibility = View.VISIBLE
        }
    }

    private fun setMapInit(coordinates: List<LatLng>, totalDistance: Double, totalTime: Double) {
        val polylineOptions = PolylineOptions()
            .addAll(coordinates)
            .color(Color.RED)
            .width(20f)
        val polyline: Polyline = mMap.addPolyline(polylineOptions)
        polyline.isGeodesic = true

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(coordinates[0], zoomLevel))

        val markerstart = coordinates[0]
        mMap.addMarker(MarkerOptions().position(markerstart).title("Start"))

        val markersend = coordinates[coordinates.size - 1]
        mMap.addMarker(MarkerOptions().position(markersend).title("End"))


        currentIndex = -1
        binding.layoutMapData.totalDistance.text = "Kms $totalDistance"
        binding.layoutMapData.totalTime.text = "ETA ${RouteUtils().formatDuration(totalTime.toLong())}"
        binding.navBtn.text = getString(R.string.start_btn_txt)
        binding.navBtn.visibility = View.VISIBLE
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
    }

    private fun speak(texts: java.util.ArrayList<String?>) {
        var count = texts.size
        var toSpeak = 0;
        val utteranceId = UUID.randomUUID().toString()
        if(texts[0]!=null) {
            tts.speak(texts[0], TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        }
        // Add a listener to the TTS engine to handle speech completion
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onDone(utteranceId: String?) {
                toSpeak++
                val toSpeakUtteranceId = UUID.randomUUID().toString()
                if(texts[toSpeak]!=null) {
                    tts.speak(texts[toSpeak], TextToSpeech.QUEUE_FLUSH, null, toSpeakUtteranceId)
                }
            }

            override fun onError(utteranceId: String?) {
            }

            override fun onStart(utteranceId: String?) {
                count--
            }
        })


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

    private fun makeAPICall(startLat:Double,startLng:Double,endLat:Double,endLng:Double) {

        val locations = mutableListOf<Locations>()
        locations.add(Locations.LocationsBuilder().lat(startLat).lon(startLng).build())
        locations.add(Locations.LocationsBuilder().lat(endLat).lon(endLng).build())

        val auto = Auto.AutoBuilder().auto(2000).build()
        val costingOptions = CostingOptions.CostingOptionsBuilder().auto(auto).build()

        itemViewModel.getRoute(
            RequestObject.Builder().locations(locations as java.util.ArrayList<Locations>)
                .units("km")
                .id("new")
                .costing("auto")
                .costingOptions(costingOptions)
                .build()
        )
    }
    private fun listenForApiCall() {
        lifecycleScope.launchWhenCreated {
            itemViewModel.wMessage.collect {
                when (it) {
                    is ApiState.Loading -> {
                        binding.srlFlowers.isRefreshing = true
                        binding.srlFlowers.isEnabled = false
                    }
                    is ApiState.Failure -> {
                        binding.srlFlowers.isRefreshing = false
                        binding.srlFlowers.isEnabled = true
                        it.e.printStackTrace()
                    }
                    is ApiState.Success -> {
                        binding.srlFlowers.isRefreshing = false
                        binding.srlFlowers.isEnabled = false
                        val myObj = it.data as ResponseObject
                        val cords = myObj.trip?.legs?.get(0)?.shape?.let { it1 -> RouteUtils().decode(it1) }
                        val coordinates = mutableListOf<LatLng>()
                        if (!cords.isNullOrEmpty()) {
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



}