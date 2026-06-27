package com.example.data

import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// --- Gemini Request / Response Models ---

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    @Json(name = "contents") val contents: List<GeminiContent>,
    @Json(name = "generationConfig") val generationConfig: GeminiGenerationConfig? = null,
    @Json(name = "systemInstruction") val systemInstruction: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    @Json(name = "parts") val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    @Json(name = "text") val text: String
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    @Json(name = "temperature") val temperature: Float? = null,
    @Json(name = "topP") val topP: Float? = null,
    @Json(name = "topK") val topK: Int? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    @Json(name = "candidates") val candidates: List<GeminiCandidate>?
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    @Json(name = "content") val content: GeminiContent?
)

// --- Gemini Retrofit API ---

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object GeminiRetrofitClient {
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
        retrofit.create(GeminiApiService::class.java)
    }
}

// --- Gemini Service Helper ---

object GeminiWeatherService {
    suspend fun getPersonalizedAlert(
        condition: String,
        temp: Double,
        windSpeed: Double,
        humidity: Double,
        precipitation: Double,
        persona: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext getLocalFallbackAlert(condition, temp, windSpeed, precipitation, persona)
        }

        val prompt = """
            Generate a personalized severe weather alert/safety advisory.
            Weather condition: $condition
            Temperature: ${temp}°C
            Wind Speed: ${windSpeed} km/h
            Relative Humidity: ${humidity}%
            Precipitation: ${precipitation} mm
            User Persona / Activity Profile: $persona
            
            Based on the weather condition and the user's active profile, write a short, highly personalized, action-oriented warning (2-3 sentences max). Talk directly to the user based on their specific needs (e.g. if they are an outdoor runner, suggest home workouts; if a gardener, suggest plants to protect; if senior, alert about heat stroke/slippery walking). Keep it urgent, friendly, and practical. Avoid markdown bullet points or extra preambles.
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(parts = listOf(GeminiPart(text = prompt)))
            ),
            generationConfig = GeminiGenerationConfig(temperature = 0.7f),
            systemInstruction = GeminiContent(
                parts = listOf(GeminiPart(text = "You are a professional meteorologist crafting hyper-personalized, action-oriented weather safety alerts for individuals with specific profiles."))
            )
        )

        try {
            val response = GeminiRetrofitClient.service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                ?: getLocalFallbackAlert(condition, temp, windSpeed, precipitation, persona)
        } catch (e: Exception) {
            e.printStackTrace()
            getLocalFallbackAlert(condition, temp, windSpeed, precipitation, persona)
        }
    }

    private fun getLocalFallbackAlert(
        condition: String,
        temp: Double,
        windSpeed: Double,
        precipitation: Double,
        persona: String
    ): String {
        val isRainy = precipitation > 0.1 || condition.contains("rain", ignoreCase = true) || condition.contains("drizzle", ignoreCase = true) || condition.contains("shower", ignoreCase = true)
        val isStormy = condition.contains("thunder", ignoreCase = true) || condition.contains("storm", ignoreCase = true)
        val isCold = temp < 10.0
        val isHot = temp > 30.0
        val isWindy = windSpeed > 25.0

        return when (persona) {
            "Outdoor Runner" -> {
                when {
                    isStormy -> "Runner Warning: Severe thunderstorms detected with lightning danger! Swap your outdoor run for an indoor treadmill session or strength workout today."
                    isRainy -> "Slippery Run Advisory: Light rain is falling. Wear high-traction running shoes, wear high-visibility gear, and watch out for slick pavement and puddles."
                    isHot -> "Heat Alert: Temperatures are a scorching $temp°C. Run early in the morning, stay fully hydrated, and reduce your usual pace to avoid heat exhaustion."
                    isCold -> "Frost Run Advisory: It's cold ($temp°C). Dress in layers, cover your extremities, and spend more time warming up your muscles indoors."
                    isWindy -> "Wind Resistance Alert: High winds ($windSpeed km/h) could affect your balance and pace. Try running on a sheltered, tree-lined path."
                    else -> "Perfect Running Weather! The current conditions are ideal for hitting the trail. Keep a steady pace and enjoy your workout!"
                }
            }
            "Home Gardener" -> {
                when {
                    isStormy || precipitation > 10.0 -> "Gardener Alert: Heavy storm with excessive rain expected! Move delicate potted plants under cover, secure taller stalks with stakes, and check soil drainage."
                    isRainy -> "Rain Update: Nature is watering your plants today. Hold off on manual watering and monitor your outdoor soil beds for moisture saturation."
                    isHot -> "Hydration Advisory: Intense heat of $temp°C will dry out topsoil rapidly. Water your plants deeply in the early morning or evening, and add mulch to retain moisture."
                    isCold -> "Frost Warning: Low temperatures ($temp°C) could threaten tender crops. Cover sensitive plants with burlap or frost blankets to protect the roots."
                    isWindy -> "Wind Shear Advisory: Gusts up to $windSpeed km/h can dry leaves and break fragile stems. Move wind-sensitive hanging baskets into sheltered zones."
                    else -> "Excellent Gardening Day! The weather is gentle. Ideal time for pruning, weeding, or planting new seeds."
                }
            }
            "Senior / Health Sensitive" -> {
                when {
                    isStormy -> "Severe Safety Warning: Active storm cells can cause localized power drops and high barometric pressure shifts. Keep your medical devices charged and stay indoors."
                    isRainy -> "Slippery Walkway Hazard: Rain makes steps and sidewalks extremely slick. Please use extra caution when walking outside, hold handrails, and consider postponing non-essential trips."
                    isHot -> "Extreme Heat Risk: High temperature ($temp°C) puts a strain on cardiovascular health. Stay inside air-conditioned rooms, drink plenty of cool water, and avoid physical strain."
                    isCold -> "Cold Safety Advisory: Cold air ($temp°C) can cause blood vessels to constrict. Wear warm thermal layers, keep indoor heating stable, and wear a scarf over your mouth when breathing outdoor air."
                    isWindy -> "Balance Alert: High winds ($windSpeed km/h) can cause stability hazards and blow particulate dust. Wear protective eyewear and step carefully on uneven ground."
                    else -> "Mild Conditions: The air is pleasant today. A light stroll in the shade or open park is highly recommended for fresh air!"
                }
            }
            "Commuter" -> {
                when {
                    isStormy -> "Transit Warning: Lightning, heavy downpours, and gusty winds may cause traffic delays, transit line disruptions, and reduced visibility. Drive slow with headlights on."
                    isRainy -> "Road Hazard Update: Wet highways are prone to hydroplaning. Increase your following distance, leave 10 minutes early, and watch out for major ponding on freeway lanes."
                    isHot -> "Engine Care Advisory: Intense outdoor heat ($temp°C) can cause engines and batteries to overheat. Check your car's coolant levels and ensure your A/C is functioning."
                    isCold -> "Frosty Commute Alert: Temperatures are low enough for black ice to form on bridges and overpasses. Allow extra braking distance and defrost your windows fully before starting."
                    isWindy -> "High Wind Commute: Keep a firm grip on the steering wheel, especially when driving high-profile vehicles on open bridges or passing semi-trucks."
                    else -> "Clear Commute: Traffic conditions are clear of weather hazards. Enjoy a smooth and safe drive to your destination!"
                }
            }
            "Parent / Kids" -> {
                when {
                    isStormy -> "Indoor Play Alert: Active storms mean lightning and heavy rain. Set up an indoor play area, play board games, or enjoy crafts inside. Stay safe from the elements."
                    isRainy -> "Wet Play Advisory: If heading out, bundle kids up in waterproof raincoats and rubber boots. It's puddle-jumping season! Keep towels ready for dry-off."
                    isHot -> "Sun & Heat Hazard: Scorching sun ($temp°C). Apply SPF 50+ sunscreen, wear protective hats, and schedule playground visits only for shaded spots during cooler hours."
                    isCold -> "Cozy Bundle Alert: Chilly temperatures ($temp°C) require warm coats, mittens, and beanies. Limit outdoor playground play to short intervals to prevent raw windburn."
                    isWindy -> "Windy Outdoor Alert: Gusts up to $windSpeed km/h make lightweight objects fly. Great day to fly a kite in an open field, but keep small children securely wrapped up!"
                    else -> "Splendid Outdoor Play! The weather is absolutely perfect for the park, cycling, or backyard games. Don't forget water bottles to stay active!"
                }
            }
            else -> {
                when {
                    isStormy -> "Severe Weather Alert: Active storms in your area. Keep indoors, close windows, and stay away from open tall structures."
                    isRainy -> "Rain Advisory: Showers are active. Bring an umbrella and expect damp outdoor conditions."
                    isHot -> "Heat Warning: Temperatures are very high. Avoid direct sun and drink plenty of fluids."
                    isCold -> "Chilly Advisory: Wear warm garments to protect against the cold."
                    isWindy -> "Wind Warning: Gusty winds are occurring. Watch for flying debris."
                    else -> "Calm Weather: No severe conditions reported in your area. Have a wonderful day!"
                }
            }
        }
    }
}
