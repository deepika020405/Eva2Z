package com.developer.ev2z.ui.screens.market_screen


import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.developer.ev2z.BackPressHandler
import com.developer.ev2z.util.Analytics

class AddNewAddressScreen : Screen {

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

        BackPressHandler { navigator.pop() }  // ← back button closes screen

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

                    navigator.pop()  // screen close
                }
            },
            onCancel = { navigator.pop() }
        )
    }
}