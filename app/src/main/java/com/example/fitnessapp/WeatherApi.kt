package com.example.fitnessapp

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApiService {
    @GET("weather")
    suspend fun getCurrentWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): WeatherResponse

    @GET("forecast")
    suspend fun getForecast(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): ForecastResponse
}

data class WeatherResponse(
    val main: Main,
    val weather: List<Weather>,
    val name: String
)

data class ForecastResponse(
    val list: List<ForecastItem>
)

data class ForecastItem(
    val dt_txt: String,
    val main: Main,
    val weather: List<Weather>
)

data class Main(
    val temp: Double
)

data class Weather(
    val description: String,
    val icon: String
)

object WeatherApiManager {
    private const val BASE_URL = "https://api.openweathermap.org/data/2.5/"
    
    // Using the user provided key
    private const val API_KEY = "ea81054feb884a5d97d428dd19fcfe83"

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val service: WeatherApiService = retrofit.create(WeatherApiService::class.java)

    suspend fun fetchWeather(lat: Double, lon: Double): WeatherResponse? {
        return try {
            service.getCurrentWeather(lat, lon, API_KEY)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun fetchForecast(lat: Double, lon: Double): ForecastResponse? {
        return try {
            service.getForecast(lat, lon, API_KEY)
        } catch (e: Exception) {
            null
        }
    }
}
