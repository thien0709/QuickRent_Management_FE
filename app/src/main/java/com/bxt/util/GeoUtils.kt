package com.bxt.util

import kotlin.math.*

const val EARTH_RADIUS_KM = 6371.0

data class LatLng(val lat: Double, val lng: Double)

fun haversineKm(aLat: Double, aLng: Double, bLat: Double, bLng: Double): Double {
    val dLat = Math.toRadians(bLat - aLat)
    val dLng = Math.toRadians(bLng - aLng)
    val la1 = Math.toRadians(aLat)
    val la2 = Math.toRadians(bLat)

    val h = sin(dLat / 2).pow(2.0) + cos(la1) * cos(la2) * sin(dLng / 2).pow(2.0)
    val c = 2 * atan2(sqrt(h), sqrt(1 - h))
    return EARTH_RADIUS_KM * c
}

fun haversineKm(a: LatLng, b: LatLng): Double = haversineKm(a.lat, a.lng, b.lat, b.lng)

fun withinRadiusKm(
    aLat: Double, aLng: Double,
    bLat: Double, bLng: Double,
    radiusKm: Double
): Boolean = haversineKm(aLat, aLng, bLat, bLng) <= radiusKm + 1e-9

fun formatDistanceShort(km: Double): String =
    if (km < 1.0) "${(km * 1000).toInt()} m" else String.format("%.1f km", km)

fun boundingBoxKm(centerLat: Double, centerLng: Double, radiusKm: Double): Pair<LatLng, LatLng> {
    val dLat = Math.toDegrees(radiusKm / EARTH_RADIUS_KM)
    val dLng = Math.toDegrees(radiusKm / (EARTH_RADIUS_KM * cos(Math.toRadians(centerLat))))
    return LatLng(centerLat - dLat, centerLng - dLng) to
            LatLng(centerLat + dLat, centerLng + dLng)
}
public fun extractDistrictOrWard(fullAddress: String?): String? {
    if (fullAddress.isNullOrEmpty()) return null

    val addressParts = fullAddress.split(",").map { it.trim() }
    if (addressParts.size < 2) return fullAddress

    val district = addressParts[addressParts.size - 2]
    val city = addressParts.last()

    return "$district, $city"
}

