package com.example.fitnessapp

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.ZonedDateTime
import java.time.LocalTime

object HealthConnectManager {

    val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class)
    )

    fun isAvailable(context: Context): Boolean {
        return HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
    }

    fun installHealthConnect(context: Context) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("market://details?id=com.google.android.apps.healthdata")
            setPackage("com.android.vending")
        }
        context.startActivity(intent)
    }

    data class HealthData(
        val steps: Long = 0,
        val distanceKm: Double = 0.0,
        val caloriesBurned: Long = 0,
        val heartRateResting: Int = 0
    )

    suspend fun fetchTodayActivity(context: Context): HealthData? {
        try {
            val client = HealthConnectClient.getOrCreate(context)
            val startTime = ZonedDateTime.now().with(LocalTime.MIN).toInstant()
            val endTime = Instant.now()

            val response = client.aggregate(
                AggregateRequest(
                    metrics = setOf(
                        StepsRecord.COUNT_TOTAL,
                        DistanceRecord.DISTANCE_TOTAL,
                        TotalCaloriesBurnedRecord.ENERGY_TOTAL
                    ),
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )

            val steps = response[StepsRecord.COUNT_TOTAL] ?: 0L
            val distanceMeters = response[DistanceRecord.DISTANCE_TOTAL]?.inMeters ?: 0.0
            val calories = response[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories ?: 0.0

            return HealthData(
                steps = steps,
                distanceKm = distanceMeters / 1000.0,
                caloriesBurned = calories.toLong(),
                heartRateResting = 72 // Placeholder or fetch actual HR
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
