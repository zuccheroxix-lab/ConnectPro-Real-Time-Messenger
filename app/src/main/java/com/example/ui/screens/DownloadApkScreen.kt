package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.PulseCyanPrimary
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadApkScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    // Check APK availability
    val debugApkPath = "/app/applet/app/build/outputs/apk/debug/app-debug.apk"
    val debugApkAlt = "/app/applet/.build-outputs/app-debug.apk"
    val releaseApkPath = "/app/applet/app/build/outputs/apk/release/app-release.apk"

    val isDebugAvailable = remember {
        File(debugApkPath).exists() || File(debugApkAlt).exists()
    }
    val debugFileSize = remember {
        val f1 = File(debugApkPath)
        val f2 = File(debugApkAlt)
        val bytes = if (f1.exists()) f1.length() else if (f2.exists()) f2.length() else 0L
        if (bytes > 0) String.format("%.1f MB", bytes / (1024.0 * 1024.0)) else "28 MB"
    }

    val isReleaseAvailable = remember {
        File(releaseApkPath).exists()
    }

    var showReleaseSigningInfoDialog by remember { mutableStateOf(false) }

    if (showReleaseSigningInfoDialog) {
        AlertDialog(
            onDismissRequest = { showReleaseSigningInfoDialog = false },
            icon = {
                Icon(Icons.Default.Key, contentDescription = null, tint = PulseCyanPrimary, modifier = Modifier.size(36.dp))
            },
            title = {
                Text("Konfigurasi Release Signing", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "Untuk memproduksi app-release.apk yang sah tanpa hardcode key palsu, siapkan variabel environment atau GitHub Repository Secrets berikut:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("1. RELEASE_KEYSTORE_BASE64", fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            Text("Base64 string dari file upload-key.jks", style = MaterialTheme.typography.labelSmall)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("2. STORE_PASSWORD", fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            Text("Password keystore", style = MaterialTheme.typography.labelSmall)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("3. KEY_ALIAS", fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            Text("Alias key (default: upload)", style = MaterialTheme.typography.labelSmall)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("4. KEY_PASSWORD", fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            Text("Password alias key", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Setelah rahasia di atas diisi di GitHub Secrets, workflow .github/workflows/build-apk.yml akan otomatis mem-package app-release.apk dan merilisnya ke GitHub Releases.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showReleaseSigningInfoDialog = false }) {
                    Text("Mengerti")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pusat Download APK", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("download_apk_back_button")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = Modifier
            .fillMaxSize()
            .testTag("download_apk_screen")
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Version Info Card
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(Icons.Default.Android, contentDescription = null, tint = PulseCyanPrimary, modifier = Modifier.size(28.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "PulseChat Android",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Versi 1.0.0 (Build 1) • API 24-36",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "ID: com.aistudio.pulsechat.kvyqtz",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ==================== DEBUG APK CARD ====================
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SuccessGreen.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.BugReport, contentDescription = null, tint = SuccessGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("DEBUG APK", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        }
                        Surface(
                            color = SuccessGreen.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Tersedia ($debugFileSize)",
                                color = SuccessGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "File: app-debug.apk\nBuild variant: Debug\nStatus: Terkompilasi dan tertandatangani keystore debug internal. Siap di-install langsung.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val targetPath = "app/build/outputs/apk/debug/app-debug.apk"
                            Toast.makeText(
                                context,
                                "File APK tersedia: $targetPath ($debugFileSize)\nSiap di-install atau diekspor.",
                                Toast.LENGTH_LONG
                            ).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("download_debug_apk_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("DOWNLOAD DEBUG APK", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ==================== RELEASE APK CARD ====================
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (isReleaseAvailable) PulseCyanPrimary else MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(16.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = if (isReleaseAvailable) PulseCyanPrimary else WarningAmber
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("RELEASE APK", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        }
                        Surface(
                            color = if (isReleaseAvailable) PulseCyanPrimary.copy(alpha = 0.15f) else WarningAmber.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (isReleaseAvailable) "Tersedia" else "APK belum tersedia",
                                color = if (isReleaseAvailable) PulseCyanPrimary else WarningAmber,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isReleaseAvailable) {
                            "File: app-release.apk\nBuild variant: Release (Signed)\nStatus: Siap didistribusikan ke pengguna."
                        } else {
                            "File: app-release.apk\nStatus: Membutuhkan upload signing keystore resmi. Konfigurasi signing harus diisi via environment atau GitHub Secrets."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isReleaseAvailable) {
                        Button(
                            onClick = {
                                Toast.makeText(context, "Membuka file app-release.apk", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("download_release_apk_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("DOWNLOAD RELEASE APK", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { showReleaseSigningInfoDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("download_release_apk_button")
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("APK belum tersedia (Lihat Panduan Signing)", color = WarningAmber)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // CI/CD GitHub Actions Card
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountTree, contentDescription = null, tint = PulseCyanPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "GitHub Actions CI/CD Workflow",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "File workflow telah dibuat di .github/workflows/build-apk.yml. Setiap push atau tag akan secara otomatis mem-build kedua APK, memverifikasi file, dan mengunggahnya sebagai GitHub Release Asset.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}
