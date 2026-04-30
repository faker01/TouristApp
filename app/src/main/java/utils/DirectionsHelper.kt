package com.example.touristapp.utils

import com.example.touristapp.models.Attraction
import com.yandex.mapkit.geometry.Point

object DirectionsHelper {

    /**
     * Преобразует список достопримечательностей в список точек Яндекс MapKit.
     */
    fun attractionsToPoints(attractions: List<Attraction>): List<Point> {
        return attractions.map { Point(it.lat, it.lon) }
    }

    /**
     * Вычисляет примерное расстояние между двумя точками в километрах
     * по формуле гаверсинусов.
     */
    fun distanceKm(from: Point, to: Point): Double {
        val earthRadius = 6371.0
        val dLat = Math.toRadians(to.latitude - from.latitude)
        val dLon = Math.toRadians(to.longitude - from.longitude)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(from.latitude)) *
                Math.cos(Math.toRadians(to.latitude)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return earthRadius * c
    }

    /**
     * Вычисляет суммарное расстояние маршрута по всем точкам (в км).
     */
    fun totalDistanceKm(points: List<Point>): Double {
        if (points.size < 2) return 0.0
        var total = 0.0
        for (i in 0 until points.size - 1) {
            total += distanceKm(points[i], points[i + 1])
        }
        return total
    }

    /**
     * Форматирует дистанцию: меньше 1 км → в метрах, иначе в км.
     */
    fun formatDistance(km: Double): String {
        return if (km < 1.0) {
            "${(km * 1000).toInt()} м"
        } else {
            String.format("%.1f км", km)
        }
    }
}