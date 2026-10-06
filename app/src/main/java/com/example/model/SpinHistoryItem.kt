package com.example.model

data class SpinHistoryItem(
    val spinId: String = "",
    val userId: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val packageName: String = "",
    val result: String = "",
    val rewardType: String = "",
    val remainingSpin: Int = 0
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "spinId" to spinId,
            "userId" to userId,
            "timestamp" to timestamp,
            "packageName" to packageName,
            "result" to result,
            "rewardType" to rewardType,
            "remainingSpin" to remainingSpin
        )
    }

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>?): SpinHistoryItem {
            if (map == null) return SpinHistoryItem(spinId = id)
            return SpinHistoryItem(
                spinId = id,
                userId = map["userId"] as? String ?: "",
                timestamp = (map["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                packageName = map["packageName"] as? String ?: "",
                result = map["result"] as? String ?: "",
                rewardType = map["rewardType"] as? String ?: "",
                remainingSpin = (map["remainingSpin"] as? Number)?.toInt() ?: 0
            )
        }
    }
}
