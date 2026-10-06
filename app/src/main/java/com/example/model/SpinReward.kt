package com.example.model

data class SpinReward(
    val id: String = "",
    val name: String = "",
    val type: String = "NO_REWARD", // "CUSTOM_CHECK", "EXTRA_DURATION", "CHECK_STYLE", "EXTRA_SPIN", "NO_REWARD"
    val value: String = "", // e.g. "7" (days), "#A855F7" (color hex), "1" (spin)
    val probability: Double = 0.1,
    val isActive: Boolean = true,
    val colorHex: String = "#00D2FF"
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "name" to name,
            "type" to type.uppercase(),
            "value" to value,
            "probability" to probability,
            "isActive" to isActive,
            "colorHex" to colorHex
        )
    }

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>?): SpinReward {
            if (map == null) return SpinReward(id = id)
            return SpinReward(
                id = id,
                name = map["name"] as? String ?: "",
                type = (map["type"] as? String)?.uppercase() ?: "NO_REWARD",
                value = map["value"] as? String ?: "",
                probability = (map["probability"] as? Number)?.toDouble() ?: 0.1,
                isActive = map["isActive"] as? Boolean ?: true,
                colorHex = map["colorHex"] as? String ?: "#00D2FF"
            )
        }
    }
}
