package com.developer.ev2z.util

import com.developer.ev2z.ui.screens.BillDetails.InvoiceData

expect fun generateInvoicePdf(
    invoice: InvoiceData
): String