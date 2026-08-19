package com.developer.ev2z.ui.screens.SupportAndWarrenty

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer.ev2z.navigation.PlatformBackHandler
import com.developer.ev2z.theme.AppPageBg


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportWarrantyScreen(
    data: SupportWarrantyUi,
    onBack: () -> Unit,
    onBuyWarranty: () -> Unit
) {
    PlatformBackHandler(true){
        onBack()
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Support & Warranty") },
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
        containerColor = AppPageBg
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // 🔹 DEVICE WARRANTY INFO (TRUST SECTION)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(Modifier.padding(16.dp)) {

                    Text(
                        "Device Warranty Information",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    Spacer(Modifier.height(12.dp))

                    InfoRow("Product", data.productName)
                    InfoRow("Device ID", data.deviceId)
                }
            }

            // 🔹 WARRANTY STATUS (SERVICE SECTION)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(Modifier.padding(16.dp)) {

                    Text(
                        "Warranty Details",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    Spacer(Modifier.height(12.dp))

                    InfoRow(
                        label = "Warranty Status",
                        value = if (data.warrantyStatus == WarrantyStatus.ACTIVE)
                            "Active" else "Expired",
                        valueColor =
                            if (data.warrantyStatus == WarrantyStatus.ACTIVE)
                                Color(0xFF1B9F4A) else Color.Red
                    )

                    InfoRow(
                        label = "Expiry Date",
                        value = data.warrantyExpiryDate
                    )

                    // 🔥 CLAIM WARRANTY CTA
                    if (data.warrantyStatus == WarrantyStatus.EXPIRED) {
                        Spacer(Modifier.height(16.dp))

                        Button(
                            onClick = onBuyWarranty,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1B9F4A)
                            )
                        ) {
                            Text("Buy 1-Year Warranty @ ₹399")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String,
    valueColor: Color = Color.Black
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            fontSize = 13.sp,
            color = Color.Gray
        )
        Text(
            value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}