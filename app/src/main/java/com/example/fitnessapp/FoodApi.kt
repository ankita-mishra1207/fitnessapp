package com.example.fitnessapp

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// Models for USDA FoodData Central API
data class UsdaSearchResponse(
    @SerializedName("foods") val foods: List<UsdaFood>
)

data class UsdaFood(
    @SerializedName("fdcId") val fdcId: Int,
    @SerializedName("description") val description: String,
    @SerializedName("foodNutrients") val foodNutrients: List<UsdaNutrient>
)

data class UsdaNutrient(
    @SerializedName("nutrientId") val nutrientId: Int,
    @SerializedName("nutrientName") val nutrientName: String,
    @SerializedName("unitName") val unitName: String,
    @SerializedName("value") val value: Double
)

// Retrofit Service Interface
interface FoodApiService {
    @GET("fdc/v1/foods/search")
    suspend fun searchUsdaFood(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("pageSize") pageSize: Int = 1
    ): Response<UsdaSearchResponse>
}

// Manager Object to handle API calls
object FoodApiManager {
    private const val BASE_URL = "https://api.nal.usda.gov/"
    private val USDA_API_KEY = BuildConfig.USDA_API_KEY

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val service = retrofit.create(FoodApiService::class.java)

    suspend fun getFoodNutrition(query: String): FoodNutrition? {
        if (USDA_API_KEY.isEmpty()) return null
        
        return try {
            val response = service.searchUsdaFood(USDA_API_KEY, query)
            if (response.isSuccessful) {
                val food = response.body()?.foods?.firstOrNull() ?: return null
                
                var calories = 0
                var protein = 0f
                var carbs = 0f
                var fat = 0f

                // USDA nutrient IDs: 1008 (Energy), 1003 (Protein), 1005 (Carbohydrate), 1004 (Fat)
                food.foodNutrients.forEach { nutrient ->
                    when (nutrient.nutrientId) {
                        1008 -> calories = nutrient.value.toInt()
                        1003 -> protein = nutrient.value.toFloat()
                        1005 -> carbs = nutrient.value.toFloat()
                        1004 -> fat = nutrient.value.toFloat()
                    }
                }

                if (calories == 0) {
                    calories = food.foodNutrients.find { it.nutrientId == 2047 }?.value?.toInt() ?: 0
                }

                FoodNutrition(calories, protein, carbs, fat)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
