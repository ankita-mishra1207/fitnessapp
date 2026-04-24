package com.example.fitnessapp

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.json.JSONObject
import java.net.URL

data class FoodNutrition(
    val calories: Int,
    val proteinG: Float,
    val carbsG: Float,
    val fatG: Float
)

class HomeFragment : Fragment() {

    private var stepsText: TextView? = null
    private var caloriesText: TextView? = null
    private var distanceText: TextView? = null
    private var activeTimeText: TextView? = null
    private var stepsProgressText: TextView? = null
    private var stepsProgressBar: ProgressBar? = null
    private var stepsRemainingText: TextView? = null

    // Weather UI
    private var weatherTempText: TextView? = null
    private var weatherDescText: TextView? = null

    private var bCal = 0; private var bProtein = 0f; private var bCarbs = 0f; private var bFat = 0f
    private var lCal = 0; private var lProtein = 0f; private var lCarbs = 0f; private var lFat = 0f
    private var dCal = 0; private var dProtein = 0f; private var dCarbs = 0f; private var dFat = 0f

    private fun loadLoggedData() {
        val prefs = activity?.getSharedPreferences("FitnessAppPrefs", Context.MODE_PRIVATE) ?: return
        bCal = prefs.getInt("bCal", 0)
        lCal = prefs.getInt("lCal", 0)
        dCal = prefs.getInt("dCal", 0)
        // For a demo, we can just load calories, but you can add macros too
    }

    private fun saveLoggedData() {
        val prefs = activity?.getSharedPreferences("FitnessAppPrefs", Context.MODE_PRIVATE) ?: return
        with(prefs.edit()) {
            putInt("bCal", bCal)
            putInt("lCal", lCal)
            putInt("dCal", dCal)
            apply()
        }
    }

    private val foodDatabase = mapOf(
        "apple"             to FoodNutrition(95,   0.5f,  25f,  0.3f),
        "banana"            to FoodNutrition(105,  1.3f,  27f,  0.4f),
        "mango"             to FoodNutrition(99,   1.4f,  25f,  0.6f),
        "orange"            to FoodNutrition(62,   1.2f,  15f,  0.2f),
        "grapes"            to FoodNutrition(69,   0.7f,  18f,  0.2f),
        "papaya"            to FoodNutrition(59,   0.9f,  15f,  0.1f),
        "watermelon"        to FoodNutrition(30,   0.6f,   8f,  0.2f),
        "chicken breast"    to FoodNutrition(165, 31.0f,   0f,  3.6f),
        "chicken curry"     to FoodNutrition(240, 25.0f,   8f, 12.0f),
        "egg"               to FoodNutrition(78,   6.0f,   1f,  5.0f),
        "egg white"         to FoodNutrition(17,   3.6f,   0f,  0.1f),
        "boiled egg"        to FoodNutrition(78,   6.0f,   1f,  5.0f),
        "paneer"            to FoodNutrition(265, 18.0f,   4f, 20.0f),
        "paneer tikka"      to FoodNutrition(300, 20.0f,   8f, 21.0f),
        "dal"               to FoodNutrition(116,  8.0f,  20f,  0.4f),
        "dal tadka"         to FoodNutrition(150,  9.0f,  22f,  4.0f),
        "rajma"             to FoodNutrition(144,  8.7f,  26f,  0.5f),
        "chana"             to FoodNutrition(164,  8.9f,  27f,  2.6f),
        "fish"              to FoodNutrition(136, 20.0f,   0f,  6.0f),
        "tuna"              to FoodNutrition(132, 29.0f,   0f,  1.0f),
        "salmon"            to FoodNutrition(208, 20.0f,   0f, 13.0f),
        "mutton"            to FoodNutrition(258, 26.0f,   0f, 17.0f),
        "rice"              to FoodNutrition(206,  4.3f,  45f,  0.4f),
        "brown rice"        to FoodNutrition(215,  5.0f,  45f,  1.8f),
        "roti"              to FoodNutrition(104,  3.1f,  20f,  1.0f),
        "chapati"           to FoodNutrition(104,  3.1f,  20f,  1.0f),
        "paratha"           to FoodNutrition(260,  5.0f,  36f, 10.0f),
        "naan"              to FoodNutrition(262,  8.7f,  45f,  5.0f),
        "idli"              to FoodNutrition(39,   1.8f,   8f,  0.2f),
        "dosa"              to FoodNutrition(168,  3.9f,  32f,  3.7f),
        "oats"              to FoodNutrition(154,  5.0f,  28f,  3.0f),
        "poha"              to FoodNutrition(180,  3.0f,  36f,  3.0f),
        "upma"              to FoodNutrition(177,  4.0f,  30f,  5.0f),
        "pasta"             to FoodNutrition(220,  8.0f,  43f,  1.3f),
        "bread"             to FoodNutrition(79,   3.0f,  15f,  1.0f),
        "sandwich"          to FoodNutrition(250,  9.0f,  33f,  8.0f),
        "milk"              to FoodNutrition(122,  8.0f,  12f,  5.0f),
        "curd"              to FoodNutrition(61,   3.5f,   5f,  3.3f),
        "dahi"              to FoodNutrition(61,   3.5f,   5f,  3.3f),
        "yogurt"            to FoodNutrition(100,  17f,   6f,  0.7f),
        "greek yogurt"      to FoodNutrition(100,  17f,   6f,  0.7f),
        "cheese"            to FoodNutrition(113,  7.0f,   0f,  9.0f),
        "butter"            to FoodNutrition(102,  0.1f,   0f, 12.0f),
        "ghee"              to FoodNutrition(112,  0.0f,   0f, 12.5f),
        "samosa"            to FoodNutrition(262,  5.0f,  31f, 14.0f),
        "chole bhature"     to FoodNutrition(450,  12.0f, 55f, 22.0f),
        "chole"             to FoodNutrition(250,  10.0f, 35f, 10.0f),
        "vada pav"          to FoodNutrition(290,  7.0f,  42f, 10.0f),
        "burger"            to FoodNutrition(354, 17.0f,  33f, 17.0f),
        "pizza"             to FoodNutrition(285, 12.0f,  36f, 10.0f),
        "french fries"      to FoodNutrition(365,  3.4f,  48f, 17.0f),
        "salad"             to FoodNutrition(50,   2.0f,   8f,  1.5f),
        "trail mix"         to FoodNutrition(173,  5.0f,  16f, 11.0f),
        "almonds"           to FoodNutrition(164,  6.0f,   6f, 14.0f),
        "peanuts"           to FoodNutrition(166,  7.0f,   6f, 14.0f),
        "protein shake"     to FoodNutrition(160, 30.0f,  10f,  3.0f),
        "lassi"             to FoodNutrition(150,  5.0f,  22f,  5.0f),
        "chai"              to FoodNutrition(60,   2.0f,   9f,  2.0f),
        "coffee"            to FoodNutrition(2,    0.3f,   0f,  0.0f),
        "juice"             to FoodNutrition(110,  0.5f,  26f,  0.3f)
    )

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "sync_steps" || key == "sync_distance" || key == "sync_calories" || key == "sync_active_min" || key == "user_name" || key == "step_goal") {
            activity?.runOnUiThread { refreshDashboard() }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        loadLoggedData()
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        setupUI(view)
        refreshDashboard(view)
        return view
    }

    private fun setupUI(view: View) {
        stepsText      = view.findViewById(R.id.tv_home_steps)
        caloriesText   = view.findViewById(R.id.tv_home_calories)
        distanceText   = view.findViewById(R.id.tv_home_distance)
        activeTimeText = view.findViewById(R.id.tv_home_active_time)

        stepsProgressText  = view.findViewById(R.id.tv_step_count_progress)
        stepsRemainingText = view.findViewById(R.id.tv_steps_remaining)
        stepsProgressBar   = view.findViewById(R.id.pb_step_goal)

        val swipeRefresh = view.findViewById<SwipeRefreshLayout>(R.id.swipe_refresh)
        swipeRefresh.setOnRefreshListener {
            refreshDashboard(view)
            // Stop the spinning animation after 1.5 seconds
            view.postDelayed({ swipeRefresh.isRefreshing = false }, 1500)
        }

        weatherTempText = view.findViewById(R.id.tv_weather_temp)
        weatherDescText = view.findViewById(R.id.tv_weather_desc)

        val weatherIcon = view.findViewById<View>(R.id.iv_weather_icon)
        weatherIcon?.setOnClickListener {
            showWeatherForecastDialog()
        }

        // Quick Actions
        view.findViewById<View>(R.id.card_water)?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, WaterFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.card_bmi)?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ReportsFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.card_sleep)?.setOnClickListener {
            Toast.makeText(context, "Sleep tracking coming soon!", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<View>(R.id.card_burn)?.setOnClickListener {
            Toast.makeText(context, "Burn estimator integrated in Reports", Toast.LENGTH_SHORT).show()
        }

        val foodNameInput    = view.findViewById<android.widget.EditText>(R.id.edit_food_name)
        val logFoodButton    = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_add_food)
        val mealGroup        = view.findViewById<android.widget.RadioGroup>(R.id.rg_meals)
        val clearCaloriesBtn = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_clear_calories)

        clearCaloriesBtn.setOnClickListener {
            android.app.AlertDialog.Builder(context)
                .setTitle("Clear Calories")
                .setMessage("Are you sure you want to clear all logged food data for today?")
                .setPositiveButton("Clear") { _, _ ->
                    bCal = 0; bProtein = 0f; bCarbs = 0f; bFat = 0f
                    lCal = 0; lProtein = 0f; lCarbs = 0f; lFat = 0f
                    dCal = 0; dProtein = 0f; dCarbs = 0f; dFat = 0f
                    saveLoggedData()
                    refreshMacrosUI(view)
                    Toast.makeText(context, "Calories cleared", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        logFoodButton.setOnClickListener {
            val query = foodNameInput.text.toString().lowercase().trim()
            if (query.isNotEmpty()) {
                viewLifecycleOwner.lifecycleScope.launch {
                    var nutrition = FoodApiManager.getFoodNutrition(query)
                    if (nutrition == null) {
                        nutrition = foodDatabase[query] ?: foodDatabase.entries.firstOrNull { it.key.contains(query) }?.value
                    }
                    if (nutrition == null) {
                        nutrition = FoodNutrition((100..400).random(), (5..25).random().toFloat(), (10..60).random().toFloat(), (2..20).random().toFloat())
                    }

                    nutrition?.let { n ->
                        val mealId = mealGroup.checkedRadioButtonId
                        val mealName = when (mealId) {
                            R.id.rb_breakfast -> "Breakfast"
                            R.id.rb_lunch     -> "Lunch"
                            R.id.rb_dinner    -> "Dinner"
                            else -> "Meal"
                        }

                        android.app.AlertDialog.Builder(context)
                            .setTitle("Food Found")
                            .setMessage("${query.replaceFirstChar { it.uppercase() }}\nCalories: ${n.calories} kcal\nProtein: ${n.proteinG}g\nCarbs: ${n.carbsG}g\nFat: ${n.fatG}g\n\nDo you want to save this to $mealName?")
                            .setPositiveButton("Save") { _, _ ->
                                when (mealId) {
                                    R.id.rb_breakfast -> { bCal += n.calories; bProtein += n.proteinG; bCarbs += n.carbsG; bFat += n.fatG }
                                    R.id.rb_lunch     -> { lCal += n.calories; lProtein += n.proteinG; lCarbs += n.carbsG; lFat += n.fatG }
                                    R.id.rb_dinner    -> { dCal += n.calories; dProtein += n.proteinG; dCarbs += n.carbsG; dFat += n.fatG }
                                }
                                saveLoggedData()
                                refreshMacrosUI(view)
                                foodNameInput.setText("")
                                Toast.makeText(context, "✅ Saved to $mealName", Toast.LENGTH_SHORT).show()
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }
                }
            }
        }
    }

    private fun showWeatherForecastDialog() {
        val ctx = context ?: return
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(ctx, "Please grant location permission first", Toast.LENGTH_SHORT).show()
            return
        }

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(ctx)
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
            .addOnSuccessListener { location ->
                location?.let {
                    lifecycleScope.launch {
                        val forecast = WeatherApiManager.fetchForecast(it.latitude, it.longitude)
                        forecast?.let { f ->
                            val builder = android.app.AlertDialog.Builder(ctx)
                            builder.setTitle("Upcoming Weather Forecast")
                            
                            // Take next 8 items (approx 24 hours of data)
                            val forecastString = f.list.take(8).joinToString("\n\n") { item ->
                                // Format: "14:00 - 25°C, Clouds"
                                val time = item.dt_txt.substring(11, 16)
                                val date = item.dt_txt.substring(5, 10)
                                "[$date $time]  ${item.main.temp.toInt()}°C - ${item.weather.firstOrNull()?.description?.replaceFirstChar { it.uppercase() }}"
                            }
                            
                            builder.setMessage(forecastString)
                            builder.setPositiveButton("Awesome", null)
                            builder.show()
                        }
                    }
                }
            }
    }

    private fun refreshDashboard(rootView: View? = null) {
        val view = rootView ?: view ?: return

        // --- NEW: Fetch Profile from Supabase ---
        lifecycleScope.launch {
            try {
                val user = SupabaseManager.client.auth.currentUserOrNull()
                if (user != null) {
                    val profile = SupabaseManager.client.from("profiles")
                        .select {
                            filter {
                                eq("id", user.id)
                            }
                        }.decodeSingleOrNull<kotlinx.serialization.json.JsonObject>()

                    profile?.let {
                        val name = it["name"]?.jsonPrimitive?.content ?: "Rahul"
                        val stepGoal = it["step_goal"]?.jsonPrimitive?.int ?: 10000
                        val weight = it["weight"]?.jsonPrimitive?.double ?: 70.0

                        // Update local prefs for consistency
                        val prefs = context?.getSharedPreferences("FitnessAppPrefs", Context.MODE_PRIVATE)
                        prefs?.edit()?.apply {
                            putString("user_name", name)
                            putString("step_goal", stepGoal.toString())
                            putString("user_weight", weight.toString())
                            apply()
                        }
                    }
                }
            } catch (e: Exception) {
                // Silently fail or log
            }
        }

        // Fetch Weather (Using Mumbai coordinates as default again)
        lifecycleScope.launch {
            val weather = WeatherApiManager.fetchWeather(19.0760, 72.8777)
            weather?.let {
                weatherTempText?.text = "${it.main.temp.toInt()}°C"
                weatherDescText?.text = it.weather.firstOrNull()?.description?.replaceFirstChar { c -> c.uppercase() }
            }
        }

        val context = context ?: return
        val sharedPref = context.getSharedPreferences("FitnessAppPrefs", Context.MODE_PRIVATE)

        val name         = sharedPref.getString("user_name", "Rahul")
        val stepGoalStr  = sharedPref.getString("step_goal", "10000") ?: "10000"
        
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = when (hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..20 -> "Good evening"
            else -> "Good night"
        }

        // Use synced data from Health Connect (saved in ProfileFragment)
        val stepsStr     = sharedPref.getString("sync_steps", "0") ?: "0"
        val distStr      = sharedPref.getString("sync_distance", "0.00 km") ?: "0.00 km"
        
        val steps        = stepsStr.toIntOrNull() ?: 0
        val distance     = distStr.replace(" km", "").toDoubleOrNull() ?: 0.0
        val totalCal     = bCal + lCal + dCal
        val calories     = totalCal.toString()
        val activeMin    = sharedPref.getString("sync_active_time", "0") ?: "0"
        
        val stepGoal     = stepGoalStr.toIntOrNull() ?: 10000

        val sdf          = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        val currentDate  = sdf.format(Date())

        view.findViewById<TextView>(R.id.tv_home_greeting)?.text = "$greeting, $name 👋"
        view.findViewById<TextView>(R.id.tv_home_date)?.text     = "$currentDate - Ready to start?"

        stepsText?.text    = String.format(Locale.getDefault(), "%,d", steps)
        distanceText?.text = "%.2f km".format(distance)
        caloriesText?.text = calories
        activeTimeText?.text = "$activeMin min"

        stepsProgressText?.text = "${String.format(Locale.getDefault(), "%,d", steps)} / ${String.format(Locale.getDefault(), "%,d", stepGoal)}"
        val remaining = stepGoal - steps
        stepsRemainingText?.text = if (remaining > 0) "${String.format(Locale.getDefault(), "%,d", remaining)} steps remaining" else "Goal achieved! 🏆"

        val progressPercent = (steps.toFloat() / stepGoal.toFloat() * 100).toInt().coerceIn(0, 100)
        stepsProgressBar?.progress = progressPercent

        refreshMacrosUI(view)
    }

    private fun fetchLocationAndWeather() {
        val ctx = context ?: return
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            return
        }

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(ctx)
        // Set a timeout or use lastLocation as fallback if getCurrentLocation is stuck
        fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
            if (lastLoc != null) {
                updateWeatherUI(lastLoc.latitude, lastLoc.longitude)
            } else {
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                    .addOnSuccessListener { loc ->
                        if (loc != null) {
                            updateWeatherUI(loc.latitude, loc.longitude)
                        } else {
                            weatherDescText?.text = "Location unavailable"
                        }
                    }
                    .addOnFailureListener {
                        weatherDescText?.text = "Location error"
                    }
            }
        }
    }

    private fun updateWeatherUI(lat: Double, lon: Double) {
        lifecycleScope.launch {
            try {
                val weather = WeatherApiManager.fetchWeather(lat, lon)
                if (weather != null) {
                    weatherTempText?.text = "${weather.main.temp.toInt()}°C"
                    weatherDescText?.text = weather.weather.firstOrNull()?.description?.replaceFirstChar { c -> c.uppercase() }
                } else {
                    weatherDescText?.text = "Weather Error"
                }
            } catch (e: Exception) {
                weatherDescText?.text = "Check Connection"
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            fetchLocationAndWeather()
        } else {
            weatherDescText?.text = "Location denied"
        }
    }

    private fun refreshMacrosUI(view: View) {
        val calorieResultText = view.findViewById<TextView>(R.id.tv_calorie_result)
        val macroSummaryText  = view.findViewById<TextView?>(R.id.tv_macro_summary)
        val macroProteinBar   = view.findViewById<ProgressBar?>(R.id.pb_protein)
        val macroCarbsBar     = view.findViewById<ProgressBar?>(R.id.pb_carbs)
        val macroFatBar       = view.findViewById<ProgressBar?>(R.id.pb_fat)
        val tvProteinLabel    = view.findViewById<TextView?>(R.id.tv_macro_protein)
        val tvCarbsLabel      = view.findViewById<TextView?>(R.id.tv_macro_carbs)
        val tvFatLabel        = view.findViewById<TextView?>(R.id.tv_macro_fat)

        val totalCal     = bCal + lCal + dCal
        val totalProtein = bProtein + lProtein + dProtein
        val totalCarbs   = bCarbs + lCarbs + dCarbs
        val totalFat     = bFat + lFat + dFat

        calorieResultText?.text = "Total: $totalCal kcal  •  B:$bCal  L:$lCal  D:$dCal kcal"
        calorieResultText?.setTextColor(resources.getColor(R.color.accent_purple, null))
        macroSummaryText?.text = "P: ${"%.1f".format(totalProtein)}g  •  C: ${"%.1f".format(totalCarbs)}g  •  F: ${"%.1f".format(totalFat)}g"

        // Sync main calorie card with the food diary
        caloriesText?.text = totalCal.toString()

        tvProteinLabel?.text = "Protein  ${"%.1f".format(totalProtein)}g"
        tvCarbsLabel?.text   = "Carbs    ${"%.1f".format(totalCarbs)}g"
        tvFatLabel?.text     = "Fat      ${"%.1f".format(totalFat)}g"

        macroProteinBar?.progress = (totalProtein / 50f * 100).toInt().coerceIn(0, 100)
        macroCarbsBar?.progress   = (totalCarbs  / 250f * 100).toInt().coerceIn(0, 100)
        macroFatBar?.progress     = (totalFat    / 65f  * 100).toInt().coerceIn(0, 100)
    }

    override fun onResume() {
        super.onResume()
        loadLoggedData() // Load the newly scanned calories
        val prefs = activity?.getSharedPreferences("FitnessAppPrefs", Context.MODE_PRIVATE)
        prefs?.registerOnSharedPreferenceChangeListener(prefListener)
        refreshDashboard()
    }

    override fun onPause() {
        super.onPause()
        activity?.getSharedPreferences("FitnessAppPrefs", Context.MODE_PRIVATE)
            ?.unregisterOnSharedPreferenceChangeListener(prefListener)
    }
}
