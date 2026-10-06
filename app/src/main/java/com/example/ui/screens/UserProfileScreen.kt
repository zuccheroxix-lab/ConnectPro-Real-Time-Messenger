package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FirebaseRepository
import com.example.ui.components.AvatarView
import com.example.ui.components.VerifiedBadge
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.PulseCyanPrimary
import com.example.ui.theme.SuccessGreen
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    userId: String,
    onNavigateBack: () -> Unit,
    onNavigateToChat: (conversationId: String, otherUserId: String) -> Unit
) {
    val context = LocalContext.current
    val repository = remember { FirebaseRepository.getInstance() }
    val coroutineScope = rememberCoroutineScope()

    val user by repository.observeUser(userId).collectAsState(initial = null)
    val blockedUsers by repository.observeBlockedUsers().collectAsState(initial = emptyList())
    val isBlocked = remember(blockedUsers, userId) {
        blockedUsers.any { it.blockedUserId == userId }
    }

    var isProcessingBlock by remember { mutableStateOf(false) }

    fun toggleBlock() {
        isProcessingBlock = true
        coroutineScope.launch {
            if (isBlocked) {
                repository.unblockUser(userId)
                Toast.makeText(context, "User unblocked", Toast.LENGTH_SHORT).show()
            } else {
                repository.blockUser(userId)
                Toast.makeText(context, "User blocked", Toast.LENGTH_SHORT).show()
            }
            isProcessingBlock = false
        }
    }

    fun startChat() {
        coroutineScope.launch {
            val result = repository.getOrCreateConversationId(userId)
            result.fold(
                onSuccess = { convId -> onNavigateToChat(convId, userId) },
                onFailure = { Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_SHORT).show() }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("profile_back_button")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = Modifier
            .fillMaxSize()
            .testTag("user_profile_screen")
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val profile = user

            AvatarView(
                photoUrl = profile?.photoUrl?.ifBlank { null },
                name = profile?.displayName ?: "User",
                size = 110.dp,
                isOnline = profile?.isOnline == true,
                showOnlineIndicator = true,
                isPremium = profile?.isPremium == true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = profile?.displayName?.ifBlank { "Pulse User" } ?: "Loading...",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (profile?.isPremium == true) {
                    Spacer(modifier = Modifier.width(6.dp))
                    VerifiedBadge(modifier = Modifier.size(22.dp))
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "@${profile?.username ?: "username"}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (profile?.isOnline == true) "Active Now" else "Last seen recently",
                style = MaterialTheme.typography.bodySmall,
                color = if (profile?.isOnline == true) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!profile?.bio.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Bio",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = profile?.bio ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Real Entitlement Status Card
            val userEntitlement by repository.observeUserEntitlement(userId).collectAsState(initial = com.example.model.UserEntitlement(userId = userId))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "STATUS KEANGGOTAAN",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Paket", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = userEntitlement.plan,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (userEntitlement.isPremiumTier) PulseCyanPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Masa Berlaku", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = if (userEntitlement.permanent) "Permanent"
                            else if (userEntitlement.expiresAt > 0L) "Expires: " + SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(userEntitlement.expiresAt))
                            else "Standar",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Custom Check", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = if (userEntitlement.customCheckEnabled) "Enabled" else "Not Available",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = if (userEntitlement.customCheckEnabled) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Actions: Start Chat / Block
            Button(
                onClick = { startChat() },
                enabled = !isBlocked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("profile_start_chat_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Chat, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Send Message", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = { toggleBlock() },
                enabled = !isProcessingBlock,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (isBlocked) MaterialTheme.colorScheme.primary else ErrorRed
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("profile_block_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = if (isBlocked) Icons.Default.Check else Icons.Default.Block,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isBlocked) "Unblock User" else "Block User",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
