package com.developer.ev2z.ui.components

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGPointMake
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.MapKit.MKAnnotationProtocol
import platform.MapKit.MKAnnotationView
import platform.MapKit.MKMapView
import platform.MapKit.MKPointAnnotation
import platform.UIKit.UIImage

/** One address chip, as the map needs it: where it goes and what it says. */
internal data class AddressChip(
    val deviceId: String,
    val latitude: Double,
    val longitude: Double,
    /** Already rasterised — the caller caches these, since a chip changes rarely. */
    val image: UIImage,
    /** How far to lift the chip off the vehicle, in points. */
    val liftPoints: Double,
)

/**
 * The address chips on the fleet map, kept and moved rather than replaced.
 *
 * The MapKit counterpart of the extra `Marker` the Android map adds per vehicle. It
 * exists for the same reason [VehicleAnnotations] does: an annotation re-added every
 * frame breeds instead of moving, and flickers the map at animation speed.
 *
 * Separate from [VehicleAnnotations] rather than folded into it because the two are
 * drawn by different rules — a vehicle turns to its heading and lies flat on the map,
 * a chip is text and must stay upright and screen-facing however the map is turned.
 */
@OptIn(ExperimentalForeignApi::class)
internal class AddressChipAnnotations {

    private class Pin(val annotation: MKPointAnnotation) {
        var chip: AddressChip? = null
        /** The image currently on the view, so a real change can be told from a frame. */
        var appliedImage: UIImage? = null
    }

    private val pins = mutableMapOf<String, Pin>()

    /**
     * Which device [candidate] labels, or null if it is not one of these chips.
     *
     * A chip is a label rather than a target, but it sits on top of the vehicle it
     * belongs to, so a tap aimed at the vehicle often lands here instead. Resolving it
     * lets the caller select the same device either way — which is what the fleet map
     * on Android already does.
     */
    fun deviceIdFor(candidate: MKAnnotationProtocol): String? =
        pins.entries.firstOrNull { it.value.annotation === candidate }?.key

    /**
     * The view for one of these chips, or null when [candidate] is some other
     * annotation — which the caller must go on to handle.
     */
    fun annotationView(mapView: MKMapView, candidate: MKAnnotationProtocol): MKAnnotationView? {
        val pin = pins.values.firstOrNull { it.annotation === candidate } ?: return null
        val chip = pin.chip ?: return null

        val id = "fleet_address_label"
        val view = mapView.dequeueReusableAnnotationViewWithIdentifier(id)
            ?: MKAnnotationView(annotation = candidate, reuseIdentifier = id)
        view.annotation = candidate
        view.image = chip.image
        // A label, not a target: tapping it should do nothing of its own.
        view.canShowCallout = false
        view.centerOffset = chip.offset()
        pin.appliedImage = chip.image
        return view
    }

    /** Moves and re-labels every chip, adding and retiring them as [chips] changes. */
    fun update(mapView: MKMapView, chips: List<AddressChip>) {
        val live = chips.map { it.deviceId }.toSet()
        pins.keys.toList().forEach { id ->
            if (id !in live) {
                pins.remove(id)?.let { mapView.removeAnnotation(it.annotation) }
            }
        }

        chips.forEach { chip ->
            val existing = pins[chip.deviceId]
            if (existing == null) {
                val pin = Pin(MKPointAnnotation())
                pin.chip = chip
                pins[chip.deviceId] = pin
                pin.annotation.setCoordinate(
                    CLLocationCoordinate2DMake(chip.latitude, chip.longitude)
                )
                // The delegate draws and offsets the view as it builds it.
                mapView.addAnnotation(pin.annotation)
                return@forEach
            }

            existing.chip = chip
            existing.annotation.setCoordinate(
                CLLocationCoordinate2DMake(chip.latitude, chip.longitude)
            )

            // MapKit asks the delegate for an image only when it builds or recycles a
            // view, so a chip already on screen has to be updated directly or it keeps
            // the street the vehicle has driven away from.
            if (existing.appliedImage !== chip.image) {
                mapView.viewForAnnotation(existing.annotation)?.let { view ->
                    view.image = chip.image
                    view.centerOffset = chip.offset()
                }
                existing.appliedImage = chip.image
            }
        }
    }

    /** Half the chip's own height plus the lift, so it floats above the vehicle. */
    private fun AddressChip.offset() =
        CGPointMake(0.0, -(image.size.useContents { height } / 2.0 + liftPoints))
}
