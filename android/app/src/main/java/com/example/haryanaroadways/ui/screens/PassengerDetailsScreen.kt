package com.example.haryanaroadways.ui.screens

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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EventSeat
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.haryanaroadways.data.api.ApiClient
import com.example.haryanaroadways.data.api.formatJourneyDate
import com.example.haryanaroadways.theme.*
import com.example.haryanaroadways.ui.AppViewModelProvider
import com.example.haryanaroadways.ui.components.AppTextField
import com.example.haryanaroadways.ui.components.AuthBottomSheet
import com.example.haryanaroadways.ui.components.GradientHeader
import com.example.haryanaroadways.ui.viewmodels.AuthViewModel
import com.example.haryanaroadways.ui.viewmodels.BookingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassengerDetailsScreen(
    viewModel: BookingViewModel,
    onNavigateToPayment: (String) -> Unit,
    onBack: () -> Unit,
    authViewModel: AuthViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
    val authUserName by authViewModel.userName.collectAsState()
    val authUserMobile by authViewModel.userMobile.collectAsState()
    var showAuthSheet by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.bookingResult) {
        val bookingId = uiState.bookingResult?.data?.booking?.id
        if (bookingId != null) {
            viewModel.resetBookingResult()
            onNavigateToPayment(bookingId)
        }
    }

    LaunchedEffect(isLoggedIn, authUserName, authUserMobile) {
        if (isLoggedIn) {
            ApiClient.reset()
            if (uiState.passengerName.isBlank() && !authUserName.isNullOrBlank()) {
                viewModel.updatePassengerDetails(
                    name = authUserName,
                    mobile = if (uiState.passengerMobile.isBlank()) authUserMobile else null
                )
            }
        }
    }

    if (showAuthSheet) {
        AuthBottomSheet(
            authViewModel = authViewModel,
            onDismiss = { showAuthSheet = false }
        )
    }

    val isNameValid = uiState.passengerName.trim().length >= 3
    val isMobileValid = uiState.passengerMobile.length == 10 && uiState.passengerMobile.all { it.isDigit() }
    val ageInt = uiState.passengerAge.toIntOrNull() ?: 0
    val isAgeValid = ageInt in 1..120
    val isGenderValid = uiState.passengerGender.isNotBlank()
    val isFormComplete = isNameValid && isMobileValid && isAgeValid && isGenderValid

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Passenger Information", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 16.dp)
                ) {
                    Button(
                        onClick = {
                            if (isLoggedIn) {
                                viewModel.createBooking()
                            } else {
                                showAuthSheet = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                        enabled = !uiState.isBooking && (isFormComplete || !isLoggedIn)
                    ) {
                        if (uiState.isBooking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                if (isLoggedIn) "Confirm & Proceed to Pay (₹${uiState.totalFare}) ➔" else "Login to Confirm Booking",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .background(BgLight)
        ) {
            GradientHeader(
                title = "Passenger Details",
                subtitle = "Tickets and SMS confirmation will be sent here",
                badgeText = "Dhan-Gaadi Fleet Booking",
                bottomPadding = 28.dp
            )

            // Booking Summary Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .offset(y = (-20).dp),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Trip Overview", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PrimaryTealDark)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = PrimaryTealUltraLight
                        ) {
                            Text(
                                text = "${uiState.selectedSeats.size} Seat(s)",
                                color = PrimaryTealDark,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(formatJourneyDate(uiState.journeyDate), fontSize = 13.sp, color = TextPrimaryLight, fontWeight = FontWeight.Medium)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.EventSeat, contentDescription = null, tint = AccentSaffronDark, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Seats: ${uiState.selectedSeats.sorted().joinToString(", ")}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimaryLight)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = BorderSubtle)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total Amount Due", fontSize = 14.sp, color = TextSecondaryLight)
                        Text("₹${uiState.totalFare}", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = PrimaryTeal)
                    }
                }
            }

            // Passenger Input Form Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Primary Passenger Info", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PrimaryTealDark)
                        if (isLoggedIn && !authUserName.isNullOrBlank()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                val isSelfSelected = uiState.passengerName == authUserName
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelfSelected) PrimaryTealUltraLight else Color(0xFFF1F5F9),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelfSelected) PrimaryTeal else BorderSubtle),
                                    modifier = Modifier.clickable {
                                        viewModel.updatePassengerDetails(name = authUserName, mobile = authUserMobile)
                                    }
                                ) {
                                    Text(
                                        text = "👤 Self",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelfSelected) PrimaryTealDark else TextSecondaryLight,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (!isSelfSelected) PrimaryTealUltraLight else Color(0xFFF1F5F9),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (!isSelfSelected) PrimaryTeal else BorderSubtle),
                                    modifier = Modifier.clickable {
                                        viewModel.updatePassengerDetails(name = "", mobile = "")
                                    }
                                ) {
                                    Text(
                                        text = "👥 Other",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (!isSelfSelected) PrimaryTealDark else TextSecondaryLight,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Full Name Input
                    Column {
                        AppTextField(
                            value = uiState.passengerName,
                            onValueChange = { viewModel.updatePassengerDetails(name = it) },
                            label = "Full Name (as per Govt ID)",
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Name", tint = PrimaryTeal) },
                            trailingIcon = {
                                if (isNameValid) Icon(Icons.Filled.CheckCircle, contentDescription = "Valid", tint = SuccessGreen, modifier = Modifier.size(18.dp))
                            }
                        )
                        if (uiState.passengerName.isNotEmpty() && !isNameValid) {
                            Text("Name must be at least 3 characters", color = MaterialTheme.colorScheme.error, fontSize = 11.sp, modifier = Modifier.padding(start = 8.dp, top = 2.dp))
                        }
                    }

                    // Mobile Number Input
                    Column {
                        AppTextField(
                            value = uiState.passengerMobile,
                            onValueChange = { if (it.length <= 10 && it.all { c -> c.isDigit() }) viewModel.updatePassengerDetails(mobile = it) },
                            label = "10-Digit Mobile Number",
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = "Phone", tint = PrimaryTeal) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            trailingIcon = {
                                if (isMobileValid) Icon(Icons.Filled.CheckCircle, contentDescription = "Valid", tint = SuccessGreen, modifier = Modifier.size(18.dp))
                            }
                        )
                        if (uiState.passengerMobile.isNotEmpty() && !isMobileValid) {
                            Text("Enter a valid 10-digit Indian mobile number", color = MaterialTheme.colorScheme.error, fontSize = 11.sp, modifier = Modifier.padding(start = 8.dp, top = 2.dp))
                        }
                    }

                    // Age Input
                    Column {
                        AppTextField(
                            value = uiState.passengerAge,
                            onValueChange = { if (it.length <= 3 && it.all { c -> c.isDigit() }) viewModel.updatePassengerDetails(age = it) },
                            label = "Age (in Years)",
                            leadingIcon = { Icon(Icons.Default.Numbers, contentDescription = "Age", tint = PrimaryTeal) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            trailingIcon = {
                                if (isAgeValid && uiState.passengerAge.isNotEmpty()) Icon(Icons.Filled.CheckCircle, contentDescription = "Valid", tint = SuccessGreen, modifier = Modifier.size(18.dp))
                            }
                        )
                        if (uiState.passengerAge.isNotEmpty() && !isAgeValid) {
                            Text("Please enter a valid age (1-120)", color = MaterialTheme.colorScheme.error, fontSize = 11.sp, modifier = Modifier.padding(start = 8.dp, top = 2.dp))
                        }
                    }

                    // Gender Selector
                    Text("Gender", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimaryLight)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("male" to "Male", "female" to "Female", "other" to "Other").forEach { (genderKey, label) ->
                            val isSelected = uiState.passengerGender == genderKey
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.updatePassengerDetails(gender = genderKey) },
                                label = { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryTealUltraLight,
                                    selectedLabelColor = PrimaryTealDark
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Trust & Encryption Seal
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Filled.Shield, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Official Haryana Transport Gateway • 256-Bit Encrypted",
                    fontSize = 11.sp,
                    color = TextSecondaryLight,
                    fontWeight = FontWeight.Medium
                )
            }

            if (uiState.bookingError != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = ErrorRedLight),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = uiState.bookingError ?: "Booking failed. Please try again.",
                        color = ErrorRed,
                        modifier = Modifier.padding(14.dp),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}
