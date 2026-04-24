package com.example.fitnessapp

object FitbitManager {
    data class FitbitActivityData(
        val steps: Int,
        val distanceKm: Double,
        val caloriesBurned: Int,
        val activeMinutes: Int,
        val heartRateResting: Int,
        val floorsClimbed: Int
    )
}
