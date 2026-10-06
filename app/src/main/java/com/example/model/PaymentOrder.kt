package com.example.model

data class PaymentOrder(
    val id: String = "",
    val userId: String = "",
    val userPhone: String = "",
    val packageId: String = "",
    val packageName: String = "",
    val amount: String = "",
    val status: String = "PENDING", // "PENDING", "VERIFIED", "REJECTED"
    val referenceNote: String = "",
    val assignedCode: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val verifiedAt: Long = 0L,
    val verifiedBy: String = ""
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "userId" to userId,
            "userPhone" to userPhone,
            "packageId" to packageId,
            "packageName" to packageName,
            "amount" to amount,
            "status" to status.uppercase(),
            "referenceNote" to referenceNote,
            "assignedCode" to assignedCode,
            "createdAt" to createdAt,
            "verifiedAt" to verifiedAt,
            "verifiedBy" to verifiedBy
        )
    }

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>?): PaymentOrder {
            if (map == null) return PaymentOrder(id = id)
            return PaymentOrder(
                id = id,
                userId = map["userId"] as? String ?: "",
                userPhone = map["userPhone"] as? String ?: "",
                packageId = map["packageId"] as? String ?: "",
                packageName = map["packageName"] as? String ?: "",
                amount = map["amount"] as? String ?: "",
                status = (map["status"] as? String)?.uppercase() ?: "PENDING",
                referenceNote = map["referenceNote"] as? String ?: "",
                assignedCode = map["assignedCode"] as? String ?: "",
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                verifiedAt = (map["verifiedAt"] as? Number)?.toLong() ?: 0L,
                verifiedBy = map["verifiedBy"] as? String ?: ""
            )
        }
    }
}
