package com.example.fitnessapp

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

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

        signupTv.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }
}