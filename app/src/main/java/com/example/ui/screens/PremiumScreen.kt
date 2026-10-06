package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import com.example.model.PackagePlan
import com.example.model.UserEntitlement
import com.example.ui.components.VerifiedBadge
import com.example.ui.theme.PulseCyanPrimary
import com.example.ui.theme.PulseVerifiedBlue
import com.example.ui.theme.SuccessGreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSubscription: () -> Unit,
    onNavigateToRedeem: () -> Unit,
    onNavigateToSpinner: () -> Unit,
    onNavigateToCustomCheck: () -> Unit,
    onChatAdminWithPackage: (packageId: String, packageName: String) -> Unit
) {
    val context = LocalContext.current
    val repository = remember { FirebaseRepository.getInstance() }
    val currentUserId = repository.currentUserId ?: ""
    val coroutineScope = rememberCoroutineScope()

    val entitlement by repository.observeUserEntitlement(currentUserId).collectAsState(initial = UserEntitlement())
    val backendPackages by repository.observePackages().collectAsState(initial = emptyList())

    var selectedPackageId by remember { mutableStateOf("") }
    var showPaymentInstructionsDialog by remember { mutableStateOf<PackagePlan?>(null) }
    var orderReferenceInput by remember { mutableStateOf("") }
    var isSubmittingOrder by remember { mutableStateOf(false) }

    LaunchedEffect(backendPackages) {
        if (selectedPackageId.isBlank() && backendPackages.isNotEmpty()) {
            selectedPackageId = backendPackages.first().id
        }
    }

    if (showPaymentInstructionsDialog != null) {
        val pkg = showPaymentInstructionsDialog!!
        AlertDialog(
            onDismissRequest = { showPaymentInstructionsDialog = null },
            icon = {
                Icon(Icons.Default.SupportAgent, contentDescription = null, tint = PulseCyanPrimary, modifier = Modifier.size(36.dp))
            },
            title = {
                Text("Instruksi Pembayaran Manual", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "Paket Dipilih: ${pkg.name}\nHarga: ${pkg.price}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Alur Pembayaran:\n" +
                                "1. Hubungi Admin via Chat atau transfer ke rekening resmi Admin.\n" +
                                "2. Admin akan memverifikasi mutasi pembayaran Anda di sistem.\n" +
                                "3. Setelah terverifikasi, Admin membuat kode voucher resmi.\n" +
                                "4. Anda menerima kode voucher dan me-redeem di menu 'Redeem Code'.",
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = orderReferenceInput,
                        onValueChange = { orderReferenceInput = it },
                        label = { Text("Catatan / Rekening Pengirim") },
                        placeholder = { Text("BCA an Budi / OVO 0812...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val ref = orderReferenceInput.trim()
                        isSubmittingOrder = true
                        coroutineScope.launch {
                            repository.createPaymentOrder(
                                packageId = pkg.id,
                                packageName = pkg.name,
                                amount = pkg.price,
                                note = ref
                            )
                            isSubmittingOrder = false
                            showPaymentInstructionsDialog = null
                            orderReferenceInput = ""
                            Toast.makeText(context, "Permintaan dikirim ke Admin. Silakan lanjutkan chat dengan Admin.", Toast.LENGTH_LONG).show()
                            onChatAdminWithPackage(pkg.id, pkg.name)
                        }
                    },
                    enabled = !isSubmittingOrder
                ) {
                    Text("Kirim & Chat Admin")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPaymentInstructionsDialog = null }) {
                    Text("Batal")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pulse Premium", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("premium_back_button")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (entitlement.isPremiumTier) {
                        TextButton(
                            onClick = onNavigateToSubscription,
                            modifier = Modifier.testTag("view_subscription_button")
                        ) {
                            Text(entitlement.plan, fontWeight = FontWeight.Bold, color = PulseVerifiedBlue)
                        }
                    }
                }
            )
        },
        modifier = Modifier
            .fillMaxSize()
            .testTag("premium_screen")
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Banner
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
            ) {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Tingkatkan Pengalaman Chat Anda",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Harga paket dan durasi di bawah diambil langsung secara real-time dari backend database.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Navigation Shortcuts (Redeem, Spinner, Custom Check)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateToRedeem,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(46.dp)
                ) {
                    Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Redeem Code", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onNavigateToSpinner,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(46.dp)
                ) {
                    Icon(Icons.Default.Casino, contentDescription = null, tint = PulseCyanPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Spinner", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onNavigateToCustomCheck,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(46.dp)
                ) {
                    Icon(Icons.Default.DoneAll, contentDescription = null, tint = PulseVerifiedBlue, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Centang", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Dynamic Backend Packages
            backendPackages.forEach { pkg ->
                val isSelected = selectedPackageId == pkg.id
                BackendPackageCard(
                    pkg = pkg,
                    isSelected = isSelected,
                    onClick = { selectedPackageId = pkg.id }
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // "Chat Admin" action button
            Button(
                onClick = {
                    val pkg = backendPackages.firstOrNull { it.id == selectedPackageId }
                    if (pkg != null) {
                        showPaymentInstructionsDialog = pkg
                    } else {
                        Toast.makeText(context, "Silakan pilih paket terlebih dahulu.", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("chat_admin_purchase_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.SupportAgent, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Chat Admin untuk Beli Paket",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Pembayaran manual diverifikasi langsung oleh Admin. Status Premium dan hak akses hanya akan aktif setelah kode voucher resmi diredeem.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
fun BackendPackageCard(
    pkg: PackagePlan,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp)
            )
            .testTag("package_card_${pkg.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = pkg.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = pkg.price,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                RadioButton(selected = isSelected, onClick = onClick)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Durasi: ${if (pkg.durationDays == 0) "Permanent (Seumur Hidup)" else "${pkg.durationDays} Hari"} • ${pkg.spinLimit}x Spin",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(10.dp))

            pkg.features.forEach { feature ->
                Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = PulseCyanPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = feature,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
