package com.bxt.data.api.dto.response

data class PagedResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Int,
    val totalPages: Int,
    val last: Boolean
)