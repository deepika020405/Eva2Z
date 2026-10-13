package com.developer.ev2z.ui.screens.device_Report


import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.developer.ev2z.ui.screens.trackTrace.TrailPoint

@Composable
expect fun PlatformMapView(
    modifier: Modifier,
    latitude: Double,
    longitude: Double,
    zoom: Double,
    trailPoints: List<TrailPoint> = emptyList(),
    /**
     * The vehicle's state in the app's own vocabulary — a value out of
     * `statusKindOf()`, never a raw server string, because the arrow's colour has to
     * agree with the badge next to it and the two read different fields.
     *
     * Defaulted so a caller that has no status still gets a drawn marker rather than
     * no marker; it comes out in the neutral colour.
     */
    statusKind: String = "unknown",
    /**
     * The vehicle type the tracker is fitted to, as the slug out of
     * `LatestLocation.iconSlug` ("electric-scooter"). The marker draws that vehicle
     * in the status colour.
     *
     * Null — which is every device the server has no vehicle for — falls back to the
     * heading arrow, so a device with nothing to say about itself still shows which
     * way it is pointing.
     */
    vehicleSlug: String? = null,
    /**
     * Street address drawn in a chip above the marker, so the map answers "where is
     * it" without the owner having to read the map.
     *
     * Null draws no chip at all — an empty or half-resolved address would put a blank
     * white box over the vehicle.
     */
    addressLabel: String? = null,
    /**
     * Which way the vehicle is facing, in degrees clockwise from north.
     *
     * Supplied by the caller when it knows better than this map can: the live screen
     * walks the marker along snapped road geometry and takes the heading from the road
     * ahead, which is what brings the vehicle round a roundabout facing the way it is
     * going. NaN — the default — falls back to measuring it here from how the
     * coordinates move, which is all a caller with no road to follow can offer.
     */
    headingDegrees: Double = Double.NaN
)
