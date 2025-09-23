package com.bxt.data.api.dto.status

enum class ConditionStatus(val label: String) {
    NEW("New"),
    GOOD("Good"),
    POOR("Poor")
}

enum class AvailabilityStatus(val label: String) {
    AVAILABLE("Available"),
    RENTED("Rented"),
    UNAVAILABLE("Unavailable")
}

val CONDITION_OPTIONS = ConditionStatus.entries.toList()
val AVAILABILITY_OPTIONS = AvailabilityStatus.entries.toList()
