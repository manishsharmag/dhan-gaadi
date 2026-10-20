package com.example.haryanaroadways.ui.screens

import android.content.Intent
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.haryanaroadways.theme.AccentSaffron
import com.example.haryanaroadways.theme.ErrorRed
import com.example.haryanaroadways.theme.InfoBlue
import com.example.haryanaroadways.theme.PrimaryTeal
import com.example.haryanaroadways.theme.PrimaryTealDark
import com.example.haryanaroadways.theme.SuccessGreen
import com.example.haryanaroadways.ui.AppViewModelProvider
import com.example.haryanaroadways.ui.viewmodels.PosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    onNavigateBack: () -> Unit,
    onNavigateToTicket: (String) -> Unit,
    posViewModel: PosViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by posViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val view = LocalView.current

    if (uiState.successPnr != null) {
        // Success Receipt Screen
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PrimaryTeal)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.size(90.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = "Success",
                            tint = Color.White,
                            modifier = Modifier.size(60.dp)
                        )
                    }
                }

                Text(
                    "Ticket Issued Successfully!",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("BOARDING PNR", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            uiState.successPnr!!,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryTealDark,
                            textAlign = TextAlign.Center,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Seats: ${uiState.selectedSeats.sorted().joinToString(", ")}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // WhatsApp Direct Share Button
                val selectedSched = uiState.schedules.find { it._id == uiState.selectedScheduleId }
                val routeSource = selectedSched?.route?.source ?: "Haryana"
                val routeDest = selectedSched?.route?.destination ?: "Roadways"
                val departureTime = selectedSched?.departureTime ?: "--"
                val busNum = selectedSched?.bus?.busNumber ?: ""
                val busType = selectedSched?.bus?.type ?: ""
                val fare = (selectedSched?.fare ?: 100) * uiState.selectedSeats.size

                Button(
                    onClick = {
                        val pnr = uiState.successPnr!!
                        val cleanMobile = uiState.passengerMobile.replace(Regex("\\D"), "").takeLast(10)
                        val seatStr = uiState.selectedSeats.sorted().joinToString(", ")
                        val shareMessage = """
                            🚌 *DHAN-GAADI FLEET e-TICKET*
                            ━━━━━━━━━━━━━━━━━━━━
                            🎫 *PNR:* $pnr
                            👤 *Passenger:* ${uiState.passengerName}
                            📍 *Route:* $routeSource ➔ $routeDest
                            📅 *Time:* $departureTime
                            💺 *Seats:* $seatStr
                            💵 *Fare Paid:* Rs. $fare (${uiState.paymentMethod.uppercase()})
                            ${if (busNum.isNotBlank()) "🚍 *Bus:* $busNum ($busType)\n" else ""}
                            📲 *View & Download Official Ticket:*
                            ${com.example.haryanaroadways.BuildConfig.API_BASE_URL.removeSuffix("/api")}/ticket/$pnr
                            ━━━━━━━━━━━━━━━━━━━━
                            _Valid for journey on Dhan-Gaadi Fleet. Show this QR code to the conductor._
                        """.trimIndent()

                        try {
                            val uri = android.net.Uri.parse("https://api.whatsapp.com/send?phone=91$cleanMobile&text=${android.net.Uri.encode(shareMessage)}")
                            val waIntent = Intent(Intent.ACTION_VIEW, uri)
                            context.startActivity(waIntent)
                        } catch (e: Exception) {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareMessage)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Ticket via WhatsApp"))
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("📲 Share Ticket on WhatsApp", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        val pnr = uiState.successPnr!!
                        posViewModel.reset()
                        onNavigateToTicket(pnr)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = PrimaryTeal),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("View / Print Ticket", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { posViewModel.reset() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Issue Next Ticket", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                TextButton(
                    onClick = onNavigateBack
                ) {
                    Text("Return to Dashboard", color = Color.White.copy(alpha = 0.8f))
                }
            }
        }
        return
    }

    // Normal State (Conductor Walk-in Terminal)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Conductor POS Terminal", color = Color.White, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryTealDark),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Schedule Selector Header
            Text("1. Select Bus Schedule", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PrimaryTealDark)

            if (uiState.schedules.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Text("No active bus schedules for today.", modifier = Modifier.padding(16.dp), color = Color.Gray)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    uiState.schedules.forEach { schedule ->
                        val isSelected = schedule._id == uiState.selectedScheduleId
                        Card(
                            onClick = { posViewModel.updateForm(scheduleId = schedule._id) },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) PrimaryTeal.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) PrimaryTeal else Color.LightGray.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { posViewModel.updateForm(scheduleId = schedule._id) },
                                    colors = RadioButtonDefaults.colors(selectedColor = PrimaryTeal)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${schedule.route?.source} ➔ ${schedule.route?.destination}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Schedule, contentDescription = "Time", modifier = Modifier.size(13.dp), tint = Color.Gray)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("${schedule.departureTime} • ${schedule.bus?.type?.uppercase() ?: "ORDINARY"}", style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                                    }
                                }
                                Text(
                                    "₹${schedule.effectiveFare}",
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryTealDark,
                                    fontSize = 17.sp
                                )
                            }
                        }
                    }
                }
            }

            // Passenger Info
            Text("2. Passenger Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PrimaryTealDark)

            OutlinedTextField(
                value = uiState.passengerName,
                onValueChange = { posViewModel.updateForm(name = it) },
                label = { Text("Passenger Name") },
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryTeal) },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.passengerMobile,
                onValueChange = { if (it.length <= 10 && it.all { c -> c.isDigit() }) posViewModel.updateForm(mobile = it) },
                label = { Text("10-Digit Mobile Number") },
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = PrimaryTeal) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )

            // Seat Selector
            if (uiState.selectedScheduleId.isNotEmpty()) {
                Text("3. Choose Seats", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PrimaryTealDark)

                if (uiState.isFetchingSeats) {
                    Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryTeal)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        PosLegendItem(color = PrimaryTeal, label = "Available")
                        PosLegendItem(color = AccentSaffron, label = "Selected")
                        PosLegendItem(color = Color(0xFFCBD5E1), label = "Booked")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val totalSeats = uiState.totalSeats
                    val totalRegular = maxOf(0, totalSeats - 5)
                    val rows = (totalRegular + 3) / 4

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Front Cockpit (Indian RHD: Door on Left, Driver on Right)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MeetingRoom, contentDescription = "Door", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Door (Entry)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Driver (RHD)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryTealDark)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.DirectionsBus, contentDescription = "Driver", tint = PrimaryTealDark, modifier = Modifier.size(16.dp))
                            }
                        }

                        // Column Guide
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Win", fontSize = 9.sp, color = Color.Gray, modifier = Modifier.width(38.dp), textAlign = TextAlign.Center)
                            Text("Aisle", fontSize = 9.sp, color = Color.Gray, modifier = Modifier.width(38.dp), textAlign = TextAlign.Center)
                            Text("AISLE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = PrimaryTeal, modifier = Modifier.width(38.dp), textAlign = TextAlign.Center)
                            Text("Aisle", fontSize = 9.sp, color = Color.Gray, modifier = Modifier.width(38.dp), textAlign = TextAlign.Center)
                            Text("Win", fontSize = 9.sp, color = Color.Gray, modifier = Modifier.width(38.dp), textAlign = TextAlign.Center)
                        }

                        for (r in 0 until rows) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val s1 = r * 4 + 1
                                val s2 = r * 4 + 2
                                val s3 = r * 4 + 3
                                val s4 = r * 4 + 4

                                if (s1 <= totalRegular) PosSeatTile(s1, uiState, posViewModel, view, context) else Spacer(modifier = Modifier.size(38.dp))
                                if (s2 <= totalRegular) PosSeatTile(s2, uiState, posViewModel, view, context) else Spacer(modifier = Modifier.size(38.dp))
                                Box(modifier = Modifier.size(38.dp), contentAlignment = Alignment.Center) {
                                    Text("${r + 1}", fontSize = 10.sp, color = Color.LightGray)
                                }
                                if (s3 <= totalRegular) PosSeatTile(s3, uiState, posViewModel, view, context) else Spacer(modifier = Modifier.size(38.dp))
                                if (s4 <= totalRegular) PosSeatTile(s4, uiState, posViewModel, view, context) else Spacer(modifier = Modifier.size(38.dp))
                            }
                        }

                        // Back row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            for (s in (totalRegular + 1)..totalSeats) {
                                PosSeatTile(s, uiState, posViewModel, view, context)
                            }
                        }
                    }
                }
            }

            // Payment Method
            Text("4. Payment Collection", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PrimaryTealDark)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf("cash" to "💵 Cash Collected", "upi_direct" to "📱 Direct UPI / QR").forEach { (method, label) ->
                    val isSelected = uiState.paymentMethod == method
                    FilterChip(
                        selected = isSelected,
                        onClick = { posViewModel.updateForm(paymentMethod = method) },
                        label = { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryTeal.copy(alpha = 0.15f),
                            selectedLabelColor = PrimaryTealDark
                        )
                    )
                }
            }

            if (uiState.error != null) {
                Text(uiState.error!!, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }

            val selectedSchedule = uiState.schedules.find { it._id == uiState.selectedScheduleId }
            val numPassengers = uiState.selectedSeats.size
            val totalFare = (selectedSchedule?.effectiveFare ?: 0) * numPassengers

            if (selectedSchedule != null && numPassengers > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = PrimaryTeal.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Fare Summary", fontWeight = FontWeight.Bold, color = PrimaryTealDark)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Collection ($numPassengers Seats)")
                            Text("₹$totalFare", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PrimaryTeal)
                        }
                    }
                }
            }

            // Submit Button
            val canSubmit = uiState.selectedScheduleId.isNotEmpty() &&
                    uiState.selectedSeats.isNotEmpty() &&
                    uiState.passengerName.trim().length >= 3 &&
                    uiState.passengerMobile.length == 10 &&
                    !uiState.isLoading

            Button(
                onClick = { posViewModel.submitBooking() },
                enabled = canSubmit,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.PointOfSale, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Issue Walk-in Ticket (₹$totalFare)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun PosSeatTile(
    seatNumber: Int,
    uiState: com.example.haryanaroadways.ui.viewmodels.PosUiState,
    viewModel: PosViewModel,
    view: android.view.View,
    context: android.content.Context
) {
    val isSelected = uiState.selectedSeats.contains(seatNumber)
    val isUnavailable = uiState.unavailableSeats.contains(seatNumber)

    val bgColor = when {
        isUnavailable -> Color(0xFFCBD5E1)
        isSelected -> AccentSaffron
        else -> PrimaryTeal
    }

    val textColor = when {
        isUnavailable -> Color(0xFF64748B)
        isSelected -> Color.Black
        else -> Color.White
    }

    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .then(
                if (isSelected) Modifier.border(2.dp, PrimaryTealDark, RoundedCornerShape(8.dp))
                else Modifier
            )
            .clickable(enabled = !isUnavailable) {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                viewModel.toggleSeat(seatNumber)
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = seatNumber.toString(),
            color = textColor,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
    }
}

@Composable
fun PosLegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}
