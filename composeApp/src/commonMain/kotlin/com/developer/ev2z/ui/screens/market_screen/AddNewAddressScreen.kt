package com.developer.ev2z.ui.screens.market_screen


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.developer.ev2z.BackPressHandler
import com.developer.ev2z.util.Analytics
import kotlin.compareTo

class AddNewAddressScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel = ShopViewModelProvider.viewModel
        val navigator = LocalNavigator.currentOrThrow

        LaunchedEffect(Unit) {
            Analytics.logEvent(
                name = "screen_view",
                params = mapOf(
                    "screen_name" to "add_shipping_address",
                    "screen_class" to "AddNewAddressScreen"
                )
            )
        }

        BackPressHandler { navigator.pop() }

        Scaffold(
            containerColor = Color(0xFFF5F5F5), // ✅ background color
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Add New Address",
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Text(
                                text = "←",
                                fontSize = 22.sp,
                                color = Color.Black
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White,      // ✅ top bar background
                        titleContentColor = Color.Black,   // title color
                        navigationIconContentColor = Color.Black
                    )
                )
            },
            contentWindowInsets = WindowInsets(0)

        ) { padding ->

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color(0xFFF5F5F5))
            ) {
                AddAddressUI(
                    onSave = { fname, phone, email, address, city, state, pincode ->

                        viewModel.addNewAddress(
                            fname = fname,
                            phone = phone,
                            email = email,
                            address = address,
                            city = city,
                            state = state,
                            pincode = pincode
                        ) {
                            Analytics.logEvent(
                                name = "add_shipping_info",
                                params = mapOf(
                                    "shipping_city" to city,
                                    "shipping_state" to state,
                                    "shipping_pincode" to pincode,
                                    "country" to "IN"
                                )
                            )

                            navigator.pop()
                        }
                    },
                    onCancel = { navigator.pop() }
                )
            }
        }
    }
}

@Composable
fun AddAddressUI(
    onSave: (
        String, String, String, String, String, String, String
    ) -> Unit,
    onCancel: () -> Unit
) {
    var fname by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var pincode by remember { mutableStateOf("") }

    // 🔴 Errors
    var fnameErr by remember { mutableStateOf<String?>(null) }
    var phoneErr by remember { mutableStateOf<String?>(null) }
    var emailErr by remember { mutableStateOf<String?>(null) }
    var addressErr by remember { mutableStateOf<String?>(null) }
    var cityErr by remember { mutableStateOf<String?>(null) }
    var stateErr by remember { mutableStateOf<String?>(null) }
    var pincodeErr by remember { mutableStateOf<String?>(null) }

    fun validate(): Boolean {
        var valid = true

        fnameErr = if (fname.isBlank()) "Full name required" else null
        phoneErr =
            if (phone.length != 10) "Phone number must be 10 digits" else null
        emailErr =
            if (!isValidEmail(email))
                "Invalid email address" else null
        addressErr = if (address.isBlank()) "Address required" else null
        cityErr = if (city.isBlank()) "City required" else null
        stateErr = if (state.isBlank()) "State required" else null
        pincodeErr =
            if (pincode.length != 6) "Pincode must be 6 digits" else null

        listOf(
            fnameErr, phoneErr, emailErr,
            addressErr, cityErr, stateErr, pincodeErr
        ).forEach { if (it != null) valid = false }

        return valid
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {

//        Text(
//            "Add New Address",
//            fontSize = 22.sp,
//            fontWeight = FontWeight.Bold
//        )
//
//        Spacer(Modifier.height(20.dp))

        ValidatedInput("Full Name", fname, { fname = it }, fnameErr)

        ValidatedInput(
            "Phone Number",
            phone,
            {
                if (it.all { ch -> ch.isDigit() } && it.length <= 10) {
                    phone = it
                }
            },
            phoneErr,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            )
        )

        ValidatedInput(
            "Email Address",
            email,
            { email = it },
            emailErr,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        ValidatedInput("House / Street", address, { address = it }, addressErr)
        ValidatedInput("City", city, { city = it }, cityErr)
        ValidatedInput("State", state, { state = it }, stateErr)

        ValidatedInput(
            "Pincode",
            pincode,
            {
                if (it.all { ch -> ch.isDigit() } && it.length <= 6) {
                    pincode = it
                }
            },
            pincodeErr,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            )
        )

        Spacer(Modifier.height(10.dp))

        Button(
            onClick = {
                if (validate()) {
                    onSave(
                        fname, phone, email,
                        address, city, state, pincode
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(Color(0xFF34A853))
        ) {
            Text("Save Address", color = Color.White)
        }

        Spacer(Modifier.height(8.dp))

        TextButton(
            onClick = onCancel,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Cancel")
        }
    }
}

private fun isValidEmail(email: String): Boolean {
    val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"
    return email.matches(emailRegex.toRegex())
}


@Composable
fun ValidatedInput(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    error: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    var isFocused by remember { mutableStateOf(false) }

    // ✅ LaunchedEffect OUTSIDE callbacks
    LaunchedEffect(isFocused) {
        if (isFocused) {
            bringIntoViewRequester.bringIntoView()
        }
    }

    Column(
        modifier = Modifier
            .bringIntoViewRequester(bringIntoViewRequester)
    ) {
        Text(label, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))

        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged {
                    isFocused = it.isFocused
                },
            isError = error != null,
            singleLine = true,
            keyboardOptions = keyboardOptions,
            shape = RoundedCornerShape(10.dp)
        )

        if (error != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp
            )
        }

        Spacer(Modifier.height(14.dp))
    }
}