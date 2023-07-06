package com.kogi.turnbyturn.objects

import com.google.gson.annotations.SerializedName


data class LocationsRequest (

  @SerializedName("lat" ) var lat : Double? = null,
  @SerializedName("lon" ) var lon : Double? = null

)