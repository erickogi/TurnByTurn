package com.kogi.turnbyturn.objects

import com.google.gson.annotations.SerializedName


data class Legs (

  @SerializedName("maneuvers" ) var maneuvers : ArrayList<Maneuvers> = arrayListOf(),
  @SerializedName("summary"   ) var summary   : Summary?             = Summary(),
  @SerializedName("shape"     ) var shape     : String?              = null

)