package com.example.fitnessapp

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment

class ReportsFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_reports, container, false)

        val bmiValueText = view.findViewById<TextView>(R.id.tv_bmi_value)
        val bmiStatusText = view.findViewById<TextView>(R.id.tv_bmi_status)
        val dietItemsLayout = view.findViewById<LinearLayout>(R.id.ll_diet_items)
        val totalStepsText = view.findViewById<TextView>(R.id.tv_report_total_steps)

        val sharedPref = activity?.getSharedPreferences("FitnessAppPrefs", Context.MODE_PRIVATE)
        
        // BMI Calculation logic
        val weight = sharedPref?.getString("user_weight", "70")?.toFloatOrNull() ?: 70f
        val height = sharedPref?.getString("user_height", "175")?.toFloatOrNull() ?: 175f
        val bmi = if (height > 0) weight / ((height / 100) * (height / 100)) else 22.5f

        bmiValueText.text = "%.1f".format(bmi)
        
        val status: String
        val recommendations: List<Pair<String, String>>
        
        when {
            bmi < 18.5 -> {
                status = getString(R.string.bmi_underweight)
                recommendations = listOf(
                    "Nut-butter toast" to "350 kcal",
                    "Avocado smoothie" to "400 kcal",
                    "Pasta with cheese" to "500 kcal",
                    "Trail mix snacks" to "250 kcal"
                )
            }
            bmi < 25 -> {
                status = getString(R.string.bmi_normal)
                recommendations = listOf(
                    "Oats with berries" to "300 kcal",
                    "Grilled chicken salad" to "450 kcal",
                    "Greek yogurt" to "150 kcal",
                    "Salmon and quinoa" to "550 kcal"
                )
            }
            bmi < 30 -> {
                status = getString(R.string.bmi_overweight)
                recommendations = listOf(
                    "Leafy green salad" to "200 kcal",
                    "Baked fish" to "350 kcal",
                    "Vegetable soup" to "180 kcal",
                    "Apple with almonds" to "150 kcal"
                )
            }
            else -> {
                status = getString(R.string.bmi_obese)
                recommendations = listOf(
                    "Steamed broccoli" to "100 kcal",
                    "Egg white omelet" to "150 kcal",
                    "Lentil stew" to "250 kcal",
                    "Berry medley" to "80 kcal"
                )
            }
        }

        bmiStatusText.text = status
        
        // Dynamic diet items
        dietItemsLayout.removeAllViews()
        recommendations.forEach { (food, kcal) ->
            val textView = TextView(context).apply {
                text = "• $food ($kcal)"
                setPadding(0, 8, 0, 8)
                setTextColor(resources.getColor(R.color.black, null))
                textSize = 15f
            }
            dietItemsLayout.addView(textView)
        }

        // Update steps from "sync"
        val syncedSteps = sharedPref?.getString("sync_steps", "42180")
        totalStepsText.text = syncedSteps

        return view
    }
}