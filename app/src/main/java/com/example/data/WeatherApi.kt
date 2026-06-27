package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// --- Geocoding Models ---

@JsonClass(generateAdapter = true)
data class GeocodingResponse(
    @Json(name = "results") val results: List<GeocodingResult>?
)

@JsonClass(generateAdapter = true)
data class GeocodingResult(
    @Json(name = "name") val name: String,
    @Json(name = "latitude") val latitude: Double,
    @Json(name = "longitude") val longitude: Double,
    @Json(name = "country") val country: String?,
    @Json(name = "admin1") val admin1: String?, // State/Province
    @Json(name = "timezone") val timezone: String?
)

// --- Forecast Models ---

@JsonClass(generateAdapter = true)
data class ForecastResponse(
    @Json(name = "latitude") val latitude: Double,
    @Json(name = "longitude") val longitude: Double,
    @Json(name = "timezone") val timezone: String,
    @Json(name = "current") val current: CurrentWeather?,
    @Json(name = "hourly") val hourly: HourlyForecast?,
    @Json(name = "daily") val daily: DailyForecast?
)

@JsonClass(generateAdapter = true)
data class CurrentWeather(
    @Json(name = "time") val time: String,
    @Json(name = "temperature_2m") val temperature: Double,
    @Json(name = "relative_humidity_2m") val humidity: Double,
    @Json(name = "apparent_temperature") val apparentTemperature: Double,
    @Json(name = "precipitation") val precipitation: Double,
    @Json(name = "rain") val rain: Double,
    @Json(name = "weather_code") val weatherCode: Int,
    @Json(name = "wind_speed_10m") val windSpeed: Double
)

@JsonClass(generateAdapter = true)
data class HourlyForecast(
    @Json(name = "time") val time: List<String>,
    @Json(name = "temperature_2m") val temperature: List<Double>,
    @Json(name = "precipitation_probability") val precipitationProbability: List<Int>,
    @Json(name = "weather_code") val weatherCode: List<Int>
)

@JsonClass(generateAdapter = true)
data class DailyForecast(
    @Json(name = "time") val time: List<String>,
    @Json(name = "weather_code") val weatherCode: List<Int>,
    @Json(name = "temperature_2m_max") val tempMax: List<Double>,
    @Json(name = "temperature_2m_min") val tempMin: List<Double>,
    @Json(name = "precipitation_sum") val precipitationSum: List<Double>,
    @Json(name = "precipitation_probability_max") val precipitationProbabilityMax: List<Int>
)

// --- Retrofit Interfaces ---

interface OpenMeteoGeocodingService {
    @GET("v1/search")
    suspend fun searchCity(
        @Query("name") name: String,
        @Query("count") count: Int = 10,
        @Query("language") language: String = "en",
        @Query("format") format: String = "json"
    ): GeocodingResponse
}

interface OpenMeteoForecastService {
    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,rain,weather_code,wind_speed_10m",
        @Query("hourly") hourly: String = "temperature_2m,precipitation_probability,weather_code",
        @Query("daily") daily: String = "weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum,precipitation_probability_max",
        @Query("timezone") timezone: String = "auto"
    ): ForecastResponse
}

// --- Retrofit Clients ---

object WeatherRetrofitClient {
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .build()

    private val geocodingRetrofit = Retrofit.Builder()
        .baseUrl("https://geocoding-api.open-meteo.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()

    private val forecastRetrofit = Retrofit.Builder()
        .baseUrl("https://api.open-meteo.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()

    val geocodingService: OpenMeteoGeocodingService by lazy {
        geocodingRetrofit.create(OpenMeteoGeocodingService::class.java)
    }

    val forecastService: OpenMeteoForecastService by lazy {
        forecastRetrofit.create(OpenMeteoForecastService::class.java)
    }
}
