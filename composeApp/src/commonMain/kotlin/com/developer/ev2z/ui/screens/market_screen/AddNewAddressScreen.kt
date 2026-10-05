package com.developer.ev2z.ui.screens.market_screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

/**
 * Add a delivery address, or edit one when [existing] is passed — the cart's address
 * list opens this screen with the address to change.
 */
class AddNewAddressScreen(
    private val existing: BillingAddress? = null
) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel = ShopViewModelProvider.viewModel
        val navigator = LocalNavigator.currentOrThrow
        val isEdit = existing != null

        LaunchedEffect(Unit) {
            Analytics.logEvent("screen_view", mapOf(
                "screen_name" to if (isEdit) "edit_shipping_address" else "add_shipping_address"))
        }
        BackPressHandler { navigator.pop() }

        Scaffold(
            containerColor = P_Bg,
            topBar = {
                TopAppBar(
                    title = { Text(if (isEdit) "Edit Address" else "Add Address",
                        fontWeight = FontWeight.SemiBold, fontSize = 17.sp) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Box(modifier = Modifier.size(34.dp).clip(CircleShape)
                                .background(P_Bg), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.ArrowBack, null,
                                    tint = P_Dark, modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = P_Card,
                        titleContentColor = P_Dark)
                )
            },
            contentWindowInsets = WindowInsets(0)
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding).background(P_Bg)) {
                AddAddressUI(
                    existing = existing,
                    onSave = { fname, phone, email, address, city, state, pincode ->
                        val onDone: () -> Unit = {
                            Analytics.logEvent("add_shipping_info", mapOf(
                                "shipping_city" to city, "shipping_state" to state,
                                "shipping_pincode" to pincode, "country" to "IN"))
                            navigator.pop()
                        }
                        if (existing != null) {
                            viewModel.updateAddress(
                                existing.id, fname, phone, email,
                                address, city, state, pincode, onDone
                            )
                        } else {
                            viewModel.addNewAddress(
                                fname, phone, email, address, city, state, pincode, onDone
                            )
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
    onSave: (String, String, String, String, String, String, String) -> Unit,
    onCancel: () -> Unit,
    existing: BillingAddress? = null
) {
    // Prefilled when editing, blank when adding
    var fname by remember { mutableStateOf(existing?.fullName ?: "") }
    var phone by remember { mutableStateOf(existing?.phone ?: "") }
    var email by remember { mutableStateOf(existing?.email ?: "") }
    var address by remember { mutableStateOf(existing?.address ?: "") }
    var city by remember { mutableStateOf(existing?.city ?: "") }
    var state by remember { mutableStateOf(existing?.state ?: "") }
    var pincode by remember { mutableStateOf(existing?.pincode ?: "") }

    var fnameErr by remember { mutableStateOf<String?>(null) }
    var phoneErr by remember { mutableStateOf<String?>(null) }
    var emailErr by remember { mutableStateOf<String?>(null) }
    var addressErr by remember { mutableStateOf<String?>(null) }
    var cityErr by remember { mutableStateOf<String?>(null) }
    var stateErr by remember { mutableStateOf<String?>(null) }
    var pincodeErr by remember { mutableStateOf<String?>(null) }

    fun validate(): Boolean {
        var valid = true
        fnameErr = if (fname.isBlank()) "Required" else null
        phoneErr = if (phone.length != 10) "Must be 10 digits" else null
        emailErr = if (!isValidEmail(email)) "Invalid email" else null
        addressErr = if (address.isBlank()) "Required" else null
        cityErr = if (city.isBlank()) "Required" else null
        stateErr = if (state.isBlank()) "Required" else null
        pincodeErr = if (pincode.length != 6) "Must be 6 digits" else null
        listOf(fnameErr, phoneErr, emailErr, addressErr, cityErr, stateErr, pincodeErr)
            .forEach { if (it != null) valid = false }
        return valid
    }

    Column(modifier = Modifier.fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(20.dp)) {

        // Form fields
        PremiumInput("Full Name", fname, { fname = it }, fnameErr)
        PremiumInput("Phone Number", phone,
            { if (it.all { c -> c.isDigit() } && it.length <= 10) phone = it },
            phoneErr, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        PremiumInput("Email", email, { email = it }, emailErr,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
        PremiumInput("Street Address", address, { address = it }, addressErr)
        PremiumInput("City", city, { city = it }, cityErr)
        PremiumInput("State", state, { state = it }, stateErr)
        PremiumInput("Pincode", pincode,
            { if (it.all { c -> c.isDigit() } && it.length <= 6) pincode = it },
            pincodeErr, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = { if (validate()) onSave(fname, phone, email, address, city, state, pincode) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = P_Green),
            elevation = ButtonDefaults.buttonElevation(0.dp)
        ) {
            Text(
                if (existing != null) "Update Address" else "Save Address",
                fontSize = 15.sp, fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(10.dp))

        TextButton(onClick = onCancel, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Cancel", color = P_Mid, fontSize = 14.sp)
        }
    }
}

@Composable
private fun PremiumInput(
    label: String, value: String, onChange: (String) -> Unit,
    error: String? = null, keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    val bringIntoView = remember { BringIntoViewRequester() }
    var isFocused by remember { mutableStateOf(false) }

    LaunchedEffect(isFocused) { if (isFocused) bringIntoView.bringIntoView() }

    Column(modifier = Modifier.bringIntoViewRequester(bringIntoView)
        .padding(bottom = 14.dp)) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium,
            color = P_Mid, modifier = Modifier.padding(bottom = 6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth().onFocusChanged { isFocused = it.isFocused },
            isError = error != null,
            singleLine = true,
            keyboardOptions = keyboardOptions,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = P_Green,
                unfocusedBorderColor = P_Border,
                errorBorderColor = P_Red,
                focusedContainerColor = P_Card,
                unfocusedContainerColor = P_Card,
                focusedTextColor = P_Dark,
                unfocusedTextColor = P_Dark,
                cursorColor = P_Green)
        )
        error?.let {
            Text(it, color = P_Red, fontSize = 11.sp,
                modifier = Modifier.padding(top = 4.dp, start = 2.dp))
        }
    }
}

@Composable
fun ValidatedInput(
    label: String, value: String, onChange: (String) -> Unit,
    error: String? = null, keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    PremiumInput(label, value, onChange, error, keyboardOptions)
}

private fun isValidEmail(email: String): Boolean =
    email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".toRegex())