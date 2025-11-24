package com.developer.ev2z.ui.screens.signupScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.TextAutoSizeDefaults
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
//import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
//import com.developer.ev2z.request.SignupRequest
import com.developer.ev2z.ui.components.CommonPhoneField
import com.developer.ev2z.ui.components.CommonTextField
import com.developer.ev2z.viewmodel.AuthViewModel
import org.jetbrains.compose.ui.tooling.preview.Preview



@Preview(showBackground = true)
@Composable
fun SignupScreen(
    viewModel: AuthViewModel,
    onSignupSuccess: () -> Unit
) {

    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    val isLoading by viewModel.loading.collectAsState()
    val signupSuccess by viewModel.signupSuccess.collectAsState()
    val error by viewModel.error.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color(0xFFE6EDE3)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ){
            val screenWidth = maxWidth
            val screenHeight = maxHeight

            Column(
                Modifier
                    .fillMaxSize()
                    .padding(top = screenHeight*0.25f)
                    .background(color = Color(0xFFFFFFFF), RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)),
                horizontalAlignment = Alignment.CenterHorizontally
            )
            {
                Column (Modifier.fillMaxSize().padding(48.dp))
                {
                    Spacer(modifier = Modifier.height(48.dp))

                    Text(
                        text = "Log in or Sign up today",
                        Modifier
                            .width(344.dp)
                            .height(31.dp),
                        style = TextStyle(
                            fontSize = 24.sp,
                            lineHeight = 31.2.sp,
//                        fontFamily = FontFamily(Font(R.font.inter)),
                            fontWeight = FontWeight(500),
                            color = Color(0xFF252A23),
                            textAlign = TextAlign.Center,
                        )
                    )

                    Spacer(modifier = Modifier.height(48.dp))

                    Row(
                        Modifier
                            .width(344.dp)
                            .height(58.dp)
                            .background(color = Color(0xFFE6EDE3), shape = RoundedCornerShape(size = 40.dp))
                            .padding(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(0.dp, Alignment.Start),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            Modifier
                                .shadow(elevation = 4.dp, spotColor = Color(0x336A9F50), ambientColor = Color(0x336A9F50))
                                .width(164.dp)
                                .height(42.dp)
                                .background(color = Color(0xFFFFFFFF), shape = RoundedCornerShape(size = 40.dp))
                                .padding(start = 12.dp, top = 10.dp, end = 12.dp, bottom = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(11.dp, Alignment.Top),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = "Sign Up",
                                Modifier
                                    .width(68.dp)
                                    .height(22.dp),
                                style = TextStyle(
                                    fontSize = 18.sp,
//                                fontFamily = FontFamily(Font(R.font.inter)),
                                    fontWeight = FontWeight(500),
                                    color = Color(0xFF494E47),
                                    textAlign = TextAlign.Center,
                                )
                            )

                            // Child views.
                        }

                        Column(
                            Modifier
                                .width(164.dp)
                                .height(42.dp)
                                .padding(start = 12.dp, top = 10.dp, end = 12.dp, bottom = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(11.dp, Alignment.Top),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = "Log In",
                                Modifier
                                    .width(53.dp)
                                    .height(22.dp),
                                style = TextStyle(
                                    fontSize = 18.sp,
//                                fontFamily = FontFamily(Font(R.font.inter)),
                                    fontWeight = FontWeight(400),
                                    color = Color(0xFF5D6858),
                                    textAlign = TextAlign.Center,
                                )
                            )

                            // Child views.
                        }
                        // Child views.
                    }

                    Spacer(modifier = Modifier.height(48.dp))


                    // Name Field
                    Column {
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = {
                                if (it.length <= 50) {
                                    fullName = it
                                }
                            },
                            label = { Text("Full Name*") },
                            modifier = Modifier
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(120.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color.LightGray,
                                focusedBorderColor = MaterialTheme.colorScheme.primary
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                            singleLine = true,
                            isError = fullName.isNotEmpty()
                        )

                        // Error message
                        if (fullName.isNotEmpty()) {
                            Text(
                                text = "Name cannot be empty",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Phone Number Field
                    Column {
                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = {
                                // Only allow digits and limit to 10
                                if (it.length <= 10 && it.all { char -> char.isDigit() }) {
                                    phoneNumber = it
                                }
                            },
                            label = { Text("Phone Number*") },
                            prefix = {
                                Text(
                                    text = "🇮🇳 +91 ",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Black
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(120.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color.LightGray,
                                focusedBorderColor = MaterialTheme.colorScheme.primary
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            isError = phoneNumber.isNotEmpty() && phoneNumber.length < 10
                        )

                        // Error message
                        if (phoneNumber.isNotEmpty() && phoneNumber.length < 10) {
                            Text(
                                text = "Phone number must be 10 digits",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Password Field
                    Column {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password*") },
                            modifier = Modifier
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(120.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color.LightGray,
                                focusedBorderColor = MaterialTheme.colorScheme.primary
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            isError = password.isNotEmpty() && password.length < 6
                        )
                        // Error message
                        if (password.isNotEmpty() && password.length < 6) {
                            Text(
                                text = "Password must be at least 6 characters",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Signup Button
                    Button(
                        onClick = {
//                        viewModel.signup(
//                            fullName = fullName,
//                            phoneNumber = phoneNumber,
//                            password = password
//                        )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(30.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6A9F50),
                            contentColor = Color.White
                        ),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Send OTP",
                                color = Color.White
                            )
                        }
                    }


                    Spacer(modifier = Modifier.height(24.dp))

                    // Error Message
                    error?.let { errorMessage ->
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }

    // Navigate on success
    LaunchedEffect(signupSuccess) {
        if (signupSuccess) {
            onSignupSuccess()
            viewModel.resetSignupSuccess()
        }
    }
}