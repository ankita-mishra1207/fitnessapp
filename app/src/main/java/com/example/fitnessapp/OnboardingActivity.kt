package com.example.fitnessapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class OnboardingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        val nameEt = findViewById<TextInputEditText>(R.id.et_onboarding_name)
        val weightEt = findViewById<TextInputEditText>(R.id.et_onboarding_weight)
        val heightEt = findViewById<TextInputEditText>(R.id.et_onboarding_height)
        val stepsEt = findViewById<TextInputEditText>(R.id.et_onboarding_steps)
        val waterEt = findViewById<TextInputEditText>(R.id.et_onboarding_water)
        val syncBtn = findViewById<MaterialButton>(R.id.btn_onboarding_sync)
        val finishBtn = findViewById<MaterialButton>(R.id.btn_onboarding_finish)

        val requestPermissionLauncher = registerForActivityResult(
            PermissionController.createRequestPermissionResultContract()
        ) { granted ->
            if (granted.isNotEmpty()) {
                val grantedCount = granted.size
                val totalCount = HealthConnectManager.permissions.size
                Toast.makeText(this, "Health Connect Linked ($grantedCount/$totalCount permissions)", Toast.LENGTH_SHORT).show()
                syncBtn.text = "✅ Health Data Linked"
                syncBtn.isEnabled = false
            } else {
                Toast.makeText(this, "No permissions granted. Manual entry required.", Toast.LENGTH_LONG).show()
            }
        }

        syncBtn.setOnClickListener {
            if (!HealthConnectManager.isAvailable(this)) {
                HealthConnectManager.installHealthConnect(this)
            } else {
                requestPermissionLauncher.launch(HealthConnectManager.permissions)
            }
        }

        finishBtn.setOnClickListener {
            val name = nameEt.text.toString().trim()
            val weight = weightEt.text.toString().toDoubleOrNull() ?: 0.0
            val height = heightEt.text.toString().toIntOrNull() ?: 0
            val steps = stepsEt.text.toString().toIntOrNull() ?: 10000
            val water = waterEt.text.toString().toDoubleOrNull() ?: 2.5

            if (name.isEmpty() || weight == 0.0 || height == 0) {
                Toast.makeText(this, "Please enter all basic details", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    val user = SupabaseManager.client.auth.currentUserOrNull()
                    if (user != null) {
                        // 1. Update Supabase Profile
                        SupabaseManager.client.from("profiles").upsert(
                            buildJsonObject {
                                put("id", user.id)
                                put("name", name)
                                put("weight", weight)
                                put("height", height)
                                put("step_goal", steps)
                                put("water_goal", water)
                            }
                        )

                        // 2. Update Local Room DB
                        val dbUser = User(
                            id = 1,
                            fullName = name,
                            email = user.email ?: "",
                            weight = weight,
                            height = height,
                            stepGoal = steps,
                            waterGoal = water,
                            usePhoneSensors = false
                        )
                        AppDatabase.getDatabase(this@OnboardingActivity).userDao().insertUser(dbUser)

                        // 3. Update SharedPreferences
                        val prefs = getSharedPreferences("FitnessAppPrefs", Context.MODE_PRIVATE)
                        prefs.edit().apply {
                            putString("user_name", name)
                            putString("step_goal", steps.toString())
                            putString("water_goal", water.toString())
                            putBoolean("onboarding_completed", true)
                            apply()
                        }

                        Toast.makeText(this@OnboardingActivity, "Profile Created!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@OnboardingActivity, MainActivity::class.java))
                        finish()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@OnboardingActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
