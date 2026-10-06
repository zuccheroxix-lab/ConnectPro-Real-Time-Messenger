package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FirebaseRepository
import com.example.ui.components.AvatarView
import com.example.ui.theme.ErrorRed
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettingsScreen(
    onNavigateBack: () -> Unit,
    onAccountDeleted: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { FirebaseRepository.getInstance() }
    val coroutineScope = rememberCoroutineScope()
    val currentUser by repository.observeCurrentUser().collectAsState(initial = null)

    var displayName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var photoUrl by remember { mutableStateOf("") }

    var isInitialized by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }

    LaunchedEffect(currentUser) {
        if (currentUser != null && !isInitialized) {
            displayName = currentUser?.displayName ?: ""
            username = currentUser?.username ?: ""
            bio = currentUser?.bio ?: ""
            photoUrl = currentUser?.photoUrl ?: ""
            isInitialized = true
        }
    }

    fun saveProfile() {
        val cleanName = displayName.trim()
        val cleanUser = username.lowercase().trim()

        if (cleanName.length < 2) {
            Toast.makeText(context, "Display name must be at least 2 characters", Toast.LENGTH_SHORT).show()
            return
        }
        if (!cleanUser.matches(Regex("^[a-z0-9_]{3,20}$"))) {
            Toast.makeText(context, "Username must be 3-20 letters/numbers/underscores", Toast.LENGTH_SHORT).show()
            return
        }

        isSaving = true
        coroutineScope.launch {
            if (cleanUser != currentUser?.username) {
                val available = repository.isUsernameAvailable(cleanUser)
                if (!available) {
                    isSaving = false
                    Toast.makeText(context, "Username @$cleanUser is already taken", Toast.LENGTH_SHORT).show()
                    return@launch
                }
            }

            val result = repository.updateUserProfile(
                displayName = cleanName,
                bio = bio,
                photoUrl = photoUrl,
                username = cleanUser
            )
            isSaving = false

            result.fold(
                onSuccess = {
                    Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                },
                onFailure = {
                    Toast.makeText(context, "Failed to update: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    fun confirmDeleteAccount() {
        isDeleting = true
        coroutineScope.launch {
            val result = repository.deleteAccount()
            isDeleting = false
            result.fold(
                onSuccess = {
                    Toast.makeText(context, "Account permanently deleted", Toast.LENGTH_SHORT).show()
                    onAccountDeleted()
                },
                onFailure = {
                    Toast.makeText(context, "Error deleting account: ${it.message}", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Account Permanently", color = ErrorRed, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to permanently delete your PulseChat account? " +
                    "All your profile data, personal username, and chat associations will be permanently removed. This action cannot be undone."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        confirmDeleteAccount()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                    modifier = Modifier.testTag("confirm_delete_account_button")
                ) {
                    Text("Delete Account")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Account Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("account_settings_back_button")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = Modifier
            .fillMaxSize()
            .testTag("account_settings_screen")
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AvatarView(
                photoUrl = photoUrl.ifBlank { null },
                name = displayName.ifBlank { "User" },
                size = 90.dp,
                isPremium = currentUser?.isPremium == true
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Display Name") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("account_display_name_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it.lowercase().trim() },
                label = { Text("Username") },
                leadingIcon = { Text("@", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("account_username_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                label = { Text("Bio") },
                maxLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("account_bio_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = photoUrl,
                onValueChange = { photoUrl = it },
                label = { Text("Photo URL") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("account_photo_url_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = { saveProfile() },
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_account_profile_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Changes", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "DANGER ZONE",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = ErrorRed,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = { showDeleteDialog = true },
                enabled = !isDeleting,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("delete_account_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Delete Account", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
