package com.bxt.data.api.dto.request;
import android.net.Uri
import java.math.BigDecimal

data class ItemRequest (
    val ownerId: Long,
    val categoryId: Long,
    val title: String,
    val description: String,
    val depositAmount: BigDecimal,
    val rentalPricePerHour: BigDecimal,
    val conditionStatus: String,
    val availabilityStatus: String,
    val isActive: Boolean,
)
