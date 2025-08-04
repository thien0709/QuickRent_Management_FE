package com.bxt.data.api.dto.response

data class PagedResponse<T>(
    val content: List<T>,
    val totalPages: Int,
    val totalElements: Int,
    val number: Int,
    val size: Int,
    val first: Boolean,
    val last: Boolean,
    val empty: Boolean
)
