# PulseChat - Realtime Verified Direct Messaging App

PulseChat is an original, modern Android messaging application built entirely with Kotlin, Jetpack Compose (Material 3), and Firebase Cloud Backend (Auth, Cloud Firestore, Cloud Messaging).

---

## Key Features & Architecture

### 1. Carrier Phone Authentication & Real SMS OTP
- Primary registration and login via mobile phone number with country code picker.
- Live carrier verification through Firebase Phone Authentication (`PhoneAuthProvider`).
- 60-second countdown for resend cooldown.
- Protection against brute-force attacks: 5-attempt limit with automatic session locking.
- Expiration checks and server-side verification.

### 2. User Profiles & Unique Username Validation
- Mandatory profile creation upon initial registration.
- Unique username (`@username`) validated directly against Cloud Firestore to prevent duplicates.
- Display name, bio, and profile avatar customization.
- Permanent account deletion with complete data cleanup.

### 3. Home & Realtime Chat List
- Real-time conversation list ordered by recent message timestamp.
- Partner avatar, display name, username, last message snippet, and timestamp.
- Unread message counter badge.
- Live online indicator and presence tracking.
- Search conversations filter and New Chat FAB.

### 4. Real User Search
- Search users in real time directly from Cloud Firestore using prefix queries.
- Instant conversation initiation from search results.

### 5. Realtime 1-on-1 Chat
- Instant bidirectional messaging powered by Cloud Firestore real-time snapshot listeners.
- Message reply preview with quote reference.
- Copy message text to clipboard.
- Soft-delete own messages for both participants.
- Auto-scroll to latest messages.

### 6. Authentic Message Status & Ticks System
- **1 Tick (`✓`)**: Message stored on the server.
- **2 Ticks (`✓✓`)**: Message delivered to receiver's device (`deliveredAt`).
- **2 Blue Ticks (`✓✓` in Cyan/Blue)**: Message actively read by receiver (`readAt`).
- Status is updated and synchronized via Cloud Firestore in real time.

### 7. Free & Premium Subscription System
- **Free Account**: Full access to core 1-on-1 realtime messaging.
- **Premium Account**:
  - Exclusive Verified Blue Badge (`VerifiedBadge`) displayed on profile and conversation headers.
  - Priority delivery indicator.
  - Custom Neon Accent chat theme tokens.
  - Unlimited user search.
- Server-side subscription validation: checks `/subscriptions/{userId}` for `status == "active"` and `expiresAt > currentTimeMillis()`. Automatic expiry handling.

### 8. Payment Provider Architecture
- Integrated Google Play In-App Billing / Merchant Backend structure (`PaymentBillingService`).
- Zero simulated transactions. If Google Play credentials or backend verification webhooks are not yet connected, the application provides clear setup guidance.

### 9. Push Notifications
- Integrated `PulseFirebaseMessagingService` with Android Notification Channel (`pulse_messages`).
- Supports opening specific conversations directly from push notifications.

### 10. Contact Blocking
- Block and unblock contacts with database persistence.
- Blocked contacts cannot send messages or view active presence.

---

## Configuration Guide

### 1. Firebase Setup (Live SMS OTP, Firestore, FCM)
1. Create a project at [Firebase Console](https://console.firebase.google.com/).
2. Add an Android app with package name: `com.aistudio.pulsechat.kvyqtz`.
3. Add your SHA-1 and SHA-256 fingerprint in Firebase Project Settings (required for Phone Auth & SafetyNet/reCAPTCHA).
4. In **Authentication -> Sign-in method**, enable **Phone**.
   - *(Optional for testing)*: Under "Phone numbers for testing", add `+6281234567890` with test code `123456` to test without carrier SMS fees.
5. In **Firestore Database**, create a database and publish the contents of `firestore.rules`.
6. Download `google-services.json` and place it in the `app/` directory.

### 2. Google Play Billing Setup
1. Publish the application on Google Play Console.
2. In Monetization -> Subscriptions, create products:
   - `pulse_monthly_pro`
   - `pulse_annual_pro`
3. Configure a server-side Cloud Function webhook to verify purchase tokens against the Google Play Developer API and update `/subscriptions/{userId}`.

---

## Building the APKs

To build the APKs locally or in CI:

```bash
# Build Debug APK
gradle :app:assembleDebug

# Build Release APK
gradle :app:assembleRelease
```

Build outputs:
- **Debug APK**: `app/build/outputs/apk/debug/app-debug.apk`
- **Release APK**: `app/build/outputs/apk/release/app-release-unsigned.apk` (or signed if upload key provided)

---

## CI / CD
GitHub Actions workflow is located at `.github/workflows/android-build.yml` to automatically build APKs and upload them as workflow artifacts upon every push.
