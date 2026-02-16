package com.developer.ev2z.phonepe


//object PaymentBus {
//
//    private var listener: ((Boolean, String?, String?) -> Unit)? = null
//
//    fun register(
//        block: (success: Boolean, txnId: String?, reason: String?) -> Unit
//    ) {
//        listener = block
//    }
//
//    fun onResult(
//        success: Boolean,
//        txnId: String?,
//        reason: String?
//    ) {
//        listener?.invoke(success, txnId, reason)
//    }
//
//    fun clear() {
//        listener = null
//    }
//
//    fun notify(success: Boolean, txnId: String?, reason: String?) {
//        listener?.invoke(success, txnId, reason)  // ✅ callback ki jagah listener
//        listener = null  // ✅ callback ki jagah listener
//    }


object PaymentBus {

    private var listener: ((Boolean, String?, String?) -> Unit)? = null

    fun register(
        block: (success: Boolean, txnId: String?, reason: String?) -> Unit
    ) {
        listener = block
    }

    fun notify(success: Boolean, txnId: String?, reason: String?) {
        listener?.invoke(success, txnId, reason)
        listener = null
    }

    fun clear() {
        listener = null
    }
}