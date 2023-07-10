package com.kogi.turnbyturn.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.kogi.turnbyturn.networkService.ApiState
import com.kogi.turnbyturn.objects.RequestObject
import com.kogi.turnbyturn.repository.ItemRepository
import com.kogi.turnbyturn.repository.LocationMockRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

class ItemViewModel(private var itemRepository: ItemRepository,private var locationMockRepository: LocationMockRepository): ViewModel() {

    /**
     * Instead of using live data using flow
     */
    val wMessage: MutableStateFlow<ApiState> = MutableStateFlow(ApiState.Empty)
    val wMessageETA: MutableStateFlow<ApiState> = MutableStateFlow(ApiState.Empty)
    val wMessageLocation: MutableStateFlow<ApiState> = MutableStateFlow(ApiState.Empty)

    fun getRoute(option: RequestObject) = viewModelScope.launch {
        wMessage.value = ApiState.Loading
        itemRepository.getRoute(option)
            .catch { e ->
                wMessage.value = ApiState.Failure(e)
            }.collect { data ->
                wMessage.value = ApiState.Success(data)
            }
    }
    fun getETA(option: RequestObject) = viewModelScope.launch {
        wMessageETA.value = ApiState.Loading
        itemRepository.getRoute(option)
            .catch { e ->
                wMessageETA.value = ApiState.Failure(e)
            }.collect { data ->
                wMessageETA.value = ApiState.Success(data)
            }
    }

     fun getUserLocation(coordinates: MutableList<LatLng>, length: Double, time: Double) = viewModelScope.launch {
        wMessageLocation.value = ApiState.Loading
        locationMockRepository.getUserLocation(coordinates,length,time)
            .catch { e ->
                wMessageLocation.value = ApiState.Failure(e)
            }.collect { data ->
                wMessageLocation.value = ApiState.Success(data)
            }
    }


}