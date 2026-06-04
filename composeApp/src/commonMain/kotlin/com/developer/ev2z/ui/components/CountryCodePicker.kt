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
    val flag: String,
    val phoneLength: Int
)

val allCountries: List<CountryData> = listOf(
    CountryData("Afghanistan",          "+93",  "AF", "🇦🇫", 9),
    CountryData("Albania",              "+355", "AL", "🇦🇱", 9),
    CountryData("Algeria",              "+213", "DZ", "🇩🇿", 9),
    CountryData("Argentina",            "+54",  "AR", "🇦🇷", 10),
    CountryData("Australia",            "+61",  "AU", "🇦🇺", 9),
    CountryData("Austria",              "+43",  "AT", "🇦🇹", 10),
    CountryData("Azerbaijan",           "+994", "AZ", "🇦🇿", 9),
    CountryData("Bahrain",              "+973", "BH", "🇧🇭", 8),
    CountryData("Bangladesh",           "+880", "BD", "🇧🇩", 10),
    CountryData("Belarus",              "+375", "BY", "🇧🇾", 9),
    CountryData("Belgium",              "+32",  "BE", "🇧🇪", 9),
    CountryData("Bolivia",              "+591", "BO", "🇧🇴", 8),
    CountryData("Brazil",               "+55",  "BR", "🇧🇷", 11),
    CountryData("Bulgaria",             "+359", "BG", "🇧🇬", 9),
    CountryData("Cambodia",             "+855", "KH", "🇰🇭", 9),
    CountryData("Cameroon",             "+237", "CM", "🇨🇲", 9),
    CountryData("Canada",               "+1",   "CA", "🇨🇦", 10),
    CountryData("Chile",                "+56",  "CL", "🇨🇱", 9),
    CountryData("China",                "+86",  "CN", "🇨🇳", 11),
    CountryData("Colombia",             "+57",  "CO", "🇨🇴", 10),
    CountryData("Croatia",              "+385", "HR", "🇭🇷", 9),
    CountryData("Cyprus",               "+357", "CY", "🇨🇾", 8),
    CountryData("Czech Republic",       "+420", "CZ", "🇨🇿", 9),
    CountryData("Denmark",              "+45",  "DK", "🇩🇰", 8),
    CountryData("Ecuador",              "+593", "EC", "🇪🇨", 9),
    CountryData("Egypt",                "+20",  "EG", "🇪🇬", 10),
    CountryData("Ethiopia",             "+251", "ET", "🇪🇹", 9),
    CountryData("Finland",              "+358", "FI", "🇫🇮", 10),
    CountryData("France",               "+33",  "FR", "🇫🇷", 9),
    CountryData("Georgia",              "+995", "GE", "🇬🇪", 9),
    CountryData("Germany",              "+49",  "DE", "🇩🇪", 11),
    CountryData("Ghana",                "+233", "GH", "🇬🇭", 9),
    CountryData("Greece",               "+30",  "GR", "🇬🇷", 10),
    CountryData("Guatemala",            "+502", "GT", "🇬🇹", 8),
    CountryData("Honduras",             "+504", "HN", "🇭🇳", 8),
    CountryData("Hong Kong",            "+852", "HK", "🇭🇰", 8),
    CountryData("Hungary",              "+36",  "HU", "🇭🇺", 9),
    CountryData("India",                "+91",  "IN", "🇮🇳", 10),
    CountryData("Indonesia",            "+62",  "ID", "🇮🇩", 11),
    CountryData("Iran",                 "+98",  "IR", "🇮🇷", 10),
    CountryData("Iraq",                 "+964", "IQ", "🇮🇶", 10),
    CountryData("Ireland",              "+353", "IE", "🇮🇪", 9),
    CountryData("Israel",               "+972", "IL", "🇮🇱", 9),
    CountryData("Italy",                "+39",  "IT", "🇮🇹", 10),
    CountryData("Japan",                "+81",  "JP", "🇯🇵", 11),
    CountryData("Jordan",               "+962", "JO", "🇯🇴", 9),
    CountryData("Kazakhstan",           "+7",   "KZ", "🇰🇿", 10),
    CountryData("Kenya",                "+254", "KE", "🇰🇪", 9),
    CountryData("Kuwait",               "+965", "KW", "🇰🇼", 8),
    CountryData("Kyrgyzstan",           "+996", "KG", "🇰🇬", 9),
    CountryData("Latvia",               "+371", "LV", "🇱🇻", 8),
    CountryData("Lebanon",              "+961", "LB", "🇱🇧", 8),
    CountryData("Libya",                "+218", "LY", "🇱🇾", 9),
    CountryData("Lithuania",            "+370", "LT", "🇱🇹", 8),
    CountryData("Malaysia",             "+60",  "MY", "🇲🇾", 10),
    CountryData("Maldives",             "+960", "MV", "🇲🇻", 7),
    CountryData("Mexico",               "+52",  "MX", "🇲🇽", 10),
    CountryData("Moldova",              "+373", "MD", "🇲🇩", 8),
    CountryData("Mongolia",             "+976", "MN", "🇲🇳", 8),
    CountryData("Morocco",              "+212", "MA", "🇲🇦", 9),
    CountryData("Myanmar",              "+95",  "MM", "🇲🇲", 9),
    CountryData("Nepal",                "+977", "NP", "🇳🇵", 10),
    CountryData("Netherlands",          "+31",  "NL", "🇳🇱", 9),
    CountryData("New Zealand",          "+64",  "NZ", "🇳🇿", 9),
    CountryData("Nigeria",              "+234", "NG", "🇳🇬", 10),
    CountryData("Norway",               "+47",  "NO", "🇳🇴", 8),
    CountryData("Oman",                 "+968", "OM", "🇴🇲", 8),
    CountryData("Pakistan",             "+92",  "PK", "🇵🇰", 10),
    CountryData("Panama",               "+507", "PA", "🇵🇦", 8),
    CountryData("Paraguay",             "+595", "PY", "🇵🇾", 9),
    CountryData("Peru",                 "+51",  "PE", "🇵🇪", 9),
    CountryData("Philippines",          "+63",  "PH", "🇵🇭", 10),
    CountryData("Poland",               "+48",  "PL", "🇵🇱", 9),
    CountryData("Portugal",             "+351", "PT", "🇵🇹", 9),
    CountryData("Qatar",                "+974", "QA", "🇶🇦", 8),
    CountryData("Romania",              "+40",  "RO", "🇷🇴", 9),
    CountryData("Russia",               "+7",   "RU", "🇷🇺", 10),
    CountryData("Saudi Arabia",         "+966", "SA", "🇸🇦", 9),
    CountryData("Senegal",              "+221", "SN", "🇸🇳", 9),
    CountryData("Serbia",               "+381", "RS", "🇷🇸", 9),
    CountryData("Singapore",            "+65",  "SG", "🇸🇬", 8),
    CountryData("Slovakia",             "+421", "SK", "🇸🇰", 9),
    CountryData("Somalia",              "+252", "SO", "🇸🇴", 8),
    CountryData("South Africa",         "+27",  "ZA", "🇿🇦", 9),
    CountryData("South Korea",          "+82",  "KR", "🇰🇷", 10),
    CountryData("Spain",                "+34",  "ES", "🇪🇸", 9),
    CountryData("Sri Lanka",            "+94",  "LK", "🇱🇰", 9),
    CountryData("Sudan",                "+249", "SD", "🇸🇩", 9),
    CountryData("Sweden",               "+46",  "SE", "🇸🇪", 9),
    CountryData("Switzerland",          "+41",  "CH", "🇨🇭", 9),
    CountryData("Taiwan",               "+886", "TW", "🇹🇼", 9),
    CountryData("Tajikistan",           "+992", "TJ", "🇹🇯", 9),
    CountryData("Tanzania",             "+255", "TZ", "🇹🇿", 9),
    CountryData("Thailand",             "+66",  "TH", "🇹🇭", 9),
    CountryData("Tunisia",              "+216", "TN", "🇹🇳", 8),
    CountryData("Turkey",               "+90",  "TR", "🇹🇷", 10),
    CountryData("Turkmenistan",         "+993", "TM", "🇹🇲", 8),
    CountryData("Uganda",               "+256", "UG", "🇺🇬", 9),
    CountryData("Ukraine",              "+380", "UA", "🇺🇦", 9),
    CountryData("United Arab Emirates", "+971", "AE", "🇦🇪", 9),
    CountryData("United Kingdom",       "+44",  "GB", "🇬🇧", 10),
    CountryData("United States",        "+1",   "US", "🇺🇸", 10),
    CountryData("Uruguay",              "+598", "UY", "🇺🇾", 8),
    CountryData("Uzbekistan",           "+998", "UZ", "🇺🇿", 9),
    CountryData("Venezuela",            "+58",  "VE", "🇻🇪", 10),
    CountryData("Vietnam",              "+84",  "VN", "🇻🇳", 10),
    CountryData("Yemen",                "+967", "YE", "🇾🇪", 9),
    CountryData("Zimbabwe",             "+263", "ZW", "🇿🇼", 9)
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
                .heightIn(min = fieldHeight)
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
                if (digits.length <= selectedCountry.phoneLength) onPhoneChange(digits)
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
                .heightIn(min = fieldHeight),
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