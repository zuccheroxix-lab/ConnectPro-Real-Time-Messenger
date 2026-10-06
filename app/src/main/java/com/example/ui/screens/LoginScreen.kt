package com.example.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FirebaseRepository
import com.example.ui.Routes
import com.example.ui.components.ServiceNoticeCard
import com.example.ui.theme.ErrorRed

@Composable
fun LoginScreen(
    onNavigateToOtp: (verificationId: String, phoneNumber: String) -> Unit,
    onNavigateToHome: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val repository = remember { FirebaseRepository.getInstance() }
    val keyboardController = LocalSoftwareKeyboardController.current

    var countryCode by remember { mutableStateOf("+62") }
    var phoneNumber by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showSetupDialog by remember { mutableStateOf(false) }

    fun sendOtp() {
        val cleanNumber = phoneNumber.trim().trimStart('0')
        if (cleanNumber.length < 8) {
            errorMessage = "Please enter a valid phone number (at least 8 digits)."
            return
        }

        val fullPhoneNumber = "$countryCode$cleanNumber"
        errorMessage = null
        isLoading = true
        keyboardController?.hide()

        if (!repository.isConfigured) {
            isLoading = false
            showSetupDialog = true
            return
        }

        if (activity == null) {
            isLoading = false
            errorMessage = "Context error: Activity is not attached."
            return
        }

        repository.verifyPhoneNumber(
            phoneNumber = fullPhoneNumber,
            activity = activity,
            onVerificationCompleted = { credential ->
                // Auto-retrieval / instant verification
                isLoading = false
                onNavigateToHome()
            },
            onVerificationFailed = { e ->
                isLoading = false
                errorMessage = e.localizedMessage ?: "Verification request failed. Please check phone number and network."
            },
            onCodeSent = { verificationId, token ->
                isLoading = false
                onNavigateToOtp(verificationId, fullPhoneNumber)
            }
        )
    }

    if (showSetupDialog) {
        AlertDialog(
            onDismissRequest = { showSetupDialog = false },
            title = {
                Text("Firebase Phone Auth Required", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        "PulseChat uses authentic Firebase Phone Authentication for SMS OTP delivery.\n\n" +
                        "To send live SMS OTPs:\n" +
                        "1. Download your google-services.json from Firebase Console.\n" +
                        "2. Place it in the /app folder.\n" +
                        "3. In Firebase Console -> Authentication -> Sign-in method, enable 'Phone'.\n" +
                        "4. (Optional for testing) Add a test phone number (e.g., +6281234567890 with SMS code 123456) in Firebase Console to test without SMS charges.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showSetupDialog = false },
                    modifier = Modifier.testTag("dismiss_setup_dialog_button")
                ) {
                    Text("Understood")
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("login_screen")
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(64.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Enter Phone Number",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "PulseChat will send a one-time SMS verification code to verify your carrier number.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (!repository.isConfigured) {
                ServiceNoticeCard(
                    title = "Firebase Credentials Required",
                    description = "Live SMS OTP requires google-services.json in the /app directory with Phone Auth enabled.",
                    onConfigureClick = { showSetupDialog = true }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = countryCode,
                    onValueChange = { if (it.startsWith("+") && it.length <= 5) countryCode = it },
                    label = { Text("Code") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier
                        .width(90.dp)
                        .testTag("country_code_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() }) phoneNumber = input
                    },
                    label = { Text("Phone Number") },
                    placeholder = { Text("8123456789") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { sendOtp() }
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("phone_number_input"),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMessage ?: "",
                    color = ErrorRed,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.testTag("login_error_text")
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = { sendOtp() },
                enabled = phoneNumber.isNotBlank() && !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("send_otp_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Send SMS OTP",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "End-to-end verified carrier authentication",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
