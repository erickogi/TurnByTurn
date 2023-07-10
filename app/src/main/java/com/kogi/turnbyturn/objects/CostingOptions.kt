package com.kogi.turnbyturn.objects

import com.google.gson.annotations.SerializedName


data class CostingOptions (

  @SerializedName("auto" ) var auto : Auto? = Auto()

) {
  class CostingOptionsBuilder {
    private var auto: Auto? = null
    fun auto(auto: Auto?) = apply { this.auto = auto }

    @JvmOverloads
    fun build() = CostingOptions(auto)
  }
}