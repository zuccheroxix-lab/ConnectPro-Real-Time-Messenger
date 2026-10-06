package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FirebaseRepository
import com.example.ui.components.AvatarView
import com.example.ui.components.VerifiedBadge
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.PulseCyanPrimary
import com.example.ui.theme.WarningAmber as AmberColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAccount: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToBlocked: () -> Unit,
    onNavigateToPremium: () -> Unit,
    onNavigateToRedeem: () -> Unit,
    onNavigateToSpinner: () -> Unit,
    onNavigateToCustomCheck: () -> Unit,
    onNavigateToAdminPanel: () -> Unit,
    onLoggedOut: () -> Unit
) {
    val repository = remember { FirebaseRepository.getInstance() }
    val currentUser by repository.observeCurrentUser().collectAsState(initial = null)
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showConfigInfoDialog by remember { mutableStateOf(false) }

    fun logout() {
        repository.signOut()
        onLoggedOut()
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Log Out") },
            text = { Text("Are you sure you want to log out of PulseChat?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        logout()
                    },
                    modifier = Modifier.testTag("confirm_logout_button")
                ) {
                    Text("Log Out", color = ErrorRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showConfigInfoDialog) {
        AlertDialog(
            onDismissRequest = { showConfigInfoDialog = false },
            title = { Text("Service Architecture", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        "PulseChat Backend Specifications:\n\n" +
                        "1. Firebase Authentication: Carrier SMS OTP.\n" +
                        "2. Firebase Cloud Firestore: Realtime messages, user profiles, unread counters, delivery receipts.\n" +
                        "3. Firebase Cloud Messaging (FCM): Push notification delivery to devices.\n" +
                        "4. Security: Input validation, server-side verified subscription state, zero private secrets stored in APK.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showConfigInfoDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_screen")
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // User Header
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToAccount() }
                    .testTag("settings_profile_header"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AvatarView(
                        photoUrl = currentUser?.photoUrl?.ifBlank { null },
                        name = currentUser?.displayName ?: "User",
                        size = 60.dp,
                        isOnline = true,
                        isPremium = currentUser?.isPremium == true
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = currentUser?.displayName?.ifBlank { "User" } ?: "User",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (currentUser?.isPremium == true) {
                                Spacer(modifier = Modifier.width(4.dp))
                                VerifiedBadge()
                            }
                        }
                        Text(
                            text = "@${currentUser?.username ?: "username"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = currentUser?.phoneNumber ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "PREFERENCES",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingsItem(
                icon = Icons.Default.ManageAccounts,
                title = "Account Settings",
                subtitle = "Display name, bio, unique username, delete account",
                onClick = onNavigateToAccount,
                testTag = "settings_account_item"
            )

            SettingsItem(
                icon = Icons.Default.Notifications,
                title = "Notifications",
                subtitle = "Alert sounds, message preview, vibrations",
                onClick = onNavigateToNotifications,
                testTag = "settings_notifications_item"
            )

            SettingsItem(
                icon = Icons.Default.Block,
                title = "Blocked Users",
                subtitle = "Manage restricted and blocked contacts",
                onClick = onNavigateToBlocked,
                testTag = "settings_blocked_users_item"
            )

            SettingsItem(
                icon = Icons.Default.WorkspacePremium,
                title = "Pulse Premium",
                subtitle = if (currentUser?.isPremium == true) "Active subscription perks" else "Upgrade to get Verified Badge & perks",
                onClick = onNavigateToPremium,
                iconTint = PulseCyanPrimary,
                testTag = "settings_premium_item"
            )

            SettingsItem(
                icon = Icons.Default.Key,
                title = "Redeem Premium Code",
                subtitle = "Tukarkan kode voucher resmi dari Admin",
                onClick = onNavigateToRedeem,
                testTag = "settings_redeem_item"
            )

            SettingsItem(
                icon = Icons.Default.Casino,
                title = "Premium Spinner",
                subtitle = "Putar keberuntungan untuk hadiah eksklusif",
                onClick = onNavigateToSpinner,
                testTag = "settings_spinner_item"
            )

            SettingsItem(
                icon = Icons.Default.DoneAll,
                title = "Custom Check",
                subtitle = "Kustomisasi warna, style, dan ukuran centang pesan",
                onClick = onNavigateToCustomCheck,
                testTag = "settings_custom_check_item"
            )

            SettingsItem(
                icon = Icons.Default.CloudQueue,
                title = "Backend & Security Info",
                subtitle = "Firebase setup, data policies, credentials info",
                onClick = { showConfigInfoDialog = true },
                testTag = "settings_backend_info_item"
            )

            if (currentUser?.isAdmin == true) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "ADMINISTRATION",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = AmberColor,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsItem(
                    icon = Icons.Default.AdminPanelSettings,
                    title = "Admin Panel",
                    subtitle = "Kelola pengguna, buat kode, paket, spinner & audit log",
                    onClick = onNavigateToAdminPanel,
                    iconTint = AmberColor,
                    titleColor = AmberColor,
                    testTag = "settings_admin_panel_item"
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "SESSION",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingsItem(
                icon = Icons.AutoMirrored.Filled.Logout,
                title = "Log Out",
                subtitle = "Sign out from this device",
                onClick = { showLogoutDialog = true },
                iconTint = ErrorRed,
                titleColor = ErrorRed,
                testTag = "settings_logout_item"
            )
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    iconTint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
    titleColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    testTag: String
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = titleColor
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
