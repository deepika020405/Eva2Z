package com.developer.ev2z.ui.screens.CriminalVerification

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.developer.ev2z.network.ApiService
import io.ktor.websocket.Frame
import kotlinx.coroutines.launch

@Composable
fun CriminalVerificationFlow(
    apiService: ApiService,
    onBack: () -> Unit
) {
    val vm = remember { CriminalVerificationViewModel(apiService) }
    val scope = rememberCoroutineScope()

    // 🔁 SCREEN FLOW
    when (vm.step) {

        VerificationStep.FORM -> {
            CriminalVerificationScreen(
                isLoading = vm.loading,
                onBack = onBack,
                onSubmit = { form ->
                    scope.launch { vm.submitForm(form) }
                }
            )
        }

        VerificationStep.PAYMENT -> {
            CriminalPaymentScreen(
                candidateId = vm.candidateId ?: "Existing",
                amount = 299,
                onSuccess = { vm.paymentSuccess() },
                onBack = { vm.goToForm() }   // ✅ FIX
            )
        }

        VerificationStep.STATUS -> {
            CriminalStatusScreen(
                candidateId = vm.candidateId ?: "N/A",
                onDone = onBack
            )
        }
    }

    // 🔔 DUPLICATE CANDIDATE DIALOG
    if (vm.showDuplicateDialog) {
        AlertDialog(
            onDismissRequest = { vm.dismissDuplicateDialog() },
            title = { Text("Candidate Exists") },
            text = { Text("Candidate already exists. Continue verification?") },
            confirmButton = {
                TextButton(onClick = { vm.continueWithDuplicate() }) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(onClick = { vm.dismissDuplicateDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }
}