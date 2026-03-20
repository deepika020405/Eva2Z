package com.developer.ev2z.ui.screens.CriminalVerification

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.developer.ev2z.data.local.UserPrefs
import com.developer.ev2z.data.local.createStorageManager
import com.developer.ev2z.navigation.PlatformBackHandler
import com.developer.ev2z.network.ApiService
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CriminalVerificationFlow(
    apiService: ApiService,
    onBack: () -> Unit
) {
    val userPrefs = remember { UserPrefs(createStorageManager()) }
    val vm        = remember { CriminalVerificationViewModel(apiService, userPrefs) }
    val scope     = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        vm.loadPersistedState()
    }

    PlatformBackHandler(true) {
        when (vm.step) {
            VerificationStep.SUCCESS -> vm.reset()
            else -> onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Criminal Verification") },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            when (vm.step) {
                                VerificationStep.SUCCESS -> vm.reset()
                                else -> onBack()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (vm.step == VerificationStep.SUCCESS)
                                Icons.Default.Close
                            else
                                Icons.Default.ArrowBack,
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
                .padding(padding)
        ) {
            when (vm.step) {

                VerificationStep.FORM ->
                    CriminalVerificationEntryScreen(
                        isLoading      = vm.loading,
                        onSubmitNew    = { form -> scope.launch { vm.submitForm(form) } },
                        onSearchStatus = { candidateId, name ->
                            scope.launch { vm.searchByCandidate(candidateId, name) }
                        }
                    )

                VerificationStep.CREATING_ORDER ->
                    ProcessingScreen("Creating payment order...")

                VerificationStep.PAYMENT ->
                    vm.paymentToken?.let { token ->
                        CriminalPaymentScreen(
                            paymentToken = token,
                            amount       = vm.amount,
                            onSuccess    = { txnId ->
                                scope.launch { vm.onPaymentSuccess(txnId ?: "") }
                            },
                            onFailure = { error ->
                                vm.errorMessage = error ?: "Payment failed"
                                vm.step = VerificationStep.ERROR
                            },
                            onBack = { vm.reset() }
                        )
                    }

                VerificationStep.VERIFYING_PAYMENT ->
                    ProcessingScreen("Verifying payment...")

                VerificationStep.CREATING_CANDIDATE ->
                    ProcessingScreen("Creating candidate profile...")

                VerificationStep.SUBMITTING_BGV ->
                    ProcessingScreen("Submitting background verification...")

                VerificationStep.SUCCESS -> {
                    // ✅ Sirf ek baar status check karo — screen open hone par
                    // Polling nahi — bas latest status fetch karo
                    LaunchedEffect(vm.createdCandidateId) {
                        if (vm.createdCandidateId != null) {
                            vm.checkVerificationStatus()
                        }
                    }

                    CriminalSuccessScreen(
                        form              = vm.cachedForm,
                        candidateId       = vm.createdCandidateId,
                        status            = vm.bgvStatus,
                        reportUrl         = vm.reportUrl,
                        candidateName     = vm.searchedCandidateName,
                        onDone            = onBack,
                        onNewVerification = { vm.reset() }
                    )
                }

                VerificationStep.ERROR ->
                    CriminalErrorScreen(
                        message = vm.errorMessage,
                        onRetry = { vm.reset() },
                        onBack  = onBack
                    )
            }
        }
    }
}