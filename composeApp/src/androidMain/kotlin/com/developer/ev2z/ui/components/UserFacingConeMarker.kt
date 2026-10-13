package com.developer.ev2z.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import com.developer.ev2z.util.DeviceFacing
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMapComposable
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberUpdatedMarkerState

/**
 * The wedge showing where the phone is and which way its holder is facing, for the
 * maps built with `GoogleMap { … }`.
 *
 * Draws nothing until there is both a position and a compass reading — which covers
 * refused location permission, a device with no magnetometer, and the first second
 * after the screen opens. A cone pointing north because nothing has been measured yet
 * would be a straight-faced lie about where someone is looking.
 *
 * The blue dot underneath is the map's own my-location indicator; this is only the
 * wedge, so the two cannot end up as two dots a metre apart.
 */
@Composable
@GoogleMapComposable
internal fun UserFacingConeMarker(facing: DeviceFacing?) {
    if (facing == null || !facing.hasHeading) return

    val context = LocalContext.current
    val icon = remember { userFacingIcon(context) } ?: return

    Marker(
        state = rememberUpdatedMarkerState(
            position = LatLng(facing.latitude, facing.longitude)
        ),
        icon = icon,
        // The wedge's apex is the middle of its bitmap, and that is the spot the
        // person is standing on.
        anchor = Offset(0.5f, 0.5f),
        // Flat: lies on the map and turns with it, so the heading stays measured from
        // real north however the user has spun the map round.
        flat = true,
        rotation = facing.headingDegrees.toFloat(),
        // Under the vehicles. It is context for them, not a thing to read in itself.
        zIndex = -1f,
        // Swallowed rather than handled: a callout on "you are here, facing that way"
        // is noise, and without this the map would recentre on every accidental tap.
        onClick = { true },
    )
}
