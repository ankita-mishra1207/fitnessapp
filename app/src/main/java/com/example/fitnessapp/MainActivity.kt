package com.example.fitnessapp

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        arrayOf(
            android.Manifest.permission.ACTIVITY_RECOGNITION,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
    } else {
        arrayOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val denied = permissions.entries.filter { !it.value }
        if (denied.isNotEmpty()) {
            Toast.makeText(this, "Some permissions were denied. Some features may not work.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check login status with Supabase
        if (SupabaseManager.client.auth.currentUserOrNull() == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        checkAndRequestPermissions()

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        val navView: BottomNavigationView = findViewById(R.id.bottom_navigation)
        ViewCompat.setOnApplyWindowInsetsListener(navView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }
        navView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_dashboard -> {
                    loadFragment(HomeFragment())
                    true
                }
                R.id.navigation_workouts -> {
                    loadFragment(YouTubeWorkoutFragment())
                    true
                }
                R.id.navigation_quotes -> {
                    loadFragment(QuotesFragment())
                    true
                }
                R.id.navigation_profile -> {
                    loadFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }

        // Load default fragment
        if (savedInstanceState == null) {
            val navigateTo = intent.getStringExtra("navigate_to")
            if (navigateTo == "profile") {
                loadFragment(ProfileFragment())
                navView.selectedItemId = R.id.navigation_profile
            } else {
                loadFragment(HomeFragment())
            }
            // Check if we were started by a Fitbit deep link (cold start)
            intent?.let { handleIntent(it) }
        }

        // --- NEW: Start Background Health Connect Sync ---
        startAutoSync()
    }

    private fun startAutoSync() {
        lifecycleScope.launch {
            while (true) {
                try {
                    val client = androidx.health.connect.client.HealthConnectClient.getOrCreate(this@MainActivity)
                    val granted = client.permissionController.getGrantedPermissions()
                    
                    if (granted.containsAll(HealthConnectManager.permissions)) {
                        val data = HealthConnectManager.fetchTodayActivity(this@MainActivity)
                        val activeTime = HealthConnectManager.fetchActiveTime(this@MainActivity)
                        
                        if (data != null) {
                            val prefs = getSharedPreferences("FitnessAppPrefs", MODE_PRIVATE)
                            prefs.edit().apply {
                                putString("sync_steps", data.steps.toString())
                                putString("sync_distance", "%.2f km".format(data.distanceKm))
                                putString("sync_calories", data.caloriesBurned.toString())
                                putString("sync_active_time", activeTime.toString())
                                putInt("sync_resting_hr", data.heartRateResting)
                                apply()
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("AutoSync", "Sync failed: ${e.message}")
                }
                delay(300_000) // Sync every 5 minutes
            }
        }
    }

    private fun handleIntent(intent: Intent) {
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
