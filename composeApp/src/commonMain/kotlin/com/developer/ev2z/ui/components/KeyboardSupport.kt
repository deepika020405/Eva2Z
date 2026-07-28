package com.developer.ev2z.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp

/**
 * True while the soft keyboard is on screen. Backed by [WindowInsets.ime], which both
 * Android and Compose/iOS report, so screens can collapse headers to make room for the
 * fields that are still visible above the keyboard.
 */
@Composable
fun isKeyboardVisible(): Boolean {
    val density = LocalDensity.current
    val ime = WindowInsets.ime
    // derivedStateOf so callers recompose when the keyboard appears/disappears, not on
    // every frame of its slide-in animation.
    val visible = remember(density, ime) { derivedStateOf { ime.getBottom(density) > 0 } }
    return visible.value
}

/**
 * Height of the whole window, not of the nearest parent. `BoxWithConstraints` inside a
 * pager/card only sees the slice it was given, which made every auth screen think it was
 * running on a compact device.
 */
@Composable
fun screenHeightDp(): Dp =
    with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() }

/** Taps on empty space dismiss the keyboard; child clicks still win. */
fun Modifier.dismissKeyboardOnTap(focusManager: FocusManager): Modifier =
    pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) }