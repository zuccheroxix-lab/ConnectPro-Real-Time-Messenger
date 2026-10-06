package com.example.model

enum class MessageStatus {
    SENDING,
    SENT,       // 1 tick (stored in server)
    DELIVERED,  // 2 ticks (delivered to receiver device)
    READ,       // 2 blue ticks (read by receiver)
    FAILED
}

data class Message(
    val id: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val text: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val deliveredAt: Long? = null,
    val readAt: Long? = null,
    val deletedAt: Long? = null,
    val replyToMessageId: String? = null,
    val replyToText: String? = null,
    val replyToSenderName: String? = null,
    val localStatus: MessageStatus? = null
) {
    val isDeleted: Boolean get() = deletedAt != null && deletedAt > 0L

    val effectiveStatus: MessageStatus
        get() {
            if (localStatus == MessageStatus.FAILED || localStatus == MessageStatus.SENDING) {
                return localStatus
            }
            return when {
                readAt != null && readAt > 0L -> MessageStatus.READ
                deliveredAt != null && deliveredAt > 0L -> MessageStatus.DELIVERED
                createdAt > 0L -> MessageStatus.SENT
                else -> MessageStatus.SENDING
            }
        }

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "conversationId" to conversationId,
            "senderId" to senderId,
            "text" to text,
            "createdAt" to createdAt,
            "deliveredAt" to deliveredAt,
            "readAt" to readAt,
            "deletedAt" to deletedAt,
            "replyToMessageId" to replyToMessageId,
            "replyToText" to replyToText,
            "replyToSenderName" to replyToSenderName
        )
    }

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>?): Message {
            if (map == null) return Message(id = id)
            return Message(
                id = id,
                conversationId = map["conversationId"] as? String ?: "",
                senderId = map["senderId"] as? String ?: "",
                text = map["text"] as? String ?: "",
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                deliveredAt = (map["deliveredAt"] as? Number)?.toLong(),
                readAt = (map["readAt"] as? Number)?.toLong(),
                deletedAt = (map["deletedAt"] as? Number)?.toLong(),
                replyToMessageId = map["replyToMessageId"] as? String,
                replyToText = map["replyToText"] as? String,
                replyToSenderName = map["replyToSenderName"] as? String
            )
        }
    }
}
