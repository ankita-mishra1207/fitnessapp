package com.example.fitnessapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Auto login
        lifecycleScope.launch {
            SupabaseManager.client.auth.sessionStatus.collectLatest {
                if (it is SessionStatus.Authenticated) {
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                }
            }
        }

        val emailEt = findViewById<TextInputEditText>(R.id.et_login_email)
        val passwordEt = findViewById<TextInputEditText>(R.id.et_login_password)
        val loginBtn = findViewById<MaterialButton>(R.id.btn_login)
        val googleBtn = findViewById<SignInButton>(R.id.btn_google_sign_in)
        val signupTv = findViewById<TextView>(R.id.tv_signup)

        // Email login
        loginBtn.setOnClickListener {
            lifecycleScope.launch {
                try {
                    SupabaseManager.client.auth.signInWith(Email) {
                        email = emailEt.text.toString()
                        password = passwordEt.text.toString()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@LoginActivity, e.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Google config
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        googleBtn.setOnClickListener {
            startActivityForResult(googleSignInClient.signInIntent, RC_SIGN_IN)
        }

        signupTv.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)

            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken

                if (idToken != null) {
                    lifecycleScope.launch {
                        try {
                            SupabaseManager.client.auth.signInWith(IDToken) {
                                this.idToken = idToken
                                provider = Google
                            }

                            val user = SupabaseManager.client.auth.currentUserOrNull()

                            if (user != null) {
                                SupabaseManager.client.from("profiles").upsert(
                                    mapOf(
                                        "id" to user.id,
                                        "email" to (account.email ?: ""),
                                        "name" to (account.displayName ?: ""),
                                        "goal" to "fitness"
                                    )
                                )
                            }

                            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                            finish()

                        } catch (e: Exception) {
                            Log.e("LOGIN", e.message.toString())
                        }
                    }
                }

            } catch (e: ApiException) {
                Toast.makeText(this, "Google error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}