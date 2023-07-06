package com.kogi.turnbyturn.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kogi.turnbyturn.networkService.ApiState
import com.kogi.turnbyturn.objects.RequestObject
import com.kogi.turnbyturn.repository.ItemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

class ItemViewModel(private var itemRepository: ItemRepository): ViewModel() {

    /**
     * Instead of using live data using flow
     */
    val wMessage: MutableStateFlow<ApiState> = MutableStateFlow(ApiState.Empty)

    fun getRoute(option: RequestObject) = viewModelScope.launch {
        wMessage.value = ApiState.Loading
        itemRepository.getRoute(option)
            .catch { e ->
                wMessage.value = ApiState.Failure(e)
            }.collect { data ->
                wMessage.value = ApiState.Success(data)
            }
    }



}