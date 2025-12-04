// composeApp/src/iosMain/kotlin/com/developer/ev2z/ui/screens/AddDevice/AddDeviceScreen.ios.kt

@Composable
actual fun AddDeviceDeteails(
    onNavigate: (String) -> Unit,
    viewModel: AddDeviceViewModel
) {
    var deviceName by remember { mutableStateOf("") }
    var deviceId by remember { mutableStateOf("") }
    var simNumber by remember { mutableStateOf("") }
    var deviceType by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val deviceTypes = listOf("Wired", "Wireless")

    val scannedDeviceId by viewModel.scannedDeviceId.collectAsState()
    val scannedSimNumber by viewModel.scannedSimNumber.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val addDeviceResult by viewModel.addDeviceResult.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val storage = remember { com.developer.ev2z.data.local.createStorageManager() }
    val userPrefs = remember { com.developer.ev2z.data.local.UserPrefs(storage) }
    val authToken = remember { userPrefs.getToken() ?: "" }

    LaunchedEffect(scannedDeviceId, scannedSimNumber) {
        deviceId = scannedDeviceId
        simNumber = scannedSimNumber
    }

    LaunchedEffect(addDeviceResult) {
        when (val result = addDeviceResult) {
            is AddDeviceResult.Success -> {
                println("✅ Device added successfully")
            }
            is AddDeviceResult.Error -> {
                println("❌ Error: ${result.message}")
            }
            null -> { /* No result yet */ }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(color = Color.White)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        // Back button
        IconButton(
            onClick = { onNavigate("home") },
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color = Color.White)
                .border(width = 2.dp, color = Color.Gray, shape = CircleShape)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardBackspace,
                contentDescription = "Close",
                tint = Color.Black
            )
        }

        // Header
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                Modifier.weight(0.6f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Add Device Details",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.Black
                )
                Text(
                    text = "All fields required",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Black
                )
            }

            TextButton(
                onClick = { onNavigate("addDevice") },
                modifier = Modifier.weight(0.4f)
            ) {
                Text(
                    text = "Scan",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF00C853)
                )
            }
        }

        // Device Name
        OutlinedTextField(
            value = deviceName,
            onValueChange = { deviceName = it },
            label = { Text("Device Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(8.dp)
        )

        // Device ID with validation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = deviceId,
                onValueChange = { newValue ->
                    if (newValue.all { it.isDigit() } && newValue.length <= 16) {
                        deviceId = newValue
                    }
                },
                label = { Text("Device ID") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                isError = deviceId.isNotEmpty() && deviceId.length < 15,
                supportingText = {
                    if (deviceId.isNotEmpty() && deviceId.length < 15) {
                        Text(
                            text = "Device ID must be 15-16 digits",
                            color = Color.Red,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            )
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = "Info",
                tint = Color.Black,
                modifier = Modifier.size(24.dp)
            )
        }

        // SIM Number with validation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = simNumber,
                onValueChange = { newValue ->
                    val withoutU = newValue.removeSuffix("U")
                    if (withoutU.all { it.isDigit() } && withoutU.length <= 20) {
                        if (newValue.matches(Regex("^[0-9]*U?$"))) {
                            simNumber = newValue
                        }
                    }
                },
                label = { Text("Sim Number") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                isError = simNumber.isNotEmpty() && simNumber.removeSuffix("U").length < 19,
                supportingText = {
                    if (simNumber.isNotEmpty() && simNumber.removeSuffix("U").length < 19) {
                        Text(
                            text = "SIM Number must be 19-20 digits",
                            color = Color.Red,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            )
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = "Info",
                tint = Color.Black,
                modifier = Modifier.size(24.dp)
            )
        }

        // Device Type dropdown
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = deviceType,
                onValueChange = {},
                readOnly = true,
                label = { Text("Device Type") },
                placeholder = { Text("Select device type", color = Color.Gray) },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00C853),
                    unfocusedBorderColor = Color.Gray
                ),
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(Color.White)
            ) {
                deviceTypes.forEach { type ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = type,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        },
                        onClick = {
                            deviceType = type
                            expanded = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (type != deviceTypes.last()) {
                        HorizontalDivider(color = Color.LightGray, thickness = 0.5.dp)
                    }
                }
            }
        }

        Text(
            text = "* Your Device will be activated within in 24 hours",
            modifier = Modifier.alpha(0.7f).fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )

        // Error message
        errorMessage?.let { error ->
            Text(
                text = error,
                color = Color.Red,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        // Buttons
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(
                onClick = { onNavigate("home") },
                modifier = Modifier
                    .weight(0.3f)
                    .border(1.dp, Color.Gray, RoundedCornerShape(100.dp)),
            ) {
                Text("Cancel", color = Color.Black)
            }

            Spacer(Modifier.width(16.dp))

            Button(
                onClick = {
                    val isDeviceIdValid = deviceId.length in 15..16
                    val isSimNumberValid = simNumber.removeSuffix("U").length in 19..20

                    if (deviceName.isNotEmpty() &&
                        deviceId.isNotEmpty() &&
                        simNumber.isNotEmpty() &&
                        deviceType.isNotEmpty() &&
                        isDeviceIdValid &&
                        isSimNumberValid) {

                        println("🔹 Calling addDevice API (iOS)")
                        println("🔹 Token: Bearer $authToken")
                        println("🔹 Device Name: $deviceName")
                        println("🔹 Device ID: $deviceId")
                        println("🔹 SIM Number: $simNumber")
                        println("🔹 Device Type: $deviceType")

                        viewModel.addDevice(
                            token = authToken,
                            deviceName = deviceName,
                            deviceId = deviceId,
                            simNumber = simNumber,
                            deviceType = deviceType.lowercase()
                        )
                    } else {
                        println("❌ Validation failed (iOS)")
                    }
                },
                enabled = !isLoading &&
                        deviceName.isNotEmpty() &&
                        deviceId.length in 15..16 &&
                        simNumber.removeSuffix("U").length in 19..20 &&
                        deviceType.isNotEmpty(),
                modifier = Modifier.weight(0.7f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                shape = RoundedCornerShape(100.dp)
            ) {
                Text(
                    text = if (isLoading) "Adding..." else "Add Device",
                    color = Color.White
                )
            }
        }
    }
}