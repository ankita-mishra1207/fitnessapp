package com.example.fitnessapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * Mandatory activity for Health Connect. 
 * This is shown when the user wants to see why we need permissions.
 */
class HealthConnectPermissionsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // You can add a simple layout here explaining why you need steps
        // For now, we'll just finish to keep it simple as it's a requirement of the manifest
        finish()
    }
}
