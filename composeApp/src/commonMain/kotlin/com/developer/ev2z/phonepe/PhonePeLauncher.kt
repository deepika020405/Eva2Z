package com.developer.ev2z.phonepe

/**
 * Opens PhonePe's checkout for an order that the backend has already created.
 *
 * Both arguments come from the create-order response and belong together: PhonePe
 * resolves [orderToken] *against* [orderId], so a locally invented id (which is what
 * this used to do on both platforms) makes the checkout page fail with a generic
 * "Something went wrong" before the user can pay.
 *
 * @param orderToken `order.raw_response.token` from the backend.
 * @param orderId    `phonepeOrderId` from the backend — never generated on device.
 */
expect fun openPhonePeSdk(
    orderToken: String,
    orderId: String,
    onResult: (PaymentResult) -> Unit
)