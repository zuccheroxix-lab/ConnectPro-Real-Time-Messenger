package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FirebaseRepository
import com.example.model.User
import com.example.ui.components.AvatarView
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreen
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CreateProfileScreen(
    onProfileCreated: () -> Unit
) {
    val repository = remember { FirebaseRepository.getInstance() }
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    var username by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var photoUrl by remember { mutableStateOf("") }

    var isCheckingUsername by remember { mutableStateOf(false) }
    var isUsernameAvailable by remember { mutableStateOf<Boolean?>(null) }
    var usernameError by remember { mutableStateOf<String?>(null) }
    var generalError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    var debounceJob by remember { mutableStateOf<Job?>(null) }

    fun checkUsername(name: String) {
        val clean = name.lowercase().trim()
        if (clean.length < 3) {
            isUsernameAvailable = null
            usernameError = "Username must be at least 3 characters"
            return
        }
        if (!clean.matches(Regex("^[a-z0-9_]{3,20}$"))) {
            isUsernameAvailable = null
            usernameError = "Letters, numbers, and underscores only (max 20 chars)"
            return
        }

        usernameError = null
        isCheckingUsername = true

        debounceJob?.cancel()
        debounceJob = coroutineScope.launch {
            delay(500)
            val available = repository.isUsernameAvailable(clean)
            isCheckingUsername = false
            isUsernameAvailable = available
            if (!available) {
                usernameError = "@$clean is already taken. Try another."
            }
        }
    }

    fun submitProfile() {
        val cleanUser = username.lowercase().trim()
        val cleanName = displayName.trim()
        if (cleanName.length < 2) {
            generalError = "Please enter your display name."
            return
        }
        if (isUsernameAvailable != true) {
            generalError = "Please provide an available username."
            return
        }

        generalError = null
        isLoading = true
        keyboardController?.hide()

        coroutineScope.launch {
            val uid = repository.currentUserId ?: ""
            val phone = repository.auth?.currentUser?.phoneNumber ?: ""
            val user = User(
                id = uid,
                phoneNumber = phone,
                username = cleanUser,
                displayName = cleanName,
                photoUrl = photoUrl.trim(),
                bio = bio.trim(),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                isOnline = true
            )

            val result = repository.saveUserProfile(user)
            isLoading = false
            result.fold(
                onSuccess = { onProfileCreated() },
                onFailure = { err ->
                    generalError = err.localizedMessage ?: "Failed to save profile. Please try again."
                }
            )
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("create_profile_screen")
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Complete Your Profile",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Choose your unique username to let contacts find you on PulseChat.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(28.dp))

            Box(contentAlignment = Alignment.BottomEnd) {
                AvatarView(
                    photoUrl = photoUrl.ifBlank { null },
                    name = displayName.ifBlank { "User" },
                    size = 90.dp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Display Name Input
            OutlinedTextField(
                value = displayName,
                onValueChange = {
                    displayName = it
                    generalError = null
                },
                label = { Text("Display Name *") },
                placeholder = { Text("Alex Miller") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("display_name_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Username Input with validation icon
            OutlinedTextField(
                value = username,
                onValueChange = {
                    username = it.lowercase().trim()
                    checkUsername(it)
                },
                label = { Text("Unique Username *") },
                placeholder = { Text("alex_miller") },
                leadingIcon = { Text("@", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                trailingIcon = {
                    if (isCheckingUsername) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else if (isUsernameAvailable == true) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Username available",
                            tint = SuccessGreen
                        )
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("username_input"),
                shape = RoundedCornerShape(12.dp)
            )

            if (usernameError != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = usernameError ?: "",
                    color = ErrorRed,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bio Input (Optional)
            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                label = { Text("Bio (Optional)") },
                placeholder = { Text("Hey there! I am using PulseChat.") },
                maxLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bio_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Photo URL (Optional)
            OutlinedTextField(
                value = photoUrl,
                onValueChange = { photoUrl = it },
                label = { Text("Photo URL (Optional)") },
                placeholder = { Text("https://example.com/avatar.jpg") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submitProfile() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("photo_url_input"),
                shape = RoundedCornerShape(12.dp)
            )

            if (generalError != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = generalError ?: "",
                    color = ErrorRed,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { submitProfile() },
                enabled = displayName.isNotBlank() && isUsernameAvailable == true && !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("create_profile_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(
                        text = "Continue to Chats",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
