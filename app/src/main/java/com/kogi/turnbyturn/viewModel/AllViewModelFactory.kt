package com.kogi.turnbyturn.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kogi.turnbyturn.repository.ItemRepository

/**
 * To pass param to ViewModel We need ViewModel Factory
 */
class ItemViewModelFactory(private val itemRepository: ItemRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return ItemViewModel(itemRepository) as T
    }
}





