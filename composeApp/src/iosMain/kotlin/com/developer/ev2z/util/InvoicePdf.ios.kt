@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.developer.ev2z.util

import com.developer.ev2z.ui.screens.BillDetails.InvoiceData
import platform.CoreGraphics.*
import platform.Foundation.*
import platform.UIKit.*

actual fun generateInvoicePdf(invoice: InvoiceData): String {

    val dir = NSSearchPathForDirectoriesInDomains(
        NSDocumentDirectory,
        NSUserDomainMask,
        true
    ).first() as String

    val path = "$dir/invoice_${invoice.orderId}.pdf"

    UIGraphicsBeginPDFContextToFile(path, CGRectMake(0.0, 0.0, 595.0, 842.0), null)
    UIGraphicsBeginPDFPage()

    var y = 40.0

    fun text(t: String, x: Double, size: Double, bold: Boolean = false) {
        val font = if (bold)
            UIFont.boldSystemFontOfSize(size)
        else
            UIFont.systemFontOfSize(size)

        NSString.create(t).drawAtPoint(
            CGPointMake(x, y),
            mapOf(NSFontAttributeName to font)
        )
    }

    text("INVOICE", 230.0, 22.0, true)
    y += 40

    text("Order ID: ${invoice.orderId}", 40.0, 12.0)
    y += 18
    text("Date: ${invoice.orderDate}", 40.0, 12.0)
    y += 18
    text("Total: ${invoice.totalAmount}", 40.0, 12.0)

    y += 30
    text("BILL TO", 40.0, 14.0, true)
    y += 18
    text(invoice.customerName, 40.0, 12.0)

    y += 30
    text("ITEMS", 40.0, 14.0, true)
    y += 20

    invoice.items.forEach {
        text("${it.name} x${it.quantity} - ${it.price}", 40.0, 12.0)
        y += 18
    }

    UIGraphicsEndPDFContext()
    return path
}