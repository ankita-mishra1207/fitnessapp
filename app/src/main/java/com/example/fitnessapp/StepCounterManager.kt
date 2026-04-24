package com.example.fitnessapp

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log

class StepCounterManager(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepCounterSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val prefs = context.getSharedPreferences("FitnessAppPrefs", Context.MODE_PRIVATE)

    private var initialStepCount = -1

    fun startListening() {
        stepCounterSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            val totalStepsSinceBoot = event.values[0].toInt()
            
            if (initialStepCount == -1) {
                initialStepCount = totalStepsSinceBoot
            }

            val stepsToday = totalStepsSinceBoot - initialStepCount
            
            // We store the local steps. In the UI, we can choose the higher value 
            // between Fitbit and Local Phone steps for maximum accuracy.
            prefs.edit().putInt("local_steps", stepsToday).apply()
            Log.d("StepCounter", "Steps recorded by phone: $stepsToday")
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
