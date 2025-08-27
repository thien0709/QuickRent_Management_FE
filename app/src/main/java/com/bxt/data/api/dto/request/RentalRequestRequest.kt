package com.bxt.data.api.dto.request;

import java.math.BigDecimal
import java.time.Instant


data class RentalRequestRequest (
     val itemId: Long,
     val renterId: Long,
     val rentalStartTime: Instant,
     val rentalEndTime: Instant,
     val totalPrice: BigDecimal,
     val paymentMethod: String,
     val latTo : BigDecimal,
     val lngTo : BigDecimal,
)