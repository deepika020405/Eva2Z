package com.developer.ev2z.ui.screens.CriminalVerification

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SearchBarDefaults.InputField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CriminalVerificationFormScreen(
    isLoading: Boolean,
    onBack: () -> Unit,
    onSubmit: (CriminalForm) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var employeeId by remember { mutableStateOf("") }
    var houseNo by remember { mutableStateOf("") }
    var colony by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var pincode by remember { mutableStateOf("") }
    var landmark by remember { mutableStateOf("") }
    var stayingPeriod by remember { mutableStateOf("") }
    var consent by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {

        // 🔹 Header
        Text(
            text = "Criminal Verification",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(Modifier.height(16.dp))

        // 🔹 Personal Details Card
        FormSectionCard(title = "Personal Details") {

            FormInputField("Full Name", name) { name = it }
            FormInputField("Email Address", email) { email = it }
            FormInputField("Phone Number", phone) { phone = it }
            FormInputField("Employee ID", employeeId) { employeeId = it }
        }

        Spacer(Modifier.height(16.dp))

        // 🔹 Address Card
        FormSectionCard(title = "Address Details") {

            FormInputField("House No", houseNo) { houseNo = it }
            FormInputField("Colony", colony) { colony = it }
            FormInputField("City", city) { city = it }
            FormInputField("State", state) { state = it }
            FormInputField("Pincode", pincode) { pincode = it }
            FormInputField("Landmark", landmark) { landmark = it }
            FormInputField("Staying Period", stayingPeriod) { stayingPeriod = it }
        }

        Spacer(Modifier.height(16.dp))

        // 🔹 Consent Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F7F7))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = consent,
                    onCheckedChange = { consent = it }
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "I confirm that the provided information is accurate.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // 🔹 Submit Button
        Button(
            onClick = {
                onSubmit(
                    CriminalForm(
                        name, email, phone, employeeId, consent,
                        houseNo, colony, city, state, pincode,
                        landmark, stayingPeriod
                    )
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = !isLoading
        ) {
            Text(
                if (isLoading) "Processing..." else "Proceed to Payment",
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun FormSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}


@Composable
fun FormInputField(
    label: String,
    value: String,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    )
}


@Composable
fun InputField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    )
}