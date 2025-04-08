package com.example.blooddonor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.blooddonor.ui.theme.BlooddonorTheme

const val MINIMUM_AGE = 18
const val MAXIMUM_AGE = 60
const val MINIMUM_WEIGHT_KG = 45.0
const val WEIGHT_NEEDED_FOR_450ML = 50.0

enum class CheckStatus {
    NotCheckedYet,
    CanDonate,
    CannotDonate,
    InputError
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BlooddonorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DonorCheckScreen()
                }
            }
        }
    }
}

@Composable
fun DonorCheckScreen() {
    var ageText by remember { mutableStateOf("") }
    var weightText by remember { mutableStateOf("") }
    var isHealthyCheck by remember { mutableStateOf(true) }
    var sleptEnoughCheck by remember { mutableStateOf(true) }
    var currentStatus by remember { mutableStateOf(CheckStatus.NotCheckedYet) }
    var donationVolume by remember { mutableStateOf<Int?>(null) }
    var displayMessage by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()
    val webLinkOpener = LocalUriHandler.current
    val bloodBankInfoUrl = "https://www.pdn.gov.my/"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Blood Donor Eligibility Check",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 60.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = ageText,
            onValueChange = { typedValue ->
                ageText = typedValue.filter { it.isDigit() }
            },
            label = { Text("Your Age (years)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            isError = currentStatus == CheckStatus.InputError && ageText.toIntOrNull() == null
        )

        OutlinedTextField(
            value = weightText,
            onValueChange = { typedValue ->
                if (isValidWeightInput(typedValue)) {
                    weightText = typedValue
                }
            },
            label = { Text("Your Weight (kg)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            isError = currentStatus == CheckStatus.InputError && weightText.toDoubleOrNull() == null
        )

        Spacer(modifier = Modifier.height(8.dp))

        CheckboxWithLabel(
            labelText = "Are you healthy and feeling well?",
            isChecked = isHealthyCheck,
            onCheckedChange = { isHealthyCheck = it }
        )

        CheckboxWithLabel(
            labelText = "Did you sleep more than 5 hours last night?",
            isChecked = sleptEnoughCheck,
            onCheckedChange = { sleptEnoughCheck = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                currentStatus = CheckStatus.NotCheckedYet
                displayMessage = null
                donationVolume = null

                val age = ageText.trim().toIntOrNull()
                val weight = weightText.trim().toDoubleOrNull()

                if (age == null) {
                    displayMessage = "Please enter a valid whole number for age (like 25)."
                    currentStatus = CheckStatus.InputError
                    return@Button
                }

                if (weight == null) {
                    displayMessage = "Please enter a valid number for weight (like 55.5)."
                    currentStatus = CheckStatus.InputError
                    return@Button
                }

                if (!isHealthyCheck) {
                    displayMessage = "Sorry, you need to be healthy and feel well to donate."
                    currentStatus = CheckStatus.CannotDonate
                    return@Button
                }

                if (age < MINIMUM_AGE || age > MAXIMUM_AGE) {
                    displayMessage = "Sorry, your age must be between $MINIMUM_AGE and $MAXIMUM_AGE years."
                    currentStatus = CheckStatus.CannotDonate
                    return@Button
                }

                if (weight < MINIMUM_WEIGHT_KG) {
                    displayMessage = "Sorry, your weight must be at least $MINIMUM_WEIGHT_KG kg."
                    currentStatus = CheckStatus.CannotDonate
                    return@Button
                }

                if (!sleptEnoughCheck) {
                    displayMessage = "Sorry, you need to have slept more than 5 hours."
                    currentStatus = CheckStatus.CannotDonate
                    return@Button
                }

                currentStatus = CheckStatus.CanDonate
                displayMessage = null
                donationVolume = if (weight > WEIGHT_NEEDED_FOR_450ML) 450 else 350

            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Check Eligibility")
        }

        Spacer(modifier = Modifier.height(16.dp))

        ShowResult(
            checkStatus = currentStatus,
            amount = donationVolume,
            message = displayMessage
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                try {
                    webLinkOpener.openUri(bloodBankInfoUrl)
                } catch (e: Exception) {
                    println("Error opening URL: $e")
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Visit Blood Donation Info Website")
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun CheckboxWithLabel(labelText: String, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Checkbox(
            checked = isChecked,
            onCheckedChange = onCheckedChange
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(labelText, modifier = Modifier.weight(1f))
    }
}

@Composable
fun ShowResult(checkStatus: CheckStatus, amount: Int?, message: String?) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        when (checkStatus) {
            CheckStatus.NotCheckedYet -> {
                Text("Please fill in your details and click 'Check Eligibility'.")
            }
            CheckStatus.CanDonate -> {
                Text(
                    "Result: YES! You are likely eligible to donate.",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                if (amount != null) {
                    Text("Based on your weight, you could donate about $amount ml.")
                }
            }
            CheckStatus.CannotDonate -> {
                Text(
                    "Result: Sorry, you don't seem eligible right now.",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
                if (message != null) {
                    Text("Reason: $message", color = MaterialTheme.colorScheme.error)
                }
            }
            CheckStatus.InputError -> {
                Text(
                    "Input Error",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
                if (message != null) {
                    Text(message, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

fun isValidWeightInput(text: String): Boolean {
    return text.isEmpty() || text.matches(Regex("^\\d*\\.?\\d*\$"))
}
