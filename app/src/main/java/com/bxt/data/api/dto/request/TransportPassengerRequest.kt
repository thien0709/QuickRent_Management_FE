package com.bxt.data.api.dto.request

import java.math.BigDecimal

data class TransportPassengerRequest(
    val transportServiceId: Long,
    val userId: Long,
    val pickupLatitude: BigDecimal,
    val pickupLongitude: BigDecimal,
    val dropoffLatitude: BigDecimal? = null,
    val dropoffLongitude: BigDecimal? = null
)