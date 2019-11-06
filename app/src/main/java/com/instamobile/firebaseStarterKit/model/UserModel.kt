package com.instamobile.firebaseStarterKit.model

data class UserModel(
    var userID: String = "",
    var profilePictureURL: String = "",
    var phoneNumber: String = "",
    var active: Boolean = false,
    var firstName: String = "",
    var lastName: String = "",
    var email: String = "",
    var fcmToken: String = "",
    var selected: Boolean = false,
    var userName: String = ""
)