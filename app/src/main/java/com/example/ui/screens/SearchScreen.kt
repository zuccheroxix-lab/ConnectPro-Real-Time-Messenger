package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FirebaseRepository
import com.example.model.User
import com.example.ui.components.AvatarView
import com.example.ui.components.EmptyState
import com.example.ui.components.VerifiedBadge
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateBack: () -> Unit,
    onNavigateToChat: (conversationId: String, otherUserId: String) -> Unit,
    onNavigateToProfile: (userId: String) -> Unit
) {
    val repository = remember { FirebaseRepository.getInstance() }
    val coroutineScope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var isStartingChat by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val searchResults by repository.searchUsers(searchQuery).collectAsState(initial = emptyList())

    fun startChat(user: User) {
        isStartingChat = user.id
        errorMessage = null
        coroutineScope.launch {
            val result = repository.getOrCreateConversationId(user.id)
            isStartingChat = null
            result.fold(
                onSuccess = { conversationId ->
                    onNavigateToChat(conversationId, user.id)
                },
                onFailure = { err ->
                    errorMessage = err.localizedMessage ?: "Failed to open conversation."
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Find Users", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("search_back_button")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = Modifier
            .fillMaxSize()
            .testTag("search_screen")
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by username (e.g. alex)...") },
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
                    .testTag("user_search_input"),
                shape = RoundedCornerShape(12.dp)
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            if (searchQuery.isBlank()) {
                EmptyState(
                    icon = Icons.Default.PersonSearch,
                    title = "Search PulseChat Users",
                    subtitle = "Enter a username to search directly from the real database and start messaging."
                )
            } else if (searchResults.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.Search,
                    title = "No users found",
                    subtitle = "No user found matching '@$searchQuery'. Please verify the username."
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("search_results_list")
                ) {
                    items(searchResults, key = { it.id }) { user ->
                        val isOpening = isStartingChat == user.id

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToProfile(user.id) }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .testTag("search_user_row_${user.id}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AvatarView(
                                photoUrl = user.photoUrl.ifBlank { null },
                                name = user.displayName.ifBlank { "User" },
                                size = 48.dp,
                                isOnline = user.isOnline,
                                showOnlineIndicator = true,
                                isPremium = user.isPremium
                            )

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = user.displayName.ifBlank { "Pulse User" },
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (user.isPremium) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        VerifiedBadge()
                                    }
                                }
                                Text(
                                    text = "@${user.username}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (user.bio.isNotBlank()) {
                                    Text(
                                        text = user.bio,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        maxLines = 1
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = { startChat(user) },
                                enabled = !isOpening,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("start_chat_button_${user.id}")
                            ) {
                                if (isOpening) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(imageVector = Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Chat", fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
