package com.kogi.turnbyturn.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.kogi.turnbyturn.R
import com.kogi.turnbyturn.objects.Maneuvers
import com.kogi.turnbyturn.objects.ResponseObject
import java.lang.Math.atan2
import java.lang.Math.cos
import java.lang.Math.sin
import java.lang.Math.sqrt
import java.text.DecimalFormat
import java.util.ArrayList
import kotlin.math.pow

class RouteUtils {
    fun calculateDistance(loc1:LatLng,
                          loc2: LatLng): Double {
        val earthRadius = 6371.0

        val dLat = Math.toRadians(loc2.latitude  - loc1.latitude)
        val dLon = Math.toRadians(loc2.longitude  - loc2.longitude )

        val a = kotlin.math.sin(dLat / 2).pow(2) + kotlin.math.cos(Math.toRadians(loc1.latitude)) * kotlin.math.cos(
            Math.toRadians(loc2.latitude)
        ) *
                kotlin.math.sin(dLon / 2).pow(2)
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))

        val dis = (earthRadius * c)
        println("calculated_line_distance ->"+dis);
        return dis/1000
    }

    fun calculateTimestampDifference(current: Long,last: Long): Long {
        val timeDifference =  current-last

        val seconds = timeDifference / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return seconds
    }
    fun roundToOneDecimalPlace(number: Double): Double {
        val decimalFormat = DecimalFormat("#.#")
        return decimalFormat.format(number).toDouble()
    }
    fun decode(str: String): List<DoubleArray> {
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
        val streetNames: String?,
        val coordinates: List<LatLng>
    )

    fun getEndCapIcon(context: Context?, color: Int): BitmapDescriptor {

        // mipmap icon - white arrow, pointing up, with point at center of image
        // you will want to create:  mdpi=24x24, hdpi=36x36, xhdpi=48x48, xxhdpi=72x72, xxxhdpi=96x96
        val drawable = ContextCompat.getDrawable(context!!, R.mipmap.endcap)

        // set the bounds to the whole image (may not be necessary ...)
        drawable!!.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)

        // overlay (multiply) your color over the white icon
        //drawable.setColorFilter(color, PorterDuff.Mode.MULTIPLY)

        // create a bitmap from the drawable
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight, Bitmap.Config.ARGB_8888
        )

        // render the bitmap on a blank canvas
        val canvas = Canvas(bitmap)
        drawable.draw(canvas)

        // create a BitmapDescriptor from the new bitmap
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    fun formatDuration(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val remainingSeconds = seconds % 60

        return String.format("%02d:%02d:%02d", hours, minutes, remainingSeconds)
    }

    companion object {
        fun createFeatures(coordinates: List<LatLng>, response: ResponseObject): MutableList<Feature> {
            var features = mutableListOf<Feature>()
            response.trip?.legs?.forEachIndexed { index, leg ->
                val route = coordinates

                leg.maneuvers.forEach { maneuver ->
                    //val maneuverx =Maneuvers(maneuver.beginShapeIndex,maneuver.endShapeIndex,index)

                    maneuver.leg = index
                    //val (beginShapeIndex, endShapeIndex: Int?) = maneuver
                    val beginShapeIndex = maneuver.beginShapeIndex
                    val endShapeIndex = maneuver.endShapeIndex

                    val coordinates = route.subList(beginShapeIndex!!, endShapeIndex!! + 1)
                    val geometry = if (coordinates.size > 1) {
                        Geometry("LineString",concatenateWithCommas( maneuver.streetNames), coordinates)
                    } else {
                        Geometry("Point",concatenateWithCommas( maneuver.streetNames), coordinates)
                    }

                    features.add(Feature("Feature", maneuver, geometry))
                }
            }
            return features
        }
        private fun concatenateWithCommas(strings: List<String>?): String? {
            return strings?.joinToString(", ")
        }
    }

}