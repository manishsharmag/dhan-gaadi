package com.example.haryanaroadways.ui.screens

import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AirlineSeatReclineExtra
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsMotorsports
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.haryanaroadways.theme.*
import com.example.haryanaroadways.ui.components.GradientHeader
import com.example.haryanaroadways.ui.viewmodels.BookingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeatSelectionScreen(
    viewModel: BookingViewModel,
    onNavigateToPassengerDetails: () -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val view = LocalView.current
    val context = LocalContext.current

    LaunchedEffect(uiState.scheduleId, uiState.journeyDate) {
        if (uiState.scheduleId.isNotEmpty() && uiState.journeyDate.isNotEmpty()) {
            viewModel.fetchSeatAvailability(uiState.scheduleId, uiState.journeyDate)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Bus Seats", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.fetchSeatAvailability(uiState.scheduleId, uiState.journeyDate) }) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh Seats",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryTealDark
                )
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = 16.dp,
                color = Color.White,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (uiState.selectedSeats.isEmpty()) "Select at least 1 seat" else "${uiState.selectedSeats.size} Seat(s) Selected",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (uiState.selectedSeats.isEmpty()) TextSecondaryLight else PrimaryTealDark
                        )
                        if (uiState.selectedSeats.isNotEmpty()) {
                            Text(
                                text = "Seats: ${uiState.selectedSeats.sorted().joinToString(", ")}",
                                fontSize = 12.sp,
                                color = AccentSaffronDark,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "₹${uiState.totalFare}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            color = PrimaryTeal
                        )
                    }

                    Button(
                        onClick = onNavigateToPassengerDetails,
                        enabled = uiState.selectedSeats.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .height(52.dp)
                            .padding(start = 16.dp)
                    ) {
                        Text("Continue ➔", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BgLight)
        ) {
            GradientHeader(
                title = "Choose Your Seat",
                subtitle = "Executive 2+2 Semi-Sleeper & Seater Coach",
                badgeText = "Front Cockpit Layout",
                bottomPadding = 24.dp
            )

            if (uiState.isFetchingSeats) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = PrimaryTeal)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Loading live seat chart...", color = TextSecondaryLight, fontSize = 14.sp)
                    }
                }
            } else if (uiState.seatsError != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = uiState.seatsError ?: "Error fetching seats",
                            color = ErrorRed,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(24.dp)
                        )
                        Button(
                            onClick = { viewModel.fetchSeatAvailability(uiState.scheduleId, uiState.journeyDate) },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Retry")
                        }
                    }
                }
            } else {
                // Legend Bar
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LegendItem(color = PrimaryTeal, label = "Available")
                        LegendItem(color = AccentSaffron, label = "Selected")
                        LegendItem(color = Color(0xFFCBD5E1), label = "Booked")
                    }
                }

                // Bus Cabin Frame Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                        .weight(1f),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp)
                    ) {
                        // Front Cockpit Cabin — Indian Right-Hand Drive (RHD): Passenger Door on Left, Driver on Right
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // LEFT: Passenger Entry Door
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFF1F5F9),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.MeetingRoom, contentDescription = "Door", tint = TextSecondaryLight, modifier = Modifier.size(20.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Passenger Door", fontSize = 12.sp, color = TextPrimaryLight, fontWeight = FontWeight.Bold)
                                    Text("Entry (Left)", fontSize = 10.sp, color = TextMutedLight)
                                }
                            }

                            // RIGHT: Driver Cabin (RHD)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Driver Cabin", fontSize = 12.sp, color = TextPrimaryLight, fontWeight = FontWeight.Bold)
                                    Text("RHD (Right)", fontSize = 10.sp, color = AccentSaffronDark, fontWeight = FontWeight.SemiBold)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = PrimaryTealUltraLight,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryTealLight.copy(alpha = 0.5f)),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.DirectionsBus, contentDescription = "Driver", tint = PrimaryTealDark, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = BorderSubtle
                        )

                        // Column Orientation Guides (Window & Aisle labels)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🪟 Win", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = TextMutedLight, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            Text("🚶 Aisle", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = TextMutedLight, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            Text("AISLE", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = PrimaryTeal, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            Text("🚶 Aisle", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = TextMutedLight, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            Text("🪟 Win", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = TextMutedLight, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        }

                        // 5-Column Grid Layout (2 seats, 1 aisle, 2 seats + 5 back row)
                        val totalSeats = uiState.totalSeats
                        val totalRegularSeats = maxOf(0, totalSeats - 5)
                        val regularRows = (totalRegularSeats + 3) / 4

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(5),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            // Regular 2+2 rows
                            for (row in 0 until regularRows) {
                                val s1 = row * 4 + 1
                                val s2 = row * 4 + 2
                                val s3 = row * 4 + 3
                                val s4 = row * 4 + 4

                                if (s1 <= totalRegularSeats) {
                                    item { SeatTile(s1, uiState, viewModel, view, context) }
                                } else {
                                    item { Spacer(modifier = Modifier.size(44.dp)) }
                                }

                                if (s2 <= totalRegularSeats) {
                                    item { SeatTile(s2, uiState, viewModel, view, context) }
                                } else {
                                    item { Spacer(modifier = Modifier.size(44.dp)) }
                                }

                                // Aisle
                                item {
                                    Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                                        Text(
                                            "${row + 1}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextMutedLight
                                        )
                                    }
                                }

                                if (s3 <= totalRegularSeats) {
                                    item { SeatTile(s3, uiState, viewModel, view, context) }
                                } else {
                                    item { Spacer(modifier = Modifier.size(44.dp)) }
                                }

                                if (s4 <= totalRegularSeats) {
                                    item { SeatTile(s4, uiState, viewModel, view, context) }
                                } else {
                                    item { Spacer(modifier = Modifier.size(44.dp)) }
                                }
                            }

                            // Back Row (last 5 seats across all 5 columns)
                            for (s in (totalRegularSeats + 1)..totalSeats) {
                                item { SeatTile(s, uiState, viewModel, view, context) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SeatTile(
    seatNumber: Int,
    uiState: com.example.haryanaroadways.ui.viewmodels.BookingState,
    viewModel: BookingViewModel,
    view: android.view.View,
    context: android.content.Context
) {
    val isSelected = uiState.selectedSeats.contains(seatNumber)
    val isUnavailable = uiState.unavailableSeats.contains(seatNumber)

    val bgColor = when {
        isUnavailable -> Color(0xFFE2E8F0) // Muted Slate Gray for booked
        isSelected -> AccentSaffron
        else -> PrimaryTeal
    }

    val contentColor = when {
        isUnavailable -> Color(0xFF94A3B8)
        isSelected -> Color.Black
        else -> Color.White
    }

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .then(
                if (isSelected) Modifier.border(2.dp, AccentSaffronDark, RoundedCornerShape(12.dp))
                else if (isUnavailable) Modifier.border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                else Modifier.border(1.dp, PrimaryTealLight.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            )
            .clickable(enabled = !isUnavailable) {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                if (!isSelected && uiState.selectedSeats.size >= 6) {
                    Toast.makeText(context, "Maximum 6 seats allowed per booking", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.toggleSeatSelection(seatNumber)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.AirlineSeatReclineExtra,
                contentDescription = null,
                tint = contentColor.copy(alpha = 0.8f),
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = seatNumber.toString(),
                color = contentColor,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp,
                lineHeight = 12.sp
            )
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = TextPrimaryLight,
            fontWeight = FontWeight.SemiBold
        )
    }
}
