package com.example.model

data class Subscription(
    val id: String = "",
    val userId: String = "",
    val plan: String = "free", // "free", "monthly_pro", "annual_pro"
    val status: String = "inactive", // "active", "expired", "cancelled", "inactive"
    val provider: String = "google_play_billing", // "google_play_billing", "stripe", "midtrans"
    val transactionId: String = "",
    val startedAt: Long = 0L,
    val expiresAt: Long = 0L
) {
    val isCurrentlyActive: Boolean
        get() = status.equals("active", ignoreCase = true) && expiresAt > System.currentTimeMillis()

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "userId" to userId,
            "plan" to plan,
            "status" to status,
            "provider" to provider,
            "transactionId" to transactionId,
            "startedAt" to startedAt,
            "expiresAt" to expiresAt
        )
    }

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>?): Subscription {
            if (map == null) return Subscription(id = id)
            return Subscription(
                id = id,
                userId = map["userId"] as? String ?: "",
                plan = map["plan"] as? String ?: "free",
                status = map["status"] as? String ?: "inactive",
                provider = map["provider"] as? String ?: "google_play_billing",
                transactionId = map["transactionId"] as? String ?: "",
                startedAt = (map["startedAt"] as? Number)?.toLong() ?: 0L,
                expiresAt = (map["expiresAt"] as? Number)?.toLong() ?: 0L
            )
        }
    }
}
