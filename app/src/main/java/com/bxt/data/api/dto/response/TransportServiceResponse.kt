package com.bxt.data.api.dto.response

import java.math.BigDecimal
import java.time.Instant

data class TransportServiceResponse(
    val id: Long? = null,
    val driverId: Long? = null,
    val fromLatitude: BigDecimal? = null,
    val fromLongitude: BigDecimal? = null,
    val toLatitude: BigDecimal? = null,
    val toLongitude: BigDecimal? = null,
    val departTime: Instant? = null,
    val availableSeat: Long? = null,
    val deliveryFee: BigDecimal? = null,
    val description: String? = null,
    val status: String? = null,
    val createdAt: String? = null
)
