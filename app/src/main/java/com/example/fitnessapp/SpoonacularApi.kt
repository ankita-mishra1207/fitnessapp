package com.example.fitnessapp

import com.google.gson.annotations.SerializedName
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

// Models for Spoonacular Image Recognition
data class SpoonacularImageResponse(
    @SerializedName("status") val status: String,
    @SerializedName("category") val category: String,
    @SerializedName("probability") val probability: Float,
    @SerializedName("nutrition") val nutrition: SpoonacularNutrition
)

data class SpoonacularNutrition(
    @SerializedName("calories") val calories: SpoonacularValue,
    @SerializedName("protein") val protein: SpoonacularValue,
    @SerializedName("fat") val fat: SpoonacularValue,
    @SerializedName("carbs") val carbs: SpoonacularValue
)

data class SpoonacularValue(
    @SerializedName("value") val value: Double,
    @SerializedName("unit") val unit: String
)

interface SpoonacularApiService {
    @Multipart
    @POST("food/images/analyze")
    suspend fun analyzeImage(
        @Query("apiKey") apiKey: String,
        @Part file: MultipartBody.Part
    ): Response<SpoonacularImageResponse>
}

object SpoonacularApiManager {
    private const val BASE_URL = "https://api.spoonacular.com/"
    private val API_KEY = BuildConfig.SPOONACULAR_API_KEY

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val service = retrofit.create(SpoonacularApiService::class.java)

    suspend fun analyzeFoodImage(imageBytes: ByteArray): SpoonacularImageResponse? {
        if (API_KEY == "YOUR_SPOONACULAR_KEY_HERE" || API_KEY.isEmpty()) return null

        val requestFile = imageBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("file", "image.jpg", requestFile)

        return try {
            val response = service.analyzeImage(API_KEY, body)
            if (response.isSuccessful) {
                response.body()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
