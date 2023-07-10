package com.kogi.turnbyturn.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kogi.turnbyturn.repository.ItemRepository
import com.kogi.turnbyturn.repository.LocationMockRepository

/**
 * To pass param to ViewModel We need ViewModel Factory
 */
class ItemViewModelFactory(
    private val itemRepository: ItemRepository,
    private val locationMockRepository: LocationMockRepository
    ) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return ItemViewModel(itemRepository,locationMockRepository) as T
    }
}





