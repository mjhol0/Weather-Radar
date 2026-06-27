package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CurrentWeather
import com.example.data.GeocodingResult
import com.example.data.HourlyForecast
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(
    viewModel: WeatherViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    // Background gradient depending on weather condition
    val currentCode = state.forecast?.current?.weatherCode ?: 0

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B131E))
            .drawBehind {
                // Draw beautiful soft weather-based background ambient glow blobs
                val colorTheme = when (currentCode) {
                    0, 1 -> Color(0xFFFFD54F) // Sunshine warm gold
                    2, 3 -> Color(0xFF90A4AE) // Cloudy slate grey
                    45, 48 -> Color(0xFFB0BEC5) // Foggy light grey
                    51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> Color(0xFF29B6F6) // Rain beautiful sky blue
                    71, 73, 75, 77, 85, 86 -> Color(0xFFE0F7FA) // Snowy ice blue
                    95, 96, 99 -> Color(0xFFFFB74D) // Thunder electric orange/yellow
                    else -> Color(0xFF1E2640)
                }
                
                // Draw a soft glowing sphere at the top-right
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(colorTheme.copy(alpha = 0.16f), Color.Transparent),
                        center = Offset(size.width * 0.85f, size.height * 0.15f),
                        radius = size.width * 0.8f
                    ),
                    radius = size.width * 0.8f,
                    center = Offset(size.width * 0.85f, size.height * 0.15f)
                )
                
                // Draw a complementary soft glowing sphere at middle-left
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF5C6F7E).copy(alpha = 0.10f), Color.Transparent),
                        center = Offset(size.width * 0.15f, size.height * 0.55f),
                        radius = size.width * 0.6f
                    ),
                    radius = size.width * 0.6f,
                    center = Offset(size.width * 0.15f, size.height * 0.55f)
                )
            }
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // --- HEADER TITLE ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Thunderstorm,
                        contentDescription = null,
                        tint = Color(0xFFFFD54F),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SkyRadar",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                }

                // Small badge for online status / API key check
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFF81C784), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Doppler Active",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // --- CITY SEARCH BAR ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .zIndex(2f)
            ) {
                Column {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        placeholder = { Text("Search city (e.g. London, Paris...)", color = Color(0xFFE2E2E6).copy(alpha = 0.5f)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFFE2E2E6)) },
                        trailingIcon = {
                            if (state.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color(0xFFE2E2E6))
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFD3E3FD),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                            focusedContainerColor = Color(0xFF1F2A37).copy(alpha = 0.5f),
                            unfocusedContainerColor = Color(0xFF1F2A37).copy(alpha = 0.3f),
                            focusedLabelColor = Color(0xFFE2E2E6),
                            unfocusedLabelColor = Color(0xFFE2E2E6).copy(alpha = 0.6f),
                            focusedTextColor = Color(0xFFE2E2E6),
                            unfocusedTextColor = Color(0xFFE2E2E6)
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(
                            onSearch = { focusManager.clearFocus() }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("city_search_input")
                    )

                    // Dropdown search results list
                    AnimatedVisibility(
                        visible = state.searchResults.isNotEmpty() || state.isSearching,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF0F172A).copy(alpha = 0.95f)
                            ),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                                .padding(top = 8.dp)
                        ) {
                            if (state.isSearching) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = Color(0xFFFFD54F), modifier = Modifier.size(28.dp))
                                }
                            } else {
                                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                    state.searchResults.forEach { result ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    viewModel.selectCity(result)
                                                    focusManager.clearFocus()
                                                }
                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFFFD54F), modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = "${result.name}, ${result.admin1 ?: ""}",
                                                    color = Color.White,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = result.country ?: "",
                                                    color = Color.White.copy(alpha = 0.6f),
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else {
                // --- CURRENT WEATHER CARD ---
                CurrentWeatherSection(
                    city = state.selectedCity,
                    current = state.forecast?.current,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // --- HOURLY FORECAST (HORIZONTAL SCROLL) ---
                state.forecast?.hourly?.let { hourly ->
                    HourlyForecastSection(
                        hourly = hourly,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )
                }

                // --- INTERACTIVE DOPPLER RADAR ---
                DopplerRadarSection(
                    cityName = state.selectedCity.name,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                // --- PERSONALIZED SEVERE ALERTS & ADVISORIES ---
                PersonalizedAlertsSection(
                    state = state,
                    onChangePersona = { viewModel.changePersona(it) },
                    onRefresh = { viewModel.generatePersonalizedAlert() },
                    modifier = Modifier.padding(bottom = 40.dp)
                )
            }
        }
    }
}

// --- CURRENT CONDITIONS COMPONENT ---
@Composable
fun CurrentWeatherSection(
    city: GeocodingResult,
    current: CurrentWeather?,
    modifier: Modifier = Modifier
) {
    if (current == null) return

    val conditionName = WeatherViewModel.getWeatherConditionName(current.weatherCode)

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1F2A37).copy(alpha = 0.45f)
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Location Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color(0xFFEF5350),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = city.name,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Text(
                text = "${city.admin1 ?: ""}, ${city.country ?: ""}",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Main Temperature and Icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                WeatherConditionIcon(
                    code = current.weatherCode,
                    modifier = Modifier.size(76.dp)
                )
                Spacer(modifier = Modifier.width(20.dp))
                Column {
                    Text(
                        text = "${current.temperature.toInt()}°C",
                        fontSize = 58.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        lineHeight = 58.sp
                    )
                    Text(
                        text = conditionName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFFD54F)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Sub-details grid (Feels like, wind, humidity)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(20.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
                    .padding(vertical = 14.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                WeatherDetailItem(
                    icon = Icons.Outlined.DeviceThermostat,
                    label = "Feels Like",
                    value = "${current.apparentTemperature.toInt()}°C"
                )
                WeatherDetailItem(
                    icon = Icons.Outlined.Air,
                    label = "Wind Speed",
                    value = "${current.windSpeed.toInt()} km/h"
                )
                WeatherDetailItem(
                    icon = Icons.Outlined.WaterDrop,
                    label = "Humidity",
                    value = "${current.humidity.toInt()}%"
                )
            }
        }
    }
}

@Composable
fun WeatherDetailItem(
    icon: ImageVector,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(90.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

// --- HOURLY FORECAST LIST COMPONENT ---
@Composable
fun HourlyForecastSection(
    hourly: HourlyForecast,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1F2A37).copy(alpha = 0.40f)
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Hourly Forecast",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Horizontal Scroll list of next 24 hours (subsampled to fit nicely)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Show hourly forecast for the next 12 hours
                val displayCount = minOf(hourly.time.size, 16)
                itemsIndexed((0 until displayCount).toList()) { index, _ ->
                    val rawTime = hourly.time[index]
                    // Parse "2026-06-27T11:00" -> "11:00"
                    val hourString = rawTime.substringAfter("T").take(5)
                    val temp = hourly.temperature[index].toInt()
                    val code = hourly.weatherCode[index]
                    val rainProb = hourly.precipitationProbability[index]

                    HourlyItem(
                        time = hourString,
                        temp = temp,
                        code = code,
                        rainProbability = rainProb,
                        isSelected = (index == 0)
                    )
                }
            }
        }
    }
}

@Composable
fun HourlyItem(
    time: String,
    temp: Int,
    code: Int,
    rainProbability: Int,
    isSelected: Boolean = false
) {
    val containerBg = if (isSelected) Color(0xFFD3E3FD).copy(alpha = 0.12f) else Color.White.copy(alpha = 0.04f)
    val borderAlpha = if (isSelected) 0.15f else 0.04f
    Column(
        modifier = Modifier
            .width(68.dp)
            .background(containerBg, RoundedCornerShape(20.dp))
            .border(1.dp, Color.White.copy(alpha = borderAlpha), RoundedCornerShape(20.dp))
            .padding(vertical = 14.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = time,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.7f),
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        WeatherConditionIcon(
            code = code,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "$temp°C",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.WaterDrop,
                contentDescription = null,
                tint = Color(0xFF4FC3F7),
                modifier = Modifier.size(10.dp)
            )
            Text(
                text = "$rainProbability%",
                fontSize = 10.sp,
                color = Color(0xFF4FC3F7),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// --- INTERACTIVE DOPPLER RADAR SIMULATION ---
data class RadarCell(
    val x: Float, // -1f to 1f relative to center
    val y: Float, // -1f to 1f relative to center
    val radius: Float,
    val intensity: Color,
    val name: String
)

@Composable
fun DopplerRadarSection(
    cityName: String,
    modifier: Modifier = Modifier
) {
    // Interactive radar state: zoom, scan speed, play/pause state
    var zoomLevel by remember { mutableFloatStateOf(1.0f) }
    var scanSpeedMultiplier by remember { mutableFloatStateOf(1.0f) }
    var isScanning by remember { mutableStateOf(true) }

    // Scan line animation
    val infiniteTransition = rememberInfiniteTransition(label = "RadarSweep")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (4000 / scanSpeedMultiplier).toInt(),
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarSweepAngle"
    )

    // Drift offset for storm cell movement (simulates active weather moving)
    val driftOffset by infiniteTransition.animateFloat(
        initialValue = -0.15f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "StormDrift"
    )

    // Radar colors
    val radarPrimaryColor = Color(0xFF00E676) // Bright green
    val radarHeavyColor = Color(0xFFEF5350)   // Red (Heavy storm)
    val radarMediumColor = Color(0xFFFFCA28)  // Orange/Yellow (Moderate)
    val radarLightColor = Color(0xFF42A5F5)   // Blue (Light rain)

    // Predefined active storm cell echoes centered loosely around user city
    val stormCells = remember {
        listOf(
            RadarCell(0.25f, -0.3f, 40f, radarHeavyColor, "Cell Alpha (Severe Hail)"),
            RadarCell(0.32f, -0.28f, 75f, radarMediumColor, "Precip Core"),
            RadarCell(0.18f, -0.34f, 90f, radarLightColor, "Rain Shield"),
            RadarCell(-0.45f, 0.2f, 30f, radarMediumColor, "Cell Beta (Thunderstorm)"),
            RadarCell(-0.48f, 0.25f, 60f, radarLightColor, "Rain Shield"),
            RadarCell(-0.1f, 0.45f, 45f, radarLightColor, "Scattered Showers")
        )
    }

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1F2A37).copy(alpha = 0.40f)
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Radar Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Doppler Radar HD",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Active Scope: $cityName Radar Station",
                        fontSize = 11.sp,
                        color = Color(0xFF00E676),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(if (isScanning) Color(0xFF00E676) else Color.Red, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isScanning) "LIVE STREAM" else "PAUSED",
                        color = if (isScanning) Color(0xFF00E676) else Color.Red,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // THE RADAR CANVAS VIEW
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.2f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF111827))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("radar_doppler_canvas")
                ) {
                    val center = Offset(size.width / 2f, size.height / 2f)

                    // Draw beautiful soft stylized terrain/weather radar background blobs (Frosted Glass accent blobs)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF4CAF50).copy(alpha = 0.15f), Color.Transparent),
                            center = Offset(size.width * 0.35f, size.height * 0.25f),
                            radius = size.width * 0.35f
                        ),
                        radius = size.width * 0.35f,
                        center = Offset(size.width * 0.35f, size.height * 0.25f)
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFFFC107).copy(alpha = 0.12f), Color.Transparent),
                            center = Offset(size.width * 0.6f, size.height * 0.35f),
                            radius = size.width * 0.45f
                        ),
                        radius = size.width * 0.45f,
                        center = Offset(size.width * 0.6f, size.height * 0.35f)
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF2196F3).copy(alpha = 0.18f), Color.Transparent),
                            center = Offset(size.width * 0.25f, size.height * 0.6f),
                            radius = size.width * 0.3f
                        ),
                        radius = size.width * 0.3f,
                        center = Offset(size.width * 0.25f, size.height * 0.6f)
                    )
                    val maxRadius = minOf(size.width, size.height) / 2.1f

                    // Draw circular radar range grids
                    val gridCounts = 4
                    for (i in 1..gridCounts) {
                        val ringRadius = maxRadius * (i.toFloat() / gridCounts)
                        drawCircle(
                            color = radarPrimaryColor.copy(alpha = 0.12f),
                            radius = ringRadius,
                            center = center,
                            style = Stroke(
                                width = 1.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                            )
                        )
                    }

                    // Draw crosshairs
                    drawLine(
                        color = radarPrimaryColor.copy(alpha = 0.15f),
                        start = Offset(center.x - maxRadius, center.y),
                        end = Offset(center.x + maxRadius, center.y),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = radarPrimaryColor.copy(alpha = 0.15f),
                        start = Offset(center.x, center.y - maxRadius),
                        end = Offset(center.x, center.y + maxRadius),
                        strokeWidth = 1.dp.toPx()
                    )

                    // Draw storm cells with animation drift and zoom scaling
                    stormCells.forEach { cell ->
                        // Apply simulated drift direction (heading North-East)
                        val animX = cell.x + driftOffset * 0.4f
                        val animY = cell.y - driftOffset * 0.3f

                        val drawX = center.x + animX * maxRadius * zoomLevel
                        val drawY = center.y + animY * maxRadius * zoomLevel

                        // Only draw if within radar perimeter
                        val distanceToCenter = kotlin.math.hypot(drawX - center.x, drawY - center.y)
                        if (distanceToCenter < maxRadius) {
                            // Radar echoes are drawn as soft blurry overlapping circles (pulsing rain echoes)
                            drawCircle(
                                color = cell.intensity.copy(alpha = 0.35f),
                                radius = cell.radius * zoomLevel,
                                center = Offset(drawX, drawY)
                            )
                            drawCircle(
                                color = cell.intensity.copy(alpha = 0.6f),
                                radius = cell.radius * 0.5f * zoomLevel,
                                center = Offset(drawX, drawY)
                            )
                            drawCircle(
                                color = Color.White.copy(alpha = 0.4f),
                                radius = 2.dp.toPx(),
                                center = Offset(drawX, drawY)
                            )
                        }
                    }

                    // Draw Doppler sweeping sweep beam
                    if (isScanning) {
                        val sweepRad = Math.toRadians(rotationAngle.toDouble())
                        val sweepX = center.x + maxRadius * cos(sweepRad).toFloat()
                        val sweepY = center.y + maxRadius * sin(sweepRad).toFloat()

                        // Sweeping Line
                        drawLine(
                            color = radarPrimaryColor,
                            start = center,
                            end = Offset(sweepX, sweepY),
                            strokeWidth = 1.5.dp.toPx(),
                            cap = StrokeCap.Round
                        )

                        // Visual sweep trails (Soft gradient circle representing trailing radar echo)
                        for (i in 1..12) {
                            val trailAngle = rotationAngle - (i * 2.5f)
                            val trailRad = Math.toRadians(trailAngle.toDouble())
                            val tx = center.x + maxRadius * cos(trailRad).toFloat()
                            val ty = center.y + maxRadius * sin(trailRad).toFloat()
                            drawLine(
                                color = radarPrimaryColor.copy(alpha = 0.25f / i),
                                start = center,
                                end = Offset(tx, ty),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                    }

                    // Range Ring Labels
                    drawCircle(
                        color = radarPrimaryColor.copy(alpha = 0.2f),
                        radius = maxRadius,
                        center = center,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }

                // N, S, E, W cardinal labels overlay
                Text("N", color = Color(0xFF00E676), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.TopCenter).padding(top = 4.dp))
                Text("S", color = Color(0xFF00E676), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp))
                Text("W", color = Color(0xFF00E676), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterStart).padding(start = 6.dp))
                Text("E", color = Color(0xFF00E676), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 6.dp))

                // Zoom Indicator overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "ZOOM: ${zoomLevel.toInt()}x",
                        color = Color(0xFF00E676),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // RADAR CONTROL ROW
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Zoom selector
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Zoom", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, modifier = Modifier.padding(end = 6.dp))
                    listOf(1.0f, 2.0f, 4.0f).forEach { z ->
                        val active = zoomLevel == z
                        Box(
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (active) Color(0xFF00E676) else Color.White.copy(alpha = 0.1f))
                                .clickable { zoomLevel = z }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${z.toInt()}x",
                                color = if (active) Color.Black else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Sweep speed selector
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Speed", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, modifier = Modifier.padding(end = 6.dp))
                    listOf(0.5f, 1.0f, 2.0f).forEach { speed ->
                        val active = scanSpeedMultiplier == speed
                        val speedLabel = when(speed) {
                            0.5f -> "Slow"
                            1.0f -> "Norm"
                            2.0f -> "Fast"
                            else -> "Norm"
                        }
                        Box(
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (active) Color(0xFF00E676) else Color.White.copy(alpha = 0.1f))
                                .clickable { scanSpeedMultiplier = speed }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = speedLabel,
                                color = if (active) Color.Black else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Play / Pause scope sweep
                IconButton(
                    onClick = { isScanning = !isScanning },
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isScanning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "Toggle sweep animation",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Storm tracking status details
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(16.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.TrackChanges,
                    contentDescription = null,
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Cell Alpha heading North-East at 28 km/h.",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Estimated ETA: 35 minutes to metro center. Local heavy rain & hail possible.",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

// --- PERSONALIZED SEVERE WEATHER WARNINGS COMPONENT ---
@Composable
fun PersonalizedAlertsSection(
    state: WeatherState,
    onChangePersona: (String) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val personas = listOf("Outdoor Runner", "Home Gardener", "Senior / Health Sensitive", "Commuter", "Parent / Kids")

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1F2A37).copy(alpha = 0.40f)
        ),
        border = BorderStroke(1.5.dp, Color(0xFFFFB74D).copy(alpha = 0.25f)), // Glowing alert orange border
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header with Alert Pulsating Light
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFFB74D),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Personalized Warnings",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                IconButton(
                    onClick = onRefresh,
                    enabled = !state.isAlertLoading,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Advisory",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Text(
                text = "Select your daily activity profile to customize meteorologist safety insights:",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            // Horizontal Persona Picker
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                itemsIndexed(personas) { _, persona ->
                    val isSelected = state.selectedPersona == persona
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Color(0xFFFFB74D) else Color.White.copy(alpha = 0.08f))
                            .clickable { onChangePersona(persona) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("persona_chip_${persona.replace(" ", "_").lowercase()}")
                    ) {
                        Text(
                            text = persona,
                            color = if (isSelected) Color.Black else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Advisory Content Block with dynamic loading state
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(20.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
                    .padding(16.dp)
                    .testTag("personalized_alert_box")
            ) {
                if (state.isAlertLoading) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = Color(0xFFFFB74D), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Analyzing conditions with Gemini AI...",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = when (state.selectedPersona) {
                                    "Outdoor Runner" -> Icons.Default.DirectionsRun
                                    "Home Gardener" -> Icons.Default.Agriculture
                                    "Senior / Health Sensitive" -> Icons.Default.AccessibilityNew
                                    "Commuter" -> Icons.Default.DriveEta
                                    "Parent / Kids" -> Icons.Default.ChildCare
                                    else -> Icons.Default.Info
                                },
                                contentDescription = null,
                                tint = Color(0xFFFFB74D),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ADVISORY FOR: ${state.selectedPersona.uppercase()}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFFFB74D),
                                letterSpacing = 0.8.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = state.personalizedAlert,
                            fontSize = 14.sp,
                            color = Color.White,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

// --- DYNAMIC WEATHER VECTOR ICON COMPONENT ---
@Composable
fun WeatherConditionIcon(
    code: Int,
    modifier: Modifier = Modifier
) {
    val (icon, tint) = when (code) {
        0 -> Pair(Icons.Filled.WbSunny, Color(0xFFFFD54F)) // clear
        1 -> Pair(Icons.Filled.WbCloudy, Color(0xFFFFE082)) // mainly clear
        2 -> Pair(Icons.Filled.Cloud, Color(0xFF90A4AE)) // partly cloudy
        3 -> Pair(Icons.Filled.Cloud, Color(0xFF78909C)) // overcast
        45, 48 -> Pair(Icons.Filled.Dehaze, Color(0xFFB0BEC5)) // fog
        51, 53, 55, 56, 57 -> Pair(Icons.Filled.Umbrella, Color(0xFF4FC3F7)) // drizzle
        61, 63, 65, 66, 67, 80, 81, 82 -> Pair(Icons.Filled.Water, Color(0xFF29B6F6)) // rain / showers
        71, 73, 75, 77, 85, 86 -> Pair(Icons.Filled.AcUnit, Color(0xFFE0F7FA)) // snow
        95, 96, 99 -> Pair(Icons.Filled.FlashOn, Color(0xFFFFD54F)) // thunderstorm
        else -> Pair(Icons.Filled.Cloud, Color(0xFF90A4AE))
    }

    Icon(
        imageVector = icon,
        contentDescription = "Weather Icon",
        tint = tint,
        modifier = modifier
    )
}

// --- HELPER DYNAMIC GRADIENT GENERATOR ---
fun getBackgroundGradientForCode(code: Int): Brush {
    return when (code) {
        0, 1 -> Brush.verticalGradient(
            colors = listOf(
                Color(0xFF2196F3), // Bright Sky Blue
                Color(0xFF1565C0)  // Deep Blue
            )
        )
        2, 3 -> Brush.verticalGradient(
            colors = listOf(
                Color(0xFF607D8B), // Slate Grey
                Color(0xFF37474F)  // Dark Slate
            )
        )
        45, 48 -> Brush.verticalGradient(
            colors = listOf(
                Color(0xFF78909C), // Fog Blue
                Color(0xFF455A64)  // Fog Charcoal
            )
        )
        51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> Brush.verticalGradient(
            colors = listOf(
                Color(0xFF455A64), // Wet Slate
                Color(0xFF1A237E)  // Navy Rain
            )
        )
        71, 73, 75, 77, 85, 86 -> Brush.verticalGradient(
            colors = listOf(
                Color(0xFF80DEEA), // Ice Cyan
                Color(0xFF006064)  // Deep Ice
            )
        )
        95, 96, 99 -> Brush.verticalGradient(
            colors = listOf(
                Color(0xFF263238), // Thunder Gray
                Color(0xFF0D1B2A)  // Dark Midnight
            )
        )
        else -> Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1E2640),
                Color(0xFF0D1220)
            )
        )
    }
}
