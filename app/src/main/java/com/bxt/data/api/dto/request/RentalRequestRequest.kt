package com.bxt.data.api.dto.request;

import java.math.BigDecimal
import java.time.Instant


class RentalRequestRequest {
    private val itemId: Long? = null
    private val renterId: Long? = null
    private val rentalStartTime: Instant? = null
    private val rentalEndTime: Instant? = null
    private val totalPrice: BigDecimal? = null
}
