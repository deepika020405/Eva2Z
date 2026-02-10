package com.developer.ev2z.ui.screens.BillDetails


import com.developer.ev2z.response.MyBillingResponse

fun MyBillingResponse.toUiBills(): List<BillTransactionUi> {
    return billings.map { order ->
        BillTransactionUi(
            orderId = order.orderId,
            transactionId = order.transactionId,
            orderDate = order.orderDate.substring(0, 10),
            totalAmount = "₹${order.orderTotal}",

            billing = BillingUi(
                name = order.billing.name,
                phone = order.billing.phone,
                email = order.billing.email,
                city = order.billing.city,
                state = order.billing.state,
                pincode = order.billing.pincode
            ),

            items = order.items.map { item ->
                BillItemUi(
                    productName = item.productName.trim(),
                    quantity = item.quantity,
                    planType = item.planType,
                    price = "₹${item.price}",
                    rechargeAmount = "₹${item.rechargeAmount}"
                )
            },

            invoiceUrl = "" // local PDF later
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