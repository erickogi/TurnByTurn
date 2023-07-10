package com.kogi.turnbyturn.repository



import com.google.gson.Gson
import com.kogi.turnbyturn.networkService.RetrofitClient
import com.kogi.turnbyturn.objects.RequestObject
import com.kogi.turnbyturn.objects.ResponseObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
class ItemRepository {
    fun getRoute(option: RequestObject) : Flow<ResponseObject> = flow {
        val optionx = Gson().toJson(option)
        println(optionx)
        val p = RetrofitClient.retrofit.getRoute(optionx)
        emit(p)
    }.flowOn(Dispatchers.IO)
}