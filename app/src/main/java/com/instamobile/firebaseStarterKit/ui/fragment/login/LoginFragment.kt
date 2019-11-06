package com.instamobile.firebaseStarterKit.ui.fragment.login

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginResult
import com.instamobile.firebaseStarterKit.ui.activity.host.HostActivity
import com.instamobile.ui.fragment.onBoarding.walkthroughactivity.databinding.FragmentLoginBinding

class LoginFragment : Fragment() {
    private val RC_SIGN_IN = 1
    private lateinit var mCallbackManager: CallbackManager
    private lateinit var binding: FragmentLoginBinding
    private lateinit var viewModel: LoginViewModel
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mCallbackManager = CallbackManager.Factory.create()
        viewModel = ViewModelProviders.of(this).get(LoginViewModel::class.java)
        binding = FragmentLoginBinding.inflate(inflater, container, false)
        binding.loginViewModel = viewModel
        binding.lifecycleOwner = this

        setupListeners()
        setUpObservers()

        binding.btLoginFacebook.setPermissions("email")
        binding.btLoginFacebook.registerCallback(mCallbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    viewModel.handleFacebookToken(result)
                }

                override fun onCancel() {
                    viewModel.onFinishLoading()
                }

                override fun onError(error: FacebookException?) {
                    viewModel.onFinishLoading()
                    viewModel.setError(error?.message!!)
                }
            })

        return binding.root
    }

    private fun setupListeners() {
        binding.etEmail.doAfterTextChanged { email -> viewModel.username = email.toString() }
        binding.etPassword.doAfterTextChanged { pass -> viewModel.password = pass.toString() }
        binding.signInButton.setOnClickListener {
            signInWithGoogle()
        }
    }

    private fun setUpObservers() {
        viewModel.navigateToHome.observe(this, Observer {
            if (it) {
                startActivity(Intent(context, HostActivity::class.java))
                (activity as HostActivity).finish()
                viewModel.doneHomeNavigation()
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        mCallbackManager.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            viewModel.handleGoogleSignInResult(data)
        }

    }

    private fun signInWithGoogle() {
        val signInIntent = (activity as HostActivity).googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }
}