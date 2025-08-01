package com.bxt.data.api.dto.response

import java.math.BigDecimal

data class TransportPassengerResponse(
    val id: Long? = null,
    val transportServiceId: Long? = null,
    val userId: Long? = null,
    val pickupLatitude: BigDecimal? = null,
    val pickupLongitude: BigDecimal? = null,
    val dropoffLatitude: BigDecimal? = null,
    val dropoffLongitude: BigDecimal? = null
)
