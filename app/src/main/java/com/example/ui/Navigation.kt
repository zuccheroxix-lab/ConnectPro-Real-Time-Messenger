package com.example.ui

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val OTP = "otp/{verificationId}/{phoneNumber}"
    const val CREATE_PROFILE = "create_profile"
    const val HOME = "home"
    const val SEARCH = "search"
    const val USER_PROFILE = "user_profile/{userId}"
    const val CHAT = "chat/{conversationId}/{otherUserId}"
    const val PREMIUM = "premium"
    const val SUBSCRIPTION = "subscription"
    const val SETTINGS = "settings"
    const val BLOCKED_USERS = "blocked_users"
    const val NOTIFICATIONS = "notifications"
    const val ACCOUNT_SETTINGS = "account_settings"
    const val REDEEM_CODE = "redeem_code"
    const val SPINNER = "spinner"
    const val SPIN_HISTORY = "spin_history"
    const val CUSTOM_CHECK = "custom_check"
    const val ADMIN_PANEL = "admin_panel"

    fun otp(verificationId: String, phoneNumber: String): String =
        "otp/${java.net.URLEncoder.encode(verificationId, "UTF-8")}/${java.net.URLEncoder.encode(phoneNumber, "UTF-8")}"

    fun userProfile(userId: String): String =
        "user_profile/$userId"

    fun chat(conversationId: String, otherUserId: String): String =
        "chat/$conversationId/$otherUserId"
}
