package com.bxt.data.api.dto.response;


import java.math.BigDecimal

data class ItemResponse(
    val id: Long?,
    val title: String?,
    val description: String?,
    val depositAmount: BigDecimal?,
    val rentalPricePerHour: BigDecimal?,
    val conditionStatus: String?,
    val availabilityStatus: String?,
    val isActive: Boolean?,
    val ownerId: Long?,
    val categoryId: Long?,
    val createdAt: String?,
    val updatedAt: String?
)
