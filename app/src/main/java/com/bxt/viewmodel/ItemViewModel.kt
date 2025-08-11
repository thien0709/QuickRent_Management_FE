package com.bxt.viewmodel

import androidx.lifecycle.ViewModel
import com.bxt.data.local.DataStoreManager
import com.bxt.data.repository.impl.ItemRepositoryImpl
import javax.inject.Inject

class ItemViewModel @Inject constructor(
    private val itemRepository: ItemRepositoryImpl,
    private val dataStore: DataStoreManager
)
    : ViewModel(){

}