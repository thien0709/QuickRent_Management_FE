package com.bxt.data.api.dto.request;
import java.math.BigDecimal

data class ItemRequest(
    val categoryId: Long? = null,
    val title: String? = null,
    val description: String? = null,
    val depositAmount: BigDecimal? = null,
    val rentalPricePerHour: BigDecimal? = null,
    val conditionStatus: String? = null,
    val availabilityStatus: String? = null,
    val isActive: Boolean? = null
)