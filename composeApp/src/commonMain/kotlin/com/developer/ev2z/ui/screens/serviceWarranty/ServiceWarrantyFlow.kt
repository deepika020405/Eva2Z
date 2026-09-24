package com.developer.ev2z.ui.screens.serviceWarranty

import androidx.compose.runtime.Composable
import com.developer.ev2z.network.ApiService
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember

/** One screen in the Service & Warranty flow. */
sealed interface ServiceStep {
    data object Dashboard : ServiceStep
    data class Raise(val vehicleId: String?) : ServiceStep
    data class Complaints(val filter: ComplaintFilter) : ServiceStep
    data class Detail(val ticketId: String) : ServiceStep
}

/**
 * Entry point for the whole feature — the single composable HomeNavGraph calls.
 *
 * The app navigates with hoisted state rather than a NavHost, so this keeps its
 * own small back stack instead of pulling in navigation-compose. Each screen
 * registers its own [com.developer.ev2z.navigation.PlatformBackHandler], which
 * pops this stack; backing out of the dashboard calls [onExit].
 *
 * Because of that, HomeNavGraph must NOT register a back handler for this flow —
 * its handlers are composed last and would win the LIFO race, closing the whole
 * feature instead of stepping back one screen. It only needs to include the
 * feature in `isAnyOverlayOpen` so the root handler stays disabled.
 */
@Composable
fun ServiceWarrantyFlow(
    apiService: ApiService,
    onExit: () -> Unit,
) {
    val backStack = remember { mutableStateListOf<ServiceStep>(ServiceStep.Dashboard) }

    fun push(step: ServiceStep) {
        backStack.add(step)
    }

    fun pop() {
        if (backStack.size <= 1) onExit() else backStack.removeAt(backStack.lastIndex)
    }

    when (val step = backStack.last()) {

        is ServiceStep.Dashboard -> {
            val vm = rememberServiceViewModel("dashboard") { ServiceDashboardViewModel(apiService) }
            ServiceDashboardScreen(
                viewModel = vm,
                onRaiseComplaint = { vehicleId -> push(ServiceStep.Raise(vehicleId)) },
                onOpenComplaints = { filter -> push(ServiceStep.Complaints(filter)) },
                onOpenTicket = { ticketId -> push(ServiceStep.Detail(ticketId)) },
                onBack = onExit,
            )
        }

        is ServiceStep.Raise -> {
            val vm = rememberServiceViewModel("raise-${step.vehicleId}") {
                RaiseComplaintViewModel(apiService, step.vehicleId)
            }
            RaiseComplaintScreen(
                viewModel = vm,
                onBack = { pop() },
                onComplaintCreated = { ticketId ->
                    // Replace the compose screen with the new ticket so backing out
                    // of the detail lands on the dashboard, not the filled-in form.
                    backStack.removeAt(backStack.lastIndex)
                    push(ServiceStep.Detail(ticketId))
                },
                onPaidWithoutTicket = {
                    // Paid, but no ticket number to open — the list is where it will
                    // appear once the server files it, so that is where this lands.
                    backStack.removeAt(backStack.lastIndex)
                    push(ServiceStep.Complaints(ComplaintFilter.ALL))
                },
            )
        }

        is ServiceStep.Complaints -> {
            val vm = rememberServiceViewModel("complaints-${step.filter.name}") {
                MyComplaintsViewModel(apiService, step.filter)
            }
            MyComplaintsScreen(
                viewModel = vm,
                onOpenTicket = { ticketId -> push(ServiceStep.Detail(ticketId)) },
                onRaiseComplaint = { push(ServiceStep.Raise(null)) },
                onBack = { pop() },
            )
        }

        is ServiceStep.Detail -> {
            val vm = rememberServiceViewModel("detail-${step.ticketId}") {
                ComplaintDetailViewModel(apiService, step.ticketId)
            }
            ComplaintDetailScreen(
                viewModel = vm,
                onBack = { pop() },
            )
        }
    }
}

/**
 * Creates a ViewModel tied to composition and cancels its scope on the way out.
 * Without the dispose, a call still in flight when the screen closes would write
 * into a state nothing is showing any more.
 */
@Composable
private fun <T : ServiceScopedViewModel> rememberServiceViewModel(
    key: String,
    factory: () -> T,
): T {
    val vm = remember(key) { factory() }
    DisposableEffect(key) {
        onDispose { vm.dispose() }
    }
    return vm
}