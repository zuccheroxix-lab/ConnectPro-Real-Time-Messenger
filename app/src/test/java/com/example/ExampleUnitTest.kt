package com.example

import com.example.model.Conversation
import com.example.model.Message
import com.example.model.MessageStatus
import com.example.model.Subscription
import com.example.model.User
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testMessageStatusCalculations() {
        // 1 tick: sent to server (createdAt > 0, deliveredAt is null)
        val sentMessage = Message(
            id = "msg1",
            conversationId = "conv1",
            senderId = "user1",
            text = "Hello PulseChat",
            createdAt = 1000L,
            deliveredAt = null,
            readAt = null
        )
        assertEquals(MessageStatus.SENT, sentMessage.effectiveStatus)

        // 2 ticks: delivered (deliveredAt > 0, readAt is null)
        val deliveredMessage = sentMessage.copy(deliveredAt = 2000L)
        assertEquals(MessageStatus.DELIVERED, deliveredMessage.effectiveStatus)

        // 2 blue ticks: read by receiver (readAt > 0)
        val readMessage = deliveredMessage.copy(readAt = 3000L)
        assertEquals(MessageStatus.READ, readMessage.effectiveStatus)
    }

    @Test
    fun testUserSerialization() {
        val user = User(
            id = "u123",
            phoneNumber = "+6281234567890",
            username = "alex_dev",
            displayName = "Alex Dev",
            bio = "Realtime chat developer",
            isPremium = true
        )

        val map = user.toMap()
        assertEquals("u123", map["id"])
        assertEquals("alex_dev", map["username"])
        assertEquals(true, map["isPremium"])

        val parsed = User.fromMap("u123", map)
        assertEquals(user.id, parsed.id)
        assertEquals(user.username, parsed.username)
        assertEquals(user.displayName, parsed.displayName)
        assertEquals(user.isPremium, parsed.isPremium)
    }

    @Test
    fun testSubscriptionActiveValidation() {
        val now = System.currentTimeMillis()

        val activeSub = Subscription(
            id = "sub1",
            userId = "u123",
            plan = "monthly_pro",
            status = "active",
            provider = "google_play_billing",
            transactionId = "GPA.1234-5678",
            startedAt = now - 10000L,
            expiresAt = now + 1000000L // in future
        )
        assertTrue(activeSub.isCurrentlyActive)

        val expiredSub = activeSub.copy(
            expiresAt = now - 5000L // in past
        )
        assertFalse(expiredSub.isCurrentlyActive)

        val cancelledSub = activeSub.copy(
            status = "cancelled"
        )
        assertFalse(cancelledSub.isCurrentlyActive)
    }

    @Test
    fun testConversationUnreadCounter() {
        val conv = Conversation(
            id = "conv_1",
            members = listOf("user1", "user2"),
            unreadCounts = mapOf("user1" to 0L, "user2" to 3L),
            lastMessageText = "See you tomorrow!"
        )

        val map = conv.toMap()
        val parsed = Conversation.fromMap("conv_1", map)
        assertEquals(0L, parsed.unreadCounts["user1"])
        assertEquals(3L, parsed.unreadCounts["user2"])
        assertEquals("See you tomorrow!", parsed.lastMessageText)
    }

    @Test
    fun testUserEntitlementPermanentAndTrial() {
        val now = System.currentTimeMillis()

        val permanentEntitlement = com.example.model.UserEntitlement(
            userId = "user_p",
            plan = "PERMANENT",
            permanent = true,
            status = "ACTIVE"
        )
        assertTrue(permanentEntitlement.isCurrentlyActive)
        assertTrue(permanentEntitlement.isPremiumTier)

        val expiredTrial = com.example.model.UserEntitlement(
            userId = "user_t",
            plan = "TRIAL",
            permanent = false,
            expiresAt = now - 1000L,
            status = "ACTIVE"
        )
        assertFalse(expiredTrial.isCurrentlyActive)
        assertFalse(expiredTrial.isPremiumTier)
    }

    @Test
    fun testPremiumCodeRedeemability() {
        val now = System.currentTimeMillis()

        val validCode = com.example.model.PremiumCode(
            codeId = "c1",
            code = "PULSE-PRO-1234",
            packageType = "PREMIUM",
            status = "AVAILABLE",
            expiresAt = now + 100000L
        )
        assertTrue(validCode.isRedeemable)

        val usedCode = validCode.copy(status = "USED", usedBy = "user_1")
        assertFalse(usedCode.isRedeemable)

        val expiredCode = validCode.copy(expiresAt = now - 500L)
        assertFalse(expiredCode.isRedeemable)
    }
}
