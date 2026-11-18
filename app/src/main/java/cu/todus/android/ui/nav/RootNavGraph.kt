package cu.todus.android.ui.nav

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cu.todus.android.ui.screens.*
import cu.todus.android.ui.viewmodel.AuthViewModel
import cu.todus.android.ui.viewmodel.ConversationViewModel
import cu.todus.android.ui.viewmodel.HomeViewModel
import cu.todus.android.ui.viewmodel.ProfileViewModel

/**
 * Root navigation graph. The start destination is decided by whether the user is already
 * logged in (i.e. a session token exists in DataStore).
 */
@Composable
fun RootNavGraph() {
    val nav = rememberNavController()
    val authVm: AuthViewModel = hiltViewModel()
    val isLoggedIn by authVm.sessionStore.isLoggedIn.collectAsState(initial = false)

    val start = if (isLoggedIn) Route.Home.path else Route.Welcome.path

    NavHost(
        navController = nav,
        startDestination = start,
        enterTransition = { slideInHorizontally(tween(220)) { it / 4 } + fadeIn(tween(220)) },
        exitTransition = { fadeOut(tween(180)) },
        popEnterTransition = { slideInHorizontally(tween(220)) { -it / 4 } + fadeIn(tween(220)) },
        popExitTransition = { slideOutHorizontally(tween(220)) { it / 4 } + fadeOut(tween(180)) },
    ) {
        composable(Route.Welcome.path) {
            WelcomeScreen(onAccept = {
                nav.navigate(Route.Phone.path) {
                    popUpTo(Route.Welcome.path) { inclusive = true }
                }
            })
        }
        composable(Route.Phone.path) {
            PhoneScreen(
                onContinue = { nav.navigate(Route.ProfileInitial.path) },
                vm = authVm,
            )
        }
        composable(Route.ProfileInitial.path) {
            ProfileInitialScreen(
                onStart = {
                    nav.navigate(Route.Home.path) {
                        popUpTo(Route.ProfileInitial.path) { inclusive = true }
                    }
                },
                vm = authVm,
            )
        }
        composable(Route.Home.path) {
            HomeScreen(
                vm = hiltViewModel<HomeViewModel>(),
                onNewChat = { nav.navigate(Route.Contacts.path) },
                onChatClick = { phone -> nav.navigate(Route.Conversation.build(phone)) },
                onMyProfile = { nav.navigate(Route.MyProfile.path) },
            )
        }
        composable(Route.Contacts.path) {
            ContactsScreen(
                vm = hiltViewModel<HomeViewModel>(),
                onBack = { nav.popBackStack() },
                onContactClick = { phone ->
                    nav.navigate(Route.Conversation.build(phone)) {
                        popUpTo(Route.Home.path)
                    }
                },
            )
        }
        composable(
            Route.Conversation.path,
            arguments = listOf(navArgument(Route.Conversation.ARG) { type = NavType.StringType })
        ) { entry ->
            val phone = entry.arguments?.getString(Route.Conversation.ARG).orEmpty()
            ConversationScreen(
                peerPhone = phone,
                vm = hiltViewModel<ConversationViewModel>(),
                onBack = { nav.popBackStack() },
                onProfileClick = { nav.navigate(Route.UserProfile.build(phone)) },
            )
        }
        composable(Route.MyProfile.path) {
            MyProfileScreen(
                vm = hiltViewModel<ProfileViewModel>(),
                onBack = { nav.popBackStack() },
                onEdit = { nav.navigate(Route.EditProfile.path) },
            )
        }
        composable(Route.EditProfile.path) {
            EditProfileScreen(
                vm = hiltViewModel<ProfileViewModel>(),
                onBack = { nav.popBackStack() },
                onChangeTodusId = { nav.navigate(Route.ChangeTodusId.path) },
                onSaved = { nav.popBackStack() },
            )
        }
        composable(Route.ChangeTodusId.path) {
            ChangeTodusIdScreen(
                vm = hiltViewModel<ProfileViewModel>(),
                onBack = { nav.popBackStack() },
                onChanged = { nav.popBackStack() },
            )
        }
        composable(
            Route.UserProfile.path,
            arguments = listOf(navArgument(Route.UserProfile.ARG) { type = NavType.StringType })
        ) { entry ->
            val phone = entry.arguments?.getString(Route.UserProfile.ARG).orEmpty()
            UserProfileScreen(
                phone = phone,
                vm = hiltViewModel<ProfileViewModel>(),
                onBack = { nav.popBackStack() },
            )
        }
    }
}
