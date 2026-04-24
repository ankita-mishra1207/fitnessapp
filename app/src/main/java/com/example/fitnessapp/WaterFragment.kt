package com.example.fitnessapp

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment

class WaterFragment : Fragment() {

    private lateinit var tvCurrent: TextView
    private lateinit var tvGoal: TextView
    private lateinit var progressBar: ProgressBar
    private var currentWater: Float = 0f
    private var goalWater: Float = 2.5f

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_water, container, false)

        tvCurrent = view.findViewById(R.id.tv_water_current)
        tvGoal = view.findViewById(R.id.tv_water_goal_display)
        progressBar = view.findViewById(R.id.pb_water)

        val btn250 = view.findViewById<Button>(R.id.btn_add_250)
        val btn500 = view.findViewById<Button>(R.id.btn_add_500)
        val btn1000 = view.findViewById<Button>(R.id.btn_add_1000)

        loadData()
        updateUI()

        btn250.setOnClickListener { addWater(0.25f) }
        btn500.setOnClickListener { addWater(0.5f) }
        btn1000.setOnClickListener { addWater(1.0f) }

        return view
    }

    private fun loadData() {
        val sharedPref = activity?.getSharedPreferences("FitnessAppPrefs", Context.MODE_PRIVATE)
        val goalStr = sharedPref?.getString("water_goal", "2.5") ?: "2.5"
        goalWater = goalStr.toFloatOrNull() ?: 2.5f
        currentWater = sharedPref?.getFloat("water_current", 0f) ?: 0f
    }

    private fun addWater(amount: Float) {
        currentWater += amount
        saveData()
        updateUI()
        Toast.makeText(context, "Added ${if (amount < 1) "${(amount * 1000).toInt()}ml" else "${amount}L"}", Toast.LENGTH_SHORT).show()
    }

    private fun saveData() {
        val sharedPref = activity?.getSharedPreferences("FitnessAppPrefs", Context.MODE_PRIVATE)
        with(sharedPref?.edit()) {
            this?.putFloat("water_current", currentWater)
            this?.apply()
        }
    }

    private fun updateUI() {
        tvCurrent.text = "${"%.1f".format(currentWater)}L"
        tvGoal.text = "of ${"%.1f".format(goalWater)}L daily goal"
        
        val progress = if (goalWater > 0) (currentWater / goalWater * 100).toInt() else 0
        progressBar.progress = progress
    }
}
