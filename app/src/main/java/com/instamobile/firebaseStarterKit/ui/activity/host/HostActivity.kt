package com.instamobile.firebaseStarterKit.ui.activity.host

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.instamobile.firebaseStarterKit.model.UserModel
import com.instamobile.firebaseStarterKit.utils.FirestoreUtil
import com.instamobile.firebaseStarterKit.utils.MyApplication
import com.instamobile.firebaseStarterKit.utils.Prefs
import com.instamobile.ui.fragment.onBoarding.walkthroughactivity.R
import com.instamobile.ui.fragment.onBoarding.walkthroughactivity.databinding.DrawerHeaderLayoutBinding
import kotlinx.android.synthetic.main.activity_host.*

class HostActivity : AppCompatActivity() {
    lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var navController: NavController
    private val mAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navViewBinding: DrawerHeaderLayoutBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_host)
        val toolbar = customToolbar
        setSupportActionBar(toolbar)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        drawerLayout = drawer_layout
        navViewBinding = DrawerHeaderLayoutBinding.inflate(layoutInflater, navView, true)
        val navHost =
            supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHost.navController

        val navInflater = navController.navInflater

        val graph = navInflater.inflate(R.navigation.main_graph)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.onBoarding ||
                destination.id == R.id.authFragment ||
                destination.id == R.id.loginFragment ||
                destination.id == R.id.signUpFragment
            ) {
                toolbar.visibility = View.GONE
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            } else {
                toolbar.visibility = View.VISIBLE
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
            }
        }
        if (!Prefs.getInstance(this)!!.hasCompletedWalkthrough!!) {
            if (mAuth.currentUser == null) {
                graph.startDestination = R.id.authFragment
            } else {
                getUserData()
                graph.startDestination = R.id.homeFragment
            }
        } else {
            graph.startDestination = R.id.onBoarding

        }
        navController.graph = graph

        NavigationUI.setupActionBarWithNavController(this, navController, drawerLayout)
        navView.setupWithNavController(navController)
        navView.setNavigationItemSelectedListener {
            it.isChecked
            drawerLayout.closeDrawers()
            when (it.itemId) {
                R.id.action_logout -> {
                    MyApplication.currentUser!!.active = false
                    FirestoreUtil.updateUser(MyApplication.currentUser!!) {
                        mAuth.signOut()
                    }
                    googleSignInClient.signOut()
                    MyApplication.currentUser = null
                    navController.navigate(R.id.action_logout)
                }
            }
            true
        }
    }

    private fun getUserData() {

        val ref = db.collection("users").document(mAuth.currentUser!!.uid)

        ref.get().addOnSuccessListener {
            val userInfo = it.toObject(UserModel::class.java)
            navViewBinding.user = userInfo
            MyApplication.currentUser = userInfo
            MyApplication.currentUser!!.active = true
            FirestoreUtil.updateUser(MyApplication.currentUser!!) {
            }
        }.addOnFailureListener {
            val intent = Intent(this, MyApplication::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return NavigationUI.navigateUp(navController, drawerLayout)
    }

}