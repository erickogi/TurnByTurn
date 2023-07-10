package com.kogi.turnbyturn

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.os.Bundle
import android.preference.PreferenceManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.model.LatLng
import com.kogi.turnbyturn.databinding.ActivityOsmmapBinding
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
import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration.getInstance
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.milestones.MilestoneManager
import org.osmdroid.views.overlay.milestones.MilestonePathDisplayer
import org.osmdroid.views.overlay.milestones.MilestonePixelDistanceLister
import java.util.Locale
import java.util.UUID


class OSMMAP : AppCompatActivity() {
    private lateinit var map : MapView
    private lateinit var binding: ActivityOsmmapBinding
    private lateinit var tts: TextToSpeech
    private lateinit var itemViewModel: ItemViewModel
    private var mapController: IMapController? = null
    private val featureZoomLevel = 20.0
    private val zoomLevel = 20.0
    private var lastETACallTimestamp: Long?= null
    private var etaCallsMinInterval = 2
    private var minCurrentCoordinatesForAPreAlert = 3
    private var minNextCoordinatesForAPreAlert = 3



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        getInstance().userAgentValue = this.packageName
        setContentView(R.layout.activity_osmmap)
        binding = ActivityOsmmapBinding.inflate(layoutInflater)
        setContentView(binding.root)
        map = findViewById(R.id.osrm_map)
        map.setTileSource(TileSourceFactory.MAPNIK)
        itemViewModel = ViewModelProvider(
            this,
            ItemViewModelFactory(ItemRepository(), LocationMockRepository())
        )[ItemViewModel::class.java]

        val startLat = intent.getDoubleExtra("startLat", 0.0)
        val startLng = intent.getDoubleExtra("startLng", 0.0)
        val endLat = intent.getDoubleExtra("endLat", 0.0)
        val endLng = intent.getDoubleExtra("endLng", 0.0)

        binding.osrmLayoutMapData.txtInstructions.text = ""
        binding.osrmNavBtn.text = "Start"
        binding.osrmNavBtn.visibility = View.GONE
        speakInt()
        makeAPICall(startLat,startLng,endLat,endLng)
        listenForApiCall()
        initMap(map)
    }

    private fun initMap(map: MapView?) {
        map?.setTileSource(TileSourceFactory.MAPNIK)
        map?.setBuiltInZoomControls(true)
        map?.setMultiTouchControls(true)
        mapController = map!!.controller
        mapController?.setZoom(zoomLevel)
        val startPoint = GeoPoint(0.34, 32.58)
        mapController?.setCenter(startPoint)
    }

    private var currentIndex = -1
    private var features: MutableList<RouteUtils.Feature> = ArrayList()
    private fun setUpNav(coordinates: List<LatLng>, response: ResponseObject) {
        features = RouteUtils.createFeatures(coordinates,response)
        setInitFeatureState(features)
        binding.osrmNavBtn.setOnClickListener {
            if (currentIndex == -1 && features.size > 0) {
                currentIndex = 0
                setFeature(features[currentIndex], true, currentIndex >= features.size - 1)
            } else if (currentIndex < features.size) {
                currentIndex += 1
                setFeature(features[currentIndex], false, currentIndex >= features.size - 1)

            }
            makeAPICallETA(features[currentIndex].geometry.coordinates[0],features[features.size-1].geometry.coordinates)
        }
    }

    private fun setInitFeatureState(features: MutableList<RouteUtils.Feature>) {
        if(features[0].properties.instruction != null && features[0].properties.verbalPostTransitionInstruction != null) {
            binding.osrmLayoutMapData.txtInstructions.text =
                "${features[0].properties.instruction}\n${features[0].properties.verbalPostTransitionInstruction}"
        }else {
            features[0].properties.verbalPreTransitionInstruction?.let {
                binding.osrmLayoutMapData.txtInstructions.text = it
            }
        }
        if (featurePolyline == null) {
            createFeaturePolyLine(features[0].geometry.coordinates)
        } else {
            featurePolyline!!.setPoints(convertCoordinatesToGeoPoints(features[0].geometry.coordinates))
        }
        mapController?.setZoom(featureZoomLevel)
        val startPoint = convertCoordinateToGeoPoint(features[0].geometry.coordinates[0])
        mapController?.setCenter(startPoint)
    }

    private var featurePolyline: Polyline? = null

    private fun createFeaturePolyLine(coordinates: List<LatLng>) {
        val arrowPaint = Paint()
        arrowPaint.color = Color.BLUE
        arrowPaint.strokeWidth = 10.0f
        arrowPaint.style = Paint.Style.FILL_AND_STROKE
        arrowPaint.isAntiAlias = true
        val arrowPath = Path() // a simple arrow towards the right

        arrowPath.moveTo(-10F, -10F)
        arrowPath.lineTo(10F, 0F)
        arrowPath.lineTo(-10F, 10F)
        arrowPath.close()
        val managers: MutableList<MilestoneManager> = ArrayList()
        managers.add(
            MilestoneManager(
                MilestonePixelDistanceLister(50.0, 50.0),
                MilestonePathDisplayer(0.0, true, arrowPath, arrowPaint)
            )
        )

        val geoPoints = convertCoordinatesToGeoPoints(coordinates)
        featurePolyline = Polyline()   //see note below!
        featurePolyline!!.setPoints(geoPoints)
        featurePolyline!!.outlinePaint.color = Color.BLUE
        featurePolyline!!.outlinePaint.strokeWidth = 18F
        featurePolyline!!.outlinePaint.strokeCap = Paint.Cap.ROUND
        featurePolyline!!.outlinePaint.strokeJoin = Paint.Join.ROUND
        featurePolyline!!.isGeodesic = true
        featurePolyline!!.setMilestoneManagers(managers)

        map.overlays.add(featurePolyline)
    }

    private var preFeaturePolyline: Polyline? = null

    private fun createPreFeaturePolyLine(coordinates: List<LatLng>) {
        val arrowPaint = Paint()
        arrowPaint.color = Color.GREEN
        arrowPaint.strokeWidth = 10.0f
        arrowPaint.style = Paint.Style.FILL_AND_STROKE
        arrowPaint.isAntiAlias = true
        val arrowPath = Path() // a simple arrow towards the right

        arrowPath.moveTo(-10F, -10F)
        arrowPath.lineTo(10F, 0F)
        arrowPath.lineTo(-10F, 10F)
        arrowPath.close()
        val managers: MutableList<MilestoneManager> = ArrayList()
        managers.add(
            MilestoneManager(
                MilestonePixelDistanceLister(50.0, 50.0),
                MilestonePathDisplayer(0.0, true, arrowPath, arrowPaint)
            )
        )

        val geoPoints = convertCoordinatesToGeoPoints(coordinates)
        preFeaturePolyline = Polyline()   //see note below!
        preFeaturePolyline!!.setPoints(geoPoints)
        preFeaturePolyline!!.outlinePaint.color = Color.GREEN
        preFeaturePolyline!!.outlinePaint.strokeWidth = 18F
        preFeaturePolyline!!.outlinePaint.strokeCap = Paint.Cap.ROUND
        preFeaturePolyline!!.outlinePaint.strokeJoin = Paint.Join.ROUND
        preFeaturePolyline!!.isGeodesic = true
        preFeaturePolyline!!.setMilestoneManagers(managers)
        map.overlays.add(preFeaturePolyline)
    }


    private fun setFeature(feature: RouteUtils.Feature, isFirst: Boolean, isLast: Boolean) {
        preFeaturePolyline?.isVisible = false
        binding.osrmLayoutMapData.txtPreInstructions.visibility = View.GONE
        if(feature.properties.instruction != null && feature.properties.verbalPostTransitionInstruction != null) {
            binding.osrmLayoutMapData.txtInstructions.text =
                "${feature.properties.instruction}\n${feature.properties.verbalPostTransitionInstruction}"
        }else {
            feature.properties.verbalPreTransitionInstruction?.let {
                binding.osrmLayoutMapData.txtInstructions.text = it
            }
        }

        if (featurePolyline == null) {
            createFeaturePolyLine(feature.geometry.coordinates)
        } else {
            featurePolyline!!.setPoints(convertCoordinatesToGeoPoints(feature.geometry.coordinates))
            featurePolyline!!.outlinePaint.strokeCap = Paint.Cap.ROUND
        }
        featurePolyline!!.title =feature.geometry.streetNames


        mapController?.setZoom(featureZoomLevel)
        val startPoint = convertCoordinateToGeoPoint(feature.geometry.coordinates[0])
        mapController?.setCenter(startPoint)

        val textsToSpeak = java.util.ArrayList<String?>()
        textsToSpeak.add(feature.properties.verbalPreTransitionInstruction)
        textsToSpeak.add(feature.properties.verbalPostTransitionInstruction)

        speak(textsToSpeak)

        binding.osrmNavBtn.text = getString(R.string.next_btn_text)
        binding.osrmNavBtn.visibility = View.GONE
        if (isLast) {
            binding.osrmNavBtn.text = getString(R.string.done_btn_txt)
            binding.osrmNavBtn.visibility = View.GONE
        }
//        val bearing: Float = feature.geometry.coordinates[0].getBearing()
//
//        var t = 360 - bearing
//        if (t < 0) {
//            t += 360f
//        }
//        if (t > 360) {
//            t -= 360f
//        }
//
////help smooth everything out
//
////help smooth everything out
//        t = t.toInt().toFloat()
//        t = t / 5
//        t = t.toInt().toFloat()
//        t = t * 5

//        map.setMapOrientation(t)
    }
//    fun rotateToNextCheckPoint(): Double {
//        val lineId: Int = OfferPreference.getInstance().getCurrentLineId()
//        if (rotates != null && lineId >= 0 && road.mRouteHigh.size() > 0) {
//            if (lineId < rotates.size() - 1) {
//                val nextPoint: GeoPoint = rotates.get(lineId).getLast()
//                val currPoint: GeoPoint = rotates.get(lineId).getFirst()
//                if (nextPoint == null || currPoint == null) {
//                    return 0
//                }
//                val lat1 = Math.toRadians(currPoint.latitude)
//                val lon1 = Math.toRadians(currPoint.longitude)
//                val lat2 = Math.toRadians(nextPoint.latitude)
//                val lon2 = Math.toRadians(nextPoint.longitude)
//                val cos1 = Math.cos(lat1)
//                val cos2 = Math.cos(lat2)
//                val sin1 = Math.sin(lat1)
//                val sin2 = Math.sin(lat2)
//                val delta = lon2 - lon1
//                val deltaCos = Math.cos(delta)
//                val deltaSin = Math.sin(delta)
//                val x = cos1 * sin2 - sin1 * cos2 * deltaCos
//                val y = deltaSin * cos2
//                var z = Math.toDegrees(Math.atan(-y / x))
//                if (x < 0) {
//                    z += 180.0
//                }
//                var z2 = (z + 180) % 360 - 180
//                z2 = -Math.toRadians(z2)
//                val angleRad = z2 - Math.PI * 2 * Math.floor(z2 / (2 * Math.PI))
//                val angle = Math.toDegrees(angleRad)
//                rotationGestureOverlay.onRotate(-angle.toFloat(), false)
//                return angle
//            }
//        }
//        return 0
//    }

    private fun setPreManeuver(feature: RouteUtils.Feature) {
        preFeaturePolyline?.isVisible = true
        binding.osrmLayoutMapData.txtPreInstructions.visibility = View.VISIBLE
        if(feature.properties.instruction != null) {
            binding.osrmLayoutMapData.txtPreInstructions.text =
                "Coming up, ${feature.properties.instruction}"
        }

        if (preFeaturePolyline == null) {
            createPreFeaturePolyLine(feature.geometry.coordinates)
        } else {
            preFeaturePolyline!!.setPoints(convertCoordinatesToGeoPoints(feature.geometry.coordinates))
            preFeaturePolyline!!.outlinePaint.strokeCap = Paint.Cap.ROUND
        }
        preFeaturePolyline!!.title =feature.geometry.streetNames

        val textsToSpeak = java.util.ArrayList<String?>()
        textsToSpeak.add("Coming up, "+feature.properties.instruction)
        speak(textsToSpeak)

    }

    private var markerMyLocation: Marker? = null
    private fun createMyLocationMarker(loc: LatLng){
        val drawable = ContextCompat.getDrawable(this, R.drawable.baseline_my_location)

        markerMyLocation = Marker(map)
        markerMyLocation!!.position = convertCoordinateToGeoPoint(loc)
        markerMyLocation!!.icon = drawable
        markerMyLocation!!.title = "My Location"
        markerMyLocation!!.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        map.overlays.add(markerMyLocation)
        map.invalidate()
    }
    private fun myLocation(loc: LatLng){
        mapController?.setZoom(zoomLevel)
        val startPoint = GeoPoint(loc.latitude, loc.longitude)
        mapController?.setCenter(startPoint)

        if(markerMyLocation != null){
            markerMyLocation!!.position = convertCoordinateToGeoPoint(loc)
        }else{
            createMyLocationMarker(loc)
        }


        var currentFeature = features[0]
        var nextFeature = features[0]
        var comparable = currentFeature.geometry.coordinates[0]
        var comparablePre = currentFeature.geometry.coordinates[0]
        if(currentIndex>-1) {
            currentFeature = features[currentIndex]
            comparable = currentFeature.geometry.coordinates[currentFeature.geometry.coordinates.size-1]
            comparablePre = currentFeature.geometry.coordinates.getOrElse(currentFeature.geometry.coordinates.size-2) { comparable }
            makeAPICallETA(
                loc,
                features[features.size - 1].geometry.coordinates
            )
        }


        //Current Location is close to the next maneuver
        println("calculated_line_distance ->"+currentFeature.properties.instruction)
        if(loc == comparable  && RouteUtils().calculateDistance(loc,comparable)<=0){
            if (currentIndex == -1 && features.size > 0) {
                currentIndex = 0
                nextFeature = features[currentIndex]
                setFeature(nextFeature, true, currentIndex >= features.size - 1)
            } else if (currentIndex < features.size) {
                currentIndex += 1
                nextFeature = features[currentIndex]
                setFeature(features[currentIndex], false, currentIndex >= features.size - 1)
            }
            makeAPICallETA(features[currentIndex].geometry.coordinates[0],features[features.size-1].geometry.coordinates)
        }else if(
            loc == comparablePre
            &&
//            currentFeature.geometry.coordinates.size>=minCurrentCoordinatesForAPreAlert
//            &&
//            nextFeature.geometry.coordinates.size>=minNextCoordinatesForAPreAlert
//            &&
            RouteUtils().calculateDistance(loc,comparable)<=50
        ){
            val preIndex = currentIndex+1;
            setPreManeuver(features[preIndex])
        }
    }



    private fun setMapInit(coordinates: List<LatLng>, totalDistance: Double, totalTime: Double) {

        mapController?.setZoom(zoomLevel)
        val startPoint = GeoPoint(coordinates[0].latitude, coordinates[0].longitude)
        mapController?.setCenter(startPoint)

        //ADD MARKERS
        val markerstart = Marker(map)
        markerstart.position = convertCoordinateToGeoPoint(coordinates[0])
        markerstart.title = "Start"
        markerstart.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        map.overlays.add(markerstart)
        map.invalidate()

        val markerend = Marker(map)
        markerend.position = convertCoordinateToGeoPoint(coordinates[coordinates.size - 1])
        markerend.title = "End"
        markerend.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        map.overlays.add(markerend)
        map.invalidate()

        //ADD POLYLINE
        val geoPoints = convertCoordinatesToGeoPoints(coordinates)
        val line = Polyline()   //see note below!
        line.setPoints(geoPoints)
        line.outlinePaint.color = Color.RED
        line.outlinePaint.strokeWidth = 13F
        line.outlinePaint.strokeCap = Paint.Cap.ROUND
        line.outlinePaint.strokeJoin = Paint.Join.ROUND
        line.isGeodesic = true
        map.overlays.add(line)




        currentIndex = -1

        binding.osrmNavBtn.text = getString(R.string.start_btn_txt)
        binding.osrmNavBtn.visibility = View.GONE

        setTitle(totalDistance,totalTime)
        setRemainder(totalDistance,totalTime)
    }

    private fun setRemainder(totalDistance: Double,totalTime: Double) {
        binding.osrmLayoutMapData.totalDistance.text = "Kms ${RouteUtils().roundToOneDecimalPlace(totalDistance)}"
        binding.osrmLayoutMapData.totalTime.text = "ETA ${RouteUtils().formatDuration(totalTime.toLong())}"
    }

    private fun setTitle(totalDistance: Double,totalTime: Double) {
        supportActionBar?.title = "Total -> KMs ${RouteUtils().roundToOneDecimalPlace(totalDistance)} ETA ${RouteUtils().formatDuration(totalTime.toLong())}"
    }


    private fun convertCoordinatesToGeoPoints(coordinates: List<LatLng>): MutableList<GeoPoint> {

        var geoPoints = mutableListOf<GeoPoint>()
        for (coordinate in coordinates){
            geoPoints.add(GeoPoint(coordinate.latitude,coordinate.longitude))
        }
        return geoPoints
    }

    private fun convertCoordinateToGeoPoint(coordinate: LatLng): GeoPoint {

        return GeoPoint(coordinate.latitude,coordinate.longitude)
    }





    private fun listenForApiCall() {
        lifecycleScope.launchWhenCreated {
            itemViewModel.wMessage.collect {
                when (it) {
                    is ApiState.Loading -> {
                        binding.osrmSrlFlowers.isEnabled= true
                        binding.osrmSrlFlowers.isRefreshing = true
                    }
                    is ApiState.Failure -> {
                        binding.osrmSrlFlowers.isEnabled= true
                        binding.osrmSrlFlowers.isRefreshing = false
                        it.e.printStackTrace()
                    }
                    is ApiState.Success -> {
                        binding.osrmSrlFlowers.isRefreshing = false
                        binding.osrmSrlFlowers.isEnabled= false
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
                                makeLocationStreamEmulator(coordinates,it1,it2)
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
        lifecycleScope.launchWhenCreated {
            itemViewModel.wMessageETA.collect {
                when (it) {
                    is ApiState.Loading -> {
                        println("ETA Loading...")
                        binding.osrmSrlFlowers.isRefreshing = false
                    }
                    is ApiState.Failure -> {
                        println("ETA Failure...")
                        binding.osrmSrlFlowers.isRefreshing = false
                        it.e.printStackTrace()
                    }
                    is ApiState.Success -> {
                        println("ETA SUCCESS...")
                        binding.osrmSrlFlowers.isRefreshing = false
                        val myObj = it.data as ResponseObject
                        myObj.trip?.summary?.length?.let { it1 ->
                            myObj.trip!!.summary?.time?.let { it2 ->
                                setRemainder(
                                    it1, it2
                                )
                            }
                        }

                    }
                    is ApiState.Empty -> {
                        println("Empty...")
                    }
                }
            }

        }
        lifecycleScope.launchWhenCreated {
            itemViewModel.wMessageLocation.collect {
                when (it) {
                    is ApiState.Loading -> {
                        println("ETA Loading...")
                        binding.osrmSrlFlowers.isRefreshing = false
                    }
                    is ApiState.Failure -> {
                        println("ETA Failure...")
                        binding.osrmSrlFlowers.isRefreshing = false
                        it.e.printStackTrace()
                    }
                    is ApiState.Success -> {
                        println("ETA SUCCESS...")
                        binding.osrmSrlFlowers.isRefreshing = false
                        val myObj = it.data as LatLng
                        myLocation(myObj)

                    }
                    is ApiState.Empty -> {
                        println("Empty...")
                    }
                }
            }

        }

    }

    private fun makeLocationStreamEmulator(
        coordinates: MutableList<LatLng>,
        length: Double,
        time: Double
    ) {
         itemViewModel.getUserLocation(
             coordinates,
             length,
             time
         )
    }

    private fun makeAPICall(startLat:Double,startLng:Double,endLat:Double,endLng:Double) {

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

    }

    private fun makeAPICallETA(start: LatLng, end: List<LatLng>) {

        if(lastETACallTimestamp != null && RouteUtils().calculateTimestampDifference(System.currentTimeMillis(),lastETACallTimestamp!!) < etaCallsMinInterval) {
            println("calculated_line_distance ->"+RouteUtils().calculateTimestampDifference(System.currentTimeMillis(),lastETACallTimestamp!!))
            return
        }

            val locations = mutableListOf<Locations>()
        locations.add(Locations.LocationsBuilder().lat(start.latitude).lon(start.longitude).build())
        locations.add(Locations.LocationsBuilder().lat(end[end.size-1].latitude).lon(end[end.size-1].longitude).build())

        val auto = Auto.AutoBuilder().auto(2000).build()
        val costingOptions = CostingOptions.CostingOptionsBuilder().auto(auto).build()
         lastETACallTimestamp = System.currentTimeMillis()
        itemViewModel.getETA(
            RequestObject.Builder().locations(locations as ArrayList<Locations>)
                .units("km")
                .id("eta")
                .costing("auto")
                .costingOptions(costingOptions)
                .build()
        )
    }

    override fun onResume() {
        super.onResume()
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        map.onResume() //needed for compass, my location overlays, v6.0.0 and up
    }

    override fun onPause() {
        super.onPause()
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        map.onPause()  //needed for compass, my location overlays, v6.0.0 and up
    }
    private fun speakInt() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts.setLanguage(Locale.US)

                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(this@OSMMAP, "Language not supported", Toast.LENGTH_SHORT)
                        .show()
                }
            } else {
                Toast.makeText(this@OSMMAP, "Initialization failed", Toast.LENGTH_SHORT)
                    .show()
            }
        }

    }

    private fun speak(texts: java.util.ArrayList<String?>) {
        var count = texts.size
        var toSpeak = 0
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

}