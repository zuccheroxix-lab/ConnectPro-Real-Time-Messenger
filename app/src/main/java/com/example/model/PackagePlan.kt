package com.example.model

data class PackagePlan(
    val id: String = "",
    val type: String = "PREMIUM", // "TRIAL", "PREMIUM", "PERMANENT", "CUSTOM_CHECK"
    val name: String = "",
    val price: String = "",
    val durationDays: Int = 30, // 0 for permanent
    val features: List<String> = emptyList(),
    val spinLimit: Int = 3,
    val customCheck: Boolean = false,
    val isActive: Boolean = true,
    val sortOrder: Int = 0
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "type" to type.uppercase(),
            "name" to name,
            "price" to price,
            "durationDays" to durationDays,
            "features" to features,
            "spinLimit" to spinLimit,
            "customCheck" to customCheck,
            "isActive" to isActive,
            "sortOrder" to sortOrder
        )
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(id: String, map: Map<String, Any?>?): PackagePlan {
            if (map == null) return PackagePlan(id = id)
            return PackagePlan(
                id = id,
                type = (map["type"] as? String)?.uppercase() ?: "PREMIUM",
                name = map["name"] as? String ?: "",
                price = map["price"] as? String ?: "",
                durationDays = (map["durationDays"] as? Number)?.toInt() ?: 30,
                features = (map["features"] as? List<String>) ?: emptyList(),
                spinLimit = (map["spinLimit"] as? Number)?.toInt() ?: 3,
                customCheck = map["customCheck"] as? Boolean ?: false,
                isActive = map["isActive"] as? Boolean ?: true,
                sortOrder = (map["sortOrder"] as? Number)?.toInt() ?: 0
            )
        }
    }
}
