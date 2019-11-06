package com.instamobile.firebaseStarterKit.ui.fragment.authHost

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AuthViewModel : ViewModel() {
    private val _navigateToLogin = MutableLiveData<Boolean>().apply { value = false }
    val navigateToLogin: LiveData<Boolean>
        get() = _navigateToLogin

    private val _navigateToSignUp = MutableLiveData<Boolean>().apply { value = false }
    val navigateToSignUp: LiveData<Boolean>
        get() = _navigateToSignUp

    fun startNavigationToLogin() {
        _navigateToLogin.value = true
    }

    fun doneNavigationToLogin() {
        _navigateToLogin.value = false
    }

    fun navigateToSignUp() {
        _navigateToSignUp.value = true
    }

    fun doneNavigationToSignUp() {
        _navigateToSignUp.value = false
    }
}