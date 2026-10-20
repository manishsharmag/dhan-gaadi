package com.example.haryanaroadways.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.EventSeat
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.haryanaroadways.BuildConfig
import com.example.haryanaroadways.data.api.Booking
import com.example.haryanaroadways.data.api.formattedJourneyDate
import com.example.haryanaroadways.theme.AccentSaffron
import com.example.haryanaroadways.theme.PrimaryTeal
import com.example.haryanaroadways.theme.PrimaryTealDark
import com.example.haryanaroadways.theme.SuccessGreen
import com.example.haryanaroadways.ui.AppViewModelProvider
import com.example.haryanaroadways.ui.components.AuthBottomSheet
import com.example.haryanaroadways.ui.components.StatusChip
import com.example.haryanaroadways.ui.viewmodels.AuthViewModel
import com.example.haryanaroadways.ui.viewmodels.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onTicketClick: (String) -> Unit,
    onNavigateToAdminDashboard: () -> Unit = {},
    onNavigateToPos: () -> Unit = {},
    onNavigateToDuty: () -> Unit = {},
    viewModel: ProfileViewModel = viewModel(factory = AppViewModelProvider.Factory),
    authViewModel: AuthViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsState()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
    val userRole by authViewModel.userRole.collectAsState()
    val context = LocalContext.current
    var showAuthBottomSheet by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            viewModel.fetchProfileAndBookings()
        }
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Log Out", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to log out of Dhan-Gaadi?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutConfirm = false
                        authViewModel.logout()
                        viewModel.logout()
                    }
                ) {
                    Text("Logout", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Account & Bookings", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    if (isLoggedIn) {
                        IconButton(onClick = { viewModel.fetchProfileAndBookings() }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = Color.White)
                        }
                        IconButton(onClick = { showLogoutConfirm = true }) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryTealDark
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (!isLoggedIn || state.error?.contains("401") == true || state.error?.contains("Not authorized") == true) {
                // Clean Login Required UI state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .background(PrimaryTeal.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = PrimaryTeal,
                            modifier = Modifier.size(52.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Login Required",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Log in to view your booked tickets, boarding passes, and account privileges.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { showAuthBottomSheet = true },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text("Log In / Sign Up", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else if (state.isLoading && state.user == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryTeal)
                }
            } else {
                // Modern Header Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(PrimaryTealDark, PrimaryTeal)
                            )
                        )
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // User Avatar
                        Surface(
                            shape = CircleShape,
                            color = AccentSaffron,
                            shadowElevation = 8.dp,
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                val initial = state.user?.name?.firstOrNull()?.uppercase() ?: "U"
                                Text(
                                    text = initial,
                                    color = Color.White,
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = state.user?.name ?: "Valued Passenger",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Filled.Phone,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = state.user?.mobile ?: "No Mobile Number",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        val activeRole = state.user?.role ?: userRole ?: "user"
                        if (activeRole.lowercase() in listOf("owner", "admin", "superadmin", "conductor")) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                color = AccentSaffron,
                                shape = RoundedCornerShape(12.dp),
                                shadowElevation = 4.dp
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Shield,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "ROLE: ${activeRole.uppercase()}",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Staff Portal Quick Launcher
                val effectiveRole = state.user?.role ?: userRole ?: "user"
                val isConductor = effectiveRole.equals("conductor", ignoreCase = true)
                val isAdmin = effectiveRole.lowercase() in listOf("owner", "admin", "superadmin")

                if (isConductor || isAdmin) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Shield, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isConductor) "Conductor Duty & POS Portal" else "Admin & Staff Management",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryTealDark
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (isConductor) {
                                    Button(
                                        onClick = onNavigateToDuty,
                                        modifier = Modifier.weight(1f).height(46.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                                    ) {
                                        Icon(Icons.Filled.DirectionsBus, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("My Duty & GPS", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                    }
                                } else {
                                    Button(
                                        onClick = onNavigateToAdminDashboard,
                                        modifier = Modifier.weight(1f).height(46.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                                    ) {
                                        Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Dashboard", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                    }
                                }

                                Button(
                                    onClick = onNavigateToPos,
                                    modifier = Modifier.weight(1f).height(46.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentSaffron)
                                ) {
                                    Icon(Icons.Filled.PointOfSale, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Conductor POS", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                }
                            }
                        }
                    }
                }

                // My Bookings Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "My Bookings (${state.myBookings.size})",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (state.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = PrimaryTeal)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (state.myBookings.isEmpty() && !state.isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFF1F5F9),
                                    modifier = Modifier.size(80.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.DirectionsBus,
                                            contentDescription = "Empty Bookings",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(40.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "No bookings found for this account",
                                    color = Color.DarkGray,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tickets booked with your account or mobile will appear here.",
                                    color = Color.Gray,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 20.dp)
                        ) {
                            items(state.myBookings) { booking ->
                                ModernBookingCard(
                                    booking = booking,
                                    onViewTicket = { onTicketClick(booking.pnr) },
                                    onShareWhatsApp = {
                                        shareTicketWhatsApp(context, booking)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showAuthBottomSheet) {
            AuthBottomSheet(
                authViewModel = authViewModel,
                onDismiss = { showAuthBottomSheet = false }
            )
        }
    }
}

@Composable
fun ModernBookingCard(
    booking: Booking,
    onViewTicket: () -> Unit,
    onShareWhatsApp: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewTicket() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: PNR + Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = PrimaryTeal.copy(alpha = 0.1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            Icons.Filled.ConfirmationNumber,
                            contentDescription = null,
                            tint = PrimaryTealDark,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = booking.pnr,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryTealDark,
                            letterSpacing = 1.sp
                        )
                    }
                }
                StatusChip(status = booking.status)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Route Visual
            val source = booking.schedule?.route?.source ?: "Haryana"
            val dest = booking.schedule?.route?.destination ?: "Destination"
            val departureTime = booking.schedule?.departureTime ?: "--"
            val busNum = booking.schedule?.bus?.busNumber ?: ""

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "$source ➔ $dest",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = PrimaryTealDark
                    )
                    if (busNum.isNotBlank()) {
                        Text(
                            text = "Bus: $busNum (${booking.schedule?.bus?.type ?: "Express"})",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
                Text(
                    text = "₹${booking.totalFare}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = PrimaryTeal
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Meta row: Date, Departure Time, Seats
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${booking.formattedJourneyDate} • $departureTime",
                        color = Color.DarkGray,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.EventSeat, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Seat ${booking.seats.sorted().joinToString(", ")}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = PrimaryTealDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Conductor Badge if assigned
            val conductor = booking.schedule?.conductor
            if (conductor != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFEFF6FF),
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Badge, contentDescription = null, tint = Color(0xFF1D4ED8), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Conductor: ${conductor.name} (${conductor.mobile})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E40AF)
                        )
                    }
                }
            }

            // Rejection or Pending verification banner
            if (booking.status.equals("REJECTED", ignoreCase = true)) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFEF2F2),
                    border = BorderStroke(1.dp, Color(0xFFFECACA)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Cancel, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Booking Rejected", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF991B1B))
                            val reason = booking.rejectionReason?.takeIf { it.isNotBlank() } ?: "Payment or booking details could not be verified."
                            Text("Reason: $reason", fontSize = 12.sp, color = Color(0xFFB91C1C))
                        }
                    }
                }
            } else if (booking.status.equals("PENDING_VERIFICATION", ignoreCase = true)) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFFFBEB),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.HourglassEmpty, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Payment Verification Pending", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF92400E))
                            Text("Your seats are held. Staff will verify your transaction shortly.", fontSize = 11.sp, color = Color(0xFFB45309))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Passenger Info & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "👤 ${booking.passengerName} (${booking.passengerMobile})",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.weight(1f)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!booking.status.equals("REJECTED", ignoreCase = true)) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SuccessGreen.copy(alpha = 0.12f),
                            modifier = Modifier.clickable { onShareWhatsApp() }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = "Share", tint = SuccessGreen, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("WhatsApp", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                            }
                        }
                    }

                    val buttonText = when (booking.status.uppercase()) {
                        "CONFIRMED" -> "View Ticket"
                        "REJECTED" -> "View Reason"
                        else -> "View Status"
                    }
                    val buttonColor = when (booking.status.uppercase()) {
                        "REJECTED" -> Color(0xFFDC2626)
                        "PENDING_VERIFICATION" -> Color(0xFFD97706)
                        else -> PrimaryTeal
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = buttonColor,
                        modifier = Modifier.clickable { onViewTicket() }
                    ) {
                        Text(
                            text = buttonText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun shareTicketWhatsApp(context: Context, booking: Booking) {
    val pnr = booking.pnr
    val source = booking.schedule?.route?.source ?: "Haryana"
    val dest = booking.schedule?.route?.destination ?: "Roadways"
    val departureTime = booking.schedule?.departureTime ?: "--"
    val busNum = booking.schedule?.bus?.busNumber ?: ""
    val busType = booking.schedule?.bus?.type ?: ""
    val seatStr = booking.seats.sorted().joinToString(", ")
    val cleanMobile = booking.passengerMobile.replace(Regex("\\D"), "").takeLast(10)

    val shareMessage = """
        🚌 *DHAN-GAADI FLEET e-TICKET*
        ━━━━━━━━━━━━━━━━━━━━
        🎫 *PNR:* $pnr
        👤 *Passenger:* ${booking.passengerName}
        📍 *Route:* $source ➔ $dest
        📅 *Date:* ${booking.formattedJourneyDate} ($departureTime)
        💺 *Seats:* $seatStr
        💵 *Total Fare:* Rs. ${booking.totalFare}
        ${if (busNum.isNotBlank()) "🚍 *Bus:* $busNum ($busType)\n" else ""}
        📲 *View & Download Digital Scannable Ticket:*
        ${BuildConfig.API_BASE_URL.removeSuffix("/api")}/ticket/$pnr
        ━━━━━━━━━━━━━━━━━━━━
        _Valid for travel. Show this QR or e-Ticket to conductor._
    """.trimIndent()

    try {
        val uri = Uri.parse("https://api.whatsapp.com/send?phone=91$cleanMobile&text=${Uri.encode(shareMessage)}")
        val waIntent = Intent(Intent.ACTION_VIEW, uri)
        context.startActivity(waIntent)
    } catch (e: Exception) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareMessage)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Ticket via WhatsApp"))
    }
}
