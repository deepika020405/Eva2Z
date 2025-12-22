package com.developer.ev2z.ui.screens.GeoFencing

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import com.developer.ev2z.data.local.model.GeoFencePolygon
import kotlinx.cinterop.*
import platform.CoreLocation.CLLocationCoordinate2D
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.MapKit.MKMapView
import platform.MapKit.MKPolygon
import platform.MapKit.addOverlay
import platform.MapKit.removeOverlays
import platform.MapKit.overlays
import platform.UIKit.UIEdgeInsetsMake

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun GeoFenceMap(
    polygon: GeoFencePolygon?
) {
    UIKitView(
        modifier = Modifier.fillMaxSize(),
        factory = {
            MKMapView().apply {
                zoomEnabled = true
                scrollEnabled = true
            }
        },
        update = { mapView ->

            mapView.removeOverlays(mapView.overlays)

            polygon?.let { poly ->
                memScoped {
                    val size = poly.points.size

                    // Allocate native memory for coordinates
                    val coords = allocArray<DoubleVar>(size * 2)

                    // Fill with lat/lng pairs
                    poly.points.forEachIndexed { index, point ->
                        coords[index * 2] = point.lat
                        coords[index * 2 + 1] = point.lng
                    }

                    // Reinterpret as CLLocationCoordinate2D array
                    val coordsPtr = coords.reinterpret<CLLocationCoordinate2D>()

                    val polygonOverlay = MKPolygon.polygonWithCoordinates(
                        coordsPtr,
                        size.toULong()
                    )

                    mapView.addOverlay(polygonOverlay)

                    // Fit map to polygon
                    mapView.setVisibleMapRect(
                        polygonOverlay.boundingMapRect,
                        edgePadding = UIEdgeInsetsMake(40.0, 40.0, 40.0, 40.0),
                        animated = true
                    )
                }
            }
        }
    )
}