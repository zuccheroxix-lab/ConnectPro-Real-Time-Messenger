package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.example.data.FirebaseRepository
import com.example.model.*
import com.example.ui.components.EmptyState
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.PulseCyanPrimary
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { FirebaseRepository.getInstance() }
    val currentUserId = repository.currentUserId ?: ""
    val coroutineScope = rememberCoroutineScope()

    var isAdminChecked by remember { mutableStateOf(false) }
    var isUserAdmin by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    val tabTitles = listOf("Dashboard", "Buat Kode", "Daftar Kode", "Users", "Paket", "Spinner", "Audit")

    // Real-time admin state collections
    val allCodes by repository.adminObserveAllCodes().collectAsState(initial = emptyList())
    val allUsers by repository.adminObserveAllUsers().collectAsState(initial = emptyList())
    val allPackages by repository.observePackages().collectAsState(initial = emptyList())
    val allRewards by repository.observeSpinRewards().collectAsState(initial = emptyList())
    val allAuditLogs by repository.adminObserveAuditLogs().collectAsState(initial = emptyList())
    val paymentOrders by repository.adminObservePaymentOrders().collectAsState(initial = emptyList())

    LaunchedEffect(currentUserId) {
        isUserAdmin = repository.checkIsAdmin()
        isAdminChecked = true
    }

    fun formatDate(millis: Long): String {
        if (millis <= 0L) return "N/A"
        return SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault()).format(Date(millis))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pulse Admin Panel", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("admin_back_button")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_panel_screen")
    ) { paddingValues ->
        if (!isAdminChecked) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (!isUserAdmin) {
            // Access Denied Screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = ErrorRed,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Akses Ditolak",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = ErrorRed
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Akun Anda tidak memiliki role ADMIN di database. Akses Admin Panel dilindungi secara ketat di server-side.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onNavigateBack) {
                    Text("Kembali")
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 16.dp,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }

                when (selectedTab) {
                    0 -> AdminDashboardTab(allUsers, allCodes, paymentOrders)
                    1 -> AdminGenerateCodeTab(onCodeGenerated = { selectedTab = 2 })
                    2 -> AdminManageCodesTab(allCodes)
                    3 -> AdminManageUsersTab(allUsers)
                    4 -> AdminManagePackagesTab(allPackages)
                    5 -> AdminManageSpinnerTab(allRewards)
                    6 -> AdminAuditLogsTab(allAuditLogs)
                }
            }
        }
    }
}

@Composable
fun AdminDashboardTab(
    users: List<User>,
    codes: List<PremiumCode>,
    payments: List<PaymentOrder>
) {
    val activePremiumCount = users.count { it.isPremium }
    val availableCodesCount = codes.count { it.status == "AVAILABLE" }
    val usedCodesCount = codes.count { it.status == "USED" }
    val pendingPayments = payments.count { it.status == "PENDING" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Ringkasan Metrik Sistem", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(14.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(title = "Total Users", value = users.size.toString(), color = PulseCyanPrimary, modifier = Modifier.weight(1f))
            MetricCard(title = "Active Premium", value = activePremiumCount.toString(), color = SuccessGreen, modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(title = "Kode Tersedia", value = availableCodesCount.toString(), color = WarningAmber, modifier = Modifier.weight(1f))
            MetricCard(title = "Kode Terpakai", value = usedCodesCount.toString(), color = Color(0xFFA855F7), modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(12.dp))

        MetricCard(title = "Pembayaran Menunggu Verifikasi", value = pendingPayments.toString(), color = PulseCyanPrimary, modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(24.dp))

        Text("Status Database", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("• Firestore Collection: users, premium_codes, user_entitlements, packages, spin_rewards, audit_logs", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(4.dp))
                Text("• Enforced Server-Side Rules: firestore.rules", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(4.dp))
                Text("• Anti Double-Redeem: Atomic runTransaction", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun MetricCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold), color = color)
        }
    }
}

@Composable
fun AdminGenerateCodeTab(onCodeGenerated: () -> Unit) {
    val repository = remember { FirebaseRepository.getInstance() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val packageTypes = listOf("PREMIUM", "TRIAL", "PERMANENT", "CUSTOM_CHECK")
    var selectedType by remember { mutableStateOf("PREMIUM") }
    var durationDaysInput by remember { mutableStateOf("30") }
    var quantityInput by remember { mutableStateOf("1") }
    var referenceNote by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Generate Premium Voucher Codes", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(16.dp))

        Text("PILIH JENIS PAKET", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            packageTypes.forEach { type ->
                FilterChip(
                    selected = selectedType == type,
                    onClick = {
                        selectedType = type
                        if (type == "TRIAL") durationDaysInput = "7"
                        else if (type == "PERMANENT") durationDaysInput = "0"
                        else durationDaysInput = "30"
                    },
                    label = { Text(type, fontSize = 11.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = durationDaysInput,
            onValueChange = { durationDaysInput = it },
            label = { Text("Durasi (Hari, 0 untuk Permanent)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = quantityInput,
            onValueChange = { quantityInput = it },
            label = { Text("Jumlah Kode Dibuat (1 - 50)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = referenceNote,
            onValueChange = { referenceNote = it },
            label = { Text("Catatan Referensi / Pembeli (Opsional)") },
            placeholder = { Text("Order #102 - Bpk Joko") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val qty = quantityInput.toIntOrNull() ?: 1
                val days = durationDaysInput.toIntOrNull() ?: 30
                isGenerating = true
                coroutineScope.launch {
                    val result = repository.adminGenerateCodes(
                        packageType = selectedType,
                        durationDays = days,
                        quantity = qty,
                        referenceId = referenceNote
                    )
                    isGenerating = false
                    result.fold(
                        onSuccess = { list ->
                            Toast.makeText(context, "${list.size} kode berhasil dibuat!", Toast.LENGTH_SHORT).show()
                            onCodeGenerated()
                        },
                        onFailure = { err ->
                            Toast.makeText(context, "Gagal membuat kode: ${err.message}", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            },
            enabled = !isGenerating,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isGenerating) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generate Codes", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AdminManageCodesTab(codes: List<PremiumCode>) {
    val repository = remember { FirebaseRepository.getInstance() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }

    val filteredCodes = remember(codes, searchQuery) {
        if (searchQuery.isBlank()) codes
        else codes.filter { it.code.contains(searchQuery.trim().uppercase()) || it.referenceId.contains(searchQuery, ignoreCase = true) }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Cari kode voucher...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredCodes.isEmpty()) {
            EmptyState(icon = Icons.Default.Key, title = "Tidak Ada Kode", subtitle = "Belum ada kode premium yang cocok dengan pencarian.")
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filteredCodes, key = { it.codeId }) { code ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(code.code, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold), color = MaterialTheme.colorScheme.primary)
                                Surface(
                                    color = when (code.status) {
                                        "AVAILABLE" -> SuccessGreen.copy(alpha = 0.2f)
                                        "USED" -> Color(0xFFA855F7).copy(alpha = 0.2f)
                                        else -> ErrorRed.copy(alpha = 0.2f)
                                    },
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(code.status, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Paket: ${code.packageType} (${if (code.durationDays == 0) "Permanent" else "${code.durationDays} hari"})", style = MaterialTheme.typography.bodySmall)
                            if (code.usedBy.isNotBlank()) {
                                Text("Digunakan oleh: ${code.usedBy.take(12)}...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (code.referenceId.isNotBlank()) {
                                Text("Ref: ${code.referenceId}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            if (code.status == "AVAILABLE") {
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            repository.adminRevokeCode(code.codeId)
                                            Toast.makeText(context, "Kode dibatalkan", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Revoke / Batalkan Kode", color = ErrorRed, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminManageUsersTab(users: List<User>) {
    val repository = remember { FirebaseRepository.getInstance() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }

    val filtered = remember(users, searchQuery) {
        if (searchQuery.isBlank()) users
        else users.filter { it.username.contains(searchQuery.lowercase()) || it.displayName.contains(searchQuery, ignoreCase = true) }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Cari user by username...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(filtered, key = { it.id }) { user ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(user.displayName.ifBlank { "User" }, fontWeight = FontWeight.Bold)
                                if (user.isPremium) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.Verified, contentDescription = null, tint = PulseCyanPrimary, modifier = Modifier.size(16.dp))
                                }
                            }
                            Text("@${user.username} • Role: ${user.role}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("UID: ${user.id.take(12)}...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        if (user.isPremium) {
                            TextButton(
                                onClick = {
                                    coroutineScope.launch {
                                        repository.adminRevokeUserEntitlement(user.id)
                                        Toast.makeText(context, "Entitlement user dicabut", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Text("Revoke", color = ErrorRed)
                            }
                        } else {
                            TextButton(
                                onClick = {
                                    coroutineScope.launch {
                                        repository.adminGrantUserEntitlement(user.id, "PREMIUM", 30, true)
                                        Toast.makeText(context, "Akses premium diberikan", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Text("Grant Pro", color = PulseCyanPrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminManagePackagesTab(packages: List<PackagePlan>) {
    val repository = remember { FirebaseRepository.getInstance() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Daftar Paket & Harga Backend", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(packages, key = { it.id }) { pkg ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(pkg.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Text(pkg.price, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Text("Tipe: ${pkg.type} • Durasi: ${if (pkg.durationDays == 0) "Permanent" else "${pkg.durationDays} hari"}", style = MaterialTheme.typography.bodySmall)
                        Text("Batas Spin: ${pkg.spinLimit} • Custom Check: ${if (pkg.customCheck) "Ya" else "Tidak"}", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Fitur: ${pkg.features.joinToString(", ")}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun AdminManageSpinnerTab(rewards: List<SpinReward>) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Konfigurasi Reward Spinner", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(rewards, key = { it.id }) { r ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(try { Color(android.graphics.Color.parseColor(r.colorHex)) } catch (e: Exception) { PulseCyanPrimary })
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(r.name, fontWeight = FontWeight.Bold)
                            Text("Tipe: ${r.type} • Bobot: ${(r.probability * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminAuditLogsTab(logs: List<AuditLog>) {
    fun formatDate(millis: Long): String = SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault()).format(Date(millis))

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Real-Time Security & Financial Audit Logs", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(12.dp))

        if (logs.isEmpty()) {
            EmptyState(icon = Icons.Default.History, title = "Belum Ada Log", subtitle = "Aktivitas administratif akan tercatat di sini.")
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(logs, key = { it.id }) { log ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = PulseCyanPrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(log.action, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    Text(formatDate(log.timestamp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("Actor: ${log.actorId.take(10)}... (${log.actorRole})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (log.metadata.isNotEmpty()) {
                                    Text("Metadata: ${log.metadata}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
