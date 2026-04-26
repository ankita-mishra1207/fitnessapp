package com.example.fitnessapp

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import io.github.jan.supabase.auth.auth


class SignupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        val nameEt = findViewById<TextInputEditText>(R.id.et_signup_name)
        val emailEt = findViewById<TextInputEditText>(R.id.et_signup_email)
        val passwordEt = findViewById<TextInputEditText>(R.id.et_signup_password)
        val signupBtn = findViewById<MaterialButton>(R.id.btn_signup)
        val loginTv = findViewById<TextView>(R.id.tv_login)

        signupBtn.setOnClickListener {

            val name = nameEt.text.toString().trim()
            val email = emailEt.text.toString().trim()
            val password = passwordEt.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    SupabaseManager.client.auth.signUpWith(Email) {
                        this.email = email
                        this.password = password
                        data = buildJsonObject {
                            put("full_name", name)
                        }
                    }

                    // ✅ Get user correctly
                    val user = SupabaseManager.client.auth.currentUserOrNull()

                    if (user != null) {
                        SupabaseManager.client.from("profiles").insert(
                            buildJsonObject {
                                put("id", user.id)
                                put("email", email)
                                put("name", name)
                                put("age", 0)
                                put("weight", 0.0)
                                put("height", 0)
                                put("step_goal", 10000)
                                put("water_goal", 2.5)
                                put("goal", "fitness")
                            }
                        )
                        // Set login status
                        val prefs = getSharedPreferences("FitnessAppPrefs", MODE_PRIVATE)
                        prefs.edit().putBoolean("is_logged_in", true).apply()
                        
                        Toast.makeText(this@SignupActivity, "Signup Success", Toast.LENGTH_LONG).show()
                        
                        // Navigate to Onboarding to fill details
                        startActivity(Intent(this@SignupActivity, OnboardingActivity::class.java))
                        finish()
                    }

                } catch (e: Exception) {
                    Toast.makeText(this@SignupActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        loginTv.setOnClickListener { finish() }
    }
}