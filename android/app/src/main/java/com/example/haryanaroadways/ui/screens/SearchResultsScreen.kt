package com.example.haryanaroadways.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.EventSeat
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.haryanaroadways.BuildConfig
import com.example.haryanaroadways.data.api.Schedule
import com.example.haryanaroadways.theme.*
import com.example.haryanaroadways.ui.AppViewModelProvider
import com.example.haryanaroadways.ui.components.PriceTag
import com.example.haryanaroadways.ui.viewmodels.SearchViewModel

private fun backendAssetUrl(path: String): String {
    if (path.startsWith("http", ignoreCase = true)) return path
    val apiBase = BuildConfig.API_BASE_URL.trimEnd('/')
    val backendBase = apiBase.removeSuffix("/api")
    return "$backendBase/${path.trimStart('/')}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultsScreen(
    source: String = "",
    destination: String = "",
    date: String = "",
    onBack: () -> Unit = {},
    onNavigateBack: () -> Unit = onBack,
    onScheduleSelected: (String, Int) -> Unit,
    searchViewModel: SearchViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val searchState by searchViewModel.uiState.collectAsState()

    LaunchedEffect(source, destination, date) {
        if (source.isNotEmpty() && destination.isNotEmpty() && date.isNotEmpty()) {
            searchViewModel.updateSearchQuery(source, destination, date)
            searchViewModel.searchSchedules()
        }
    }

    val displaySource = source.ifEmpty { searchState.searchSource.ifEmpty { "Haryana" } }
    val displayDest = destination.ifEmpty { searchState.searchDestination.ifEmpty { "Destination" } }
    val displayDate = date.ifEmpty { searchState.searchDate }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "$displaySource ➔ $displayDest",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$displayDate • ${searchState.schedules.size} Available Buses",
                            color = Color.White.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { searchViewModel.searchSchedules() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryTealDark)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BgLight)
        ) {
            if (searchState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = PrimaryTeal)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Finding best departures...", color = TextSecondaryLight, fontSize = 14.sp)
                    }
                }
            } else if (searchState.error != null) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = searchState.error!!,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { searchViewModel.searchSchedules() },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Retry Search")
                    }
                }
            } else if (searchState.schedules.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = PrimaryTealUltraLight,
                        modifier = Modifier.size(88.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.DirectionsBus,
                                contentDescription = "No buses",
                                modifier = Modifier.size(44.dp),
                                tint = PrimaryTeal
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "No Buses Scheduled",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryTealDark
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No departures found for this date on the $displaySource to $displayDest route. Try selecting another date or a nearby depot.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondaryLight,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onNavigateBack,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("Modify Search Query", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(searchState.schedules) { schedule ->
                        ScheduleCard(
                            schedule = schedule,
                            onClick = { onScheduleSelected(schedule._id, schedule.effectiveFare) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleCard(
    schedule: Schedule,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Bus Header: Bus Type Badge + Bus Number
            val busType = schedule.bus?.type?.uppercase() ?: "ORDINARY EXPRESS"
            val busName = schedule.bus?.name.orEmpty().ifEmpty { "Dhan-Gaadi Fleet" }
            val isLuxury = busType.contains("VOLVO") || busType.contains("AC") || busType.contains("DELUXE")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isLuxury) AccentSaffronLight else PrimaryTealUltraLight,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isLuxury) AccentSaffron.copy(alpha = 0.5f) else PrimaryTealLight.copy(alpha = 0.5f)
                    )
                ) {
                    Text(
                        text = "$busType • $busName",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isLuxury) AccentSaffronDark else PrimaryTealDark,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                if (!schedule.bus?.busNumber.isNullOrBlank()) {
                    Text(
                        text = schedule.bus?.busNumber ?: "",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMutedLight
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bus Images Carousel (if available)
            val busImages = schedule.bus?.images ?: emptyList()
            if (busImages.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().height(100.dp)
                ) {
                    items(busImages) { url ->
                        AsyncImage(
                            model = backendAssetUrl(url),
                            contentDescription = "Bus Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .width(180.dp)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Timeline & Departure Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = schedule.departureTime.ifEmpty { "08:00 AM" },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PrimaryTealDark
                    )
                    Text(
                        text = schedule.route?.source ?: "Depot",
                        fontSize = 12.sp,
                        color = TextSecondaryLight,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Journey Duration Pill
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Direct", fontSize = 11.sp, color = TextMutedLight)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = BgLight,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = "➔➔➔",
                            fontSize = 11.sp,
                            color = PrimaryTeal,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = "${schedule.route?.totalDistance ?: 120} km",
                        fontSize = 11.sp,
                        color = TextSecondaryLight,
                        fontWeight = FontWeight.Medium
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = schedule.arrivalTime.ifEmpty { "11:30 AM" },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PrimaryTealDark
                    )
                    Text(
                        text = schedule.route?.destination ?: "Depot",
                        fontSize = 12.sp,
                        color = TextSecondaryLight,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = BorderSubtle)
            Spacer(modifier = Modifier.height(12.dp))

            // Amenities & Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EventSeat,
                        contentDescription = "Seats",
                        modifier = Modifier.size(16.dp),
                        tint = SuccessGreen
                    )
                    Text(
                        text = "${schedule.bus?.totalSeats ?: 45} Seats",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimaryLight
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "₹${schedule.effectiveFare}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PrimaryTealDark
                        )
                        Text(
                            text = "per seat",
                            fontSize = 10.sp,
                            color = TextMutedLight
                        )
                    }

                    Button(
                        onClick = onClick,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text("Select Seats", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
