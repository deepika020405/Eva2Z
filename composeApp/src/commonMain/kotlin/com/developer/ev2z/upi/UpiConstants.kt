package com.developer.ev2z.upi

import com.developer.ev2z.data.local.model.UpiApp

val UPI_APPS = listOf(
    UpiApp("Google Pay", "com.google.android.apps.nbu.paisa.user", "gpay://upi"),
    UpiApp("PhonePe", "com.phonepe.app", "phonepe://upi"),
    UpiApp("Paytm", "net.one97.paytm", "paytmmp://upi"),
    UpiApp("BHIM", "in.org.npci.upiapp", "bhim://upi")
)