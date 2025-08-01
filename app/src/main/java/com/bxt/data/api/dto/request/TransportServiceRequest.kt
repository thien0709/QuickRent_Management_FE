package com.bxt.data.api.dto.request;

import java.math.BigDecimal
import java.time.Instant

class TransportServiceRequest {
    private val driverId: Long? = null
    private val fromLatitude: BigDecimal? = null
    private val fromLongitude: BigDecimal? = null
    private val toLatitude: BigDecimal? = null
    private val toLongitude: BigDecimal? = null
    private val departTime: Instant? = null
    private val availableSeat: Long? = null
    private val deliveryFee: BigDecimal? = null
    private val description: String? = null
}
