package com.kogi.turnbyturn.networkService
import com.kogi.turnbyturn.objects.RequestObject
import com.kogi.turnbyturn.objects.ResponseObject
import retrofit2.http.*

interface RetrofitInterface {

    @GET(AllApi.ROUTE)
    suspend fun getRoute(@Query("json") option: String):ResponseObject

}













