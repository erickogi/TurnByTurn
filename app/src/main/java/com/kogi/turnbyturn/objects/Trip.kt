package com.kogi.turnbyturn.objects

import com.google.gson.annotations.SerializedName
import com.kogi.turnbyturn.objects.Legs
import com.kogi.turnbyturn.objects.Locations
import com.kogi.turnbyturn.objects.Summary


data class Trip (

    @SerializedName("locations"      ) var locations     : ArrayList<Locations> = arrayListOf(),
    @SerializedName("legs"           ) var legs          : ArrayList<Legs>      = arrayListOf(),
    @SerializedName("summary"        ) var summary       : Summary?             = Summary(),
    @SerializedName("status_message" ) var statusMessage : String?              = null,
    @SerializedName("status"         ) var status        : Int?                 = null,
    @SerializedName("units"          ) var units         : String?              = null,
    @SerializedName("language"       ) var language      : String?              = null

)