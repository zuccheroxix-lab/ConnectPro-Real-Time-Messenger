package com.example.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.FirebaseRepository
import com.example.ui.screens.*
import kotlinx.coroutines.launch
import java.net.URLDecoder

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    initialRoute: String = Routes.SPLASH,
    initialConversationId: String? = null
) {
    NavHost(
        navController = navController,
        startDestination = initialRoute,
        modifier = modifier
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onNavigate = { destination ->
                    navController.navigate(destination) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                onNavigateToOtp = { verificationId, phoneNumber ->
                    navController.navigate(Routes.otp(verificationId, phoneNumber))
                },
                onNavigateToHome = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.OTP,
            arguments = listOf(
                navArgument("verificationId") { type = NavType.StringType },
                navArgument("phoneNumber") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val rawVerId = backStackEntry.arguments?.getString("verificationId") ?: ""
            val rawPhone = backStackEntry.arguments?.getString("phoneNumber") ?: ""
            val verificationId = try { URLDecoder.decode(rawVerId, "UTF-8") } catch (e: Exception) { rawVerId }
            val phoneNumber = try { URLDecoder.decode(rawPhone, "UTF-8") } catch (e: Exception) { rawPhone }

            OtpScreen(
                verificationId = verificationId,
                phoneNumber = phoneNumber,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCreateProfile = {
                    navController.navigate(Routes.CREATE_PROFILE) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.CREATE_PROFILE) {
            CreateProfileScreen(
                onProfileCreated = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.CREATE_PROFILE) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToChat = { conversationId, otherUserId ->
                    navController.navigate(Routes.chat(conversationId, otherUserId))
                },
                onNavigateToSearch = { navController.navigate(Routes.SEARCH) },
                onNavigateToProfile = { userId -> navController.navigate(Routes.userProfile(userId)) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                onNavigateToPremium = { navController.navigate(Routes.PREMIUM) }
            )
        }

        composable(Routes.SEARCH) {
            SearchScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToChat = { conversationId, otherUserId ->
                    navController.navigate(Routes.chat(conversationId, otherUserId))
                },
                onNavigateToProfile = { userId -> navController.navigate(Routes.userProfile(userId)) }
            )
        }

        composable(
            route = Routes.CHAT,
            arguments = listOf(
                navArgument("conversationId") { type = NavType.StringType },
                navArgument("otherUserId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId") ?: ""
            val otherUserId = backStackEntry.arguments?.getString("otherUserId") ?: ""

            ChatScreen(
                conversationId = conversationId,
                otherUserId = otherUserId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToProfile = { userId -> navController.navigate(Routes.userProfile(userId)) }
            )
        }

        composable(
            route = Routes.USER_PROFILE,
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""

            UserProfileScreen(
                userId = userId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToChat = { conversationId, otherUserId ->
                    navController.navigate(Routes.chat(conversationId, otherUserId))
                }
            )
        }

        composable(Routes.PREMIUM) {
            val scope = rememberCoroutineScope()
            val repository = remember { FirebaseRepository.getInstance() }

            PremiumScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSubscription = { navController.navigate(Routes.SUBSCRIPTION) },
                onNavigateToRedeem = { navController.navigate(Routes.REDEEM_CODE) },
                onNavigateToSpinner = { navController.navigate(Routes.SPINNER) },
                onNavigateToCustomCheck = { navController.navigate(Routes.CUSTOM_CHECK) },
                onChatAdminWithPackage = { pkgId, pkgName ->
                    scope.launch {
                        val adminId = repository.getAdminUserId()
                        if (adminId != null) {
                            val convResult = repository.getOrCreateConversationId(adminId)
                            convResult.fold(
                                onSuccess = { convId -> navController.navigate(Routes.chat(convId, adminId)) },
                                onFailure = { navController.navigate(Routes.SEARCH) }
                            )
                        } else {
                            navController.navigate(Routes.SEARCH)
                        }
                    }
                }
            )
        }

        composable(Routes.SUBSCRIPTION) {
            SubscriptionScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.REDEEM_CODE) {
            val scope = rememberCoroutineScope()
            val repository = remember { FirebaseRepository.getInstance() }

            RedeemCodeScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPremium = { navController.navigate(Routes.PREMIUM) },
                onChatAdmin = {
                    scope.launch {
                        val adminId = repository.getAdminUserId()
                        if (adminId != null) {
                            val convResult = repository.getOrCreateConversationId(adminId)
                            convResult.fold(
                                onSuccess = { convId -> navController.navigate(Routes.chat(convId, adminId)) },
                                onFailure = { navController.navigate(Routes.SEARCH) }
                            )
                        } else {
                            navController.navigate(Routes.SEARCH)
                        }
                    }
                }
            )
        }

        composable(Routes.SPINNER) {
            SpinnerScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToHistory = { navController.navigate(Routes.SPIN_HISTORY) },
                onNavigateToCustomCheck = { navController.navigate(Routes.CUSTOM_CHECK) },
                onNavigateToRedeem = { navController.navigate(Routes.REDEEM_CODE) }
            )
        }

        composable(Routes.SPIN_HISTORY) {
            SpinHistoryScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.CUSTOM_CHECK) {
            val scope = rememberCoroutineScope()
            val repository = remember { FirebaseRepository.getInstance() }

            CustomCheckScreen(
                onNavigateBack = { navController.popBackStack() },
                onChatAdmin = {
                    scope.launch {
                        val adminId = repository.getAdminUserId()
                        if (adminId != null) {
                            val convResult = repository.getOrCreateConversationId(adminId)
                            convResult.fold(
                                onSuccess = { convId -> navController.navigate(Routes.chat(convId, adminId)) },
                                onFailure = { navController.navigate(Routes.SEARCH) }
                            )
                        } else {
                            navController.navigate(Routes.SEARCH)
                        }
                    }
                },
                onNavigateToRedeem = { navController.navigate(Routes.REDEEM_CODE) }
            )
        }

        composable(Routes.ADMIN_PANEL) {
            AdminPanelScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAccount = { navController.navigate(Routes.ACCOUNT_SETTINGS) },
                onNavigateToNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                onNavigateToBlocked = { navController.navigate(Routes.BLOCKED_USERS) },
                onNavigateToPremium = { navController.navigate(Routes.PREMIUM) },
                onNavigateToRedeem = { navController.navigate(Routes.REDEEM_CODE) },
                onNavigateToSpinner = { navController.navigate(Routes.SPINNER) },
                onNavigateToCustomCheck = { navController.navigate(Routes.CUSTOM_CHECK) },
                onNavigateToAdminPanel = { navController.navigate(Routes.ADMIN_PANEL) },
                onLoggedOut = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.BLOCKED_USERS) {
            BlockedUsersScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.NOTIFICATIONS) {
            NotificationSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.ACCOUNT_SETTINGS) {
            AccountSettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onAccountDeleted = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
