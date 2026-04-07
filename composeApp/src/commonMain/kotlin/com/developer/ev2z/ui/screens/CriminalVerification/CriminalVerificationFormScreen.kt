package com.developer.ev2z.ui.screens.CriminalVerification

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
        modifier = Modifier
            .fillMaxSize()
            .background(CV_Background),
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

                InputField(
                    label = "UAN Number (optional)",
                    value = uanNumber,
                    keyboardType = KeyboardType.Number
                ) {
                    uanNumber = it.filter { ch -> ch.isDigit() }
                }
            }

            Spacer(Modifier.height(14.dp))

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

                // Rented Toggle
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CV_SectionBg,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = rented,
                            onCheckedChange = { rented = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = CV_Primary,
                                uncheckedColor = CV_TextHint
                            )
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "This is a rented property",
                            fontSize = 14.sp,
                            color = CV_TextPrimary
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

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
                            Icon(
                                Icons.Outlined.KeyboardArrowDown,
                                null,
                                tint = CV_TextHint
                            )
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CV_Primary,
                            unfocusedBorderColor = CV_Border,
                            focusedLabelColor = CV_Primary,
                            cursorColor = CV_Primary
                        )
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        stayingOptions.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        option,
                                        fontSize = 14.sp,
                                        color = CV_TextPrimary
                                    )
                                },
                                onClick = {
                                    stayingPeriod = option
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Consent
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CV_PrimaryBg),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = consent,
                        onCheckedChange = { consent = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = CV_Primary,
                            uncheckedColor = CV_TextHint
                        )
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "I confirm the information is accurate.",
                        fontSize = 14.sp,
                        color = CV_TextPrimary
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

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
                            uanNumber = uanNumber,
                            uanNotAvailable = uanNumber.isBlank(),
                            rented = rented
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !isLoading && isFormValid,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CV_Primary,
                    disabledContainerColor = CV_Primary.copy(alpha = 0.4f)
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp),
                        color = Color.White
                    )
                } else {
                    Text(
                        "Proceed to Payment",
                        fontWeight = FontWeight.SemiBold
                    )
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
        colors = CardDefaults.cardColors(containerColor = CV_Surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = CV_TextPrimary
            )
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
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            isError = isError,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CV_Primary,
                unfocusedBorderColor = CV_Border,
                focusedLabelColor = CV_Primary,
                cursorColor = CV_Primary,
                errorBorderColor = CV_Error
            )
        )

        if (isError) {
            Text(
                text = errorText,
                color = CV_Error,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}