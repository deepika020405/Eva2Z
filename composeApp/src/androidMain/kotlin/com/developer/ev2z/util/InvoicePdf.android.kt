package com.developer.ev2z.util

import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.developer.ev2z.ui.screens.BillDetails.InvoiceData
import java.io.File
import java.io.FileOutputStream

actual fun generateInvoicePdf(
    invoice: InvoiceData
): String {

    val context = OpenUrlHolder.context

    val pdf = PdfDocument()
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
    val page = pdf.startPage(pageInfo)
    val canvas = page.canvas

    var y = 40f

    /* ---------------- HEADER ---------------- */

    paint.textSize = 24f
    paint.isFakeBoldText = true
    canvas.drawText("EV2Z TECHNOLOGIES", 40f, y, paint)

    paint.textSize = 12f
    paint.isFakeBoldText = false
    y += 22
    canvas.drawText("GST Invoice", 40f, y, paint)

    paint.textAlign = Paint.Align.RIGHT
    paint.isFakeBoldText = true
    canvas.drawText("INVOICE", 555f, 40f, paint)
    paint.textAlign = Paint.Align.LEFT

    y += 20
    canvas.drawLine(40f, y, 555f, y, paint)

    /* ---------------- ORDER INFO ---------------- */

    y += 30
    paint.isFakeBoldText = true
    canvas.drawText("Order ID:", 40f, y, paint)
    paint.isFakeBoldText = false
    canvas.drawText(invoice.orderId, 140f, y, paint)

    y += 18
    paint.isFakeBoldText = true
    canvas.drawText("Order Date:", 40f, y, paint)
    paint.isFakeBoldText = false
    canvas.drawText(invoice.orderDate, 140f, y, paint)

    /* ---------------- BILL TO ---------------- */

    y += 30
    paint.isFakeBoldText = true
    canvas.drawText("BILL TO", 40f, y, paint)

    paint.isFakeBoldText = false
    y += 18
    canvas.drawText(invoice.customerName, 40f, y, paint)
    y += 16
    canvas.drawText("Phone: ${invoice.phone}", 40f, y, paint)
    y += 16
    canvas.drawText("Email: ${invoice.email}", 40f, y, paint)
    y += 16
    canvas.drawText(
        "${invoice.city}, ${invoice.state} - ${invoice.pincode}",
        40f,
        y,
        paint
    )

    /* ---------------- ITEMS TABLE ---------------- */

    y += 30
    paint.isFakeBoldText = true
    canvas.drawText("ITEM DETAILS", 40f, y, paint)

    y += 15
    canvas.drawLine(40f, y, 555f, y, paint)

    y += 20
    paint.isFakeBoldText = true
    canvas.drawText("Product", 40f, y, paint)
    canvas.drawText("Plan", 260f, y, paint)
    canvas.drawText("Qty", 360f, y, paint)
    canvas.drawText("Price", 450f, y, paint)

    paint.isFakeBoldText = false
    y += 12
    canvas.drawLine(40f, y, 555f, y, paint)

    y += 20
    invoice.items.forEach { item ->
        canvas.drawText(item.name.trim(), 40f, y, paint)
        canvas.drawText(
            item.planType?.replace("_", " ")?.uppercase() ?: "-",
            260f,
            y,
            paint
        )
        canvas.drawText(item.quantity.toString(), 360f, y, paint)
        canvas.drawText(item.price, 450f, y, paint)
        y += 20
    }

    /* ---------------- TOTAL ---------------- */

    y += 15
    canvas.drawLine(40f, y, 555f, y, paint)

    y += 28
    paint.isFakeBoldText = true
    paint.textSize = 16f
    canvas.drawText("TOTAL AMOUNT", 340f, y, paint)
    canvas.drawText(invoice.totalAmount, 450f, y, paint)

    /* ---------------- FOOTER ---------------- */

    paint.textSize = 10f
    paint.isFakeBoldText = false
    y += 50
    canvas.drawText(
        "This is a computer generated invoice and does not require a signature.",
        120f,
        y,
        paint
    )

    y += 14
    canvas.drawText(
        "Thank you for choosing EV2Z 🚗📍",
        200f,
        y,
        paint
    )

    pdf.finishPage(page)

    val file = File(
        context.getExternalFilesDir(null),
        "invoice_${invoice.orderId}.pdf"
    )

    pdf.writeTo(FileOutputStream(file))
    pdf.close()

    return file.absolutePath
}