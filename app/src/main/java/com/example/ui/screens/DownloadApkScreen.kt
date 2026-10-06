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

    // Paths to verified built APK files
    val debugApkPath = "/app/applet/app/build/outputs/apk/debug/app-debug.apk"
    val debugApkAlt = "/app/applet/.build-outputs/app-debug.apk"
    val releaseApkPath = "/app/applet/app/build/outputs/apk/release/app-release.apk"
    val releaseApkAlt = "/app/applet/.build-outputs/app-release.apk"

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
        File(releaseApkPath).exists() || File(releaseApkAlt).exists()
    }
    val releaseFileSize = remember {
        val f1 = File(releaseApkPath)
        val f2 = File(releaseApkAlt)
        val bytes = if (f1.exists()) f1.length() else if (f2.exists()) f2.length() else 0L
        if (bytes > 0) String.format("%.1f MB", bytes / (1024.0 * 1024.0)) else "20 MB"
    }

    val githubRepoUrl = "https://github.com/zuccheroxix-lab/ConnectPro-React"
    val githubReleaseUrl = "https://github.com/zuccheroxix-lab/ConnectPro-React/releases/tag/v1.0.0"
    val debugDownloadUrl = "https://github.com/zuccheroxix-lab/ConnectPro-React/releases/download/v1.0.0/app-debug.apk"
    val releaseDownloadUrl = "https://github.com/zuccheroxix-lab/ConnectPro-React/releases/download/v1.0.0/app-release.apk"

    fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "URL: $url", Toast.LENGTH_LONG).show()
        }
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
                            text = "Repository: zuccheroxix-lab/ConnectPro-React",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
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
                            color = if (isDebugAvailable) SuccessGreen.copy(alpha = 0.15f) else WarningAmber.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (isDebugAvailable) "Tersedia ($debugFileSize)" else "APK belum tersedia",
                                color = if (isDebugAvailable) SuccessGreen else WarningAmber,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "File: app-debug.apk\nBuild variant: Debug\nStatus: Terkompilasi dan tertandatangani keystore internal. Siap di-install langsung.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isDebugAvailable) {
                        Button(
                            onClick = {
                                Toast.makeText(context, "Membuka link download: app-debug.apk ($debugFileSize)", Toast.LENGTH_SHORT).show()
                                openUrl(debugDownloadUrl)
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
                    } else {
                        OutlinedButton(
                            onClick = {},
                            enabled = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text("APK belum tersedia")
                        }
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
                                text = if (isReleaseAvailable) "Tersedia ($releaseFileSize)" else "APK belum tersedia",
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
                            "File: app-release.apk\nBuild variant: Release (Signed & Optimized R8)\nStatus: Terkompilasi dan tertandatangani keystore rilis. Siap di-install."
                        } else {
                            "File: app-release.apk\nStatus: Belum dibuild."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isReleaseAvailable) {
                        Button(
                            onClick = {
                                Toast.makeText(context, "Membuka link download: app-release.apk ($releaseFileSize)", Toast.LENGTH_SHORT).show()
                                openUrl(releaseDownloadUrl)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PulseCyanPrimary),
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
                            onClick = {},
                            enabled = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("download_release_apk_button")
                        ) {
                            Text("APK belum tersedia")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ==================== GITHUB RELEASE & ACTIONS CARD ====================
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
                            text = "GitHub Release & CI/CD",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Release v1.0.0 telah dikonfigurasi dengan workflow .github/workflows/build-apk.yml. Asset app-debug.apk dan app-release.apk dipublikasikan langsung ke halaman rilis repository.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { openUrl(githubReleaseUrl) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Buka GitHub Release v1.0.0")
                    }
                }
            }
        }
    }
}
