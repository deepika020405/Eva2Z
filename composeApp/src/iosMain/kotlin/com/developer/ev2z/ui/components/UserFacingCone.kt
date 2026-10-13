package com.developer.ev2z.ui.components

import com.developer.ev2z.util.DeviceFacing
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGAffineTransformMakeRotation
import platform.CoreGraphics.CGPointMake
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.MapKit.MKAnnotationProtocol
import platform.MapKit.MKAnnotationView
import platform.MapKit.MKMapView
import platform.MapKit.MKPointAnnotation
import platform.UIKit.UIImage
import kotlin.math.PI
import kotlin.math.abs

/**
 * The wedge showing where the phone is and which way its holder is facing, for the
 * MapKit maps.
 *
 * One of these per map, remembered across frames: MapKit annotations are added once
 * and moved, and the annotation view has to be rotated in place because MapKit only
 * asks the delegate for an image when it builds or recycles a view.
 *
 * Each map does three things with it — [annotationView] from its delegate,
 * [update] from its per-frame block, and [reapplyRotation] when the map itself is
 * turned. The Android side gets all three for free from a flat marker, which turns
 * with the map on its own; a MapKit annotation view is screen-space and does not.
 */
@OptIn(ExperimentalForeignApi::class)
internal class UserFacingCone(private val image: UIImage?) {

    val annotation = MKPointAnnotation()

    private var added = false
    private var heading = Double.NaN
    // Not NaN: every comparison against NaN is false, so the first frame would never
    // apply a rotation at all.
    private var appliedAngle = -999.0

    /**
     * The view for the cone, or null when [candidate] is some other annotation — a
     * vehicle, or the user-location dot — which the caller must go on to handle.
     *
     * Returning null from a delegate is also how MapKit is told to draw its own
     * default, so a map with no custom pins can pass everything else straight through.
     */
    fun annotationView(mapView: MKMapView, candidate: MKAnnotationProtocol): MKAnnotationView? {
        if (candidate !== annotation) return null

        val id = "user_facing_cone"
        val view = mapView.dequeueReusableAnnotationViewWithIdentifier(id)
            ?: MKAnnotationView(annotation = candidate, reuseIdentifier = id)
        view.annotation = candidate
        view.image = image
        // The wedge's apex is the middle of its image, and that is the spot the person
        // is standing on.
        view.centerOffset = CGPointMake(0.0, 0.0)
        // Context for the vehicles rather than a thing to read in itself: no callout,
        // and no tap target stealing the gesture from the map.
        view.canShowCallout = false
        view.enabled = false
        val angle = screenAngle(mapView)
        view.transform = CGAffineTransformMakeRotation(angle)
        appliedAngle = angle
        return view
    }

    /** Moves and turns the cone, adding or removing it as [facing] comes and goes. */
    fun update(mapView: MKMapView, facing: DeviceFacing?) {
        if (image == null || facing == null || !facing.hasHeading) {
            if (added) {
                mapView.removeAnnotation(annotation)
                added = false
                appliedAngle = -999.0
            }
            return
        }

        heading = facing.headingDegrees
        annotation.setCoordinate(
            CLLocationCoordinate2DMake(facing.latitude, facing.longitude)
        )

        if (!added) {
            mapView.addAnnotation(annotation)
            added = true
            // The delegate applies the rotation as it builds the view.
            return
        }

        applyRotation(mapView)
    }

    /**
     * Re-applies the rotation after the map's own heading changed.
     *
     * Without this the wedge keeps pointing at the old north the moment the user turns
     * the map with two fingers, because the angle it is drawn at is a screen angle.
     */
    fun reapplyRotation(mapView: MKMapView) {
        if (!added) return
        applyRotation(mapView)
    }

    private fun applyRotation(mapView: MKMapView) {
        val angle = screenAngle(mapView)
        if (abs(angle - appliedAngle) < 0.001) return
        mapView.viewForAnnotation(annotation)?.transform =
            CGAffineTransformMakeRotation(angle)
        appliedAngle = angle
    }

    /** The compass heading with the map's own rotation taken out of it, in radians. */
    private fun screenAngle(mapView: MKMapView): Double {
        if (heading.isNaN()) return 0.0
        return (heading - mapView.camera.heading) * PI / 180.0
    }
}
