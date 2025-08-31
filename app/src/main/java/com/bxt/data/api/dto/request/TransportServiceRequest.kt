package com.bxt.data.api.dto.request;

import java.math.BigDecimal
import java.time.Instant

data class TransportServiceRequest(
    val driverId: Long,
    val fromLatitude: BigDecimal,
    val fromLongitude: BigDecimal,
    val toLatitude: BigDecimal,
    val toLongitude: BigDecimal,
    val departTime: Instant,
    val availableSeat: Long?,
    val deliveryFee: BigDecimal,
    val description: String?
)