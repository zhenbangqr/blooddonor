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

const val MIN_AGE = 18
const val MAX_AGE = 60
const val MIN_WEIGHT_KG = 45.0
const val WEIGHT_THRESHOLD_FOR_450ML = 50.0

enum class EligibilityStatus {
    NotCheckedYet,
    Eligible,
    NotEligible,
    Error
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
                    EligibilityScreen()
                }
            }
        }
    }
}

@Composable
fun EligibilityScreen() {
    var ageInput by remember { mutableStateOf("") }
    var weightInput by remember { mutableStateOf("") }
    var isHealthy by remember { mutableStateOf(true) }
    var sleptEnough by remember { mutableStateOf(true) }

    var eligibilityResult by remember { mutableStateOf(EligibilityStatus.NotCheckedYet) }
    var donationAmountMl by remember { mutableStateOf<Int?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()
    val uriHandler = LocalUriHandler.current
    val bloodBankUrl = "https://www.pdn.gov.my/"

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
            modifier = Modifier.padding(top = 50.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = ageInput,
            onValueChange = { ageInput = it.filter { char -> char.isDigit() } },
            label = { Text("Your Age (years)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            isError = eligibilityResult == EligibilityStatus.Error && ageInput.toIntOrNull() == null
        )

        OutlinedTextField(
            value = weightInput,
            onValueChange = { weightInput = it },
            label = { Text("Your Weight (kg)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            isError = eligibilityResult == EligibilityStatus.Error && weightInput.toDoubleOrNull() == null
        )

        Spacer(modifier = Modifier.height(8.dp))

        CheckboxRow(
            label = "Are you healthy and feeling well?",
            checked = isHealthy,
            onCheckedChange = { isHealthy = it }
        )

        CheckboxRow(
            label = "Did you sleep more than 5 hours last night?",
            checked = sleptEnough,
            onCheckedChange = { sleptEnough = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                errorMessage = null
                eligibilityResult = EligibilityStatus.NotCheckedYet
                donationAmountMl = null

                val age = ageInput.toIntOrNull()
                val weight = weightInput.toDoubleOrNull()

                if (age == null) {
                    errorMessage = "Please enter a valid age (like 25)."
                    eligibilityResult = EligibilityStatus.Error
                    return@Button
                }
                if (weight == null) {
                    errorMessage = "Please enter a valid weight (like 55.5)."
                    eligibilityResult = EligibilityStatus.Error
                    return@Button
                }

                var reasonMessage = ""

                if (!isHealthy) {
                    reasonMessage += "You need to be healthy and feel well.\n"
                }
                if (age < MIN_AGE || age > MAX_AGE) {
                    reasonMessage += "Age must be from $MIN_AGE to $MAX_AGE years.\n"
                }
                if (weight < MIN_WEIGHT_KG) {
                    reasonMessage += "Weight must be at least $MIN_WEIGHT_KG kg.\n"
                }
                if (!sleptEnough) {
                    reasonMessage += "You need more than 5 hours of sleep.\n"
                }

                if (reasonMessage.isEmpty()) {
                    eligibilityResult = EligibilityStatus.Eligible
                    errorMessage = null
                    donationAmountMl = if (weight > WEIGHT_THRESHOLD_FOR_450ML) 450 else 350
                } else {
                    eligibilityResult = EligibilityStatus.NotEligible
                    errorMessage = reasonMessage.trim()
                    donationAmountMl = null
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Check Eligibility")
        }

        Spacer(modifier = Modifier.height(16.dp))

        ResultDisplay(
            status = eligibilityResult,
            donationAmount = donationAmountMl,
            errorMessage = errorMessage
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                try {
                    uriHandler.openUri(bloodBankUrl)
                } catch (e: Exception) {
                    println("Error opening URL: $e")
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            // Updated text to be more general
            Text("Visit Blood Donation Info Website")
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun CheckboxRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, modifier = Modifier.weight(1f))
    }
}

@Composable
fun ResultDisplay(
    status: EligibilityStatus,
    donationAmount: Int?,
    errorMessage: String?
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        when (status) {
            EligibilityStatus.NotCheckedYet -> {
                Text("Please fill in the details and click 'Check Eligibility'.")
            }
            EligibilityStatus.Eligible -> {
                Text("Result: YES! You can donate.", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                if (donationAmount != null) {
                    Text("You can give about $donationAmount ml.")
                }
            }
            EligibilityStatus.NotEligible -> {
                Text("Result: Sorry, not eligible right now.", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                if (errorMessage != null) {
                    Text("Why?\n$errorMessage", color = MaterialTheme.colorScheme.error)
                }
            }
            EligibilityStatus.Error -> {
                Text("Input Error", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                if (errorMessage != null) {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
