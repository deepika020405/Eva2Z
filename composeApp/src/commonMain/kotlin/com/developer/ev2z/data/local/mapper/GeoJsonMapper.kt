package com.developer.ev2z.data.local.mapper


import com.developer.ev2z.data.local.model.GeoFencePolygon
import com.developer.ev2z.data.local.model.LatLng
import com.developer.ev2z.response.GeoJson

//fun GeoJson.toPolygon(): GeoFencePolygon {
//    val points = coordinates.first().map {
//        LatLng(
//            lat = it[1],
//            lng = it[0]
//        )
//    }
//    return GeoFencePolygon(points)
//}