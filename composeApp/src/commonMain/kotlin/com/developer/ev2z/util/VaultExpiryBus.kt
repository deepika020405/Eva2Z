package com.developer.ev2z.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

// Carries a "document is about to expire" notification tap from the platform layer
// (MainActivity intent extras) into HomeNavGraph, which then opens the vault gate
// and deep-links to the document once the user unlocks it.
//
// replay = 1 so a cold-start tap still reaches HomeNavGraph after it starts collecting,
// mirroring SosNotificationHandler.alertFlow.
object VaultExpiryBus {

    private val _expiryFlow = MutableSharedFlow<String>(replay = 1, extraBufferCapacity = 5)
    val expiryFlow: SharedFlow<String> = _expiryFlow.asSharedFlow()

    // Document type from the FCM payload ("dl", "rc", "puc", …) — informational only,
    // used for logging and any future type-specific routing.
    var lastDocumentType: String? = null
        private set

    fun onExpiryNotificationTapped(documentId: String, documentType: String? = null) {
        lastDocumentType = documentType
        _expiryFlow.tryEmit(documentId)
    }

    // Drop the replayed value once consumed so returning to Home later does not
    // re-trigger the vault gate.
    fun clear() {
        _expiryFlow.resetReplayCache()
        lastDocumentType = null
    }
}