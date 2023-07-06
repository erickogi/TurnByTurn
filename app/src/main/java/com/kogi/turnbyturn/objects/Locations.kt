package com.kogi.turnbyturn.objects

import com.google.gson.annotations.SerializedName


data class Locations (

  @SerializedName("lat" ) var lat : Double? = null,
  @SerializedName("lon" ) var lon : Double? = null

) {
  class LocationsBuilder {
    private var lat: Double? = null
    private var lon: Double? = null

    fun lat(lat: Double?) = apply { this.lat = lat }
    fun lon(lon: Double?) = apply { this.lon = lon }

    @JvmOverloads
    fun build() = Locations(lat, lon)
  }
}
