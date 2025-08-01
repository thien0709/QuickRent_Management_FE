package com.bxt.data.api.dto.response

data class RatingResponse(
    val id: Long?,
    val transactionType: String?,
    val transactionId: Long?,
    val raterId: Long?,
    val ratedUserId: Long?,
    val ratingScore: Int?,
    val reviewComment: String?
)
