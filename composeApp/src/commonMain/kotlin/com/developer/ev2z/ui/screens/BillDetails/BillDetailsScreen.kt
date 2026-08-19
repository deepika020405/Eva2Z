package com.developer.ev2z.ui.screens.BillDetails

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer.ev2z.navigation.PlatformBackHandler
import com.developer.ev2z.util.generateInvoicePdf
import com.developer.ev2z.util.openPdfFile
import com.developer.ev2z.theme.AppPageBg

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillDetailsScreen(
    bills: List<BillTransactionUi>,
    onBack: () -> Unit
) {
    PlatformBackHandler(true) { onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Billing & Invoices",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color(0xFF222222)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Back",
                            tint = Color(0xFF333333)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppPageBg
                )
            )
        },
        containerColor = AppPageBg,
        contentWindowInsets = WindowInsets(0)
    ) { padding ->

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val screenWidth = maxWidth
            val screenHeight = maxHeight
            val isCompact = screenHeight < 700.dp
            val horizontalPad = (screenWidth * 0.04f).coerceIn(12.dp, 20.dp)

            if (bills.isEmpty()) {
                // Empty State
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(
                                    Color(0xFFF0F7EC),
                                    RoundedCornerShape(20.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Receipt,
                                contentDescription = null,
                                tint = Color(0xFF6A9F50),
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        Text(
                            "No billing history found",
                            fontSize = if (isCompact) 16.sp else 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF222222)
                        )

                        Spacer(Modifier.height(6.dp))

                        Text(
                            "Your invoices and transactions will appear here.",
                            fontSize = if (isCompact) 13.sp else 14.sp,
                            color = Color(0xFF888888),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        horizontal = horizontalPad,
                        vertical = if (isCompact) 10.dp else 14.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(if (isCompact) 12.dp else 14.dp)
                ) {
                    items(bills) { bill ->
                        BillTransactionCard(
                            bill = bill,
                            isCompact = isCompact
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BillTransactionCard(
    bill: BillTransactionUi,
    isCompact: Boolean = false
) {
    val cardPad = if (isCompact) 14.dp else 16.dp
    val titleSize = if (isCompact) 14.sp else 15.sp
    val labelSize = if (isCompact) 11.sp else 12.sp
    val amountSize = if (isCompact) 15.sp else 16.sp

    Card(
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            // Green accent strip
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFF6A9F50), Color(0xFF8BC34A))
                        )
                    )
            )

            Column(
                modifier = Modifier.padding(cardPad),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header: Order ID + Date/Amount
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "Order ID",
                            fontSize = labelSize,
                            color = Color(0xFF999999),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            bill.orderId.toString(),
                            fontWeight = FontWeight.Bold,
                            fontSize = titleSize,
                            color = Color(0xFF222222)
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            bill.orderDate,
                            fontSize = labelSize,
                            color = Color(0xFF999999),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            bill.totalAmount,
                            fontWeight = FontWeight.Bold,
                            fontSize = amountSize,
                            color = Color(0xFF6A9F50)
                        )
                    }
                }

                HorizontalDivider(
                    color = Color(0xFFF0F0F0),
                    thickness = 0.8.dp
                )

                // Purchased Items
                Text(
                    "Purchased Items",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = if (isCompact) 13.sp else 14.sp,
                    color = Color(0xFF333333)
                )

                bill.items.forEach { item ->
                    BillItemRow(item, isCompact)
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Download Invoice Button
                Button(
                    onClick = {
                        val invoice = bill.toInvoiceData()
                        val path = generateInvoicePdf(invoice)
                        openPdfFile(path)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isCompact) 44.dp else 46.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6A9F50),
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 2.dp,
                        pressedElevation = 4.dp
                    )
                ) {
                    Text(
                        "Download Invoice",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = if (isCompact) 14.sp else 15.sp
                    )
                }
            }
        }
    }
}

@Composable
fun BillItemRow(
    item: BillItemUi,
    isCompact: Boolean = false
) {
    val nameSize = if (isCompact) 13.sp else 14.sp
    val metaSize = if (isCompact) 11.sp else 12.sp
    val priceSize = if (isCompact) 13.sp else 14.sp

    Surface(
        color = Color(0xFFFAFAFA),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${item.productName}  x${item.quantity}",
                    fontWeight = FontWeight.Medium,
                    fontSize = nameSize,
                    color = Color(0xFF333333),
                    modifier = Modifier.weight(1f)
                )

                Text(
                    item.price,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = priceSize,
                    color = Color(0xFF6A9F50)
                )
            }

            val meta = listOfNotNull(
                item.planType?.replace("_", " ")?.uppercase()
            ).joinToString(" • ")

            if (meta.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    meta,
                    fontSize = metaSize,
                    color = Color(0xFF999999)
                )
            }
        }
    }
}