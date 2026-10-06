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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FirebaseRepository
import com.example.ui.Routes
import com.example.ui.theme.ErrorRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpScreen(
    verificationId: String,
    phoneNumber: String,
    onNavigateBack: () -> Unit,
    onNavigateToCreateProfile: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val repository = remember { FirebaseRepository.getInstance() }
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    var currentVerificationId by remember { mutableStateOf(verificationId) }
    var otpCode by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var attemptsLeft by remember { mutableIntStateOf(5) }
    var cooldownSeconds by remember { mutableIntStateOf(60) }
    var isExpired by remember { mutableStateOf(false) }

    // Live countdown timer for resend cooldown & expiration
    LaunchedEffect(currentVerificationId) {
        cooldownSeconds = 60
        isExpired = false
        while (cooldownSeconds > 0) {
            delay(1000L)
            cooldownSeconds--
        }
    }

    fun verifyCode() {
        if (otpCode.length != 6) {
            errorMessage = "Please enter the 6-digit SMS verification code."
            return
        }

        if (attemptsLeft <= 0) {
            errorMessage = "Too many failed attempts. Please request a new OTP code."
            return
        }

        errorMessage = null
        isLoading = true
        keyboardController?.hide()

        coroutineScope.launch {
            val result = repository.signInWithPhoneCode(currentVerificationId, otpCode)
            isLoading = false

            result.fold(
                onSuccess = { user ->
                    val db = repository.firestore
                    if (db != null) {
                        try {
                            val userDoc = db.collection("users").document(user.uid).get().await()
                            if (userDoc.exists() && !userDoc.getString("username").isNullOrBlank()) {
                                onNavigateToHome()
                            } else {
                                onNavigateToCreateProfile()
                            }
                        } catch (e: Exception) {
                            onNavigateToCreateProfile()
                        }
                    } else {
                        onNavigateToCreateProfile()
                    }
                },
                onFailure = { error ->
                    attemptsLeft--
                    val remaining = if (attemptsLeft > 0) " ($attemptsLeft attempts left)" else " (Locked)"
                    errorMessage = (error.localizedMessage ?: "Invalid verification code") + remaining
                }
            )
        }
    }

    fun resendOtp() {
        if (activity == null) {
            errorMessage = "Context error: Activity is not attached."
            return
        }
        if (cooldownSeconds > 0) return

        errorMessage = null
        isLoading = true
        attemptsLeft = 5
        otpCode = ""

        repository.verifyPhoneNumber(
            phoneNumber = phoneNumber,
            activity = activity,
            onVerificationCompleted = {
                isLoading = false
                onNavigateToHome()
            },
            onVerificationFailed = { e ->
                isLoading = false
                errorMessage = e.localizedMessage ?: "Failed to resend OTP. Please try again later."
            },
            onCodeSent = { newId, _ ->
                isLoading = false
                currentVerificationId = newId
                cooldownSeconds = 60
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Verify Phone", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("otp_back_button")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = Modifier
            .fillMaxSize()
            .testTag("otp_screen")
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(64.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Enter 6-Digit Code",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Sent via SMS to $phoneNumber",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(28.dp))

            OutlinedTextField(
                value = otpCode,
                onValueChange = { input ->
                    if (input.length <= 6 && input.all { it.isDigit() }) {
                        otpCode = input
                        if (input.length == 6) {
                            verifyCode()
                        }
                    }
                },
                label = { Text("6-Digit Code") },
                placeholder = { Text("• • • • • •") },
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 24.sp,
                    letterSpacing = 8.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { verifyCode() }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("otp_code_input"),
                shape = RoundedCornerShape(12.dp)
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMessage ?: "",
                    color = ErrorRed,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("otp_error_text")
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { verifyCode() },
                enabled = otpCode.length == 6 && !isLoading && attemptsLeft > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("verify_otp_button"),
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
                        text = "Verify Code",
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
                if (cooldownSeconds > 0) {
                    Text(
                        text = "Resend code in ${cooldownSeconds}s",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    TextButton(
                        onClick = { resendOtp() },
                        enabled = !isLoading,
                        modifier = Modifier.testTag("resend_otp_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Resend SMS Code",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
