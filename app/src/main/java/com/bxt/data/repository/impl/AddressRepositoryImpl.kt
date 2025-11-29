// File: data/repository/impl/AddressRepositoryImpl.kt
package com.bxt.data.repository.impl

import com.bxt.data.api.dto.response.ItemResponse
import com.bxt.data.repository.AddressRepository
import com.bxt.data.repository.LocationRepository
import com.bxt.data.repository.UserRepository
import com.bxt.di.ApiResult
import com.bxt.util.extractDistrictOrWard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AddressRepositoryImpl @Inject constructor(
    private val userRepository: UserRepository,
    private val locationRepository: LocationRepository
) : AddressRepository {

    private val _itemAddresses = MutableStateFlow<Map<Long, String>>(emptyMap())
    override val itemAddresses = _itemAddresses.asStateFlow()

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    override fun loadAddressesForItems(items: List<ItemResponse>) {
        repositoryScope.launch {
            items.forEach { item ->
                val itemId = item.id ?: return@forEach
                val ownerId = item.ownerId ?: return@forEach

                // Chỉ fetch nếu địa chỉ chưa có trong cache
                if (!_itemAddresses.value.containsKey(itemId)) {
                    val locationResult = userRepository.getUserLocation(ownerId)
                    if (locationResult is ApiResult.Success) {
                        val locationMap = locationResult.data
                        val lat = locationMap["lat"]?.toDouble()
                        val lng = locationMap["lng"]?.toDouble()

                        val address = if (lat != null && lng != null) {
                            val districtResult = locationRepository.getAddressFromLatLng(lat, lng)
                            extractDistrictOrWard(districtResult.getOrNull()) ?: "Vị trí không xác định"
                        } else {
                            "Chủ sở hữu chưa cập nhật vị trí"
                        }

                        _itemAddresses.update { currentMap ->
                            currentMap + (itemId to address)
                        }
                    }
                }
            }
        }
    }
}