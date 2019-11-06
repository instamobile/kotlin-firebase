package com.instamobile.firebaseStarterKit.ui.fragment.login

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.facebook.GraphRequest
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.instamobile.firebaseStarterKit.model.UserModel
import com.instamobile.firebaseStarterKit.utils.FirestoreUtil
import com.instamobile.firebaseStarterKit.utils.MyApplication
import org.json.JSONObject

class LoginViewModel : ViewModel() {
    private var mAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private var db: FirebaseFirestore = FirebaseFirestore.getInstance()

    var username: String = ""
        set(value) {
            field = value
            validateInput()
        }

    var password: String = ""
        set(value) {
            field = value
            validateInput()
        }

    private val _buttonEnabled = MutableLiveData<Boolean>()
    val buttonEnabled: LiveData<Boolean>
        get() = _buttonEnabled

    private val _progress = MutableLiveData<Boolean>()
    val progress: LiveData<Boolean>
        get() = _progress

    private val _errorString = MutableLiveData<String>()
    val errorString: LiveData<String>
        get() = _errorString

    private fun validateInput() {
        _buttonEnabled.value = !(username.isEmpty() || password.isEmpty())
    }

    private val _navigateToHome = MutableLiveData<Boolean>()
    val navigateToHome: LiveData<Boolean>
        get() = _navigateToHome

    fun login() {
        onStartLoading()
        mAuth.signInWithEmailAndPassword(username, password).addOnCompleteListener {
            if (it.isSuccessful) {
                getFirebaseUserData()
            } else {
                onFinishLoading()
                _errorString.value = it.exception?.message
            }
        }

    }

    private fun getFirebaseUserData() {
        val ref = db.collection("users").document(mAuth.currentUser!!.uid)
        ref.get().addOnSuccessListener {
            val userInfo = it.toObject(UserModel::class.java)
            MyApplication.currentUser = userInfo
            MyApplication.currentUser!!.active = true
            FirestoreUtil.updateUser(MyApplication.currentUser!!) {

            }
            onFinishLoading()
            startHomeNavigation()
        }.addOnFailureListener {
            onFinishLoading()
            _errorString.value = it.message
        }

    }

    fun handleFacebookToken(result: LoginResult) {
        onStartLoading()
        val credential =
            FacebookAuthProvider.getCredential(result.accessToken.token.toString())
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = task.result!!.user!!.uid
                    db.collection("users").document(userId)
                        .get()
                        .addOnCompleteListener {
                            if (it.isSuccessful) {
                                val document = it.result

                                if (document!!["email"] != null) {
                                    onFinishLoading()
                                    val userInfo = document.toObject(UserModel::class.java)
                                    MyApplication.currentUser = userInfo
                                    startHomeNavigation()
                                } else {
                                    createUser(result)
                                }
                            } else {
                                onFinishLoading()
                                _errorString.value = it.exception?.message
                            }
                        }

                } else {
                    onFinishLoading()
                    _errorString.value = "Authentication failed."
                }
            }
    }

    private fun createUser(loginResult: LoginResult) {

        val userId = mAuth.currentUser!!.uid

        val request = GraphRequest.newMeRequest(loginResult.accessToken) { `object`, response ->
            try {

                val items = HashMap<String, Any>()
                items["email"] = `object`.get("email").toString()
                items["firstName"] = `object`.get("name").toString()
                items["lastName"] = ""
                items["userName"] = ""
                items["phoneNumber"] = "000000"
                items["userID"] = userId
                items["active"] = true
//                items["fcmToken"] = FirebaseInstanceId.getInstance().getToken().toString()

                val picture: JSONObject = `object`.get("picture") as JSONObject
                val data: JSONObject = picture.getJSONObject("data")
                val url = data.getString("url")
                items["profilePictureURL"] = url
                saveSocialUserToFirebase(mAuth.currentUser, items)
            } catch (e: Exception) {
                onFinishLoading()
                _errorString.value = e.message
                e.printStackTrace()
            }
        }

        val parameters = Bundle()
        parameters.putString("fields", "name,email,id,picture.type(large)")
        request.parameters = parameters
        request.executeAsync()
    }

    private fun saveSocialUserToFirebase(
        currentUser: FirebaseUser?,
        items: java.util.HashMap<String, Any>
    ) {
        db.collection("users").document(currentUser!!.uid).set(items).addOnSuccessListener {
            val userInfo = UserModel()
            userInfo.userID = currentUser.uid
            userInfo.email = items["email"].toString()
            userInfo.firstName = items["firstName"].toString()
            userInfo.lastName = items["lastName"].toString()
            userInfo.userName = items["userName"].toString()
            userInfo.profilePictureURL = items["profilePictureURL"].toString()
            userInfo.active = true
//            userInfo.fcmToken = items["fcmToken"].toString()
            MyApplication.currentUser = userInfo

            onFinishLoading()
            startHomeNavigation()
        }.addOnFailureListener {
            _errorString.value = it.message
            onFinishLoading()
        }

    }

    private fun startHomeNavigation() {
        _navigateToHome.value = true

    }

    fun doneHomeNavigation() {
        _navigateToHome.value = false
    }

    private fun onStartLoading() {
        _buttonEnabled.value = false
        _progress.value = true
    }

    fun onFinishLoading() {
        _buttonEnabled.value = true
        _progress.value = false
    }

    fun setError(error: String) {
        _errorString.value = error
    }

    fun handleGoogleSignInResult(data: Intent?) {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task.getResult(ApiException::class.java)
            firebaseAuthWithGoogle(account!!)
        } catch (e: ApiException) {
            e.printStackTrace()
            _errorString.value = e.message
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        onStartLoading()
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        mAuth.signInWithCredential(credential).addOnCompleteListener {
            if (it.isSuccessful) {
                val userId = it.result!!.user!!.uid
                db.collection("users").document(userId)
                    .get()
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val document = task.result

                            if (document!!["email"] != null) {
                                onFinishLoading()
                                val userInfo = document.toObject(UserModel::class.java)
                                MyApplication.currentUser = userInfo
                                FirestoreUtil.updateUser(MyApplication.currentUser!!) {
                                }
                                startHomeNavigation()
                            } else {
                                createGoogleUser(account)
                            }
                        } else {
                            onFinishLoading()
                            _errorString.value = task.exception?.message
                        }
                    }

            } else {
                // If sign in fails, display a message to the user.
                onFinishLoading()
                _errorString.value = "Authentication failed."
            }
        }

    }

    private fun createGoogleUser(account: GoogleSignInAccount) {
        val userId = mAuth.currentUser!!.uid
        try {
            val items = HashMap<String, Any>()
            items["email"] = account.email!!
            items["firstName"] = account.displayName!!
            items["lastName"] = ""
            items["userName"] = ""
            items["phoneNumber"] = "000000"
            items["userID"] = userId
            items["active"] = true
            items["profilePictureURL"] = account.photoUrl.toString()
            saveSocialUserToFirebase(mAuth.currentUser, items)
        } catch (e: Exception) {
            onFinishLoading()
            _errorString.value = e.message
            e.printStackTrace()
        }
    }
}