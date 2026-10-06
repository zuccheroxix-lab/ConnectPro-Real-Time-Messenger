package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FirebaseRepository
import com.example.model.SpinReward
import com.example.model.UserEntitlement
import com.example.ui.theme.PulseCyanPrimary
import com.example.ui.theme.PulseVerifiedBlue
import com.example.ui.theme.SuccessGreen
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpinnerScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToCustomCheck: () -> Unit,
    onNavigateToRedeem: () -> Unit
) {
    val repository = remember { FirebaseRepository.getInstance() }
    val currentUserId = repository.currentUserId ?: ""
    val coroutineScope = rememberCoroutineScope()

    val entitlement by repository.observeUserEntitlement(currentUserId).collectAsState(initial = UserEntitlement())
    val activeRewards by repository.observeSpinRewards().collectAsState(initial = emptyList())

    var isSpinning by remember { mutableStateOf(false) }
    var rotationAngle by remember { mutableFloatStateOf(0f) }
    var rewardResult by remember { mutableStateOf<SpinReward?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val animatedRotation = remember { Animatable(0f) }

    fun spinWheel() {
        if (entitlement.totalAvailableSpins <= 0) {
            errorMessage = "Batas spin harian sudah habis. Silakan coba lagi besok atau redeem kode baru untuk menambah spin."
            return
        }
        if (isSpinning) return

        errorMessage = null
        rewardResult = null
        isSpinning = true

        coroutineScope.launch {
            val result = repository.performSpin()
            result.fold(
                onSuccess = { reward ->
                    // Calculate target wheel rotation (multiple full spins + index offset)
                    val rewardsCount = activeRewards.size.coerceAtLeast(1)
                    val targetIndex = activeRewards.indexOfFirst { it.name == reward.name }.let { if (it >= 0) it else 0 }
                    val sliceAngle = 360f / rewardsCount
                    val targetAngle = (360f * 5) + (targetIndex * sliceAngle) + (sliceAngle / 2f)

                    animatedRotation.snapTo(0f)
                    animatedRotation.animateTo(
                        targetValue = targetAngle,
                        animationSpec = tween(
                            durationMillis = 3200,
                            easing = FastOutSlowInEasing
                        )
                    )

                    rotationAngle = targetAngle % 360f
                    rewardResult = reward
                    isSpinning = false
                },
                onFailure = { err ->
                    isSpinning = false
                    errorMessage = err.message ?: "Gagal memutar spinner."
                }
            )
        }
    }

    if (rewardResult != null) {
        val reward = rewardResult!!
        AlertDialog(
            onDismissRequest = { rewardResult = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = PulseCyanPrimary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Selamat! Anda Mendapatkan:",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = reward.name,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Hadiah telah langsung diaplikasikan ke akun Anda secara server-side.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                if (reward.type == "CUSTOM_CHECK" || reward.type == "CHECK_STYLE") {
                    Button(
                        onClick = {
                            rewardResult = null
                            onNavigateToCustomCheck()
                        }
                    ) {
                        Text("Buka Custom Check")
                    }
                } else {
                    Button(onClick = { rewardResult = null }) {
                        Text("Selesai")
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Premium Spinner", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("spinner_back_button")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToHistory,
                        modifier = Modifier.testTag("spinner_history_button")
                    ) {
                        Icon(imageVector = Icons.Default.History, contentDescription = "Spin History")
                    }
                }
            )
        },
        modifier = Modifier
            .fillMaxSize()
            .testTag("spinner_screen")
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Stats Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = PulseCyanPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Paket: ${entitlement.plan}",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                Surface(
                    color = if (entitlement.totalAvailableSpins > 0) SuccessGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    border = if (entitlement.totalAvailableSpins > 0) androidx.compose.foundation.BorderStroke(1.dp, SuccessGreen) else null
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Casino,
                            contentDescription = null,
                            tint = if (entitlement.totalAvailableSpins > 0) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${entitlement.totalAvailableSpins} Spin Tersedia",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (entitlement.totalAvailableSpins > 0) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Wheel Container with Top Pointer
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(280.dp)
            ) {
                // Wheel Canvas
                val slices = remember(activeRewards) {
                    if (activeRewards.isNotEmpty()) activeRewards else listOf(
                        SpinReward(name = "Neon Cyan", colorHex = "#00D2FF"),
                        SpinReward(name = "Violet Glow", colorHex = "#A855F7"),
                        SpinReward(name = "Extra 7D", colorHex = "#10B981"),
                        SpinReward(name = "Custom Check", colorHex = "#EC4899"),
                        SpinReward(name = "+1 Spin", colorHex = "#F59E0B"),
                        SpinReward(name = "No Reward", colorHex = "#64748B")
                    )
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(animatedRotation.value)
                        .clip(CircleShape)
                ) {
                    val count = slices.size
                    val sweepAngle = 360f / count
                    val radius = size.minDimension / 2f
                    val center = Offset(size.width / 2f, size.height / 2f)

                    slices.forEachIndexed { i, slice ->
                        val color = try {
                            Color(android.graphics.Color.parseColor(slice.colorHex))
                        } catch (e: Exception) {
                            if (i % 2 == 0) Color(0xFF00D2FF) else Color(0xFF6366F1)
                        }

                        drawArc(
                            color = color,
                            startAngle = i * sweepAngle,
                            sweepAngle = sweepAngle,
                            useCenter = true,
                            size = Size(radius * 2f, radius * 2f),
                            topLeft = Offset(center.x - radius, center.y - radius),
                            style = Fill
                        )
                    }

                    // Outer Border
                    drawCircle(
                        color = Color.White.copy(alpha = 0.4f),
                        radius = radius,
                        center = center,
                        style = Stroke(width = 6f)
                    )
                }

                // Center Pin Hub
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    border = androidx.compose.foundation.BorderStroke(2.dp, PulseCyanPrimary),
                    modifier = Modifier.size(54.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Stars,
                            contentDescription = null,
                            tint = PulseCyanPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Pointer Arrow Indicator at the Top
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-14).dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Pointer",
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("spinner_error_text")
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            Button(
                onClick = { spinWheel() },
                enabled = !isSpinning && entitlement.totalAvailableSpins > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("spin_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (isSpinning) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (entitlement.totalAvailableSpins > 0) "PUTAR SPINNER" else "SPIN HABIS",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (entitlement.totalAvailableSpins <= 0) {
                OutlinedButton(
                    onClick = onNavigateToRedeem,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Redeem Voucher untuk Tambah Spin")
                }
            }
        }
    }
}
