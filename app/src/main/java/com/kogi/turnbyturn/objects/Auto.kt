package com.kogi.turnbyturn.objects

import com.google.gson.annotations.SerializedName


data class Auto (

  @SerializedName("country_crossing_penalty" ) var countryCrossingPenalty : Int? = null

) {
  class AutoBuilder {
    private var countryCrossingPenalty: Int? = null

    fun auto(countryCrossingPenalty: Int?) =
      apply { this.countryCrossingPenalty = countryCrossingPenalty }

    @JvmOverloads
    fun build() = Auto(countryCrossingPenalty)
  }
}