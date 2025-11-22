package com.developer.ev2z.ui.screens.auth


import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.developer.ev2z.ui.components.CommonButton
import com.developer.ev2z.ui.components.CommonPhoneField
import com.developer.ev2z.ui.components.CommonText
import com.developer.ev2z.ui.components.CommonTextField
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview(showBackground = true)
fun SignupScreen() {

    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.wrapContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CommonText("Create Account ✨", style = MaterialTheme.typography.titleLarge, fontSize = 24)
        Spacer(modifier = Modifier.height(16.dp))
        CommonTextField(value = name, onValueChange = { name = it }, label = "Name")
        Spacer(modifier = Modifier.height(8.dp))
        CommonPhoneField(value = phone, onValueChange = { phone = it })
        Spacer(modifier = Modifier.height(8.dp))
        CommonTextField(value = password, onValueChange = { password = it }, label = "Password", isPassword = true)
        Spacer(modifier = Modifier.height(24.dp))

        CommonButton("Sign Up", onClick = {
            // TODO: Handle signup click (navigate to HomeScreen)
            // Example: navController.navigate("home")
        }, isLoading = true)
    }
}
