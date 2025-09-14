package com.bxt.util

import com.mapbox.geojson.Point
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
fun extractDistrictOrWard(fullAddress: String?): String? {
    if (fullAddress.isNullOrEmpty()) return null

    val addressParts = fullAddress.split(",").map { it.trim() }
    if (addressParts.size < 2) return fullAddress

    val district = addressParts[addressParts.size - 2]
    val city = addressParts.last()

    return "$district, $city"
}

fun findMinimumDistanceToPolyline(point: Point, polyline: List<Point>): Double {
    if (polyline.size < 2) return Double.MAX_VALUE

    var minDistance = Double.MAX_VALUE

    for (i in 0 until polyline.size - 1) {
        val segmentStart = polyline[i]
        val segmentEnd = polyline[i + 1]

        val pLat = Math.toRadians(point.latitude())
        val pLon = Math.toRadians(point.longitude())
        val sLat = Math.toRadians(segmentStart.latitude())
        val sLon = Math.toRadians(segmentStart.longitude())
        val eLat = Math.toRadians(segmentEnd.latitude())
        val eLon = Math.toRadians(segmentEnd.longitude())

        val segmentDist = 2 * asin(sqrt(sin((eLat - sLat) / 2).pow(2) + cos(sLat) * cos(eLat) * sin((eLon - sLon) / 2).pow(2)))

        if (segmentDist == 0.0) {
            minDistance = min(minDistance, haversineKm(point.latitude(), point.longitude(), segmentStart.latitude(), segmentStart.longitude()))
            continue
        }

        val bearingS_P = atan2(sin(pLon - sLon) * cos(pLat), cos(sLat) * sin(pLat) - sin(sLat) * cos(pLat) * cos(pLon - sLon))
        val bearingS_E = atan2(sin(eLon - sLon) * cos(eLat), cos(sLat) * sin(eLat) - sin(sLat) * cos(eLat) * cos(eLon - sLon))

        val distS_P = haversineKm(point.latitude(), point.longitude(), segmentStart.latitude(), segmentStart.longitude())
        val angle = bearingS_P - bearingS_E
        val crossTrackDistance = abs(asin(sin(distS_P / EARTH_RADIUS_KM) * sin(angle))) * EARTH_RADIUS_KM

        val distS_E = haversineKm(segmentStart.latitude(), segmentStart.longitude(), segmentEnd.latitude(), segmentEnd.longitude())
        val distAlongTrack = acos(cos(distS_P / EARTH_RADIUS_KM) / cos(crossTrackDistance / EARTH_RADIUS_KM)) * EARTH_RADIUS_KM

        if (distAlongTrack > distS_E) {
            minDistance = min(minDistance, haversineKm(point.latitude(), point.longitude(), segmentEnd.latitude(), segmentEnd.longitude()))
        } else {
            minDistance = min(minDistance, crossTrackDistance)
        }
    }

    return minDistance
}


