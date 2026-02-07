package com.developer.ev2z.util


import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.developer.ev2z.ui.screens.BillDetails.InvoiceData
import com.developer.ev2z.util.OpenUrlHolder
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

    // ---------- HEADER ----------
    paint.textSize = 22f
    paint.isFakeBoldText = true
    canvas.drawText("INVOICE", 220f, y, paint)

    paint.textSize = 12f
    paint.isFakeBoldText = false
    y += 30
    canvas.drawLine(40f, y, 555f, y, paint)

    // ---------- ORDER INFO ----------
    y += 25
    paint.isFakeBoldText = true
    canvas.drawText("Order ID:", 40f, y, paint)
    paint.isFakeBoldText = false
    canvas.drawText(invoice.orderId, 120f, y, paint)

    y += 18
    paint.isFakeBoldText = true
    canvas.drawText("Order Date:", 40f, y, paint)
    paint.isFakeBoldText = false
    canvas.drawText(invoice.orderDate, 120f, y, paint)

    // ---------- BILLING INFO ----------
    y += 30
    paint.isFakeBoldText = true
    canvas.drawText("BILL TO", 40f, y, paint)

    paint.isFakeBoldText = false
    y += 18
    canvas.drawText(invoice.customerName, 40f, y, paint)
    y += 16
    canvas.drawText(invoice.phone, 40f, y, paint)
    y += 16
    canvas.drawText(invoice.email, 40f, y, paint)
    y += 16
    canvas.drawText(
        "${invoice.city}, ${invoice.state} - ${invoice.pincode}",
        40f,
        y,
        paint
    )

    // ---------- ITEMS TABLE ----------
    y += 30
    paint.isFakeBoldText = true
    canvas.drawText("ITEMS", 40f, y, paint)

    y += 15
    canvas.drawLine(40f, y, 555f, y, paint)

    y += 20
    paint.isFakeBoldText = true
    canvas.drawText("Item", 40f, y, paint)
    canvas.drawText("Qty", 350f, y, paint)
    canvas.drawText("Plan", 250f, y, paint)   // ✅ NEW
    canvas.drawText("Price", 420f, y, paint)


    paint.isFakeBoldText = false
    y += 15
    canvas.drawLine(40f, y, 555f, y, paint)

    y += 18
    invoice.items.forEach {
        canvas.drawText(it.name, 40f, y, paint)
        canvas.drawText(it.quantity.toString(), 360f, y, paint)
        canvas.drawText(it.planType ?: "-", 250f, y, paint)
        canvas.drawText("₹${it.price}", 420f, y, paint)
        y += 18
    }

    // ---------- TOTAL ----------
    y += 20
    canvas.drawLine(40f, y, 555f, y, paint)

    y += 25
    paint.isFakeBoldText = true
    paint.textSize = 16f
    canvas.drawText("TOTAL AMOUNT", 320f, y, paint)
    canvas.drawText("₹${invoice.totalAmount}", 450f, y, paint)

    // ---------- FOOTER ----------
    paint.textSize = 10f
    paint.isFakeBoldText = false
    y += 40
    canvas.drawText(
        "Thank you for your purchase!",
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