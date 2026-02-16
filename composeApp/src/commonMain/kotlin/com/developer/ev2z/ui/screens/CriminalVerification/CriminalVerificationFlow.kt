package com.developer.ev2z.ui.screens.CriminalVerification

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.developer.ev2z.network.ApiService
import kotlinx.coroutines.launch

@Composable
fun CriminalVerificationFlow(
    apiService: ApiService,
    onBack: () -> Unit
) {
    val vm = remember { CriminalVerificationViewModel(apiService) }
    val scope = rememberCoroutineScope()

//    when (vm.step) {
//
//        VerificationStep.FORM -> {
//            CriminalVerificationFormScreen(
//                isLoading = vm.loading,
//                onBack = onBack,
//                onSubmit = { form ->
//                    scope.launch { vm.submitForm(form) }
//                }
//            )
//        }
//
//        VerificationStep.CREATING_CANDIDATE -> {
//            ProcessingScreen("Creating candidate...")
//        }
//
//        VerificationStep.PAYMENT -> {
//            val url = vm.paymentUrl
//
//            if (url != null) {
//                CriminalPaymentScreen(
//                    paymentUrl = url,
//                    amount = vm.amount,
//                    onSuccess = { txnId ->
//                        scope.launch {
//                            if (txnId != null) {
//                                vm.onPaymentSuccess(txnId)
//                            } else {
//                                vm.errorMessage = "Payment cancelled or failed"
//                                vm.step = VerificationStep.ERROR
//                            }
//                        }
//                    },
//                    onFailure = { error ->
//                        vm.errorMessage = error ?: "Payment failed"
//                        vm.step = VerificationStep.ERROR
//                    },
//                    onBack = { vm.reset() }
//                )
//            } else {
//                ProcessingScreen("Initializing payment...")
//            }
//        }
//
//        VerificationStep.SUBMITTING_BGV -> {
//            ProcessingScreen("Submitting verification...")
//        }
//
//        VerificationStep.SUCCESS -> {
//            CriminalSuccessScreen(onDone = onBack)
//        }
//
//        VerificationStep.ERROR -> {
//            CriminalErrorScreen(
//                message = vm.errorMessage,
//                onRetry = { vm.reset() },
//                onBack = onBack
//            )
//        }
//    }
}