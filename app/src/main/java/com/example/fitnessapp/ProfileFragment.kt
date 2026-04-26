package com.example.fitnessapp

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.switchmaterial.SwitchMaterial
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class ProfileFragment : Fragment() {

    private lateinit var database: AppDatabase
    private var currentUser: User? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        database = AppDatabase.getDatabase(requireContext())

        val nameEdit      = view.findViewById<EditText>(R.id.edit_profile_name)
        val emailEdit     = view.findViewById<EditText>(R.id.edit_profile_email)
        val weightEdit    = view.findViewById<EditText>(R.id.edit_weight)
        val heightEdit    = view.findViewById<EditText>(R.id.edit_height)
        val stepGoalEdit  = view.findViewById<EditText>(R.id.edit_step_goal)
        val waterGoalEdit = view.findViewById<EditText>(R.id.edit_water_goal)
        val saveButton    = view.findViewById<Button>(R.id.btn_save_profile)

        // ── Health Connect UI ─────────────────────────────────────────────
        val hcStatusText = view.findViewById<TextView?>(R.id.tv_hc_status)
        val resetStatsBtn = view.findViewById<Button?>(R.id.btn_reset_stats)

        // Check Health Connect Status
        if (HealthConnectManager.isAvailable(requireContext())) {
            hcStatusText?.visibility = View.VISIBLE
            hcStatusText?.text = "✅ Health Connect is active"
            hcStatusText?.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
        } else {
            hcStatusText?.visibility = View.VISIBLE
            hcStatusText?.text = "⚠️ Health Connect not found"
            hcStatusText?.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
            hcStatusText?.setOnClickListener {
                HealthConnectManager.installHealthConnect(requireContext())
            }
        }

        val requestPermissionLauncher = registerForActivityResult(
            PermissionController.createRequestPermissionResultContract()
        ) { granted ->
            if (granted.containsAll(HealthConnectManager.permissions)) {
                Toast.makeText(requireContext(), "Permissions granted!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Permissions denied.", Toast.LENGTH_SHORT).show()
            }
        }


        resetStatsBtn?.setOnClickListener {
            val ctx = context ?: return@setOnClickListener
            val prefs = ctx.getSharedPreferences("FitnessAppPrefs", Context.MODE_PRIVATE)
            
            // Get current sync steps to use as an offset
            val currentSteps = prefs.getString("sync_steps", "0")?.toIntOrNull() ?: 0
            val currentDist = prefs.getString("sync_distance", "0.00 km")?.replace(" km", "")?.toDoubleOrNull() ?: 0.0
            val currentCals = prefs.getString("sync_calories", "0")?.toIntOrNull() ?: 0

            prefs.edit().apply {
                // Store offsets
                putInt("offset_steps", currentSteps)
                putFloat("offset_distance", currentDist.toFloat())
                putInt("offset_calories", currentCals)
                
                // Set display values to 0
                putString("sync_steps", "0")
                putString("sync_distance", "0.00 km")
                putString("sync_calories", "0")
                apply()
            }
            hcStatusText?.text = "⚪ Stats Reset: Starting from 0 now"
            Toast.makeText(ctx, "All dashboard stats reset to 0", Toast.LENGTH_SHORT).show()
        }

        // Load data from Room database
        lifecycleScope.launch {
            database.userDao().getUser().collectLatest { user ->
                currentUser = user
                user?.let {
                    nameEdit.setText(it.fullName)
                    emailEdit.setText(it.email)
                    weightEdit.setText(it.weight.toString())
                    heightEdit.setText(it.height.toString())
                    stepGoalEdit.setText(it.stepGoal.toString())
                    waterGoalEdit.setText(it.waterGoal.toString())
                }
            }
        }

        // ── Save profile ──────────────────────────────────────────────────
        saveButton.setOnClickListener {
            val weight = weightEdit.text.toString().toDoubleOrNull() ?: 0.0
            val height = heightEdit.text.toString().toIntOrNull() ?: 0
            val bmi = if (height > 0) weight / ((height.toDouble() / 100) * (height.toDouble() / 100)) else 0.0

            val user = User(
                id = 1,
                fullName = nameEdit.text.toString(),
                email = emailEdit.text.toString(),
                weight = weight,
                height = height,
                stepGoal = stepGoalEdit.text.toString().toIntOrNull() ?: 10000,
                waterGoal = waterGoalEdit.text.toString().toDoubleOrNull() ?: 2.5,
                usePhoneSensors = false
            )

            lifecycleScope.launch {
                database.userDao().insertUser(user)
                
                // Also update SharedPreferences for HomeFragment consistency
                val prefs = context?.getSharedPreferences("FitnessAppPrefs", Context.MODE_PRIVATE)
                prefs?.edit()?.apply {
                    putString("user_name", user.fullName)
                    putString("step_goal", user.stepGoal.toString())
                    putString("water_goal", user.waterGoal.toString())
                    apply()
                }

                // --- NEW: Sync to Supabase ---
                try {
                    val supabaseUser = SupabaseManager.client.auth.currentUserOrNull()
                    if (supabaseUser != null) {
                        SupabaseManager.client.from("profiles").upsert(
                            buildJsonObject {
                                put("id", supabaseUser.id)
                                put("name", user.fullName)
                                put("email", user.email)
                                put("weight", user.weight)
                                put("height", user.height)
                                put("step_goal", user.stepGoal)
                                put("water_goal", user.waterGoal)
                            }
                        )
                        Toast.makeText(context, "Profile synced to cloud!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Cloud sync failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }

                Toast.makeText(context, "Profile Saved to DB! BMI: ${"%.1f".format(bmi)}", Toast.LENGTH_SHORT).show()
            }
        }

        // ── Logout ────────────────────────────────────────────────────────
        val logoutBtn = view.findViewById<Button>(R.id.btn_logout)
        logoutBtn?.setOnClickListener {
            val ctx = context ?: return@setOnClickListener
            val prefs = ctx.getSharedPreferences("FitnessAppPrefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("is_logged_in", false).apply()
            
            startActivity(android.content.Intent(ctx, LoginActivity::class.java))
            activity?.finish()
        }

        return view
    }
}
