package com.developer.ev2z.ui.components

import com.developer.ev2z.data.local.model.fleetMarkerColorOf
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGPointMake
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.MapKit.MKAnnotationProtocol
import platform.MapKit.MKAnnotationView
import platform.MapKit.MKMapView
import platform.MapKit.MKPointAnnotation
import platform.UIKit.UIImage
import kotlin.math.abs

/**
 * One vehicle on the fleet map, as the map needs it: where it is, which way it is
 * going, and what it is.
 */
internal data class VehicleMarker(
    val deviceId: String,
    val latitude: Double,
    val longitude: Double,
    /** Degrees clockwise from north, NaN for a vehicle that has not travelled yet. */
    val bearingDegrees: Double,
    /** The vehicle to draw, or null for the heading arrow — see `vehicleTopViewFor`. */
    val vehicle: VehicleTopView?,
    val statusKind: String,
    val title: String,
    val subtitle: String,
)

/**
 * The fleet map's vehicle pins, kept and turned rather than replaced.
 *
 * The MapKit counterpart of what a flat Google Maps marker does by itself. Three
 * things have to be done by hand here, and all three are why this class exists:
 *
 * * **Annotations are moved, never re-added.** Removing and re-adding a pin per frame
 *   makes the marker breed instead of move, and flickers the whole map at animation
 *   speed.
 * * **The image is only asked for when a view is built or recycled.** A vehicle that
 *   changes state mid-glide would otherwise keep the colour it was drawn with, so the
 *   image is pushed onto the live view when it really changes — and only then, since
 *   this runs on every animation frame.
 * * **An annotation view is screen-space.** Unlike a flat marker it does not turn with
 *   the map, so the map's own rotation is taken out of the heading and put back every
 *   time the user turns the map. Without that a vehicle drawn facing along its road
 *   points across the buildings the moment the map is turned with two fingers.
 */
@OptIn(ExperimentalForeignApi::class)
internal class VehicleAnnotations {

    private class Pin(val annotation: MKPointAnnotation) {
        var marker: VehicleMarker? = null
        /** The artwork currently on the view, so a real change can be told apart. */
        var appliedImageKey: String? = null
        // Not NaN: every comparison against NaN is false, so the first frame would
        // never apply a rotation at all.
        var appliedAngle = -999.0
    }

    private val pins = mutableMapOf<String, Pin>()

    /**
     * Rasterised marker artwork, keyed by vehicle and state.
     *
     * A fleet is a handful of vehicles in a handful of states, so this settles within
     * the first few frames and never grows after that — which is the point: drawing
     * an image per marker per frame is an allocation sixty times a second per vehicle.
     */
    private val images = mutableMapOf<String, UIImage?>()

    /**
     * The view for one of these vehicles, or null when [candidate] is some other
     * annotation — the cone, or the user-location dot — which the caller must go on to
     * handle. Returning null is also how MapKit is told to draw its own default.
     */
    /**
     * Which device [candidate] belongs to, or null if it is not one of these pins.
     *
     * What turns a tap on a vehicle into a selection: MapKit hands the delegate an
     * annotation, and the caller needs the device behind it. Without this the fleet
     * map's `onMarkerClick` was accepted and never called — tapping a vehicle on iOS
     * did nothing at all, while the same tap on Android selected it.
     */
    fun deviceIdFor(candidate: MKAnnotationProtocol): String? =
        pins.entries.firstOrNull { it.value.annotation === candidate }?.key

    fun annotationView(mapView: MKMapView, candidate: MKAnnotationProtocol): MKAnnotationView? {
        val pin = pins.values.firstOrNull { it.annotation === candidate } ?: return null
        val marker = pin.marker ?: return null

        val id = "vehicle_marker"
        val view = mapView.dequeueReusableAnnotationViewWithIdentifier(id)
            ?: MKAnnotationView(annotation = candidate, reuseIdentifier = id)
        view.annotation = candidate
        view.image = imageFor(marker)
        pin.appliedImageKey = imageKeyOf(marker)
        // The vehicle is *at* the coordinate, so the middle of the image belongs on it
        // rather than hanging above it the way a pin does.
        view.centerOffset = CGPointMake(0.0, 0.0)
        // Tapping still answers "which vehicle is this, and where" — the callout
        // carries the name and the address.
        view.canShowCallout = true
        val angle = screenAngle(mapView, marker)
        view.transform = vehicleViewTransform(angle)
        pin.appliedAngle = angle
        return view
    }

    /** Moves, turns and recolours every pin, adding and retiring them as [markers] changes. */
    fun update(mapView: MKMapView, markers: List<VehicleMarker>) {
        val live = markers.map { it.deviceId }.toSet()
        pins.keys.toList().forEach { id ->
            if (id !in live) {
                pins.remove(id)?.let { mapView.removeAnnotation(it.annotation) }
            }
        }

        markers.forEach { marker ->
            val existing = pins[marker.deviceId]
            if (existing == null) {
                val pin = Pin(MKPointAnnotation())
                pin.marker = marker
                pins[marker.deviceId] = pin
                pin.annotation.setCoordinate(
                    CLLocationCoordinate2DMake(marker.latitude, marker.longitude)
                )
                pin.annotation.setTitle(marker.title)
                pin.annotation.setSubtitle(marker.subtitle)
                // The delegate draws and turns the view as it builds it.
                mapView.addAnnotation(pin.annotation)
                return@forEach
            }

            existing.marker = marker
            existing.annotation.setCoordinate(
                CLLocationCoordinate2DMake(marker.latitude, marker.longitude)
            )
            existing.annotation.setSubtitle(marker.subtitle)

            val view = mapView.viewForAnnotation(existing.annotation) ?: return@forEach
            val key = imageKeyOf(marker)
            if (existing.appliedImageKey != key) {
                view.image = imageFor(marker)
                existing.appliedImageKey = key
            }
            applyRotation(mapView, existing, view)
        }
    }

    /**
     * Re-applies every rotation after the map's own heading changed.
     *
     * Without this each vehicle keeps pointing at the old north the moment the user
     * turns the map, because the angle it is drawn at is a screen angle.
     */
    fun reapplyRotations(mapView: MKMapView) {
        pins.values.forEach { pin ->
            val view = mapView.viewForAnnotation(pin.annotation) ?: return@forEach
            applyRotation(mapView, pin, view)
        }
    }

    private fun applyRotation(mapView: MKMapView, pin: Pin, view: MKAnnotationView) {
        val marker = pin.marker ?: return
        val angle = screenAngle(mapView, marker)
        if (abs(angle - pin.appliedAngle) < 0.001) return
        view.transform = vehicleViewTransform(angle)
        pin.appliedAngle = angle
    }

    /**
     * The angle to draw this vehicle at: its heading with the map's own rotation taken
     * out, since an annotation view is screen-space. It also absorbs the NaN of a
     * vehicle that has not travelled yet.
     */
    private fun screenAngle(mapView: MKMapView, marker: VehicleMarker): Double =
        vehicleScreenAngleRadians(marker.bearingDegrees, mapView.camera.heading)

    private fun imageKeyOf(marker: VehicleMarker) = "${marker.vehicle}|${marker.statusKind}"

    private fun imageFor(marker: VehicleMarker): UIImage? {
        val key = imageKeyOf(marker)
        // containsKey, not getOrPut: a rasterise that failed caches as null, and
        // getOrPut reads a null entry as a miss — it would try again every frame.
        if (images.containsKey(key)) return images[key]

        // Green while the vehicle is moving, red while it is standing still — the live
        // fleet map's two-colour answer, see fleetMarkerColorOf. statusColorOf's grey
        // offline bucket used to land here, and the trackers on a real account report
        // offline between uploads, so every standing vehicle was a grey arrow.
        val color = fleetMarkerColorOf(marker.statusKind)
        val image = if (marker.vehicle == null) vehicleArrowImage(color)
        else vehicleMarkerImage(marker.vehicle, color)
        images[key] = image
        return image
    }
}
