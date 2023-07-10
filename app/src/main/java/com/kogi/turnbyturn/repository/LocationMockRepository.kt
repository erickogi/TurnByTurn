package com.kogi.turnbyturn.repository



import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlin.math.roundToLong

class LocationMockRepository() {

    fun getUserLocation(coordinates: MutableList<LatLng>, length: Double, time: Double) : Flow<LatLng> = flow {


        for ((index, coordinate) in coordinates.withIndex()) {
            if(index>0){
                delay(7000) // pretend we fetching location
                emit(coordinate)
            }else{
                emit(coordinate)
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun calculateDelay(distance: Double, time: Double): Long {
        val speed = (distance / (time/ 3600))
        println("speed-> "+speed)
        println("delay-> "+distance/speed*10000)
        return (distance/speed*10000).roundToLong()
    }


}

