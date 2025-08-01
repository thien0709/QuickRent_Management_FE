package com.bxt.data.api.dto.response

import java.math.BigDecimal
import java.time.Instant

data class RentalRequestResponse(
    val id: Long?,
    val itemId: Long?,
    val renterId: Long?,
    val rentalStartTime: Instant?,
    val rentalEndTime: Instant?,
    val totalPrice: BigDecimal?,
    val status: String?,
    val createdAt: Instant?,
    val updatedAt: Instant?
)
