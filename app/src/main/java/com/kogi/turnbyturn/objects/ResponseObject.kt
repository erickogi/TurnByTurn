package com.kogi.turnbyturn.objects

import com.google.gson.annotations.SerializedName


data class ResponseObject (

  @SerializedName("trip" ) var trip : Trip?   = Trip(),
  @SerializedName("id"   ) var id   : String? = null

)