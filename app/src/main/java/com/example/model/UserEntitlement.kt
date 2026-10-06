package com.example.model

data class UserEntitlement(
    val userId: String = "",
    val plan: String = "FREE", // "FREE", "TRIAL", "PREMIUM", "PERMANENT", "CUSTOM_CHECK"
    val source: String = "DEFAULT", // "DEFAULT", "CODE_REDEEM", "ADMIN_GRANT", "PURCHASE", "SPIN_REWARD"
    val startedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = 0L, // 0 for permanent
    val permanent: Boolean = false,
    val customCheckEnabled: Boolean = false,
    val checkStyle: CheckStyle = CheckStyle(),
    val remainingSpins: Int = 0,
    val lastSpinTimestamp: Long = 0L,
    val status: String = "ACTIVE" // "ACTIVE", "EXPIRED", "REVOKED"
) {
    val isCurrentlyActive: Boolean
        get() {
            if (status.equals("REVOKED", ignoreCase = true)) return false
            if (permanent) return true
            if (plan.equals("FREE", ignoreCase = true)) return true
            return status.equals("ACTIVE", ignoreCase = true) &&
                    (expiresAt == 0L || expiresAt > System.currentTimeMillis())
        }

    val isPremiumTier: Boolean
        get() {
            if (!isCurrentlyActive) return false
            return plan.equals("TRIAL", ignoreCase = true) ||
                    plan.equals("PREMIUM", ignoreCase = true) ||
                    plan.equals("PERMANENT", ignoreCase = true) ||
                    plan.equals("CUSTOM_CHECK", ignoreCase = true)
        }

    val isDailySpinAvailable: Boolean
        get() {
            if (remainingSpins > 0) return true
            val now = System.currentTimeMillis()
            val cooldownMillis = 24L * 60L * 60L * 1000L
            return (now - lastSpinTimestamp) >= cooldownMillis
        }

    val totalAvailableSpins: Int
        get() {
            val daily = if ((System.currentTimeMillis() - lastSpinTimestamp) >= 24L * 60L * 60L * 1000L) 1 else 0
            return remainingSpins + daily
        }

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "userId" to userId,
            "plan" to plan.uppercase(),
            "source" to source,
            "startedAt" to startedAt,
            "expiresAt" to expiresAt,
            "permanent" to permanent,
            "customCheckEnabled" to customCheckEnabled,
            "checkStyle" to checkStyle.toMap(),
            "remainingSpins" to remainingSpins,
            "lastSpinTimestamp" to lastSpinTimestamp,
            "status" to status.uppercase()
        )
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(userId: String, map: Map<String, Any?>?): UserEntitlement {
            if (map == null) return UserEntitlement(userId = userId)
            val styleMap = map["checkStyle"] as? Map<String, Any?>
            return UserEntitlement(
                userId = userId,
                plan = (map["plan"] as? String)?.uppercase() ?: "FREE",
                source = map["source"] as? String ?: "DEFAULT",
                startedAt = (map["startedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                expiresAt = (map["expiresAt"] as? Number)?.toLong() ?: 0L,
                permanent = map["permanent"] as? Boolean ?: false,
                customCheckEnabled = map["customCheckEnabled"] as? Boolean ?: false,
                checkStyle = CheckStyle.fromMap(styleMap),
                remainingSpins = (map["remainingSpins"] as? Number)?.toInt() ?: 0,
                lastSpinTimestamp = (map["lastSpinTimestamp"] as? Number)?.toLong() ?: 0L,
                status = (map["status"] as? String)?.uppercase() ?: "ACTIVE"
            )
        }
    }
}
