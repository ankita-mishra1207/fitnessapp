package com.example.fitnessapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.fitnessapp.databinding.ActivityPermissionsRationaleBinding

class PermissionsRationaleActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPermissionsRationaleBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPermissionsRationaleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnContinue.setOnClickListener {
            finish()
        }
    }
}
