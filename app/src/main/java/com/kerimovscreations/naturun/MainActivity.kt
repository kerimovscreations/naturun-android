package com.kerimovscreations.naturun

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class MainActivity : AppCompatActivity() {

    /**
     * Variables
     */

    private var user: FirebaseUser? = null

    /**
     * Activity methods
     */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        FirebaseAuth.getInstance().currentUser?.let {
            this.user = it
        }
    }

    override fun onResume() {
        super.onResume()

        if (this.user == null) {
            toAuth()
        }
    }

    /**
     * Navigation
     */

    private fun toAuth() {
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )

        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build(),
            RC_SIGN_IN
        )
    }

    /**
     * Activity results
     */

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)

            if (resultCode == Activity.RESULT_OK) {
                FirebaseAuth.getInstance().currentUser?.let {
                    this.user = it
                }
            } else {
                response?.error?.errorCode?.let { errorCode ->
                    Log.e("APP", "Error code $errorCode")
                }

            }
        }
    }

    /**
     * Constants
     */

    companion object {
        const val RC_SIGN_IN = 1
    }
}
