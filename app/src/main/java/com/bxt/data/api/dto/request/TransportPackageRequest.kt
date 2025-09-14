package com.bxt.data.api.dto.request

import java.math.BigDecimal

data class TransportPackageRequest(
    val transportServiceId: Long,
    val senderId: Long,
    val receiptId: Long,
    val fromLatitude: BigDecimal?,
    val fromLongitude: BigDecimal?,
    val toLatitude: BigDecimal?,
    val toLongitude: BigDecimal?,
    val packageWeight: BigDecimal? = null,
    val packageDescription: String?
)