package com.example.services

import android.content.Context
import com.example.model.Subscription
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

sealed class PaymentResult {
    data class Success(val transactionId: String, val message: String) : PaymentResult()
    data class ConfigurationRequired(
        val providerName: String,
        val requiredCredentials: List<String>,
        val instructions: String
    ) : PaymentResult()
    data class Error(val message: String) : PaymentResult()
}

data class PlanDetail(
    val id: String,
    val name: String,
    val price: String,
    val billingPeriod: String,
    val discountBadge: String? = null,
    val features: List<String>
)

object PaymentBillingService {

    val AVAILABLE_PLANS = listOf(
        PlanDetail(
            id = "pulse_monthly_pro",
            name = "Pulse Pro Monthly",
            price = "Rp 39.000 / month",
            billingPeriod = "monthly",
            features = listOf(
                "Exclusive Verified Blue Checkmark badge on profile & chats",
                "Instant real-time read receipts & priority status",
                "Custom Neon Accent chat themes",
                "Unlimited user search & extended bio profile",
                "Cloud backup for media & conversation history"
            )
        ),
        PlanDetail(
            id = "pulse_annual_pro",
            name = "Pulse Pro Annual",
            price = "Rp 349.000 / year",
            billingPeriod = "yearly",
            discountBadge = "SAVE 25%",
            features = listOf(
                "All Monthly Pro perks included",
                "25% discount compared to monthly plan",
                "Early access to upcoming features",
                "Priority customer support"
            )
        )
    )

    /**
     * Checks if the Google Play Billing / Merchant Backend credentials have been connected.
     * In compliance with security guidelines, private service account keys are NOT bundled in the APK.
     */
    fun isPaymentProviderConfigured(): Boolean {
        // Live merchant verification requires Google Play Console merchant setup
        // and server-side Cloud Function webhook to verify Google Play Purchase Tokens.
        return false // Defaults to requiring developer/merchant configuration
    }

    /**
     * Required configuration specifications to activate Google Play In-App Billing or Payment Gateway.
     */
    fun getConfigurationDetails(): PaymentResult.ConfigurationRequired {
        return PaymentResult.ConfigurationRequired(
            providerName = "Google Play In-App Billing & Server Verification",
            requiredCredentials = listOf(
                "Google Play Console Developer Account",
                "Merchant Account linked to Google Play Console",
                "Subscription In-App Products: 'pulse_monthly_pro' & 'pulse_annual_pro'",
                "Google Play Developer API Service Account (configured on backend)",
                "Backend Webhook / Firebase Cloud Function to verify purchaseToken against Google Play Developer API"
            ),
            instructions = "To enable live payments in production:\n" +
                    "1. Register the application package on Google Play Console.\n" +
                    "2. Navigate to Monetization -> Subscriptions and create products 'pulse_monthly_pro' and 'pulse_annual_pro'.\n" +
                    "3. Set up a secure server-side webhook (e.g. Firebase Cloud Function) that validates purchase tokens with Google Play Developer API.\n" +
                    "4. Upon successful validation, the backend updates the Firestore /subscriptions/{userId} record with status='active' and valid expiresAt."
        )
    }

    /**
     * Initiates purchase transaction flow.
     * As mandated: Never simulates success if backend/gateway is not configured.
     */
    suspend fun initiateSubscription(
        context: Context,
        userId: String,
        planId: String
    ): PaymentResult {
        if (!isPaymentProviderConfigured()) {
            return getConfigurationDetails()
        }

        return PaymentResult.Error("Payment provider connection timeout. Please ensure network connectivity and Google Play Services.")
    }
}
