package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FirebaseRepository
import com.example.model.Message
import com.example.model.MessageStatus
import com.example.ui.components.AvatarView
import com.example.ui.components.MessageTicks
import com.example.ui.components.VerifiedBadge
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: String,
    otherUserId: String,
    onNavigateBack: () -> Unit,
    onNavigateToProfile: (userId: String) -> Unit
) {
    val context = LocalContext.current
    val repository = remember { FirebaseRepository.getInstance() }
    val currentUserId = repository.currentUserId ?: ""
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val otherUser by repository.observeUser(otherUserId).collectAsState(initial = null)
    val myEntitlement by repository.observeUserEntitlement(currentUserId).collectAsState(initial = com.example.model.UserEntitlement())
    val messages by repository.observeMessages(conversationId).collectAsState(initial = emptyList())

    var messageInput by remember { mutableStateOf("") }
    var replyingToMessage by remember { mutableStateOf<Message?>(null) }
    var selectedMessageForAction by remember { mutableStateOf<Message?>(null) }
    var showActionDialog by remember { mutableStateOf(false) }
    var isSending by remember { mutableStateOf(false) }

    // Real-time receipts: When receiver views messages, mark delivered & read in Firestore
    LaunchedEffect(messages) {
        if (messages.isNotEmpty()) {
            repository.markConversationAsRead(conversationId, messages)
            // Auto-scroll to bottom
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    fun sendMessage() {
        val text = messageInput.trim()
        if (text.isBlank() || isSending) return

        isSending = true
        val reply = replyingToMessage
        messageInput = ""
        replyingToMessage = null

        coroutineScope.launch {
            repository.sendMessage(
                conversationId = conversationId,
                receiverId = otherUserId,
                text = text,
                replyTo = reply
            )
            isSending = false
        }
    }

    fun copyMessage(msg: Message) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Chat Message", msg.text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Message copied", Toast.LENGTH_SHORT).show()
    }

    fun deleteMessage(msg: Message) {
        coroutineScope.launch {
            val result = repository.deleteMessage(conversationId, msg.id)
            result.fold(
                onSuccess = {
                    Toast.makeText(context, "Message deleted", Toast.LENGTH_SHORT).show()
                },
                onFailure = {
                    Toast.makeText(context, "Could not delete: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    if (showActionDialog && selectedMessageForAction != null) {
        val msg = selectedMessageForAction!!
        val isOwn = msg.senderId == currentUserId

        AlertDialog(
            onDismissRequest = {
                showActionDialog = false
                selectedMessageForAction = null
            },
            title = { Text("Message Options") },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text("Copy Text") },
                        leadingContent = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                        modifier = Modifier.clickable {
                            copyMessage(msg)
                            showActionDialog = false
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Reply") },
                        leadingContent = { Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null) },
                        modifier = Modifier.clickable {
                            replyingToMessage = msg
                            showActionDialog = false
                        }
                    )
                    if (isOwn && !msg.isDeleted) {
                        ListItem(
                            headlineContent = { Text("Delete for Everyone", color = ErrorRed) },
                            leadingContent = { Icon(Icons.Default.Delete, contentDescription = null, tint = ErrorRed) },
                            modifier = Modifier.clickable {
                                deleteMessage(msg)
                                showActionDialog = false
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showActionDialog = false
                    selectedMessageForAction = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onNavigateToProfile(otherUserId) }
                    ) {
                        AvatarView(
                            photoUrl = otherUser?.photoUrl?.ifBlank { null },
                            name = otherUser?.displayName ?: "User",
                            size = 40.dp,
                            isOnline = otherUser?.isOnline == true,
                            showOnlineIndicator = true,
                            isPremium = otherUser?.isPremium == true
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = otherUser?.displayName?.ifBlank { "Pulse User" } ?: "Pulse User",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (otherUser?.isPremium == true) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    VerifiedBadge()
                                }
                            }
                            Text(
                                text = if (otherUser?.isOnline == true) "Online" else "@${otherUser?.username ?: ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (otherUser?.isOnline == true) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("chat_back_button")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onNavigateToProfile(otherUserId) },
                        modifier = Modifier.testTag("chat_view_profile_action")
                    ) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = "View Profile")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = Modifier
            .fillMaxSize()
            .testTag("chat_screen")
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .testTag("messages_list"),
                verticalArrangement = Arrangement.Bottom
            ) {
                items(messages, key = { it.id }) { message ->
                    val isMine = message.senderId == currentUserId

                    MessageBubbleItem(
                        message = message,
                        isMine = isMine,
                        checkStyle = if (myEntitlement.customCheckEnabled) myEntitlement.checkStyle else null,
                        onLongClick = {
                            selectedMessageForAction = message
                            showActionDialog = true
                        }
                    )
                }
            }

            // Reply Preview Bar
            if (replyingToMessage != null) {
                val reply = replyingToMessage!!
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(36.dp)
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (reply.senderId == currentUserId) "Replying to yourself" else "Replying to ${otherUser?.displayName ?: "User"}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = reply.text,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = { replyingToMessage = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel reply", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Input Bar
            Surface(
                tonalElevation = 2.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageInput,
                        onValueChange = { messageInput = it },
                        placeholder = { Text("Write a message...") },
                        maxLines = 4,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_message_input"),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { sendMessage() },
                        enabled = messageInput.isNotBlank() && !isSending,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (messageInput.isNotBlank()) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .testTag("chat_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send Message",
                            tint = if (messageInput.isNotBlank()) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubbleItem(
    message: Message,
    isMine: Boolean,
    checkStyle: com.example.model.CheckStyle? = null,
    onLongClick: () -> Unit
) {
    val bubbleShape = if (isMine) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp)
    }

    val bubbleBg = if (isMine) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = if (isMine) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    fun formatTime(millis: Long): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = bubbleShape,
            color = bubbleBg,
            modifier = Modifier
                .widthIn(max = 280.dp)
                .combinedClickable(
                    onClick = {},
                    onLongClick = onLongClick
                )
                .testTag("message_bubble_${message.id}")
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                // Reply Quote Preview
                if (!message.replyToText.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = message.replyToText,
                            style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (message.isDeleted) {
                    Text(
                        text = "This message was deleted",
                        style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                        color = textColor.copy(alpha = 0.6f)
                    )
                } else {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = textColor
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTime(message.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor.copy(alpha = 0.65f),
                        fontSize = 10.sp
                    )

                    if (isMine && !message.isDeleted) {
                        Spacer(modifier = Modifier.width(4.dp))
                        MessageTicks(
                            status = message.effectiveStatus,
                            checkStyle = checkStyle
                        )
                    }
                }
            }
        }
    }
}
