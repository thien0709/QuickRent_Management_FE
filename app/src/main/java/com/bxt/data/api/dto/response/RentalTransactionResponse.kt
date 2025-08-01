package com.bxt.data.api.dto.response

import java.math.BigDecimal

data class RentalTransactionResponse(
    val id: Long?,
    val transactionCode: String?,
    val rentalRequestId: Long?,
    val paymentMethod: String?,
    val paymentStatus: String?,
    val amount: BigDecimal?,
    val depositAmount: BigDecimal?,
    val createdAt: String?
)
