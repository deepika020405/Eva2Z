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
import kotlin.collections.forEachIndexed

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

                    val coords =
                        allocArray<CLLocationCoordinate2D>(poly.points.size)

                    poly.points.forEachIndexed { index, point ->
                        coords[index] =
                            CLLocationCoordinate2DMake(point.lat, point.lng)
                    }

                    val polygonOverlay =
                        MKPolygon.polygonWithCoordinates(
                            coords,
                            poly.points.size.toULong()
                        )

                    mapView.addOverlay(polygonOverlay)

                    // 🟢 FIT MAP TO POLYGON
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