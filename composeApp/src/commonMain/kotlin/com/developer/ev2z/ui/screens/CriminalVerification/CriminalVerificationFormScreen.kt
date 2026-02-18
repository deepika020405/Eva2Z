package com.developer.ev2z.ui.screens.CriminalVerification

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CriminalVerificationFormScreen(
    isLoading: Boolean,
    onBack: () -> Unit,
    onSubmit: (CriminalForm) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var employeeId by rememberSaveable { mutableStateOf("") }
    var houseNo by rememberSaveable { mutableStateOf("") }
    var colony by rememberSaveable { mutableStateOf("") }
    var city by rememberSaveable { mutableStateOf("") }
    var state by rememberSaveable { mutableStateOf("") }
    var pincode by rememberSaveable { mutableStateOf("") }
    var landmark by rememberSaveable { mutableStateOf("") }
    var stayingPeriod by rememberSaveable { mutableStateOf("") }
    var consent by rememberSaveable { mutableStateOf(false) }

    // 🔥 NEW FIELDS
    var uanNumber by rememberSaveable { mutableStateOf("") }
    var rented by rememberSaveable { mutableStateOf(false) }

    var expanded by remember { mutableStateOf(false) }

    val stayingOptions = listOf(
        "Under 6 months",
        "6 months to 1 year",
        "2 years",
        "3 years",
        "More than 3 years"
    )

    val emailValid = email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"))
    val phoneValid = phone.length == 10
    val pincodeValid = pincode.length == 6

    val isFormValid =
        name.isNotBlank() &&
                emailValid &&
                phoneValid &&
                employeeId.isNotBlank() &&
                houseNo.isNotBlank() &&
                colony.isNotBlank() &&
                city.isNotBlank() &&
                state.isNotBlank() &&
                pincodeValid &&
                stayingPeriod.isNotBlank() &&
                consent

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {

            SectionCard("Personal Details") {

                InputField("Full Name", name) { name = it.trimStart() }

                InputField(
                    "Email Address",
                    email,
                    KeyboardType.Email,
                    email.isNotEmpty() && !emailValid,
                    "Enter valid email"
                ) { email = it.trim() }

                InputField(
                    "Phone Number",
                    phone,
                    KeyboardType.Phone,
                    phone.isNotEmpty() && !phoneValid,
                    "Must be 10 digits"
                ) {
                    phone = it.filter { ch -> ch.isDigit() }.take(10)
                }

                InputField("Employee ID", employeeId) {
                    employeeId = it.uppercase()
                }

                // 🔥 NEW → UAN (optional)
                InputField(
                    label = "UAN Number (optional)",
                    value = uanNumber,
                    keyboardType = KeyboardType.Number
                ) {
                    uanNumber = it.filter { ch -> ch.isDigit() }
                }
            }

            Spacer(Modifier.height(16.dp))

            SectionCard("Address Details") {

                InputField("House No", houseNo) { houseNo = it }
                InputField("Colony", colony) { colony = it }
                InputField("City", city) { city = it }
                InputField("State", state) { state = it }

                InputField(
                    "Pincode",
                    pincode,
                    KeyboardType.Number,
                    pincode.isNotEmpty() && !pincodeValid,
                    "Must be 6 digits"
                ) {
                    pincode = it.filter { ch -> ch.isDigit() }.take(6)
                }

                InputField("Landmark", landmark) { landmark = it }

                // 🔥 Rented Toggle
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = rented,
                        onCheckedChange = { rented = it }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("This is a rented property")
                }

                Spacer(Modifier.height(8.dp))

                // Staying Period Dropdown
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = stayingPeriod,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Staying Period") },
                        trailingIcon = {
                            Icon(Icons.Outlined.KeyboardArrowDown, null)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        isError = stayingPeriod.isBlank()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        stayingOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    stayingPeriod = option
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Consent
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
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
                    Text("I confirm the information is accurate.")
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    onSubmit(
                        CriminalForm(
                            name.trim(),
                            email.trim(),
                            phone,
                            employeeId.trim(),
                            consent,
                            houseNo.trim(),
                            colony.trim(),
                            city.trim(),
                            state.trim(),
                            pincode,
                            landmark.trim(),
                            stayingPeriod,

                            // 🔥 NEW FIELDS
                            uanNumber = uanNumber,
                            uanNotAvailable = uanNumber.isBlank(),
                            rented = rented
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                enabled = !isLoading && isFormValid
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text("Proceed to Payment")
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun InputField(
    label: String,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean = false,
    errorText: String = "",
    onChange: (String) -> Unit
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            label = { Text(label) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            isError = isError,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
        )

        if (isError) {
            Text(
                text = errorText,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}