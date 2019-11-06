package com.instamobile.firebaseStarterKit.ui.fragment.signUp

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.instamobile.firebaseStarterKit.ui.activity.host.HostActivity
import com.instamobile.ui.fragment.onBoarding.walkthroughactivity.R
import com.instamobile.ui.fragment.onBoarding.walkthroughactivity.databinding.FragmentSignUpBinding
import com.squareup.picasso.Picasso


class SignUpFragment : Fragment() {


    private val PERMISSION_CODE = 1
    private val PICK_IMAGE_CODE = 2
    private var imageUri: Uri? = null
    private lateinit var binding: FragmentSignUpBinding
    private lateinit var viewModel: SignUpViewModel
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = ViewModelProviders.of(this).get(SignUpViewModel::class.java)
        binding = FragmentSignUpBinding.inflate(inflater, container, false)
        binding.signUpViewModel = viewModel
        binding.lifecycleOwner = this
        setupListeners()
        setupObservers()

        return binding.root
    }

    private fun setupListeners() {
        binding.etFullName.doAfterTextChanged { fullname ->
            viewModel.fullname = fullname.toString()
        }
        binding.etPhone.doAfterTextChanged { phone -> viewModel.phoneNumber = phone.toString() }
        binding.etEmail.doAfterTextChanged { email -> viewModel.email = email.toString() }
        binding.etPassword.doAfterTextChanged { password ->
            viewModel.password = password.toString()
        }
        binding.selectPhoto.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                checkPermissions()
            } else {
                pickFromGallery()
            }
        }
    }

    private fun setupObservers() {
        viewModel.fullNameError.observe(this, Observer {
            if (it.isNotEmpty()) {
                binding.etFullName.error = it
            }
        })
        viewModel.phoneNumberError.observe(this, Observer {
            if (it.isNotEmpty()) {
                binding.etPhone.error = it
            }
        })
        viewModel.emailError.observe(this, Observer {
            if (it.isNotEmpty()) {
                binding.etEmail.error = it
            }
        })
        viewModel.passwordError.observe(this, Observer {
            if (it.isNotEmpty()) {
                binding.etPassword.error = it
            }
        })
        viewModel.navigateToHome.observe(this, Observer {
            if (it) {
                startActivity(Intent(context, HostActivity::class.java))
                (activity as HostActivity).finish()
                viewModel.doneNavigating()
            }
        })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_CODE) {
            if (permissions[0] == Manifest.permission.READ_EXTERNAL_STORAGE) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    pickFromGallery()
                }
            }
        }
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(
                context!!,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), PERMISSION_CODE
            )
        } else {
            pickFromGallery()
        }
    }

    private fun pickFromGallery() {
        val getIntent = Intent(Intent.ACTION_GET_CONTENT)
        getIntent.type = "image/*"

        val pickIntent = Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )
        pickIntent.type = "image/*"

        val chooserIntent = Intent.createChooser(getIntent, getString(R.string.select_image))
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(pickIntent))
        startActivityForResult(chooserIntent, PICK_IMAGE_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_CODE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                imageUri = data.data
                viewModel.imageUri = imageUri
                Picasso.get().load(imageUri).placeholder(R.drawable.placeholder)
                    .into(binding.ciAvatar)
            }
        }
    }
}