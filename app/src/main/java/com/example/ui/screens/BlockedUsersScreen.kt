package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.FirebaseRepository
import com.example.model.User
import com.example.ui.components.AvatarView
import com.example.ui.components.EmptyState
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedUsersScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { FirebaseRepository.getInstance() }
    val coroutineScope = rememberCoroutineScope()

    val blockedList by repository.observeBlockedUsers().collectAsState(initial = emptyList())
    val userDetails = remember { mutableStateMapOf<String, User>() }

    LaunchedEffect(blockedList) {
        val db = repository.firestore ?: return@LaunchedEffect
        for (item in blockedList) {
            val targetId = item.blockedUserId
            if (!userDetails.containsKey(targetId)) {
                try {
                    val doc = db.collection("users").document(targetId).get().await()
                    if (doc.exists()) {
                        userDetails[targetId] = User.fromMap(doc.id, doc.data)
                    }
                } catch (e: Exception) {
                    // Handled gracefully
                }
            }
        }
    }

    fun unblock(targetId: String) {
        coroutineScope.launch {
            val result = repository.unblockUser(targetId)
            result.fold(
                onSuccess = { Toast.makeText(context, "Contact unblocked", Toast.LENGTH_SHORT).show() },
                onFailure = { Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_SHORT).show() }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Blocked Users", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("blocked_users_back_button")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = Modifier
            .fillMaxSize()
            .testTag("blocked_users_screen")
    ) { paddingValues ->
        if (blockedList.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Block,
                title = "No Blocked Users",
                subtitle = "Contacts you block will appear here. Blocked users cannot send messages or view your presence."
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .testTag("blocked_users_list")
            ) {
                items(blockedList, key = { it.blockedUserId }) { blocked ->
                    val user = userDetails[blocked.blockedUserId]
                    val name = user?.displayName ?: "User (${blocked.blockedUserId.take(8)})"
                    val username = user?.username ?: ""

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .testTag("blocked_user_row_${blocked.blockedUserId}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AvatarView(
                            photoUrl = user?.photoUrl?.ifBlank { null },
                            name = name,
                            size = 48.dp
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (username.isNotBlank()) {
                                Text(
                                    text = "@$username",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = { unblock(blocked.blockedUserId) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("unblock_button_${blocked.blockedUserId}")
                        ) {
                            Icon(imageVector = Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Unblock")
                        }
                    }
                }
            }
        }
    }
}
