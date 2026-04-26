package com.example.fitnessapp

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment

class SleepFragment : Fragment() {

    private lateinit var tvHours: TextView
    private lateinit var progressBar: ProgressBar
    private var currentSleep: Float = 0f

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_sleep, container, false)

        tvHours = view.findViewById(R.id.tv_sleep_hours)
        progressBar = view.findViewById(R.id.pb_sleep)
        val editHours = view.findViewById<EditText>(R.id.edit_sleep_hours)
        val btnSave = view.findViewById<Button>(R.id.btn_save_sleep)

        loadData()
        updateUI()

        btnSave.setOnClickListener {
            val hours = editHours.text.toString().toFloatOrNull()
            if (hours != null) {
                currentSleep = hours
                saveData()
                updateUI()
                Toast.makeText(context, "Sleep logged!", Toast.LENGTH_SHORT).show()
                editHours.setText("")
            }
        }

        return view
    }

    private fun loadData() {
        val sharedPref = activity?.getSharedPreferences("FitnessAppPrefs", Context.MODE_PRIVATE)
        currentSleep = sharedPref?.getFloat("sleep_current", 0f) ?: 0f
    }

    private fun saveData() {
        val sharedPref = activity?.getSharedPreferences("FitnessAppPrefs", Context.MODE_PRIVATE)
        with(sharedPref?.edit()) {
            this?.putFloat("sleep_current", currentSleep)
            this?.apply()
        }
    }

    private fun updateUI() {
        tvHours.text = "${"%.1f".format(currentSleep)} hrs"
        progressBar.progress = (currentSleep * 10).toInt()
    }
}
