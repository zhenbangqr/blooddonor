package com.example.blooddonor // Use your actual package name

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.* // Needed for remember, mutableStateOf, getValue, setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// No ViewModel import needed anymore
import com.example.blooddonor.ui.theme.BlooddonorTheme // Use your theme package

// --- Constants ---
// Moved here from ViewModel
const val MIN_AGE = 18
const val MAX_AGE = 60
const val MIN_WEIGHT_KG = 45.0
const val WEIGHT_THRESHOLD_FOR_450ML = 50.0
// const val MIN_SLEEP_HOURS = 5 // Not directly used in boolean check logic below

// --- Enum for Eligibility Status ---
// Moved here from ViewModel
enum class EligibilityStatus {
    NotCheckedYet,
    Eligible,
    NotEligible,
    Error // For input validation errors
}

// --- Activity Setup ---
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BlooddonorTheme { // Apply your app theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    EligibilityScreen() // Call the main screen composable
                }
            }
        }
    }
}

// --- Main Screen Composable ---
@Composable
fun EligibilityScreen() {
    // --- State Variables managed within the Composable ---
    var ageInput by remember { mutableStateOf("") }
    var weightInput by remember { mutableStateOf("") }
    var isHealthy by remember { mutableStateOf(true) } // Default state
    var sleptEnough by remember { mutableStateOf(true) } // Default state

    var eligibilityResult by remember { mutableStateOf(EligibilityStatus.NotCheckedYet) }
    var donationAmountMl by remember { mutableStateOf<Int?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // --- Other Setup ---
    val scrollState = rememberScrollState()
    val uriHandler = LocalUriHandler.current
    // Example URL - Replace with the actual Malaysian National Blood Centre URL if needed
    val bloodBankUrl = "https://www.pdn.gov.my/" // Malaysia's National Blood Centre

    // --- UI Layout ---
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState), // Make column scrollable
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp) // Spacing between elements
    ) {
        Text("Blood Donor Eligibility Check", fontSize = 20.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(16.dp))

        // --- Input Fields ---
        OutlinedTextField(
            value = ageInput,
            // Update state directly on value change
            onValueChange = { ageInput = it.filter { char -> char.isDigit() } },
            label = { Text("Your Age (years)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            isError = eligibilityResult == EligibilityStatus.Error && ageInput.toIntOrNull() == null // Highlight if error related to age
        )

        OutlinedTextField(
            value = weightInput,
            // Update state directly on value change
            onValueChange = { weightInput = it },
            label = { Text("Your Weight (kg)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            isError = eligibilityResult == EligibilityStatus.Error && weightInput.toDoubleOrNull() == null // Highlight if error related to weight
        )

        Spacer(modifier = Modifier.height(8.dp))

        // --- Checkboxes/Switches ---
        CheckboxRow(
            label = "Are you healthy and feeling well?",
            checked = isHealthy,
            onCheckedChange = { isHealthy = it } // Update state directly
        )

        CheckboxRow(
            label = "Did you sleep more than 5 hours last night?",
            checked = sleptEnough,
            onCheckedChange = { sleptEnough = it } // Update state directly
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- Check Button ---
        Button(
            onClick = {
                // --- Eligibility Logic moved inside onClick ---
                // 1. Clear previous errors and results
                errorMessage = null
                eligibilityResult = EligibilityStatus.NotCheckedYet // Reset status before check
                donationAmountMl = null

                // 2. Validate and parse inputs
                val age = ageInput.toIntOrNull()
                val weight = weightInput.toDoubleOrNull()

                if (age == null) {
                    errorMessage = "Please enter a valid age (number)."
                    eligibilityResult = EligibilityStatus.Error
                    return@Button // Exit onClick lambda early
                }
                if (weight == null) {
                    errorMessage = "Please enter a valid weight (number)."
                    eligibilityResult = EligibilityStatus.Error
                    return@Button // Exit onClick lambda early
                }

                // 3. Check eligibility criteria
                val reasons = mutableListOf<String>()

                if (!isHealthy) {
                    reasons.add("Must be healthy and feeling well.")
                }
                if (age < MIN_AGE || age > MAX_AGE) {
                    reasons.add("Age must be between $MIN_AGE and $MAX_AGE.")
                }
                if (weight < MIN_WEIGHT_KG) {
                    reasons.add("Weight must be $MIN_WEIGHT_KG kg or more.")
                }
                if (!sleptEnough) {
                    reasons.add("Must have slept more than 5 hours.")
                }

                // 4. Determine final result and update states
                if (reasons.isEmpty()) {
                    eligibilityResult = EligibilityStatus.Eligible
                    // Calculate donation amount
                    donationAmountMl = if (weight > WEIGHT_THRESHOLD_FOR_450ML) {
                        450
                    } else {
                        // Weight is >= 45kg and <= 50kg
                        350
                    }
                    errorMessage = null
                } else {
                    eligibilityResult = EligibilityStatus.NotEligible
                    errorMessage = reasons.joinToString("\n")
                    donationAmountMl = null
                }
                // --- End of Eligibility Logic ---
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Check Eligibility")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Result Display ---
        // Pass the state variables directly to the helper composable
        ResultDisplay(
            status = eligibilityResult,
            donationAmount = donationAmountMl,
            errorMessage = errorMessage
        )

        Spacer(modifier = Modifier.weight(1f)) // Pushes the link to the bottom

        // --- Link to Website ---
        Button(
            onClick = {
                try {
                    uriHandler.openUri(bloodBankUrl)
                } catch (e: Exception) {
                    println("Error opening URL: $e") // Log error if URL fails
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Visit National Blood Centre Website")
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

// --- Helper Composable for Checkbox Rows --- (No changes needed here)
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

// --- Helper Composable for Result Display --- (No changes needed here)
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
                Text("Result: Eligible to Donate!", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                if (donationAmount != null) {
                    Text("Maximum donation amount: $donationAmount ml")
                }
            }
            EligibilityStatus.NotEligible -> {
                Text("Result: Not Eligible to Donate", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                if (errorMessage != null) {
                    Text("Reason(s):\n$errorMessage", color = MaterialTheme.colorScheme.error)
                }
            }
            EligibilityStatus.Error -> {
                Text("Error", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                if (errorMessage != null) {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
