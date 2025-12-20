package com.developer.ev2z.ui.screens.GeoFencing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer.ev2z.data.local.StorageManager
import com.developer.ev2z.network.ApiService
import com.developer.ev2z.response.GeoFenceDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeoFencingScreen(
    onAddGeoFence: () -> Unit,
    onBackPressed: () -> Unit
) {
    val viewModel: GeoFenceViewModel = rememberGeoFenceViewModel()
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GeoFence") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = onAddGeoFence) {
                        Text("+ Add More GeoFence")
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
            when (state) {
                GeoFenceUiState.Loading ->
                    CircularProgressIndicator(Modifier.align(Alignment.Center))

                GeoFenceUiState.Empty ->
                    EmptyGeoFenceView(onAddGeoFence)

                is GeoFenceUiState.Success ->
                    GeoFenceList((state as GeoFenceUiState.Success).list)

                is GeoFenceUiState.Error ->
                    Text(
                        text = (state as GeoFenceUiState.Error).message,
                        modifier = Modifier.align(Alignment.Center)
                    )
            }
        }
    }
}


@Composable
fun EmptyGeoFenceView(
    onAddGeoFence: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Box(
            modifier = Modifier
                .size(180.dp)
                .background(Color.LightGray, RoundedCornerShape(12.dp))
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor.",
            textAlign = TextAlign.Center,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = onAddGeoFence,
            shape = RoundedCornerShape(20.dp)
        ) {
            Text("Add First GeoFence")
        }
    }
}


@Composable
fun GeoFenceList(list: List<GeoFenceDto>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(list) { item ->
            GeoFenceCard(item)
        }
    }
}


@Composable
fun GeoFenceCard(item: GeoFenceDto) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.Red
                )
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Device ID: ${item.device_id}",
                color = Color.Gray,
                fontSize = 12.sp
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Radius: ${item.radius} m",
                fontSize = 12.sp
            )

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { /* Disable */ }) {
                    Text("Disable", color = Color.Red)
                }
                TextButton(onClick = { /* Edit */ }) {
                    Text("Edit")
                }
            }
        }
    }
}