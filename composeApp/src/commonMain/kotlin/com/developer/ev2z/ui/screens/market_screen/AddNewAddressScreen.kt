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

class AddNewAddressScreen(
    private val viewModel: ShopViewModel
) : Screen {

    @Composable
    override fun Content() {

        val navigator = LocalNavigator.currentOrThrow

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
                    navigator.pop()  // screen close
                }
            },
            onCancel = { navigator.pop() }
        )
    }
}