package com.bxt.data.api.dto.response;


import java.math.BigDecimal

data class TransportPackageResponse(
    val id: Long? = null,
    val transportServiceId: Long? = null,
    val senderId: Long? = null,
    val receiptId: Long? = null,
    val fromLatitude: BigDecimal? = null,
    val fromLongitude: BigDecimal? = null,
    val toLatitude: BigDecimal? = null,
    val toLongitude: BigDecimal? = null,
    val packageWeight: BigDecimal? = null,
    val packageDescription: String? = null
)
