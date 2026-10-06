package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import com.google.firebase.FirebaseApp

class PulseApplication : Application() {

    companion object {
        const val CHANNEL_MESSAGES_ID = "pulse_messages"
        const val CHANNEL_MESSAGES_NAME = "Chat Messages"
        private const val TAG = "PulseApp"

        lateinit var instance: PulseApplication
            private set

        fun isFirebaseReady(): Boolean {
            return try {
                FirebaseApp.getApps(instance).isNotEmpty()
            } catch (e: Exception) {
                false
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        initNotificationChannels()
        checkFirebaseConfig()
    }

    private fun checkFirebaseConfig() {
        try {
            val apps = FirebaseApp.getApps(this)
            if (apps.isEmpty()) {
                Log.w(TAG, "Firebase is not yet initialized with google-services.json.")
            } else {
                Log.i(TAG, "Firebase initialized successfully: ${apps.first().name}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase initialization check: ${e.message}")
        }
    }

    private fun initNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_MESSAGES_ID,
                CHANNEL_MESSAGES_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Realtime notifications for incoming chat messages"
                enableVibration(true)
                setShowBadge(true)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
