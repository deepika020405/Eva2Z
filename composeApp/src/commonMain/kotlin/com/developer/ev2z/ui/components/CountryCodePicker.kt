package com.developer.ev2z.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class CountryData(
    val name: String,
    val dialCode: String,
    val code: String,
    val flag: String
)

val allCountries: List<CountryData> = listOf(
    CountryData("Afghanistan", "+93", "AF", "🇦🇫"),
    CountryData("Albania", "+355", "AL", "🇦🇱"),
    CountryData("Algeria", "+213", "DZ", "🇩🇿"),
    CountryData("Argentina", "+54", "AR", "🇦🇷"),
    CountryData("Australia", "+61", "AU", "🇦🇺"),
    CountryData("Austria", "+43", "AT", "🇦🇹"),
    CountryData("Azerbaijan", "+994", "AZ", "🇦🇿"),
    CountryData("Bahrain", "+973", "BH", "🇧🇭"),
    CountryData("Bangladesh", "+880", "BD", "🇧🇩"),
    CountryData("Belarus", "+375", "BY", "🇧🇾"),
    CountryData("Belgium", "+32", "BE", "🇧🇪"),
    CountryData("Bolivia", "+591", "BO", "🇧🇴"),
    CountryData("Brazil", "+55", "BR", "🇧🇷"),
    CountryData("Bulgaria", "+359", "BG", "🇧🇬"),
    CountryData("Cambodia", "+855", "KH", "🇰🇭"),
    CountryData("Cameroon", "+237", "CM", "🇨🇲"),
    CountryData("Canada", "+1", "CA", "🇨🇦"),
    CountryData("Chile", "+56", "CL", "🇨🇱"),
    CountryData("China", "+86", "CN", "🇨🇳"),
    CountryData("Colombia", "+57", "CO", "🇨🇴"),
    CountryData("Croatia", "+385", "HR", "🇭🇷"),
    CountryData("Cyprus", "+357", "CY", "🇨🇾"),
    CountryData("Czech Republic", "+420", "CZ", "🇨🇿"),
    CountryData("Denmark", "+45", "DK", "🇩🇰"),
    CountryData("Ecuador", "+593", "EC", "🇪🇨"),
    CountryData("Egypt", "+20", "EG", "🇪🇬"),
    CountryData("Ethiopia", "+251", "ET", "🇪🇹"),
    CountryData("Finland", "+358", "FI", "🇫🇮"),
    CountryData("France", "+33", "FR", "🇫🇷"),
    CountryData("Georgia", "+995", "GE", "🇬🇪"),
    CountryData("Germany", "+49", "DE", "🇩🇪"),
    CountryData("Ghana", "+233", "GH", "🇬🇭"),
    CountryData("Greece", "+30", "GR", "🇬🇷"),
    CountryData("Guatemala", "+502", "GT", "🇬🇹"),
    CountryData("Honduras", "+504", "HN", "🇭🇳"),
    CountryData("Hong Kong", "+852", "HK", "🇭🇰"),
    CountryData("Hungary", "+36", "HU", "🇭🇺"),
    CountryData("India", "+91", "IN", "🇮🇳"),
    CountryData("Indonesia", "+62", "ID", "🇮🇩"),
    CountryData("Iran", "+98", "IR", "🇮🇷"),
    CountryData("Iraq", "+964", "IQ", "🇮🇶"),
    CountryData("Ireland", "+353", "IE", "🇮🇪"),
    CountryData("Israel", "+972", "IL", "🇮🇱"),
    CountryData("Italy", "+39", "IT", "🇮🇹"),
    CountryData("Japan", "+81", "JP", "🇯🇵"),
    CountryData("Jordan", "+962", "JO", "🇯🇴"),
    CountryData("Kazakhstan", "+7", "KZ", "🇰🇿"),
    CountryData("Kenya", "+254", "KE", "🇰🇪"),
    CountryData("Kuwait", "+965", "KW", "🇰🇼"),
    CountryData("Kyrgyzstan", "+996", "KG", "🇰🇬"),
    CountryData("Latvia", "+371", "LV", "🇱🇻"),
    CountryData("Lebanon", "+961", "LB", "🇱🇧"),
    CountryData("Libya", "+218", "LY", "🇱🇾"),
    CountryData("Lithuania", "+370", "LT", "🇱🇹"),
    CountryData("Malaysia", "+60", "MY", "🇲🇾"),
    CountryData("Maldives", "+960", "MV", "🇲🇻"),
    CountryData("Mexico", "+52", "MX", "🇲🇽"),
    CountryData("Moldova", "+373", "MD", "🇲🇩"),
    CountryData("Mongolia", "+976", "MN", "🇲🇳"),
    CountryData("Morocco", "+212", "MA", "🇲🇦"),
    CountryData("Myanmar", "+95", "MM", "🇲🇲"),
    CountryData("Nepal", "+977", "NP", "🇳🇵"),
    CountryData("Netherlands", "+31", "NL", "🇳🇱"),
    CountryData("New Zealand", "+64", "NZ", "🇳🇿"),
    CountryData("Nigeria", "+234", "NG", "🇳🇬"),
    CountryData("Norway", "+47", "NO", "🇳🇴"),
    CountryData("Oman", "+968", "OM", "🇴🇲"),
    CountryData("Pakistan", "+92", "PK", "🇵🇰"),
    CountryData("Panama", "+507", "PA", "🇵🇦"),
    CountryData("Paraguay", "+595", "PY", "🇵🇾"),
    CountryData("Peru", "+51", "PE", "🇵🇪"),
    CountryData("Philippines", "+63", "PH", "🇵🇭"),
    CountryData("Poland", "+48", "PL", "🇵🇱"),
    CountryData("Portugal", "+351", "PT", "🇵🇹"),
    CountryData("Qatar", "+974", "QA", "🇶🇦"),
    CountryData("Romania", "+40", "RO", "🇷🇴"),
    CountryData("Russia", "+7", "RU", "🇷🇺"),
    CountryData("Saudi Arabia", "+966", "SA", "🇸🇦"),
    CountryData("Senegal", "+221", "SN", "🇸🇳"),
    CountryData("Serbia", "+381", "RS", "🇷🇸"),
    CountryData("Singapore", "+65", "SG", "🇸🇬"),
    CountryData("Slovakia", "+421", "SK", "🇸🇰"),
    CountryData("Somalia", "+252", "SO", "🇸🇴"),
    CountryData("South Africa", "+27", "ZA", "🇿🇦"),
    CountryData("South Korea", "+82", "KR", "🇰🇷"),
    CountryData("Spain", "+34", "ES", "🇪🇸"),
    CountryData("Sri Lanka", "+94", "LK", "🇱🇰"),
    CountryData("Sudan", "+249", "SD", "🇸🇩"),
    CountryData("Sweden", "+46", "SE", "🇸🇪"),
    CountryData("Switzerland", "+41", "CH", "🇨🇭"),
    CountryData("Taiwan", "+886", "TW", "🇹🇼"),
    CountryData("Tajikistan", "+992", "TJ", "🇹🇯"),
    CountryData("Tanzania", "+255", "TZ", "🇹🇿"),
    CountryData("Thailand", "+66", "TH", "🇹🇭"),
    CountryData("Tunisia", "+216", "TN", "🇹🇳"),
    CountryData("Turkey", "+90", "TR", "🇹🇷"),
    CountryData("Turkmenistan", "+993", "TM", "🇹🇲"),
    CountryData("Uganda", "+256", "UG", "🇺🇬"),
    CountryData("Ukraine", "+380", "UA", "🇺🇦"),
    CountryData("United Arab Emirates", "+971", "AE", "🇦🇪"),
    CountryData("United Kingdom", "+44", "GB", "🇬🇧"),
    CountryData("United States", "+1", "US", "🇺🇸"),
    CountryData("Uruguay", "+598", "UY", "🇺🇾"),
    CountryData("Uzbekistan", "+998", "UZ", "🇺🇿"),
    CountryData("Venezuela", "+58", "VE", "🇻🇪"),
    CountryData("Vietnam", "+84", "VN", "🇻🇳"),
    CountryData("Yemen", "+967", "YE", "🇾🇪"),
    CountryData("Zimbabwe", "+263", "ZW", "🇿🇼")
).sortedBy { it.name }

fun defaultCountry(): CountryData = allCountries.first { it.code == "IN" }

// Phone input row: clickable country picker button + phone number text field
@Composable
fun PhoneInputField(
    phoneNumber: String,
    onPhoneChange: (String) -> Unit,
    selectedCountry: CountryData,
    onCountryClick: () -> Unit,
    fieldHeight: Dp,
    isError: Boolean,
    labelSize: TextUnit,
    placeholder: String = "Phone number",
    modifier: Modifier = Modifier
) {
    val unfocusedBorder = if (isError) MaterialTheme.colorScheme.error else Color(0xFFE0E0E0)
    val focusedBorder   = if (isError) MaterialTheme.colorScheme.error else Color(0xFF6A9F50)

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Country code picker button
        Surface(
            modifier = Modifier
                .width(108.dp)
                .height(fieldHeight)
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onCountryClick),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFF8FAF7),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                brush = androidx.compose.ui.graphics.SolidColor(unfocusedBorder)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = selectedCountry.flag,
                    fontSize = 20.sp
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    text = selectedCountry.dialCode,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF333333),
                    maxLines = 1
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Select country",
                    tint = Color(0xFF888888),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        // Phone number field
        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { input ->
                val digits = input.filter { it.isDigit() }
                if (digits.length <= 15) onPhoneChange(digits)
            },
            placeholder = {
                Text(
                    text     = placeholder,
                    color    = Color(0xFFAAAAAA),
                    fontSize = labelSize,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            modifier = Modifier
                .weight(1f)
                .height(fieldHeight),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor   = Color(0xFFF8FAF7),
                unfocusedContainerColor = Color(0xFFF8FAF7),
                unfocusedBorderColor    = unfocusedBorder,
                focusedBorderColor      = focusedBorder,
                cursorColor             = Color(0xFF6A9F50)
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                autoCorrect  = false
            ),
            singleLine = true,
            isError    = isError
        )
    }
}

// Searchable country picker dialog
@Composable
fun CountryPickerDialog(
    onDismiss: () -> Unit,
    onCountrySelected: (CountryData) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filtered = remember(searchQuery) {
        if (searchQuery.isEmpty()) allCountries
        else allCountries.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.dialCode.contains(searchQuery) ||
            it.code.contains(searchQuery, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Column {
                Text(
                    text = "Select Country",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF222222)
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search country or code...", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, null, tint = Color(0xFF888888))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, null, tint = Color(0xFF888888))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF6A9F50),
                        cursorColor        = Color(0xFF6A9F50)
                    ),
                    singleLine = true
                )
            }
        },
        text = {
            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No country found", color = Color(0xFF888888))
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 340.dp)
                ) {
                    items(filtered, key = { it.code }) { country ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCountrySelected(country) }
                                .padding(vertical = 13.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(country.flag, fontSize = 24.sp)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = country.name,
                                modifier = Modifier.weight(1f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF222222)
                            )
                            Text(
                                text = country.dialCode,
                                fontSize = 13.sp,
                                color = Color(0xFF888888)
                            )
                        }
                        HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 0.5.dp)
                    }
                }
            }
        },
        confirmButton = {}
    )
}