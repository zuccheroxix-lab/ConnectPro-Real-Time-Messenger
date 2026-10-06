package com.example.model

data class User(
    val id: String = "",
    val phoneNumber: String = "",
    val username: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val bio: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastSeen: Long = System.currentTimeMillis(),
    val isOnline: Boolean = false,
    val fcmToken: String = "",
    val isPremium: Boolean = false,
    val role: String = "USER" // "USER" or "ADMIN"
) {
    val isAdmin: Boolean
        get() = role.equals("ADMIN", ignoreCase = true)

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "phoneNumber" to phoneNumber,
            "username" to username.lowercase().trim(),
            "displayName" to displayName.trim(),
            "photoUrl" to photoUrl,
            "bio" to bio.trim(),
            "createdAt" to createdAt,
            "updatedAt" to updatedAt,
            "lastSeen" to lastSeen,
            "isOnline" to isOnline,
            "fcmToken" to fcmToken,
            "isPremium" to isPremium,
            "role" to role
        )
    }

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>?): User {
            if (map == null) return User(id = id)
            return User(
                id = id,
                phoneNumber = map["phoneNumber"] as? String ?: "",
                username = map["username"] as? String ?: "",
                displayName = map["displayName"] as? String ?: "",
                photoUrl = map["photoUrl"] as? String ?: "",
                bio = map["bio"] as? String ?: "",
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                updatedAt = (map["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                lastSeen = (map["lastSeen"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                isOnline = map["isOnline"] as? Boolean ?: false,
                fcmToken = map["fcmToken"] as? String ?: "",
                isPremium = map["isPremium"] as? Boolean ?: false,
                role = map["role"] as? String ?: "USER"
            )
        }
    }
}
