package com.developer.ev2z.ui.screens.BillDetails


import com.developer.ev2z.response.MyBillingResponse

fun MyBillingResponse.toUiBills(): List<BillTransactionUi> {
    return billings.map { order ->

        val safeBilling = order.billing

        BillTransactionUi(
            orderId = order.orderId,
            transactionId = order.transactionId,
            orderDate = order.orderDate.take(10),
            totalAmount = "₹${order.orderTotal}",


            billing = BillingUi(
                name = safeBilling?.name ?: "N/A",
                phone = safeBilling?.phone ?: "N/A",
                email = safeBilling?.email ?: "N/A",
                city = safeBilling?.city ?: "N/A",
                state = safeBilling?.state ?: "N/A",
                pincode = safeBilling?.pincode ?: "N/A"
            ),

            items = order.items.map { item ->
                BillItemUi(
                    productName = item.productName.trim(),
                    quantity = item.quantity,
                    planType = item.planType ?: "",
                    price = "₹${item.price}",
                    rechargeAmount = "₹${item.rechargeAmount}",
                    deviceId = null // until backend gives it
                )
            },

            invoiceUrl = ""
        )
    }
}

fun BillTransactionUi.toInvoiceData(): InvoiceData {
    return InvoiceData(
        orderId = orderId.toString(),
        orderDate = orderDate,
        totalAmount = totalAmount,

        customerName = billing.name,
        phone = billing.phone,
        email = billing.email,
        city = billing.city,
        state = billing.state,
        pincode = billing.pincode,

        items = items.map {
            InvoiceItem(
                name = it.productName,
                quantity = it.quantity,
                planType = it.planType,
                price = it.price
            )
        }
    )
}