package com.kogi.turnbyturn.objects

import com.google.gson.annotations.SerializedName


data class RequestObject (

  @SerializedName("locations"       ) var locations      : ArrayList<Locations> = arrayListOf(),
  @SerializedName("costing"         ) var costing        : String?              = null,
  @SerializedName("costing_options" ) var costingOptions : CostingOptions?      = CostingOptions(),
  @SerializedName("units"           ) var units          : String?              = null,
  @SerializedName("id"              ) var id             : String?              = null

){
  class Builder {
    private var locations: ArrayList<Locations> = arrayListOf()
    private var costing: String? = null
    private var costingOptions: CostingOptions? = CostingOptions()
    private var units: String? = null
    private var id: String? = null

    fun locations(locations: ArrayList<Locations>) = apply { this.locations = locations }
    fun costing(costing: String?) = apply { this.costing = costing }
    fun costingOptions(costingOptions: CostingOptions?) = apply { this.costingOptions = costingOptions }
    fun units(units: String?) = apply { this.units = units }
    fun id(id: String?) = apply { this.id = id }

    @JvmOverloads
    fun build() = RequestObject(locations, costing, costingOptions, units, id)
  }
}