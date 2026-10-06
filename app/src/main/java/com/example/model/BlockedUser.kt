package com.example.model

data class BlockedUser(
    val blockerId: String = "",
    val blockedUserId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val blockedUser: User? = null
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "blockerId" to blockerId,
            "blockedUserId" to blockedUserId,
            "createdAt" to createdAt
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>?): BlockedUser {
            if (map == null) return BlockedUser()
            return BlockedUser(
                blockerId = map["blockerId"] as? String ?: "",
                blockedUserId = map["blockedUserId"] as? String ?: "",
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}
