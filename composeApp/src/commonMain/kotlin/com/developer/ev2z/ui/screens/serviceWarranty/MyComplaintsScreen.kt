package com.developer.ev2z.ui.screens.serviceWarranty

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer.ev2z.navigation.PlatformBackHandler

/**
 * SCREEN 3 — My Complaints (FRD §3 / §6).
 * Search by ticket ID, component, issue or vehicle; filter by lifecycle stage.
 * Scoping to the signed-in user happens server-side.
 */
@Composable
fun MyComplaintsScreen(
    viewModel: MyComplaintsViewModel,
    onOpenTicket: (ticketId: String) -> Unit,
    onRaiseComplaint: () -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    PlatformBackHandler(true) { onBack() }

    ServiceScaffold(
        title = "My Complaints",
        onBack = onBack,
        actions = {
            if (state.isRefreshing) {
                CircularProgressIndicator(
                    Modifier.size(18.dp).padding(end = 4.dp),
                    strokeWidth = 2.dp,
                    color = ServiceColors.Brand,
                )
                Spacer(Modifier.width(12.dp))
            } else {
                IconButton(onClick = viewModel::refresh) {
                    Icon(
                        Icons.Filled.Refresh,
                        contentDescription = "Refresh complaints",
                        tint = ServiceColors.TextSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onRaiseComplaint,
                containerColor = ServiceColors.Brand,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("New complaint", fontSize = 14.sp, fontWeight = FontWeight.SemiBold) },
            )
        },
    ) { topPadding, bottomPadding ->

        Column(Modifier.fillMaxSize().padding(top = topPadding)) {

            /* ───────────────────────── search ───────────────────────── */
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::updateQuery,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                placeholder = {
                    Text(
                        "Search ticket ID, component or vehicle",
                        fontSize = 13.sp,
                        color = ServiceColors.TextMuted,
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = null,
                        tint = ServiceColors.TextSecondary,
                        modifier = Modifier.size(19.dp),
                    )
                },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateQuery("") }) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Clear search",
                                tint = ServiceColors.TextSecondary,
                                modifier = Modifier.size(17.dp),
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                textStyle = TextStyle(fontSize = 14.sp, color = ServiceColors.TextPrimary),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ServiceColors.Brand,
                    unfocusedBorderColor = ServiceColors.BorderSoft,
                    cursorColor = ServiceColors.Brand,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            )

            Spacer(Modifier.height(12.dp))

            /* ─────────────────────── filter chips ───────────────────── */
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(ComplaintFilter.entries.toList(), key = { it.name }) { filter ->
                    val count = state.countFor(filter)
                    val isSelected = state.filter == filter
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) ServiceColors.Brand else Color.White)
                            .clickable { viewModel.setFilter(filter) }
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                    ) {
                        Text(
                            if (count > 0) "${filter.label} ($count)" else filter.label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) Color.White else ServiceColors.TextSecondary,
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            /* ──────────────────────────── list ─────────────────────── */
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LoadingState(label = "Loading complaints…")
                }

                // A failed fetch is called out rather than being dressed up as an
                // empty list — "you have no complaints" would be a lie here.
                state.errorMessage != null && state.allTickets.isEmpty() -> Box(
                    Modifier.fillMaxSize().padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    EmptyState(
                        title = "Couldn't load your complaints",
                        subtitle = state.errorMessage!!,
                        icon = Icons.Filled.CloudOff,
                        action = {
                            TextButton(onClick = viewModel::refresh) {
                                Text("Try again", fontSize = 13.sp, color = ServiceColors.Brand)
                            }
                        },
                    )
                }

                state.tickets.isEmpty() -> Box(
                    Modifier.fillMaxSize().padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    EmptyState(
                        title = if (state.query.isNotBlank()) "No matching complaints"
                        else "Nothing here yet",
                        subtitle = if (state.query.isNotBlank())
                            "Try a different ticket ID, component or vehicle number."
                        else "Complaints under \"${state.filter.label}\" will appear here.",
                        icon = Icons.AutoMirrored.Filled.ListAlt,
                        action = if (state.query.isNotBlank()) {
                            {
                                TextButton(onClick = { viewModel.updateQuery("") }) {
                                    Text("Clear search", fontSize = 13.sp, color = ServiceColors.Brand)
                                }
                            }
                        } else null,
                    )
                }

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = bottomPadding + 88.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.tickets, key = { it.ticketId }) { ticket ->
                        TicketCard(ticket = ticket, onClick = { onOpenTicket(ticket.ticketId) })
                    }
                }
            }
        }
    }
}