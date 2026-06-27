package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ForecastResponse
import com.example.data.GeocodingResult
import com.example.data.GeminiWeatherService
import com.example.data.WeatherRetrofitClient
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class WeatherState(
    val isLoading: Boolean = false,
    val selectedCity: GeocodingResult = GeocodingResult(
        name = "San Francisco",
        latitude = 37.7749,
        longitude = -122.4194,
        country = "United States",
        admin1 = "California",
        timezone = "America/Los_Angeles"
    ),
    val forecast: ForecastResponse? = null,
    val error: String? = null,
    
    // Search features
    val searchQuery: String = "",
    val searchResults: List<GeocodingResult> = emptyList(),
    val isSearching: Boolean = false,
    
    // Alert personalization
    val selectedPersona: String = "Outdoor Runner",
    val personalizedAlert: String = "",
    val isAlertLoading: Boolean = false
)

class WeatherViewModel : ViewModel() {

    private val _state = MutableStateFlow(WeatherState())
    val state: StateFlow<WeatherState> = _state.asStateFlow()

    private val _searchQueryFlow = MutableStateFlow("")

    init {
        // Initial load for San Francisco
        loadWeatherForCity(_state.value.selectedCity)
        setupSearchDebounce()
    }

    fun onSearchQueryChanged(query: String) {
        _state.update { it.copy(searchQuery = query) }
        _searchQueryFlow.value = query
    }

    @OptIn(FlowPreview::class)
    private fun setupSearchDebounce() {
        viewModelScope.launch {
            _searchQueryFlow
                .debounce(500)
                .filter { it.length >= 2 }
                .distinctUntilChanged()
                .collect { query ->
                    performCitySearch(query)
                }
        }
    }

    private suspend fun performCitySearch(query: String) {
        _state.update { it.copy(isSearching = true) }
        try {
            val response = WeatherRetrofitClient.geocodingService.searchCity(query)
            _state.update {
                it.copy(
                    searchResults = response.results ?: emptyList(),
                    isSearching = false
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _state.update {
                it.copy(
                    searchResults = emptyList(),
                    isSearching = false
                )
            }
        }
    }

    fun selectCity(city: GeocodingResult) {
        _state.update {
            it.copy(
                selectedCity = city,
                searchQuery = "",
                searchResults = emptyList()
            )
        }
        loadWeatherForCity(city)
    }

    fun loadWeatherForCity(city: GeocodingResult) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val response = WeatherRetrofitClient.forecastService.getForecast(
                    latitude = city.latitude,
                    longitude = city.longitude
                )
                _state.update {
                    it.copy(
                        forecast = response,
                        isLoading = false
                    )
                }
                // Automatically generate the personalized weather alert
                generatePersonalizedAlert()
            } catch (e: Exception) {
                e.printStackTrace()
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load weather data: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    fun changePersona(persona: String) {
        _state.update { it.copy(selectedPersona = persona) }
        generatePersonalizedAlert()
    }

    fun generatePersonalizedAlert() {
        val currentState = _state.value
        val currentForecast = currentState.forecast
        val currentWeather = currentForecast?.current

        if (currentWeather == null) {
            _state.update { it.copy(personalizedAlert = "No current weather conditions found to analyze.") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isAlertLoading = true) }
            
            val conditionText = getWeatherConditionName(currentWeather.weatherCode)
            val alert = GeminiWeatherService.getPersonalizedAlert(
                condition = conditionText,
                temp = currentWeather.temperature,
                windSpeed = currentWeather.windSpeed,
                humidity = currentWeather.humidity,
                precipitation = currentWeather.precipitation,
                persona = currentState.selectedPersona
            )
            
            _state.update {
                it.copy(
                    personalizedAlert = alert,
                    isAlertLoading = false
                )
            }
        }
    }

    companion object {
        fun getWeatherConditionName(code: Int): String {
            return when (code) {
                0 -> "Clear sky"
                1 -> "Mainly clear"
                2 -> "Partly cloudy"
                3 -> "Overcast"
                45, 48 -> "Foggy"
                51, 53, 55 -> "Drizzle"
                56, 57 -> "Freezing Drizzle"
                61, 63, 65 -> "Rainy"
                66, 67 -> "Freezing Rain"
                71, 73, 75 -> "Snowy"
                77 -> "Snow grains"
                80, 81, 82 -> "Rain showers"
                85, 86 -> "Snow showers"
                95 -> "Thunderstorm"
                96, 99 -> "Thunderstorm with hail"
                else -> "Unknown weather conditions"
            }
        }
    }
}
