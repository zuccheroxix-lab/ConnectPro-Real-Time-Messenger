package com.example.model

data class PremiumCode(
    val codeId: String = "",
    val code: String = "", // e.g. "PULSE-PRO-7890-ABCD"
    val codeHash: String = "",
    val packageType: String = "PREMIUM", // "TRIAL", "PREMIUM", "PERMANENT", "CUSTOM_CHECK"
    val durationDays: Int = 30, // 0 for permanent
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = 0L, // 0 if no expiration before redemption
    val status: String = "AVAILABLE", // "AVAILABLE", "USED", "EXPIRED", "REVOKED"
    val createdBy: String = "",
    val usedBy: String = "",
    val usedAt: Long = 0L,
    val referenceId: String = ""
) {
    val isRedeemable: Boolean
        get() {
            if (!status.equals("AVAILABLE", ignoreCase = true)) return false
            if (expiresAt > 0L && expiresAt < System.currentTimeMillis()) return false
            return true
        }

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "codeId" to codeId,
            "code" to code.uppercase().trim(),
            "codeHash" to codeHash,
            "packageType" to packageType.uppercase(),
            "durationDays" to durationDays,
            "createdAt" to createdAt,
            "expiresAt" to expiresAt,
            "status" to status.uppercase(),
            "createdBy" to createdBy,
            "usedBy" to usedBy,
            "usedAt" to usedAt,
            "referenceId" to referenceId
        )
    }

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>?): PremiumCode {
            if (map == null) return PremiumCode(codeId = id)
            return PremiumCode(
                codeId = id,
                code = (map["code"] as? String)?.uppercase() ?: "",
                codeHash = map["codeHash"] as? String ?: "",
                packageType = (map["packageType"] as? String)?.uppercase() ?: "PREMIUM",
                durationDays = (map["durationDays"] as? Number)?.toInt() ?: 30,
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                expiresAt = (map["expiresAt"] as? Number)?.toLong() ?: 0L,
                status = (map["status"] as? String)?.uppercase() ?: "AVAILABLE",
                createdBy = map["createdBy"] as? String ?: "",
                usedBy = map["usedBy"] as? String ?: "",
                usedAt = (map["usedAt"] as? Number)?.toLong() ?: 0L,
                referenceId = map["referenceId"] as? String ?: ""
            )
        }
    }
}
