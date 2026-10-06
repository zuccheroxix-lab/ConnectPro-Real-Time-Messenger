package com.example.model

data class CheckStyle(
    val colorHex: String = "#00D2FF",
    val opacity: Float = 1.0f,
    val sizeDp: Int = 16,
    val styleName: String = "CLASSIC" // "CLASSIC", "GLOW", "ROUNDED", "SHIELD"
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "colorHex" to colorHex,
            "opacity" to opacity,
            "sizeDp" to sizeDp,
            "styleName" to styleName
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>?): CheckStyle {
            if (map == null) return CheckStyle()
            return CheckStyle(
                colorHex = map["colorHex"] as? String ?: "#00D2FF",
                opacity = (map["opacity"] as? Number)?.toFloat() ?: 1.0f,
                sizeDp = (map["sizeDp"] as? Number)?.toInt() ?: 16,
                styleName = map["styleName"] as? String ?: "CLASSIC"
            )
        }
    }
}
