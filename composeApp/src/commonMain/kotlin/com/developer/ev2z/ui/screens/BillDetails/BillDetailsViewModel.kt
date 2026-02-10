package com.developer.ev2z.ui.screens.BillDetails

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developer.ev2z.network.ApiService
import com.developer.ev2z.data.local.StorageManager
import kotlinx.coroutines.launch

class BillDetailsViewModel(
    private val api: ApiService,
    private val storage: StorageManager
) : ViewModel() {

    var bills by mutableStateOf<List<BillTransactionUi>>(emptyList())
        private set

    var loading by mutableStateOf(false)
        private set

    fun loadBills() {
        viewModelScope.launch {
            loading = true
            val token = storage.getString("token", "")
            val response = api.getMyBilling(token)
            bills = response.toUiBills()
            loading = false
        }
    }
}
data class BillTransactionUi(
    val orderId: Int,
    val transactionId: String,
    val orderDate: String,
    val totalAmount: String,
    val billing: BillingUi,
    val items: List<BillItemUi>,
    val invoiceUrl: String
)

data class BillingUi(
    val name: String,
    val phone: String,
    val email: String,
    val city: String,
    val state: String,
    val pincode: String
)

data class BillItemUi(
    val productName: String,
    val quantity: Int,
    val planType: String,
    val price: String,
    val rechargeAmount: String
)

data class InvoiceData(
    val orderId: String,
    val orderDate: String,
    val totalAmount: String,

    // billing details
    val customerName: String,
    val phone: String,
    val email: String,
    val city: String,
    val state: String,
    val pincode: String,

    val items: List<InvoiceItem>
)

data class InvoiceItem(
    val name: String,
    val quantity: Int,
    val planType: String?,
    val price: String
)