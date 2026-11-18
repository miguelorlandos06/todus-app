package cu.todus.android.ui.nav

/**
 * All navigation routes. Routes that take a phone argument use {phone} as a path parameter.
 */
sealed class Route(val path: String) {
    data object Welcome : Route("welcome")
    data object Phone : Route("phone")
    data object ProfileInitial : Route("profile-initial")
    data object Home : Route("home")
    data object Contacts : Route("contacts")
    data object Conversation : Route("conversation/{phone}") {
        const val ARG = "phone"
        fun build(phone: String) = "conversation/$phone"
    }
    data object MyProfile : Route("my-profile")
    data object EditProfile : Route("edit-profile")
    data object ChangeTodusId : Route("change-todus-id")
    data object UserProfile : Route("user-profile/{phone}") {
        const val ARG = "phone"
        fun build(phone: String) = "user-profile/$phone"
    }
}
