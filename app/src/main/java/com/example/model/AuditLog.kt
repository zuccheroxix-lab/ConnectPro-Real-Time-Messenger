package com.example.model

data class AuditLog(
    val id: String = "",
    val actorId: String = "",
    val actorRole: String = "USER", // "USER", "ADMIN", "SYSTEM"
    val action: String = "", // "CODE_GENERATED", "CODE_REDEEMED", "CODE_REVOKED", "PAYMENT_VERIFIED", "ENTITLEMENT_ACTIVATED", "SPIN_PERFORMED", etc.
    val targetId: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val metadata: Map<String, String> = emptyMap()
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "actorId" to actorId,
            "actorRole" to actorRole,
            "action" to action,
            "targetId" to targetId,
            "timestamp" to timestamp,
            "metadata" to metadata
        )
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(id: String, map: Map<String, Any?>?): AuditLog {
            if (map == null) return AuditLog(id = id)
            return AuditLog(
                id = id,
                actorId = map["actorId"] as? String ?: "",
                actorRole = map["actorRole"] as? String ?: "USER",
                action = map["action"] as? String ?: "",
                targetId = map["targetId"] as? String ?: "",
                timestamp = (map["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                metadata = (map["metadata"] as? Map<String, String>) ?: emptyMap()
            )
        }
    }
}
