package com.bxt.data.repository

import com.bxt.data.api.dto.response.ItemResponse
import kotlinx.coroutines.flow.StateFlow

interface AddressRepository {
    val itemAddresses: StateFlow<Map<Long, String>>
    fun loadAddressesForItems(items: List<ItemResponse>)
}