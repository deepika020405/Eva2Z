package com.developer.ev2z.calling

// Started when IncomingCallScreen is shown; stopped when the screen is dismissed.
// Platform implementations connect to the signaling server and emit to SosNotificationHandler.
expect fun startIncomingCallWatcher(roomId: String)
expect fun stopIncomingCallWatcher()