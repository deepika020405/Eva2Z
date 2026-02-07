package com.developer.ev2z.ui.screens.BillDetails

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer.ev2z.util.generateInvoicePdf
import com.developer.ev2z.util.openPdfFile
import com.developer.ev2z.ui.screens.BillDetails.toInvoiceData
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillDetailsScreen(
    bills: List<BillTransactionUi>,
    onBack: () -> Unit,
    onDownloadInvoice: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Billing & Invoices",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        containerColor = Color(0xFFF6F7FB)
    ) { padding ->

        if (bills.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No billing history found",
                    color = Color.Gray
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(bills) { bill ->
                    BillTransactionCard(
                        bill = bill,
                        onDownloadInvoice = onDownloadInvoice
                    )
                }
            }
        }
    }
}

@Composable
fun BillTransactionCard(
    bill: BillTransactionUi,
    onDownloadInvoice: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Order ID", fontSize = 12.sp, color = Color.Gray)
                    Text(bill.orderId, fontWeight = FontWeight.Bold)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(bill.orderDate, fontSize = 12.sp, color = Color.Gray)
                    Text(
                        bill.totalAmount,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF1B9F4A)
                    )
                }
            }

            Divider()

            Spacer(Modifier.height(6.dp))

            Text(
                "Purchased Items",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )

            bill.items.forEach { item ->
                BillItemRow(item)
            }

            Spacer(Modifier.height(8.dp))

            // ✅ ONLY CALLBACK — NO openUrl HERE
            Button(
                onClick = {
                    val invoice = bill.toInvoiceData()
                    val path = generateInvoicePdf(invoice)
                    openPdfFile(path)
                }
            ) {
                Text("Download Invoice")
            }
        }
    }
}

@Composable
fun BillItemRow(item: BillItemUi) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            "${item.productName}  x${item.quantity}",
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp
        )

        val meta = listOfNotNull(
            item.planType?.replace("_", " ")?.uppercase()
        ).joinToString(" • ")

        if (meta.isNotEmpty()) {
            Text(
                meta,
                fontSize = 11.sp,
                color = Color.Gray
            )
        }

        Text(
            item.price,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
        )
    }
}