package com.kogi.turnbyturn.objects

import com.google.gson.annotations.SerializedName


data class Summary (

  @SerializedName("has_time_restrictions" ) var hasTimeRestrictions : Boolean? = null,
  @SerializedName("has_toll"              ) var hasToll             : Boolean? = null,
  @SerializedName("has_highway"           ) var hasHighway          : Boolean? = null,
  @SerializedName("has_ferry"             ) var hasFerry            : Boolean? = null,
  @SerializedName("min_lat"               ) var minLat              : Double?  = null,
  @SerializedName("min_lon"               ) var minLon              : Double?  = null,
  @SerializedName("max_lat"               ) var maxLat              : Double?  = null,
  @SerializedName("max_lon"               ) var maxLon              : Double?  = null,
  @SerializedName("time"                  ) var time                : Double?  = null,
  @SerializedName("length"                ) var length              : Double?  = null,
  @SerializedName("cost"                  ) var cost                : Double?  = null

)