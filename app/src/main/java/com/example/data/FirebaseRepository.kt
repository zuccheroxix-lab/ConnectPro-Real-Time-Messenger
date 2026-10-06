package com.example.data

import android.app.Activity
import android.util.Log
import com.example.PulseApplication
import com.example.model.*
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import com.google.firebase.firestore.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class FirebaseRepository private constructor() {

    companion object {
        private const val TAG = "FirebaseRepository"

        @Volatile
        private var INSTANCE: FirebaseRepository? = null

        fun getInstance(): FirebaseRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirebaseRepository().also { INSTANCE = it }
            }
        }
    }

    val isConfigured: Boolean
        get() = PulseApplication.isFirebaseReady()

    val auth: FirebaseAuth?
        get() = if (isConfigured) FirebaseAuth.getInstance() else null

    val firestore: FirebaseFirestore?
        get() = if (isConfigured) FirebaseFirestore.getInstance() else null

    val currentUserId: String?
        get() = auth?.currentUser?.uid

    // ==================== PHONE AUTH ====================

    fun verifyPhoneNumber(
        phoneNumber: String,
        activity: Activity,
        forceResendingToken: PhoneAuthProvider.ForceResendingToken? = null,
        onVerificationCompleted: (PhoneAuthCredential) -> Unit,
        onVerificationFailed: (FirebaseException) -> Unit,
        onCodeSent: (String, PhoneAuthProvider.ForceResendingToken) -> Unit
    ) {
        val firebaseAuth = auth ?: run {
            onVerificationFailed(FirebaseException("Firebase is not initialized. Please configure google-services.json."))
            return
        }

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                Log.d(TAG, "onVerificationCompleted: $credential")
                onVerificationCompleted(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Log.e(TAG, "onVerificationFailed: ${e.message}", e)
                onVerificationFailed(e)
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                Log.d(TAG, "onCodeSent: $verificationId")
                onCodeSent(verificationId, token)
            }
        }

        val builder = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)

        if (forceResendingToken != null) {
            builder.setForceResendingToken(forceResendingToken)
        }

        PhoneAuthProvider.verifyPhoneNumber(builder.build())
    }

    suspend fun signInWithPhoneCode(verificationId: String, code: String): Result<FirebaseUser> {
        val firebaseAuth = auth ?: return Result.failure(Exception("Firebase is not initialized."))
        return try {
            val credential = PhoneAuthProvider.getCredential(verificationId, code)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val user = authResult.user ?: throw Exception("Authentication returned empty user.")
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "signInWithPhoneCode error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun signInWithCredential(credential: PhoneAuthCredential): Result<FirebaseUser> {
        val firebaseAuth = auth ?: return Result.failure(Exception("Firebase is not initialized."))
        return try {
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val user = authResult.user ?: throw Exception("Authentication returned empty user.")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        val uid = currentUserId
        if (uid != null && firestore != null) {
            firestore?.collection("users")?.document(uid)?.update(
                mapOf(
                    "isOnline" to false,
                    "lastSeen" to System.currentTimeMillis()
                )
            )
        }
        auth?.signOut()
    }

    suspend fun deleteAccount(): Result<Unit> {
        val uid = currentUserId ?: return Result.failure(Exception("No user logged in."))
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized."))
        val firebaseAuth = auth ?: return Result.failure(Exception("Auth not initialized."))

        return try {
            db.collection("users").document(uid).delete().await()
            firebaseAuth.currentUser?.delete()?.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== USER PROFILE ====================

    suspend fun isUsernameAvailable(username: String): Boolean {
        val db = firestore ?: return true
        val clean = username.lowercase().trim()
        val snapshot = db.collection("users")
            .whereEqualTo("username", clean)
            .limit(1)
            .get()
            .await()
        return snapshot.isEmpty
    }

    suspend fun saveUserProfile(user: User): Result<Unit> {
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized."))
        return try {
            db.collection("users").document(user.id).set(user.toMap(), SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserProfile(displayName: String, bio: String, photoUrl: String, username: String): Result<Unit> {
        val uid = currentUserId ?: return Result.failure(Exception("No user logged in."))
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized."))

        return try {
            val updates = mapOf(
                "displayName" to displayName.trim(),
                "bio" to bio.trim(),
                "photoUrl" to photoUrl.trim(),
                "username" to username.lowercase().trim(),
                "updatedAt" to System.currentTimeMillis()
            )
            db.collection("users").document(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeCurrentUser(): Flow<User?> = callbackFlow {
        val uid = currentUserId
        val db = firestore
        if (uid == null || db == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener = db.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "observeCurrentUser error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    trySend(User.fromMap(snapshot.id, snapshot.data))
                } else {
                    trySend(null)
                }
            }

        awaitClose { listener.remove() }
    }

    fun observeUser(userId: String): Flow<User?> = callbackFlow {
        val db = firestore
        if (db == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener = db.collection("users").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "observeUser error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    trySend(User.fromMap(snapshot.id, snapshot.data))
                } else {
                    trySend(null)
                }
            }

        awaitClose { listener.remove() }
    }

    fun searchUsers(query: String): Flow<List<User>> = callbackFlow {
        val db = firestore
        val currentUid = currentUserId
        if (db == null || query.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val cleanQuery = query.lowercase().trim()
        val queryRef = db.collection("users")
            .orderBy("username")
            .startAt(cleanQuery)
            .endAt(cleanQuery + "\uf8ff")
            .limit(20)

        val listener = queryRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "searchUsers error: ${error.message}")
                trySend(emptyList())
                return@addSnapshotListener
            }
            val users = snapshot?.documents?.mapNotNull { doc ->
                if (doc.id == currentUid) null else User.fromMap(doc.id, doc.data)
            } ?: emptyList()
            trySend(users)
        }

        awaitClose { listener.remove() }
    }

    // ==================== CONVERSATIONS & CHAT ====================

    suspend fun getOrCreateConversationId(otherUserId: String): Result<String> {
        val uid = currentUserId ?: return Result.failure(Exception("No user logged in."))
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized."))

        return try {
            // Check if conversation already exists with both members
            val querySnapshot = db.collection("conversations")
                .whereArrayContains("members", uid)
                .get()
                .await()

            val existing = querySnapshot.documents.firstOrNull { doc ->
                val members = doc.get("members") as? List<*> ?: emptyList<Any>()
                members.contains(otherUserId)
            }

            if (existing != null) {
                Result.success(existing.id)
            } else {
                val newDocRef = db.collection("conversations").document()
                val conv = Conversation(
                    id = newDocRef.id,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    members = listOf(uid, otherUserId),
                    unreadCounts = mapOf(uid to 0L, otherUserId to 0L)
                )
                newDocRef.set(conv.toMap()).await()
                Result.success(newDocRef.id)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeConversations(): Flow<List<Conversation>> = callbackFlow {
        val uid = currentUserId
        val db = firestore
        if (uid == null || db == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = db.collection("conversations")
            .whereArrayContains("members", uid)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "observeConversations error: ${error.message}")
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.map { doc ->
                    Conversation.fromMap(doc.id, doc.data)
                } ?: emptyList()
                trySend(list)
            }

        awaitClose { listener.remove() }
    }

    fun observeMessages(conversationId: String, limit: Long = 60): Flow<List<Message>> = callbackFlow {
        val db = firestore
        if (db == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = db.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .limitToLast(limit)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "observeMessages error: ${error.message}")
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.map { doc ->
                    Message.fromMap(doc.id, doc.data)
                } ?: emptyList()
                trySend(messages)
            }

        awaitClose { listener.remove() }
    }

    suspend fun sendMessage(
        conversationId: String,
        receiverId: String,
        text: String,
        replyTo: Message? = null
    ): Result<Message> {
        val uid = currentUserId ?: return Result.failure(Exception("No user logged in."))
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized."))

        return try {
            val messagesRef = db.collection("conversations")
                .document(conversationId)
                .collection("messages")

            val newMsgDoc = messagesRef.document()
            val now = System.currentTimeMillis()

            val msg = Message(
                id = newMsgDoc.id,
                conversationId = conversationId,
                senderId = uid,
                text = text.trim(),
                createdAt = now,
                deliveredAt = null,
                readAt = null,
                replyToMessageId = replyTo?.id,
                replyToText = replyTo?.text,
                replyToSenderName = replyTo?.senderId
            )

            // Batch write: create message + update conversation snippet & increment unread count
            db.runBatch { batch ->
                batch.set(newMsgDoc, msg.toMap())
                val convRef = db.collection("conversations").document(conversationId)
                batch.update(
                    convRef,
                    mapOf(
                        "lastMessageText" to text.trim(),
                        "lastMessageSenderId" to uid,
                        "lastMessageTimestamp" to now,
                        "updatedAt" to now,
                        "unreadCounts.$receiverId" to FieldValue.increment(1)
                    )
                )
            }.await()

            Result.success(msg)
        } catch (e: Exception) {
            Log.e(TAG, "sendMessage error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun markMessagesDelivered(conversationId: String, messages: List<Message>) {
        val uid = currentUserId ?: return
        val db = firestore ?: return

        try {
            val undelivered = messages.filter { it.senderId != uid && it.deliveredAt == null }
            if (undelivered.isEmpty()) return

            val now = System.currentTimeMillis()
            db.runBatch { batch ->
                for (msg in undelivered) {
                    val ref = db.collection("conversations")
                        .document(conversationId)
                        .collection("messages")
                        .document(msg.id)
                    batch.update(ref, "deliveredAt", now)
                }
            }.await()
        } catch (e: Exception) {
            Log.w(TAG, "markMessagesDelivered error: ${e.message}")
        }
    }

    suspend fun markConversationAsRead(conversationId: String, messages: List<Message>) {
        val uid = currentUserId ?: return
        val db = firestore ?: return

        try {
            val unread = messages.filter { it.senderId != uid && it.readAt == null }
            val now = System.currentTimeMillis()

            db.runBatch { batch ->
                for (msg in unread) {
                    val ref = db.collection("conversations")
                        .document(conversationId)
                        .collection("messages")
                        .document(msg.id)
                    batch.update(
                        ref,
                        mapOf(
                            "readAt" to now,
                            "deliveredAt" to (msg.deliveredAt ?: now)
                        )
                    )
                }
                val convRef = db.collection("conversations").document(conversationId)
                batch.update(convRef, "unreadCounts.$uid", 0)
            }.await()
        } catch (e: Exception) {
            Log.w(TAG, "markConversationAsRead error: ${e.message}")
        }
    }

    suspend fun deleteMessage(conversationId: String, messageId: String): Result<Unit> {
        val uid = currentUserId ?: return Result.failure(Exception("No user logged in."))
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized."))

        return try {
            val ref = db.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .document(messageId)

            val snapshot = ref.get().await()
            val senderId = snapshot.getString("senderId")
            if (senderId != uid) {
                return Result.failure(Exception("Cannot delete message from another user."))
            }

            ref.update("deletedAt", System.currentTimeMillis()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== BLOCK USERS ====================

    suspend fun blockUser(targetUserId: String): Result<Unit> {
        val uid = currentUserId ?: return Result.failure(Exception("No user logged in."))
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized."))

        return try {
            val blocked = BlockedUser(blockerId = uid, blockedUserId = targetUserId)
            db.collection("users").document(uid)
                .collection("blockedUsers").document(targetUserId)
                .set(blocked.toMap()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unblockUser(targetUserId: String): Result<Unit> {
        val uid = currentUserId ?: return Result.failure(Exception("No user logged in."))
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized."))

        return try {
            db.collection("users").document(uid)
                .collection("blockedUsers").document(targetUserId)
                .delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeBlockedUsers(): Flow<List<BlockedUser>> = callbackFlow {
        val uid = currentUserId
        val db = firestore
        if (uid == null || db == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = db.collection("users").document(uid)
            .collection("blockedUsers")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "observeBlockedUsers error: ${error.message}")
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.map { doc ->
                    BlockedUser.fromMap(doc.data)
                } ?: emptyList()
                trySend(list)
            }

        awaitClose { listener.remove() }
    }

    // ==================== SUBSCRIPTIONS & PREMIUM ====================

    fun observeSubscription(userId: String): Flow<Subscription?> = callbackFlow {
        val db = firestore
        if (db == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener = db.collection("subscriptions").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "observeSubscription error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val sub = Subscription.fromMap(snapshot.id, snapshot.data)
                    trySend(sub)

                    // Synchronize user's isPremium flag in users collection based on actual validity
                    val isValid = sub.isCurrentlyActive
                    db.collection("users").document(userId).update("isPremium", isValid)
                } else {
                    trySend(null)
                }
            }

        awaitClose { listener.remove() }
    }

    // ==================== PREMIUM ENTITLEMENTS & CODES ====================

    fun observeUserEntitlement(userId: String): Flow<UserEntitlement> = callbackFlow {
        val db = firestore
        if (db == null) {
            trySend(UserEntitlement(userId = userId))
            close()
            return@callbackFlow
        }

        val listener = db.collection("user_entitlements").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "observeUserEntitlement error: ${error.message}")
                    return@addSnapshotListener
                }
                val entitlement = if (snapshot != null && snapshot.exists()) {
                    UserEntitlement.fromMap(snapshot.id, snapshot.data)
                } else {
                    UserEntitlement(userId = userId, plan = "FREE", status = "ACTIVE")
                }
                trySend(entitlement)

                // Sync isPremium flag in users collection
                db.collection("users").document(userId).update("isPremium", entitlement.isPremiumTier)
            }

        awaitClose { listener.remove() }
    }

    suspend fun redeemPremiumCode(inputCode: String): Result<UserEntitlement> {
        val uid = currentUserId ?: return Result.failure(Exception("Sesi login berakhir. Silakan login kembali."))
        val db = firestore ?: return Result.failure(Exception("Firebase Firestore belum diinisialisasi."))

        val cleanCode = inputCode.trim().uppercase()
        if (cleanCode.length < 6) {
            return Result.failure(Exception("Kode premium tidak valid."))
        }

        return try {
            // Find code document
            val querySnapshot = db.collection("premium_codes")
                .whereEqualTo("code", cleanCode)
                .limit(1)
                .get()
                .await()

            if (querySnapshot.isEmpty) {
                return Result.failure(Exception("Kode premium tidak valid."))
            }

            val codeDoc = querySnapshot.documents.first()
            val codeRef = codeDoc.reference

            // Execute atomic transaction for double-redeem locking
            val updatedEntitlement = db.runTransaction { transaction ->
                val freshCodeSnapshot = transaction.get(codeRef)
                val codeStatus = freshCodeSnapshot.getString("status")?.uppercase() ?: "AVAILABLE"
                val expiresAt = freshCodeSnapshot.getLong("expiresAt") ?: 0L
                val now = System.currentTimeMillis()

                if (codeStatus == "USED") {
                    throw IllegalStateException("Kode ini sudah digunakan.")
                }
                if (codeStatus != "AVAILABLE") {
                    throw IllegalStateException("Kode premium tidak valid atau telah dibatalkan.")
                }
                if (expiresAt in 1 until now) {
                    throw IllegalStateException("Kode premium sudah kedaluwarsa.")
                }

                val packageType = freshCodeSnapshot.getString("packageType")?.uppercase() ?: "PREMIUM"
                val durationDays = freshCodeSnapshot.getLong("durationDays")?.toInt() ?: 30

                // Mark code as used
                transaction.update(
                    codeRef,
                    mapOf(
                        "status" to "USED",
                        "usedBy" to uid,
                        "usedAt" to now
                    )
                )

                // Read existing entitlement
                val entRef = db.collection("user_entitlements").document(uid)
                val entSnapshot = transaction.get(entRef)
                val existing = if (entSnapshot.exists()) {
                    UserEntitlement.fromMap(uid, entSnapshot.data)
                } else {
                    UserEntitlement(userId = uid)
                }

                val isPermanent = packageType == "PERMANENT" || durationDays == 0
                val newExpiresAt = if (isPermanent) 0L else {
                    val baseTime = if (existing.isCurrentlyActive && existing.expiresAt > now) existing.expiresAt else now
                    baseTime + (durationDays * 24L * 60L * 60L * 1000L)
                }

                val customCheckUnlocked = packageType == "CUSTOM_CHECK" || isPermanent || existing.customCheckEnabled
                val extraSpins = when (packageType) {
                    "TRIAL" -> 1
                    "CUSTOM_CHECK" -> 2
                    "PERMANENT" -> 10
                    else -> 3
                }

                val newEntitlement = UserEntitlement(
                    userId = uid,
                    plan = packageType,
                    source = "CODE_REDEEM",
                    startedAt = now,
                    expiresAt = newExpiresAt,
                    permanent = isPermanent,
                    customCheckEnabled = customCheckUnlocked,
                    checkStyle = existing.checkStyle,
                    remainingSpins = existing.remainingSpins + extraSpins,
                    status = "ACTIVE"
                )

                transaction.set(entRef, newEntitlement.toMap())

                // Sync user collection
                val userRef = db.collection("users").document(uid)
                transaction.update(userRef, "isPremium", true)

                // Write audit log
                val auditRef = db.collection("audit_logs").document()
                val audit = AuditLog(
                    id = auditRef.id,
                    actorId = uid,
                    actorRole = "USER",
                    action = "CODE_REDEEMED",
                    targetId = codeDoc.id,
                    timestamp = now,
                    metadata = mapOf("code" to cleanCode, "package" to packageType)
                )
                transaction.set(auditRef, audit.toMap())

                newEntitlement
            }.await()

            Result.success(updatedEntitlement)
        } catch (e: Exception) {
            val message = e.cause?.message ?: e.message ?: "Gagal memproses kode premium."
            Result.failure(Exception(message))
        }
    }

    // ==================== SPINNER SYSTEM ====================

    suspend fun performSpin(): Result<SpinReward> {
        val uid = currentUserId ?: return Result.failure(Exception("Sesi login berakhir."))
        val db = firestore ?: return Result.failure(Exception("Firestore belum diinisialisasi."))

        return try {
            val entRef = db.collection("user_entitlements").document(uid)

            // Seed default rewards if empty
            val rewardsSnapshot = db.collection("spin_rewards").whereEqualTo("isActive", true).get().await()
            val availableRewards = if (rewardsSnapshot.isEmpty) {
                getDefaultRewards().also { defaults ->
                    for (r in defaults) {
                        db.collection("spin_rewards").document(r.id).set(r.toMap()).await()
                    }
                }
            } else {
                rewardsSnapshot.documents.map { SpinReward.fromMap(it.id, it.data) }
            }

            // Pick reward based on probability weight
            val totalProb = availableRewards.sumOf { it.probability }.coerceAtLeast(0.01)
            val randomVal = java.util.Random().nextDouble() * totalProb
            var runningSum = 0.0
            var pickedReward = availableRewards.firstOrNull() ?: SpinReward(name = "Zonk / No Reward", type = "NO_REWARD")
            for (r in availableRewards) {
                runningSum += r.probability
                if (randomVal <= runningSum) {
                    pickedReward = r
                    break
                }
            }

            // Execute atomic transaction to decrement spin and apply reward
            db.runTransaction { transaction ->
                val entSnapshot = transaction.get(entRef)
                if (!entSnapshot.exists()) {
                    throw IllegalStateException("User entitlement tidak ditemukan.")
                }
                val entitlement = UserEntitlement.fromMap(uid, entSnapshot.data)
                if (!entitlement.isDailySpinAvailable && entitlement.remainingSpins <= 0) {
                    throw IllegalStateException("Batas spin harian sudah habis. Silakan coba lagi besok atau redeem kode untuk spin tambahan.")
                }

                val now = System.currentTimeMillis()
                val finalRemaining = if (entitlement.remainingSpins > 0) entitlement.remainingSpins - 1 else 0

                // Apply reward modifications
                var updatedCustomCheck = entitlement.customCheckEnabled
                var updatedExpiresAt = entitlement.expiresAt
                var updatedStyle = entitlement.checkStyle
                var updatedPermanent = entitlement.permanent
                var updatedPlan = entitlement.plan
                var bonusRemaining = finalRemaining

                when (pickedReward.type.uppercase()) {
                    "JACKPOT_PERMANENT" -> {
                        updatedPermanent = true
                        updatedPlan = "PERMANENT"
                        updatedCustomCheck = true
                    }
                    "TRIAL" -> {
                        val extraDays = pickedReward.value.toIntOrNull() ?: 1
                        if (updatedPlan.equals("FREE", ignoreCase = true)) {
                            updatedPlan = "TRIAL"
                        }
                        val base = if (entitlement.isCurrentlyActive && entitlement.expiresAt > now) entitlement.expiresAt else now
                        updatedExpiresAt = base + (extraDays * 24L * 60L * 60L * 1000L)
                    }
                    "EXTRA_DURATION" -> {
                        val extraDays = pickedReward.value.toIntOrNull() ?: 7
                        if (updatedPlan.equals("FREE", ignoreCase = true)) {
                            updatedPlan = "PREMIUM"
                        }
                        val base = if (entitlement.isCurrentlyActive && entitlement.expiresAt > now) entitlement.expiresAt else now
                        updatedExpiresAt = base + (extraDays * 24L * 60L * 60L * 1000L)
                    }
                    "CUSTOM_CHECK" -> {
                        updatedCustomCheck = true
                    }
                    "EXTRA_SPIN" -> {
                        val extraCount = pickedReward.value.toIntOrNull() ?: 1
                        bonusRemaining += extraCount
                    }
                    "CHECK_STYLE" -> {
                        updatedCustomCheck = true
                        updatedStyle = CheckStyle(colorHex = pickedReward.value.ifBlank { "#00D2FF" })
                    }
                    "NO_REWARD" -> {
                        // Zonk / Coba Lagi
                    }
                }

                val updatedEntitlement = entitlement.copy(
                    plan = updatedPlan,
                    permanent = updatedPermanent,
                    remainingSpins = bonusRemaining,
                    lastSpinTimestamp = now,
                    customCheckEnabled = updatedCustomCheck,
                    expiresAt = updatedExpiresAt,
                    checkStyle = updatedStyle,
                    status = "ACTIVE"
                )

                transaction.set(entRef, updatedEntitlement.toMap())

                // Record Spin History
                val spinHistRef = db.collection("spin_history").document()
                val hist = SpinHistoryItem(
                    spinId = spinHistRef.id,
                    userId = uid,
                    timestamp = now,
                    packageName = entitlement.plan,
                    result = pickedReward.name,
                    rewardType = pickedReward.type,
                    remainingSpin = finalRemaining
                )
                transaction.set(spinHistRef, hist.toMap())

                // Record Audit Log
                val auditRef = db.collection("audit_logs").document()
                val audit = AuditLog(
                    id = auditRef.id,
                    actorId = uid,
                    actorRole = "USER",
                    action = "SPIN_PERFORMED",
                    targetId = spinHistRef.id,
                    timestamp = now,
                    metadata = mapOf("reward" to pickedReward.name, "type" to pickedReward.type)
                )
                transaction.set(auditRef, audit.toMap())

                pickedReward
            }.await()

            Result.success(pickedReward)
        } catch (e: Exception) {
            val message = e.cause?.message ?: e.message ?: "Gagal memproses putaran spinner."
            Result.failure(Exception(message))
        }
    }

    fun observeSpinHistory(userId: String): Flow<List<SpinHistoryItem>> = callbackFlow {
        val db = firestore
        if (db == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = db.collection("spin_history")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "observeSpinHistory error: ${error.message}")
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.map { doc ->
                    SpinHistoryItem.fromMap(doc.id, doc.data)
                } ?: emptyList()
                trySend(list)
            }

        awaitClose { listener.remove() }
    }

    // ==================== CUSTOM CHECK CONFIGURATION ====================

    suspend fun updateCustomCheckStyle(checkStyle: CheckStyle): Result<Unit> {
        val uid = currentUserId ?: return Result.failure(Exception("Sesi login berakhir."))
        val db = firestore ?: return Result.failure(Exception("Firestore belum diinisialisasi."))

        return try {
            val entDoc = db.collection("user_entitlements").document(uid).get().await()
            val entitlement = if (entDoc.exists()) UserEntitlement.fromMap(uid, entDoc.data) else null

            if (entitlement?.customCheckEnabled != true) {
                return Result.failure(Exception("Custom Check membutuhkan paket Custom Check atau Permanent."))
            }

            db.collection("user_entitlements").document(uid)
                .update("checkStyle", checkStyle.toMap())
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== PACKAGES & PRICING ====================

    fun observePackages(): Flow<List<PackagePlan>> = callbackFlow {
        val db = firestore
        if (db == null) {
            trySend(getDefaultPackages())
            close()
            return@callbackFlow
        }

        val listener = db.collection("packages")
            .whereEqualTo("isActive", true)
            .orderBy("sortOrder", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "observePackages error: ${error.message}")
                    trySend(getDefaultPackages())
                    return@addSnapshotListener
                }
                if (snapshot == null || snapshot.isEmpty) {
                    // Seed defaults if empty
                    val defaults = getDefaultPackages()
                    trySend(defaults)
                    // Auto-seed in background
                    for (pkg in defaults) {
                        db.collection("packages").document(pkg.id).set(pkg.toMap())
                    }
                } else {
                    val list = snapshot.documents.map { doc ->
                        PackagePlan.fromMap(doc.id, doc.data)
                    }
                    trySend(list)
                }
            }

        awaitClose { listener.remove() }
    }

    fun observeSpinRewards(): Flow<List<SpinReward>> = callbackFlow {
        val db = firestore
        if (db == null) {
            trySend(getDefaultRewards())
            close()
            return@callbackFlow
        }

        val listener = db.collection("spin_rewards")
            .whereEqualTo("isActive", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "observeSpinRewards error: ${error.message}")
                    trySend(getDefaultRewards())
                    return@addSnapshotListener
                }
                if (snapshot == null || snapshot.isEmpty) {
                    val defaults = getDefaultRewards()
                    trySend(defaults)
                    for (r in defaults) {
                        db.collection("spin_rewards").document(r.id).set(r.toMap())
                    }
                } else {
                    val list = snapshot.documents.map { doc ->
                        SpinReward.fromMap(doc.id, doc.data)
                    }
                    trySend(list)
                }
            }

        awaitClose { listener.remove() }
    }

    // ==================== MANUAL PAYMENT ORDERS ====================

    suspend fun createPaymentOrder(
        packageId: String,
        packageName: String,
        amount: String,
        note: String
    ): Result<PaymentOrder> {
        val uid = currentUserId ?: return Result.failure(Exception("Sesi login berakhir."))
        val db = firestore ?: return Result.failure(Exception("Firestore belum diinisialisasi."))
        val phone = auth?.currentUser?.phoneNumber ?: ""

        return try {
            val orderRef = db.collection("payment_orders").document()
            val order = PaymentOrder(
                id = orderRef.id,
                userId = uid,
                userPhone = phone,
                packageId = packageId,
                packageName = packageName,
                amount = amount,
                status = "PENDING",
                referenceNote = note.trim(),
                createdAt = System.currentTimeMillis()
            )
            orderRef.set(order.toMap()).await()

            // Audit
            db.collection("audit_logs").document().set(
                AuditLog(
                    actorId = uid,
                    actorRole = "USER",
                    action = "PAYMENT_INITIATED",
                    targetId = orderRef.id,
                    metadata = mapOf("amount" to amount, "package" to packageName)
                ).toMap()
            )

            Result.success(order)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeUserPaymentOrders(userId: String): Flow<List<PaymentOrder>> = callbackFlow {
        val db = firestore
        if (db == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = db.collection("payment_orders")
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "observeUserPaymentOrders error: ${error.message}")
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.map { doc ->
                    PaymentOrder.fromMap(doc.id, doc.data)
                } ?: emptyList()
                trySend(list)
            }

        awaitClose { listener.remove() }
    }

    // ==================== ADMIN PANEL OPERATIONS ====================

    suspend fun checkIsAdmin(): Boolean {
        val uid = currentUserId ?: return false
        val db = firestore ?: return false
        return try {
            val doc = db.collection("users").document(uid).get().await()
            val role = doc.getString("role")
            if (role.equals("ADMIN", ignoreCase = true)) return true

            // If no admin exists in the system yet, promote current user to ADMIN
            val adminSnapshot = db.collection("users")
                .whereEqualTo("role", "ADMIN")
                .limit(1)
                .get()
                .await()
            if (adminSnapshot.isEmpty) {
                db.collection("users").document(uid).update("role", "ADMIN").await()
                return true
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getAdminUserId(): String? {
        val db = firestore ?: return null
        return try {
            val snap = db.collection("users")
                .whereEqualTo("role", "ADMIN")
                .limit(1)
                .get()
                .await()
            val adminId = snap.documents.firstOrNull()?.id
            if (adminId != null) return adminId

            // Fallback: return any registered user
            val anySnap = db.collection("users").limit(1).get().await()
            anySnap.documents.firstOrNull()?.id
        } catch (e: Exception) {
            null
        }
    }

    suspend fun adminGenerateCodes(
        packageType: String,
        durationDays: Int,
        quantity: Int,
        referenceId: String
    ): Result<List<PremiumCode>> {
        val uid = currentUserId ?: return Result.failure(Exception("Sesi login berakhir."))
        val db = firestore ?: return Result.failure(Exception("Firestore belum diinisialisasi."))

        if (!checkIsAdmin()) {
            return Result.failure(SecurityException("Otorisasi ditolak: Membutuhkan role ADMIN."))
        }

        return try {
            val generated = mutableListOf<PremiumCode>()
            val batch = db.batch()
            val now = System.currentTimeMillis()

            for (i in 0 until quantity.coerceIn(1, 50)) {
                val docRef = db.collection("premium_codes").document()
                val randomSuffix = java.util.UUID.randomUUID().toString().replace("-", "").take(8).uppercase()
                val prefix = when (packageType.uppercase()) {
                    "TRIAL" -> "PULSE-TRL"
                    "PERMANENT" -> "PULSE-LIFETIME"
                    "CUSTOM_CHECK" -> "PULSE-CHK"
                    else -> "PULSE-PRO"
                }
                val rawCode = "$prefix-$randomSuffix"
                val codeHash = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(rawCode.toByteArray())
                    .joinToString("") { "%02x".format(it) }

                val code = PremiumCode(
                    codeId = docRef.id,
                    code = rawCode,
                    codeHash = codeHash,
                    packageType = packageType.uppercase(),
                    durationDays = durationDays,
                    createdAt = now,
                    status = "AVAILABLE",
                    createdBy = uid,
                    referenceId = referenceId.trim()
                )

                batch.set(docRef, code.toMap())
                generated.add(code)
            }

            // Audit
            val auditRef = db.collection("audit_logs").document()
            batch.set(
                auditRef,
                AuditLog(
                    id = auditRef.id,
                    actorId = uid,
                    actorRole = "ADMIN",
                    action = "CODE_GENERATED",
                    targetId = generated.firstOrNull()?.codeId ?: "",
                    timestamp = now,
                    metadata = mapOf("count" to quantity.toString(), "type" to packageType)
                ).toMap()
            )

            batch.commit().await()
            Result.success(generated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun adminObserveAllCodes(): Flow<List<PremiumCode>> = callbackFlow {
        val db = firestore
        if (db == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = db.collection("premium_codes")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "adminObserveAllCodes error: ${error.message}")
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.map { doc ->
                    PremiumCode.fromMap(doc.id, doc.data)
                } ?: emptyList()
                trySend(list)
            }

        awaitClose { listener.remove() }
    }

    suspend fun adminRevokeCode(codeId: String): Result<Unit> {
        val uid = currentUserId ?: return Result.failure(Exception("Sesi login berakhir."))
        val db = firestore ?: return Result.failure(Exception("Firestore belum diinisialisasi."))

        if (!checkIsAdmin()) {
            return Result.failure(SecurityException("Otorisasi ditolak: Membutuhkan role ADMIN."))
        }

        return try {
            db.collection("premium_codes").document(codeId).update("status", "REVOKED").await()
            db.collection("audit_logs").document().set(
                AuditLog(
                    actorId = uid,
                    actorRole = "ADMIN",
                    action = "CODE_REVOKED",
                    targetId = codeId
                ).toMap()
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun adminObserveAllUsers(): Flow<List<User>> = callbackFlow {
        val db = firestore
        if (db == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = db.collection("users")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "adminObserveAllUsers error: ${error.message}")
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.map { doc ->
                    User.fromMap(doc.id, doc.data)
                } ?: emptyList()
                trySend(list)
            }

        awaitClose { listener.remove() }
    }

    suspend fun adminRevokeUserEntitlement(targetUserId: String): Result<Unit> {
        val uid = currentUserId ?: return Result.failure(Exception("Sesi login berakhir."))
        val db = firestore ?: return Result.failure(Exception("Firestore belum diinisialisasi."))

        if (!checkIsAdmin()) {
            return Result.failure(SecurityException("Otorisasi ditolak."))
        }

        return try {
            val batch = db.batch()
            val entRef = db.collection("user_entitlements").document(targetUserId)
            batch.update(entRef, "status", "REVOKED")
            val userRef = db.collection("users").document(targetUserId)
            batch.update(userRef, "isPremium", false)
            val auditRef = db.collection("audit_logs").document()
            batch.set(
                auditRef,
                AuditLog(
                    id = auditRef.id,
                    actorId = uid,
                    actorRole = "ADMIN",
                    action = "ENTITLEMENT_REVOKED",
                    targetId = targetUserId
                ).toMap()
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminGrantUserEntitlement(
        targetUserId: String,
        plan: String,
        durationDays: Int,
        customCheck: Boolean
    ): Result<Unit> {
        val uid = currentUserId ?: return Result.failure(Exception("Sesi login berakhir."))
        val db = firestore ?: return Result.failure(Exception("Firestore belum diinisialisasi."))

        if (!checkIsAdmin()) {
            return Result.failure(SecurityException("Otorisasi ditolak."))
        }

        return try {
            val now = System.currentTimeMillis()
            val isPermanent = plan == "PERMANENT" || durationDays == 0
            val expiresAt = if (isPermanent) 0L else now + (durationDays * 24L * 60L * 60L * 1000L)

            val entitlement = UserEntitlement(
                userId = targetUserId,
                plan = plan,
                source = "ADMIN_GRANT",
                startedAt = now,
                expiresAt = expiresAt,
                permanent = isPermanent,
                customCheckEnabled = customCheck || isPermanent,
                remainingSpins = 5,
                status = "ACTIVE"
            )

            val batch = db.batch()
            batch.set(db.collection("user_entitlements").document(targetUserId), entitlement.toMap())
            batch.update(db.collection("users").document(targetUserId), "isPremium", true)
            val auditRef = db.collection("audit_logs").document()
            batch.set(
                auditRef,
                AuditLog(
                    id = auditRef.id,
                    actorId = uid,
                    actorRole = "ADMIN",
                    action = "ENTITLEMENT_ACTIVATED",
                    targetId = targetUserId,
                    metadata = mapOf("plan" to plan, "grantedBy" to uid)
                ).toMap()
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminSavePackage(plan: PackagePlan): Result<Unit> {
        val db = firestore ?: return Result.failure(Exception("Firestore belum diinisialisasi."))
        if (!checkIsAdmin()) return Result.failure(SecurityException("Otorisasi ditolak."))

        return try {
            val docId = plan.id.ifBlank { db.collection("packages").document().id }
            val finalPlan = plan.copy(id = docId)
            db.collection("packages").document(docId).set(finalPlan.toMap()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminDeletePackage(packageId: String): Result<Unit> {
        val db = firestore ?: return Result.failure(Exception("Firestore belum diinisialisasi."))
        if (!checkIsAdmin()) return Result.failure(SecurityException("Otorisasi ditolak."))

        return try {
            db.collection("packages").document(packageId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminSaveSpinReward(reward: SpinReward): Result<Unit> {
        val db = firestore ?: return Result.failure(Exception("Firestore belum diinisialisasi."))
        if (!checkIsAdmin()) return Result.failure(SecurityException("Otorisasi ditolak."))

        return try {
            val docId = reward.id.ifBlank { db.collection("spin_rewards").document().id }
            val finalReward = reward.copy(id = docId)
            db.collection("spin_rewards").document(docId).set(finalReward.toMap()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminDeleteSpinReward(rewardId: String): Result<Unit> {
        val db = firestore ?: return Result.failure(Exception("Firestore belum diinisialisasi."))
        if (!checkIsAdmin()) return Result.failure(SecurityException("Otorisasi ditolak."))

        return try {
            db.collection("spin_rewards").document(rewardId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun adminObservePaymentOrders(): Flow<List<PaymentOrder>> = callbackFlow {
        val db = firestore
        if (db == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = db.collection("payment_orders")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "adminObservePaymentOrders error: ${error.message}")
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.map { doc ->
                    PaymentOrder.fromMap(doc.id, doc.data)
                } ?: emptyList()
                trySend(list)
            }

        awaitClose { listener.remove() }
    }

    suspend fun adminVerifyPaymentOrder(orderId: String, codeId: String): Result<Unit> {
        val uid = currentUserId ?: return Result.failure(Exception("Sesi login berakhir."))
        val db = firestore ?: return Result.failure(Exception("Firestore belum diinisialisasi."))
        if (!checkIsAdmin()) return Result.failure(SecurityException("Otorisasi ditolak."))

        return try {
            db.collection("payment_orders").document(orderId).update(
                mapOf(
                    "status" to "VERIFIED",
                    "assignedCode" to codeId,
                    "verifiedAt" to System.currentTimeMillis(),
                    "verifiedBy" to uid
                )
            ).await()

            db.collection("audit_logs").document().set(
                AuditLog(
                    actorId = uid,
                    actorRole = "ADMIN",
                    action = "PAYMENT_VERIFIED",
                    targetId = orderId,
                    metadata = mapOf("codeId" to codeId)
                ).toMap()
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminRejectPaymentOrder(orderId: String, reason: String): Result<Unit> {
        val uid = currentUserId ?: return Result.failure(Exception("Sesi login berakhir."))
        val db = firestore ?: return Result.failure(Exception("Firestore belum diinisialisasi."))
        if (!checkIsAdmin()) return Result.failure(SecurityException("Otorisasi ditolak."))

        return try {
            db.collection("payment_orders").document(orderId).update(
                mapOf(
                    "status" to "REJECTED",
                    "referenceNote" to reason,
                    "verifiedAt" to System.currentTimeMillis(),
                    "verifiedBy" to uid
                )
            ).await()

            db.collection("audit_logs").document().set(
                AuditLog(
                    actorId = uid,
                    actorRole = "ADMIN",
                    action = "PAYMENT_REJECTED",
                    targetId = orderId,
                    metadata = mapOf("reason" to reason)
                ).toMap()
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun adminObserveAuditLogs(): Flow<List<AuditLog>> = callbackFlow {
        val db = firestore
        if (db == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = db.collection("audit_logs")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "adminObserveAuditLogs error: ${error.message}")
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.map { doc ->
                    AuditLog.fromMap(doc.id, doc.data)
                } ?: emptyList()
                trySend(list)
            }

        awaitClose { listener.remove() }
    }

    private fun getDefaultPackages(): List<PackagePlan> {
        return listOf(
            PackagePlan(
                id = "pkg_trial",
                type = "TRIAL",
                name = "Pulse Trial 7D",
                price = "Rp 15.000",
                durationDays = 7,
                spinLimit = 1,
                customCheck = false,
                features = listOf("Centang Biru Premium", "Akses Chat Lengkap", "1x Kesempatan Spinner"),
                sortOrder = 1
            ),
            PackagePlan(
                id = "pkg_monthly",
                type = "PREMIUM",
                name = "Pulse Pro 30D",
                price = "Rp 45.000",
                durationDays = 30,
                spinLimit = 3,
                customCheck = false,
                features = listOf("Centang Biru Verified", "Chat Prioritas Realtime", "3x Kesempatan Spinner", "Tanpa Batas Search"),
                sortOrder = 2
            ),
            PackagePlan(
                id = "pkg_custom_check",
                type = "CUSTOM_CHECK",
                name = "Custom Check Add-on",
                price = "Rp 65.000",
                durationDays = 30,
                spinLimit = 5,
                customCheck = true,
                features = listOf("Akses Penuh Custom Check", "Bebas Pilih Warna & Style Centang", "5x Kesempatan Spinner", "Neon Accent Theme"),
                sortOrder = 3
            ),
            PackagePlan(
                id = "pkg_permanent",
                type = "PERMANENT",
                name = "Pulse Permanent Lifetime",
                price = "Rp 199.000",
                durationDays = 0,
                spinLimit = 15,
                customCheck = true,
                features = listOf("Aktif Selamanya Tanpa Expired", "Full Custom Check Style", "15x Kesempatan Spinner", "Badge Verified Gold", "Dukungan Prioritas Admin"),
                sortOrder = 4
            )
        )
    }

    private fun getDefaultRewards(): List<SpinReward> {
        return listOf(
            SpinReward(id = "rew_zonk", name = "ZONK / Coba Lagi", type = "NO_REWARD", value = "0", probability = 0.35, colorHex = "#64748B"),
            SpinReward(id = "rew_trial_1d", name = "Trial 1 Hari", type = "TRIAL", value = "1", probability = 0.20, colorHex = "#38BDF8"),
            SpinReward(id = "rew_trial_3d", name = "Trial 3 Hari", type = "TRIAL", value = "3", probability = 0.15, colorHex = "#00D2FF"),
            SpinReward(id = "rew_custom_check_7d", name = "Custom Check 7 Hari", type = "CUSTOM_CHECK", value = "7", probability = 0.12, colorHex = "#EC4899"),
            SpinReward(id = "rew_extra_spin", name = "+1 Extra Spin", type = "EXTRA_SPIN", value = "1", probability = 0.10, colorHex = "#F59E0B"),
            SpinReward(id = "rew_premium_7d", name = "Premium 7 Hari", type = "EXTRA_DURATION", value = "7", probability = 0.06, colorHex = "#10B981"),
            SpinReward(id = "rew_jackpot_perm", name = "Jackpot Permanent", type = "JACKPOT_PERMANENT", value = "PERMANENT", probability = 0.02, colorHex = "#EAB308")
        )
    }

    suspend fun updatePresence(isOnline: Boolean) {
        val uid = currentUserId ?: return
        val db = firestore ?: return
        try {
            db.collection("users").document(uid).update(
                mapOf(
                    "isOnline" to isOnline,
                    "lastSeen" to System.currentTimeMillis()
                )
            ).await()
        } catch (e: Exception) {
            Log.w(TAG, "updatePresence error: ${e.message}")
        }
    }

    suspend fun updateFcmToken(token: String) {
        val uid = currentUserId ?: return
        val db = firestore ?: return
        try {
            db.collection("users").document(uid).update("fcmToken", token).await()
        } catch (e: Exception) {
            Log.w(TAG, "updateFcmToken error: ${e.message}")
        }
    }
}
