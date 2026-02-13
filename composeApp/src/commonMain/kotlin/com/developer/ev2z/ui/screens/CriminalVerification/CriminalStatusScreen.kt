package com.developer.ev2z.ui.screens.CriminalVerification

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun CriminalStatusScreen(
    candidateId: String,
    onDone: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Verification Status") })
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("✅ Verification Submitted")
            Spacer(Modifier.height(12.dp))
            Text("Candidate ID: $candidateId")

            Spacer(Modifier.height(24.dp))

            Button(onClick = onDone) {
                Text("Done")
            }
        }
    }
}