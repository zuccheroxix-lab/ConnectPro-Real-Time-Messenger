package com.example.model

data class Conversation(
    val id: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastMessageText: String = "",
    val lastMessageSenderId: String = "",
    val lastMessageTimestamp: Long = System.currentTimeMillis(),
    val members: List<String> = emptyList(),
    val unreadCounts: Map<String, Long> = emptyMap(),
    // Transient client helpers
    val otherUser: User? = null
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt,
            "lastMessageText" to lastMessageText,
            "lastMessageSenderId" to lastMessageSenderId,
            "lastMessageTimestamp" to lastMessageTimestamp,
            "members" to members,
            "unreadCounts" to unreadCounts
        )
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(id: String, map: Map<String, Any?>?): Conversation {
            if (map == null) return Conversation(id = id)
            val rawUnread = map["unreadCounts"] as? Map<String, Any?> ?: emptyMap()
            val parsedUnread = rawUnread.mapValues { (_, v) -> (v as? Number)?.toLong() ?: 0L }
            return Conversation(
                id = id,
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                updatedAt = (map["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                lastMessageText = map["lastMessageText"] as? String ?: "",
                lastMessageSenderId = map["lastMessageSenderId"] as? String ?: "",
                lastMessageTimestamp = (map["lastMessageTimestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                members = (map["members"] as? List<String>) ?: emptyList(),
                unreadCounts = parsedUnread
            )
        }
    }
}
