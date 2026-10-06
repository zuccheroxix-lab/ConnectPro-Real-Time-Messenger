package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FirebaseRepository
import com.example.model.Conversation
import com.example.model.User
import com.example.ui.components.AvatarView
import com.example.ui.components.EmptyState
import com.example.ui.components.ServiceNoticeCard
import com.example.ui.components.VerifiedBadge
import com.example.ui.theme.PulseCyanPrimary
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToChat: (conversationId: String, otherUserId: String) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToProfile: (userId: String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPremium: () -> Unit
) {
    val repository = remember { FirebaseRepository.getInstance() }
    val currentUserId = repository.currentUserId ?: ""

    val conversationsState by repository.observeConversations().collectAsState(initial = emptyList())
    val currentUserState by repository.observeCurrentUser().collectAsState(initial = null)

    var searchQuery by remember { mutableStateOf("") }
    var showNoticeDialog by remember { mutableStateOf(false) }

    // Map of otherUserId to User profile
    val usersCache = remember { mutableStateMapOf<String, User>() }

    // Fetch user details for conversation partners
    LaunchedEffect(conversationsState) {
        val db = repository.firestore ?: return@LaunchedEffect
        for (conv in conversationsState) {
            val otherId = conv.members.firstOrNull { it != currentUserId } ?: continue
            if (!usersCache.containsKey(otherId)) {
                try {
                    val snapshot = db.collection("users").document(otherId).get().await()
                    if (snapshot.exists()) {
                        usersCache[otherId] = User.fromMap(snapshot.id, snapshot.data)
                    }
                } catch (e: Exception) {
                    // Handled gracefully
                }
            }
        }
    }

    val filteredConversations = remember(conversationsState, searchQuery, usersCache) {
        if (searchQuery.isBlank()) {
            conversationsState
        } else {
            val q = searchQuery.lowercase().trim()
            conversationsState.filter { conv ->
                val otherId = conv.members.firstOrNull { it != currentUserId }
                val otherUser = otherId?.let { usersCache[it] }
                (otherUser?.displayName?.lowercase()?.contains(q) == true) ||
                        (otherUser?.username?.lowercase()?.contains(q) == true) ||
                        (conv.lastMessageText.lowercase().contains(q))
            }
        }
    }

    fun formatTimestamp(millis: Long): String {
        if (millis <= 0L) return ""
        val now = Calendar.getInstance()
        val msgTime = Calendar.getInstance().apply { timeInMillis = millis }
        return if (now.get(Calendar.DAY_OF_YEAR) == msgTime.get(Calendar.DAY_OF_YEAR) &&
            now.get(Calendar.YEAR) == msgTime.get(Calendar.YEAR)
        ) {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))
        } else {
            SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(millis))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "PulseChat",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        if (currentUserState?.isPremium == true) {
                            Spacer(modifier = Modifier.width(6.dp))
                            VerifiedBadge()
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToSearch,
                        modifier = Modifier.testTag("home_search_action_button")
                    ) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Search Users")
                    }
                    IconButton(
                        onClick = onNavigateToPremium,
                        modifier = Modifier.testTag("home_premium_action_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = "Premium Subscription",
                            tint = if (currentUserState?.isPremium == true) PulseCyanPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("home_settings_action_button")
                    ) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToSearch,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("new_chat_fab")
            ) {
                Icon(imageVector = Icons.Default.Chat, contentDescription = "Start New Chat")
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen")
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search query field inside home
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search conversations...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("home_search_input"),
                shape = RoundedCornerShape(14.dp)
            )

            if (!repository.isConfigured) {
                ServiceNoticeCard(
                    title = "Firebase Backend Setup Needed",
                    description = "Add google-services.json to the app/ module to sync real-time chat messages to Firebase Cloud.",
                    onConfigureClick = { onNavigateToSettings() }
                )
            }

            if (filteredConversations.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.Forum,
                    title = if (searchQuery.isBlank()) "No conversations yet" else "No matching chats",
                    subtitle = if (searchQuery.isBlank()) "Find other users by their unique username to begin chatting." else "Try searching for a different name or message.",
                    actionButton = {
                        Button(
                            onClick = onNavigateToSearch,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("start_chat_empty_button")
                        ) {
                            Icon(imageVector = Icons.Default.PersonSearch, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Find Users")
                        }
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("conversations_list"),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(filteredConversations, key = { it.id }) { conv ->
                        val otherId = conv.members.firstOrNull { it != currentUserId } ?: ""
                        val otherUser = usersCache[otherId]
                        val unreadCount = conv.unreadCounts[currentUserId] ?: 0L

                        ConversationRow(
                            conversation = conv,
                            otherUser = otherUser,
                            unreadCount = unreadCount,
                            formattedTime = formatTimestamp(conv.lastMessageTimestamp),
                            onClick = { onNavigateToChat(conv.id, otherId) },
                            onAvatarClick = { onNavigateToProfile(otherId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConversationRow(
    conversation: Conversation,
    otherUser: User?,
    unreadCount: Long,
    formattedTime: String,
    onClick: () -> Unit,
    onAvatarClick: () -> Unit
) {
    val name = otherUser?.displayName?.ifBlank { "Pulse User" } ?: "Pulse User"
    val username = otherUser?.username ?: ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("conversation_row_${conversation.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.clickable { onAvatarClick() }) {
            AvatarView(
                photoUrl = otherUser?.photoUrl?.ifBlank { null },
                name = name,
                size = 52.dp,
                isOnline = otherUser?.isOnline == true,
                showOnlineIndicator = true,
                isPremium = otherUser?.isPremium == true
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (otherUser?.isPremium == true) {
                        Spacer(modifier = Modifier.width(4.dp))
                        VerifiedBadge()
                    }
                }

                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (unreadCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            if (username.isNotBlank()) {
                Text(
                    text = "@$username",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = conversation.lastMessageText.ifBlank { "No messages yet" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (unreadCount > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (unreadCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Text(
                            text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
