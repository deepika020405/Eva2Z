package com.developer.ev2z.ui.screens.AccountActivity

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountActivityScreen(
    onBackPressed: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var isDeleting by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Account Activity",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = Color(0xFFDC3545),
                    modifier = Modifier.size(80.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Delete Account",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "This action is permanent and cannot be undone",
                    fontSize = 14.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFDC3545)
                    )
                ) {
                    Text(
                        text = "Delete My Account",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    // First Dialog - Confirmation
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isDeleting) showDeleteDialog = false
            },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFDC3545),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    "Delete Account",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text("Are you sure you want to delete your account?")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "This will permanently delete:",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("• Your profile", fontSize = 13.sp, color = Color.Gray)
                    Text("• All your devices", fontSize = 13.sp, color = Color.Gray)
                    Text("• All your data", fontSize = 13.sp, color = Color.Gray)
                    Text("• Account settings", fontSize = 13.sp, color = Color.Gray)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "This action cannot be undone.",
                        color = Color(0xFFDC3545),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        showPasswordDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFDC3545)
                    )
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Second Dialog - Password Confirmation
    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isDeleting) {
                    showPasswordDialog = false
                    password = ""
                }
            },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFDC3545),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    "Confirm Password",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        "To delete your account, please confirm your password:",
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isDeleting,
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isDeleting = true
                        // TODO: Call delete account API
                        // viewModel.deleteAccount(password)
                        // On success: navigate back and logout
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFDC3545)
                    ),
                    enabled = password.isNotEmpty() && !isDeleting
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (isDeleting) "Deleting..." else "Delete Account")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPasswordDialog = false
                        password = ""
                    },
                    enabled = !isDeleting
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}