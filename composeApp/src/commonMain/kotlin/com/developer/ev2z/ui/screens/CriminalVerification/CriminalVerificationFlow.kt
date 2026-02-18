package com.developer.ev2z.ui.screens.CriminalVerification

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.developer.ev2z.navigation.PlatformBackHandler
import com.developer.ev2z.network.ApiService
import kotlinx.coroutines.launch
import kotlin.collections.listOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CriminalVerificationFlow(
    apiService: ApiService,
    onBack: () -> Unit
) {
    val vm = remember { CriminalVerificationViewModel(apiService) }
    val scope = rememberCoroutineScope()

    PlatformBackHandler(true){
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Criminal Verification") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding) // ✅ prevents overlap
        ) {

            when (vm.step) {

                VerificationStep.FORM -> {
                    CriminalVerificationFormScreen(
                        isLoading = vm.loading,
                        onBack = onBack,
                        onSubmit = { form ->
                            scope.launch { vm.submitForm(form) }
                        }
                    )
                }

                VerificationStep.CREATING_ORDER ->
                    ProcessingScreen("Initializing payment...")

                VerificationStep.VERIFYING_PAYMENT ->
                    ProcessingScreen("Verifying payment...")

                VerificationStep.CREATING_CANDIDATE ->
                    ProcessingScreen("Creating candidate...")

                VerificationStep.SUBMITTING_BGV ->
                    ProcessingScreen("Submitting verification...")

                VerificationStep.PAYMENT -> {
                    vm.paymentToken?.let { token ->
                        CriminalPaymentScreen(
                            paymentToken = token,
                            amount = vm.amount,
                            onSuccess = { txnId ->
                                scope.launch { vm.onPaymentSuccess(txnId ?: "") }
                            },
                            onFailure = { error ->
                                vm.errorMessage = error ?: "Payment failed"
                                vm.step = VerificationStep.ERROR
                            },
                            onBack = { vm.reset() }
                        )
                    }
                }

                VerificationStep.SUCCESS -> {
                    LaunchedEffect(vm.createdCandidateId) {
                        vm.createdCandidateId?.let {
                            vm.startStatusPolling(this)
                        }
                    }

                    CriminalSuccessScreen(
                        form = vm.cachedForm,
                        candidateId = vm.createdCandidateId,
                        status = vm.bgvStatus,
                        onDone = onBack
                    )
                }

                VerificationStep.ERROR ->
                    CriminalErrorScreen(
                        message = vm.errorMessage,
                        onRetry = { vm.reset() },
                        onBack = onBack
                    )
            }
        }
    }
}