package com.kogi.turnbyturn.objects

import com.google.gson.annotations.SerializedName


data class Maneuvers (

  @SerializedName("type")
  var type: Int?= null,
  @SerializedName("instruction")
  var instruction: String? = null,
  @SerializedName("verbal_succinct_transition_instruction")
  var verbalSuccinctTransitionInstruction : String?  = null,
  @SerializedName("verbal_pre_transition_instruction")
  var verbalPreTransitionInstruction: String? = null,
  @SerializedName("verbal_post_transition_instruction")
  var verbalPostTransitionInstruction: String? = null,
  @SerializedName("time")
  var time: Double? = null,
  @SerializedName("length")
  var length: Double? = null,
  @SerializedName("cost")
  var cost: Double? = null,
  @SerializedName("begin_shape_index" )
  var beginShapeIndex: Int?= null,
  @SerializedName("end_shape_index")
  var endShapeIndex: Int? = null,
  @SerializedName("verbal_multi_cue")
  var verbalMultiCue: Boolean? = null,
  @SerializedName("travel_mode")
  var travelMode: String?  = null,
  @SerializedName("travel_type")
  var travelType: String?  = null,
  var leg: Int
)