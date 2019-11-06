package com.instamobile.firebaseStarterKit.ui.fragment.signUp

import android.app.Application
import android.net.Uri
import android.util.Log
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.instamobile.firebaseStarterKit.model.UserModel
import com.instamobile.firebaseStarterKit.utils.MyApplication
import com.instamobile.ui.fragment.onBoarding.walkthroughactivity.R

class SignUpViewModel(private val myApplication: Application) : AndroidViewModel(myApplication) {
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseFirestore.getInstance()

    var fullname: String = ""
    var phoneNumber: String = ""
    var email: String = ""
    var password: String = ""
    var imageUri: Uri? = null

    private val _fullNameError = MutableLiveData<String>()
    val fullNameError: LiveData<String>
        get() = _fullNameError

    private val _phoneNumberError = MutableLiveData<String>()
    val phoneNumberError: LiveData<String>
        get() = _phoneNumberError

    private val _emailError = MutableLiveData<String>()
    val emailError: LiveData<String>
        get() = _emailError

    private val _passwordError = MutableLiveData<String>()
    val passwordError: LiveData<String>
        get() = _passwordError

    private val _buttonEnabled = MutableLiveData<Boolean>().apply { value = true }
    val buttonEnabled: LiveData<Boolean>
        get() = _buttonEnabled

    private val _navigateToHome = MutableLiveData<Boolean>().apply { value = false }
    val navigateToHome: LiveData<Boolean>
        get() = _navigateToHome

    private val _signUpError = MutableLiveData<String>()
    val signUpError: LiveData<String>
        get() = _signUpError

    fun createUser() {
        when {
            fullname.isEmpty() -> _passwordError.value =
                myApplication.getString(R.string.name_required_error)
            phoneNumber.isEmpty() -> _phoneNumberError.value =
                myApplication.getString(R.string.phone_required_error)
            email.isEmpty() -> _emailError.value =
                myApplication.getString(R.string.email_required_error)
            password.isEmpty() -> _passwordError.value =
                myApplication.getString(R.string.password_required_error)
            !Patterns.PHONE.matcher(phoneNumber).matches() -> _phoneNumberError.value =
                myApplication.getString(R.string.malformed_phone_error)
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> _emailError.value =
                myApplication.getString(R.string.malformed_email_error)
            password.length < 6 -> _passwordError.value =
                myApplication.getString(R.string.short_password_error)
            else -> {
                _buttonEnabled.value = false
                //create user without image

                auth.createUserWithEmailAndPassword(email, password).addOnSuccessListener {

                    if (imageUri == null) {
                        createUserWithoutImage()
                    } else {
                        createUserWithImage()
                    }
                }.addOnFailureListener {
                    it.printStackTrace()
                    _signUpError.value = it.message
                    _buttonEnabled.value = true

                }
            }
        }
    }

    private fun createUserWithoutImage() {
        val userid = auth.currentUser!!.uid
        val items = HashMap<String, Any>()
        items["email"] = email
        items["firstName"] = fullname
        items["lastName"] = ""
        items["userName"] = ""
        items["phoneNumber"] = phoneNumber
        items["userID"] = userid
        items["profilePictureURL"] = ""
        items["active"] = true
        saveUserToDatabase(auth.currentUser!!, items)

    }

    private fun createUserWithImage() {
        val data = FirebaseStorage.getInstance().reference
        val photoRef = data.child("images/" + auth.currentUser!!.uid + ".png")
        photoRef.putFile(this.imageUri!!).addOnProgressListener {
        }.continueWithTask { task ->
            if (!task.isSuccessful) {
                throw task.exception!!
            }
            photoRef.downloadUrl
        }.addOnSuccessListener { downloadUri ->

            val userId = auth.currentUser!!.uid
            val items = HashMap<String, Any>()
            items["email"] = email
            items["firstName"] = fullname
            items["lastName"] = ""
            items["userName"] = ""
            items["phoneNumber"] = phoneNumber
            items["userID"] = userId
            items["profilePictureURL"] = downloadUri.toString()
            items["active"] = true
            saveUserToDatabase(auth.currentUser!!, items)
        }
    }

    private fun saveUserToDatabase(user: FirebaseUser, items: HashMap<String, Any>) {

        database.collection("users").document(user.uid).set(items)
            .addOnSuccessListener {
                val userModel = UserModel()
                userModel.userID = user.uid
                userModel.email = items["email"].toString()
                userModel.firstName = items["firstName"].toString()
                userModel.lastName = items["lastName"].toString()
                userModel.userName = items["userName"].toString()
                userModel.profilePictureURL = items["profilePictureURL"].toString()
                userModel.active = true
                MyApplication.currentUser = userModel
                Log.d("SignUp state", "save user:success")
                _navigateToHome.value = true
            }.addOnFailureListener {
                _signUpError.value = it.message
                _buttonEnabled.value = true
            }
    }

    fun doneNavigating() {
        _navigateToHome.value = false
    }
}