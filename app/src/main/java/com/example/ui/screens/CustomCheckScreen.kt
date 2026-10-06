package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FirebaseRepository
import com.example.model.CheckStyle
import com.example.model.MessageStatus
import com.example.model.UserEntitlement
import com.example.ui.components.MessageTicks
import com.example.ui.theme.PulseCyanPrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomCheckScreen(
    onNavigateBack: () -> Unit,
    onChatAdmin: () -> Unit,
    onNavigateToRedeem: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { FirebaseRepository.getInstance() }
    val currentUserId = repository.currentUserId ?: ""
    val coroutineScope = rememberCoroutineScope()

    val entitlement by repository.observeUserEntitlement(currentUserId).collectAsState(initial = UserEntitlement())

    val presetColors = listOf(
        "#00D2FF" to "Cyan",
        "#2563EB" to "Blue",
        "#A855F7" to "Purple",
        "#EC4899" to "Pink",
        "#10B981" to "Emerald",
        "#F59E0B" to "Gold",
        "#FFFFFF" to "White"
    )

    val styles = listOf("CLASSIC", "GLOW", "SHIELD")

    var selectedColorHex by remember { mutableStateOf(entitlement.checkStyle.colorHex) }
    var selectedOpacity by remember { mutableFloatStateOf(entitlement.checkStyle.opacity) }
    var selectedSizeDp by remember { mutableIntStateOf(entitlement.checkStyle.sizeDp) }
    var selectedStyle by remember { mutableStateOf(entitlement.checkStyle.styleName) }
    var customHexInput by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(entitlement) {
        selectedColorHex = entitlement.checkStyle.colorHex
        selectedOpacity = entitlement.checkStyle.opacity
        selectedSizeDp = entitlement.checkStyle.sizeDp
        selectedStyle = entitlement.checkStyle.styleName
    }

    val previewStyle = remember(selectedColorHex, selectedOpacity, selectedSizeDp, selectedStyle) {
        CheckStyle(
            colorHex = selectedColorHex,
            opacity = selectedOpacity,
            sizeDp = selectedSizeDp,
            styleName = selectedStyle
        )
    }

    fun applyCustomCheck() {
        if (!entitlement.customCheckEnabled) {
            Toast.makeText(context, "Custom Check membutuhkan paket Custom.", Toast.LENGTH_SHORT).show()
            return
        }

        isSaving = true
        coroutineScope.launch {
            val result = repository.updateCustomCheckStyle(previewStyle)
            isSaving = false
            result.fold(
                onSuccess = {
                    Toast.makeText(context, "Centang berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                },
                onFailure = {
                    Toast.makeText(context, "Gagal menyimpan: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Custom Check", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("custom_check_back_button")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = Modifier
            .fillMaxSize()
            .testTag("custom_check_screen")
    ) { paddingValues ->
        if (!entitlement.customCheckEnabled) {
            // Locked screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Akses Terkunci",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Custom Check membutuhkan paket Custom.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Dapatkan paket Custom Check atau Permanent melalui Admin untuk menyesuaikan warna, style, ukuran, dan opacity centang pesan Anda.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick = onChatAdmin,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("chat_admin_locked_custom_check_button")
                ) {
                    Icon(Icons.Default.SupportAgent, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Chat Admin", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onNavigateToRedeem,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Redeem Kode Custom Check")
                }
            }
        } else {
            // Unlocked Configuration View
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Live Preview Bubble
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "PREVIEW CENTANG ANDA",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        // Simulated Chat Bubble with Custom Ticks
                        Surface(
                            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.widthIn(max = 280.dp)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                Text(
                                    text = "Halo! Ini adalah preview tampilan pesan Anda.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.align(Alignment.End),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "10:30",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    MessageTicks(
                                        status = MessageStatus.READ,
                                        checkStyle = previewStyle
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Color Selection
                Text(
                    text = "PILIH WARNA CENTANG",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    presetColors.forEach { (hex, name) ->
                        val isSelected = selectedColorHex.equals(hex, ignoreCase = true)
                        val color = Color(android.graphics.Color.parseColor(hex))
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                                    shape = CircleShape
                                )
                                .clickable { selectedColorHex = hex }
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = if (hex == "#FFFFFF") Color.Black else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Custom Hex input
                OutlinedTextField(
                    value = customHexInput,
                    onValueChange = {
                        customHexInput = it
                        if (it.matches(Regex("^#?[0-9a-fA-F]{6}$"))) {
                            selectedColorHex = if (it.startsWith("#")) it else "#$it"
                        }
                    },
                    label = { Text("Custom Color Hex (#RRGGBB)") },
                    placeholder = { Text("#00E5FF") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Style Selection
                Text(
                    text = "STYLE CENTANG",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    styles.forEach { style ->
                        val isSelected = selectedStyle == style
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedStyle = style },
                            label = { Text(style) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Size Slider
                Text(
                    text = "UKURAN CENTANG: ${selectedSizeDp}dp",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Slider(
                    value = selectedSizeDp.toFloat(),
                    onValueChange = { selectedSizeDp = it.toInt() },
                    valueRange = 12f..24f,
                    steps = 11
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Opacity Slider
                Text(
                    text = "OPACITY: ${(selectedOpacity * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Slider(
                    value = selectedOpacity,
                    onValueChange = { selectedOpacity = it },
                    valueRange = 0.4f..1.0f
                )

                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick = { applyCustomCheck() },
                    enabled = !isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("save_custom_check_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Simpan & Terapkan Centang", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}
