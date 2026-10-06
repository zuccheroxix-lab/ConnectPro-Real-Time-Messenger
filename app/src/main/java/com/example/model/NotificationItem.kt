package com.example.model

data class NotificationItem(
    val id: String = "",
    val userId: String = "",
    val type: String = "message", // "message", "subscription", "system"
    val data: Map<String, String> = emptyMap(),
    val createdAt: Long = System.currentTimeMillis(),
    val read: Boolean = false
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "userId" to userId,
            "type" to type,
            "data" to data,
            "createdAt" to createdAt,
            "read" to read
        )
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(id: String, map: Map<String, Any?>?): NotificationItem {
            if (map == null) return NotificationItem(id = id)
            return NotificationItem(
                id = id,
                userId = map["userId"] as? String ?: "",
                type = map["type"] as? String ?: "message",
                data = (map["data"] as? Map<String, String>) ?: emptyMap(),
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                read = map["read"] as? Boolean ?: false
            )
        }
    }
}
