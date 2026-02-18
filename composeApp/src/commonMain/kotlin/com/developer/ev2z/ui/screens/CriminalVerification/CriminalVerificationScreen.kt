//package com.developer.ev2z.ui.screens.CriminalVerification
//
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.shape.RoundedCornerShape
//
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Badge
//import androidx.compose.material.icons.filled.Email
//import androidx.compose.material.icons.filled.Person
//import androidx.compose.material.icons.filled.Phone
//
//import androidx.compose.material3.*
//
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.graphics.vector.ImageVector
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//
//import com.developer.ev2z.navigation.PlatformBackHandler
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun CriminalVerificationScreen(
//    onSubmit: (CriminalForm) -> Unit,
//    isLoading: Boolean = false,
//    onBack: () -> Unit
//) {
//    PlatformBackHandler(true) { onBack() }
//
//    var name by remember { mutableStateOf("") }
//    var email by remember { mutableStateOf("") }
//    var phone by remember { mutableStateOf("") }
//    var employeeId by remember { mutableStateOf("") }
//    var consent by remember { mutableStateOf(false) }
//    var houseNo by remember { mutableStateOf("") }
//    var colony by remember { mutableStateOf("") }
//    var city by remember { mutableStateOf("") }
//    var state by remember { mutableStateOf("") }
//    var pincode by remember { mutableStateOf("") }
//    var landmark by remember { mutableStateOf("") }
//    var stayingPeriod by remember { mutableStateOf("") }
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text("Criminal Verification") },
//                navigationIcon = {
//                    IconButton(onClick = onBack) {
//                        Icon(Icons.Default.Person, contentDescription = null)
//                    }
//                }
//            )
//        },
//        bottomBar = {
//            BottomAppBar(containerColor = Color.White) {
//                Button(
//                    onClick = {
//                        println("🟢 Submit Button Clicked")
//                        println("📋 Form Data → name=$name email=$email phone=$phone empId=$employeeId consent=$consent")
//
//                        onSubmit(
//                            CriminalForm(
//                                name = name,
//                                email = email,
//                                phone = phone,
//                                employeeId = employeeId,
//                                consent = consent,
//                                houseNo = houseNo,
//                                colony = colony,
//                                city = city,
//                                state = state,
//                                pincode = pincode,
//                                landmark = landmark,
//                                stayingPeriod = stayingPeriod
//                            )
//                        )
//                    },
//                    enabled = consent && name.isNotBlank(),
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(12.dp)
//                        .height(54.dp),
//                    shape = RoundedCornerShape(16.dp)
//                ) {
//                    Text("Continue to Payment", fontSize = 16.sp)
//                }
//            }
//        },
//        containerColor = Color(0xFFF6F7FB)
//    ) { padding ->
//
//        Box(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(padding)
//        ) {
//
//            LazyColumn(
//                contentPadding = PaddingValues(16.dp),
//                verticalArrangement = Arrangement.spacedBy(16.dp)
//            ) {
//
//                item {
//                    FormSection(title = "Personal Details") {
//                        FormInput(
//                            label = "Full Name",
//                            value = name,
//                            icon = Icons.Default.Person
//                        ) { name = it }
//
//                        FormInput(
//                            label = "Email Address",
//                            value = email,
//                            icon = Icons.Default.Email
//                        ) { email = it }
//
//                        FormInput(
//                            label = "Phone Number",
//                            value = phone,
//                            icon = Icons.Default.Phone
//                        ) { phone = it }
//
//                        FormInput(
//                            label = "Employee ID",
//                            value = employeeId,
//                            icon = Icons.Default.Badge
//                        ) { employeeId = it }
//                    }
//                }
//
//                item {
//                    FormSection(title = "Consent") {
//                        Row(verticalAlignment = Alignment.CenterVertically) {
//                            Checkbox(
//                                checked = consent,
//                                onCheckedChange = { consent = it }
//                            )
//                            Spacer(Modifier.width(8.dp))
//                            Text(
//                                "I agree to consent & undertaking",
//                                fontSize = 14.sp
//                            )
//                        }
//                    }
//                }
//
//                item {
//                    FormSection(title = "Verification Fee") {
//                        Row(
//                            modifier = Modifier.fillMaxWidth(),
//                            horizontalArrangement = Arrangement.SpaceBetween
//                        ) {
//                            Text("Criminal Background Check")
//                            Text("₹1", style = MaterialTheme.typography.titleMedium)
//                        }
//                    }
//                }
//
//                item { Spacer(Modifier.height(80.dp)) }
//            }
//
//            if (isLoading) {
//                Box(
//                    Modifier
//                        .fillMaxSize()
//                        .background(Color.Black.copy(alpha = 0.2f)),
//                    contentAlignment = Alignment.Center
//                ) {
//                    CircularProgressIndicator()
//                }
//            }
//        }
//    }
//}
//
//@Composable
//fun FormSection(
//    title: String,
//    content: @Composable ColumnScope.() -> Unit
//) {
//    Column {
//        Text(
//            text = title,
//            style = MaterialTheme.typography.labelLarge,
//            color = Color.Gray,
//            modifier = Modifier.padding(bottom = 6.dp)
//        )
//
//        Card(
//            shape = RoundedCornerShape(20.dp),
//            elevation = CardDefaults.cardElevation(4.dp),
//            colors = CardDefaults.cardColors(containerColor = Color.White)
//        ) {
//            Column(
//                modifier = Modifier.padding(16.dp),
//                verticalArrangement = Arrangement.spacedBy(12.dp),
//                content = content
//            )
//        }
//    }
//}
//
//
//@Composable
//fun FormInput(
//    label: String,
//    value: String,
//    icon: ImageVector,
//    onChange: (String) -> Unit
//) {
//    OutlinedTextField(
//        value = value,
//        onValueChange = onChange,
//        label = { Text(label) },
//        leadingIcon = { Icon(icon, null) },
//        modifier = Modifier.fillMaxWidth(),
//        singleLine = true,
//        shape = RoundedCornerShape(14.dp)
//    )
//}