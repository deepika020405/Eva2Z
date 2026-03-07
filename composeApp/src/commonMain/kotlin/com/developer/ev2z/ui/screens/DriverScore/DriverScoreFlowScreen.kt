package com.developer.ev2z.ui.screens.DriverScore


import androidx.compose.runtime.*

// ─────────────────────────────────────────────────────────────────────────────
//  DriverScoreFlowScreen
//  HomeNavGraph calls this:
//
//      if (showDriverScore) {
//          DriverScoreFlowScreen(onBack = { showDriverScore = false })
//      }
//
//  Internal navigation: Dashboard → Detail → Leaderboard → Compare
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DriverScoreFlowScreen(onBack: () -> Unit) {

    val viewModel = remember { DriverScoreViewModel() }
    val uiState  by viewModel.uiState.collectAsState()
    val lbState  by viewModel.leaderboardState.collectAsState()

    var currentRoute by remember { mutableStateOf<DriverScoreRoute>(DriverScoreRoute.Dashboard) }

    when (val route = currentRoute) {

        is DriverScoreRoute.Dashboard -> {
            DriverScoreScreen(
                uiState  = uiState,
                onSelectVehicle = { viewModel.selectVehicle(it) },
                onNavigateToDetail = { vehicle ->
                    val all = (uiState as? DriverScoreUiState.MultiVehicle)?.vehicles ?: listOf(vehicle)
                    val idx = all.indexOf(vehicle).coerceAtLeast(0)
                    currentRoute = DriverScoreRoute.Detail(vehicle, all, idx)
                },
                onNavigateToLeaderboard = { currentRoute = DriverScoreRoute.Leaderboard },
                onNavigateToCompare     = { currentRoute = DriverScoreRoute.Compare },
                onBack = onBack   // ← HomeNavGraph ko wapas
            )
        }

        is DriverScoreRoute.Detail -> {
            ScoreDetailScreen(
                vehicle = route.vehicle,
                allVehicles = route.allVehicles,
                selectedVehicleIndex = route.selectedIndex,
                onSelectVehicle = { idx ->
                    val newVehicle = route.allVehicles.getOrNull(idx) ?: route.vehicle
                    currentRoute = route.copy(vehicle = newVehicle, selectedIndex = idx)
                },
                onBack = { currentRoute = DriverScoreRoute.Dashboard }
            )
        }

        is DriverScoreRoute.Leaderboard -> {
            LeaderboardScreen(
                state  = lbState,
                onBack = { currentRoute = DriverScoreRoute.Dashboard }
            )
        }

        is DriverScoreRoute.Compare -> {
            val vehicles = (uiState as? DriverScoreUiState.MultiVehicle)?.vehicles
                ?: listOfNotNull((uiState as? DriverScoreUiState.SingleVehicle)?.vehicle)
            VehicleCompareScreen(
                vehicles = vehicles,
                onBack   = { currentRoute = DriverScoreRoute.Dashboard }
            )
        }
    }
}

// Internal route sealed class
private sealed class DriverScoreRoute {
    object Dashboard   : DriverScoreRoute()
    data class Detail(
        val vehicle: VehicleScore,
        val allVehicles: List<VehicleScore> = emptyList(),
        val selectedIndex: Int = 0
    ) : DriverScoreRoute()
    object Leaderboard : DriverScoreRoute()
    object Compare     : DriverScoreRoute()
}