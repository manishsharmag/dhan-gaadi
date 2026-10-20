package com.example.haryanaroadways.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.haryanaroadways.BuildConfig
import com.example.haryanaroadways.data.api.ConductorDutyData
import com.example.haryanaroadways.data.api.PassengerManifestItem
import com.example.haryanaroadways.data.api.Schedule
import com.example.haryanaroadways.data.api.UserData
import com.example.haryanaroadways.data.local.TokenManager
import com.example.haryanaroadways.data.repository.AuthRepository
import com.example.haryanaroadways.data.repository.ScheduleRepository
import com.example.haryanaroadways.services.LocationTrackingService
import com.example.haryanaroadways.theme.*
import com.example.haryanaroadways.ui.AppViewModelProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConductorDutyUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val user: UserData? = null,
    val dutyData: ConductorDutyData? = null,
    val isTrackingActive: Boolean = false,
    val speedKmh: Int = 0,
    val packetsCount: Int = 0
)

class ConductorDutyViewModel(
    private val scheduleRepository: ScheduleRepository,
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConductorDutyUiState())
    val uiState: StateFlow<ConductorDutyUiState> = _uiState.asStateFlow()

    init {
        loadDutyDetails()
        monitorTrackingService()
    }

    private fun monitorTrackingService() {
        viewModelScope.launch {
            LocationTrackingService.isRunningFlow.collect { running ->
                _uiState.update {
                    it.copy(
                        isTrackingActive = running,
                        speedKmh = if (running) LocationTrackingService.currentSpeedKmh else 0,
                        packetsCount = if (running) LocationTrackingService.packetsSentCount else 0
                    )
                }
            }
        }
        viewModelScope.launch {
            while (true) {
                if (LocationTrackingService.isRunning) {
                    _uiState.update {
                        it.copy(
                            speedKmh = LocationTrackingService.currentSpeedKmh,
                            packetsCount = LocationTrackingService.packetsSentCount
                        )
                    }
                }
                delay(2000)
            }
        }
    }

    fun loadDutyDetails() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val profileRes = authRepository.getProfile()
                if (profileRes.success && profileRes.data != null) {
                    _uiState.update { it.copy(user = profileRes.data) }
                }

                val dutyRes = scheduleRepository.getConductorDuty()
                if (dutyRes.success && dutyRes.data != null) {
                    _uiState.update {
                        it.copy(
                            dutyData = dutyRes.data,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            error = dutyRes.message.ifBlank { "Could not load duty details" },
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = e.message ?: "Failed to connect to server",
                        isLoading = false
                    )
                }
            }
        }
    }

    suspend fun getAuthToken(): String {
        return tokenManager.tokenFlow.first() ?: tokenManager.getTokenSync() ?: ""
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConductorDutyScreen(
    onBack: () -> Unit,
    onNavigateToPos: () -> Unit,
    onNavigateToTicket: (String) -> Unit = {},
    viewModel: ConductorDutyViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    val locationPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.POST_NOTIFICATIONS
        )
    } else {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    var hasLocationPermission by remember {
        mutableStateOf(
            locationPermissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                      permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = granted
        if (!granted) {
            Toast.makeText(context, "Location permission is required for bus GPS broadcasting", Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("My Duty & Bus GPS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Live Conductor Terminal", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadDutyDetails() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryTealDark)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (state.isLoading && state.dutyData == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryTeal)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Staff Header Banner
                    item {
                        StaffBannerCard(
                            user = state.user,
                            isBroadcasting = state.isTrackingActive,
                            speedKmh = state.speedKmh,
                            packetsCount = state.packetsCount
                        )
                    }

                    // 2. Assigned Bus & Route Details
                    val schedule = state.dutyData?.schedule
                    if (schedule != null) {
                        item {
                            AssignedScheduleCard(schedule = schedule)
                        }

                        // 3. Live Journey & GPS Broadcast Controls
                        item {
                            JourneyBroadcastCard(
                                schedule = schedule,
                                isRunning = state.isTrackingActive,
                                onStartTrip = {
                                    if (!hasLocationPermission) {
                                        permissionLauncher.launch(locationPermissions)
                                    } else {
                                        coroutineScope.launch {
                                            val token = viewModel.getAuthToken()
                                            val serverUrl = BuildConfig.API_BASE_URL.trimEnd('/')
                                                .removeSuffix("/api")
                                            LocationTrackingService.start(
                                                context = context,
                                                scheduleId = schedule._id,
                                                serverUrl = serverUrl,
                                                token = token
                                            )
                                            Toast.makeText(context, "🚀 Bus Trip Started! Broadcasting GPS live to passengers.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                onEndTrip = {
                                    LocationTrackingService.stop(context)
                                    Toast.makeText(context, "🛑 Bus Trip Ended. GPS broadcast stopped.", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }

                        // 4. Conductor POS Quick Launcher
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNavigateToPos() },
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = AccentSaffron.copy(alpha = 0.12f)),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, AccentSaffron.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(18.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = CircleShape,
                                            color = AccentSaffron,
                                            modifier = Modifier.size(46.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Filled.PointOfSale, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column {
                                            Text("Conductor POS Terminal", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PrimaryTealDark)
                                            Text("Issue Instant Walk-in Tickets (Cash / UPI)", fontSize = 12.sp, color = Color.DarkGray)
                                        }
                                    }
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = AccentSaffron, modifier = Modifier.size(20.dp))
                                }
                            }
                        }

                        // 5. Passenger Manifest Heading & Count
                        val passengers = state.dutyData?.passengers ?: emptyList()
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.People, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Today's Booked Manifest (${passengers.size})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                        color = PrimaryTealDark
                                    )
                                }
                            }
                        }

                        if (passengers.isEmpty()) {
                            item {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color.White,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(Icons.Filled.EventSeat, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(40.dp))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("No online passengers booked for today's trip yet", color = Color.Gray, fontSize = 13.sp, textAlign = TextAlign.Center)
                                        Text("Passengers booking online or via app will appear here automatically.", color = Color.LightGray, fontSize = 11.sp, textAlign = TextAlign.Center)
                                    }
                                }
                            }
                        } else {
                            items(passengers) { item ->
                                ManifestPassengerCard(
                                    item = item,
                                    context = context,
                                    onViewPass = { onNavigateToTicket(item.pnr) }
                                )
                            }
                        }
                    } else {
                        // Unassigned Schedule Warning
                        item {
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = AccentSaffron.copy(alpha = 0.15f),
                                        modifier = Modifier.size(64.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Filled.DirectionsBus, contentDescription = null, tint = AccentSaffron, modifier = Modifier.size(36.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("No Bus Schedule Assigned", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PrimaryTealDark)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = state.dutyData?.message ?: "You have not been assigned to an active bus schedule yet. Please contact the Depot Admin/Manager.",
                                        color = Color.Gray,
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Button(
                                        onClick = { viewModel.loadDutyDetails() },
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Check Again")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StaffBannerCard(
    user: UserData?,
    isBroadcasting: Boolean,
    speedKmh: Int,
    packetsCount: Int
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PrimaryTealDark),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = AccentSaffron,
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = user?.name?.take(1)?.uppercase() ?: "C",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(user?.name ?: "Conductor", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text("+91 ${user?.mobile ?: ""}", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isBroadcasting) SuccessGreen else Color.White.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = if (isBroadcasting) "● LIVE BROADCAST" else "OFFLINE",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (isBroadcasting) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Current Speed: $speedKmh km/h", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text("Telemetry Packets: $packetsCount", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun AssignedScheduleCard(schedule: Schedule) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = PrimaryTeal.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = schedule.route?.routeNumber ?: "ROUTE",
                        color = PrimaryTealDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = "₹${schedule.effectiveFare}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = PrimaryTeal
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "${schedule.route?.source ?: "Origin"} ➔ ${schedule.route?.destination ?: "Destination"}",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = PrimaryTealDark
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.DirectionsBus, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Bus No: ${schedule.bus?.busNumber ?: "Assigned"} • ${schedule.bus?.type?.uppercase() ?: "EXPRESS"} (${schedule.bus?.totalSeats ?: 45} Seats)",
                    fontSize = 13.sp,
                    color = Color.DarkGray
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Schedule, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Departure: ${schedule.departureTime} • Depot: ${schedule.bus?.depot ?: "Dhan-Gaadi Fleet"}",
                    fontSize = 13.sp,
                    color = Color.DarkGray
                )
            }
        }
    }
}

@Composable
fun JourneyBroadcastCard(
    schedule: Schedule,
    isRunning: Boolean,
    onStartTrip: () -> Unit,
    onEndTrip: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isRunning) Color(0xFFF0FDF4) else Color(0xFFFFFBEB)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (isRunning) SuccessGreen.copy(alpha = 0.5f) else AccentSaffron.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isRunning) SuccessGreen else AccentSaffron,
                    modifier = Modifier.size(14.dp)
                ) {}
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isRunning) "TRIP RUNNING • BROADCASTING GPS" else "BUS AT TERMINAL • READY TO START",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (isRunning) SuccessGreen else Color(0xFFB45309)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isRunning)
                    "Passengers booked on this bus are actively tracking your live GPS location on their map."
                else
                    "Press 'Start Bus / Begin Journey' when departing. Your phone will transmit live GPS coordinates to passengers.",
                fontSize = 12.sp,
                color = Color.DarkGray
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (!isRunning) {
                Button(
                    onClick = onStartTrip,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("🚀 Start Bus / Begin Journey", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onEndTrip,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("🛑 End Journey / Reached Destination", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ManifestPassengerCard(
    item: PassengerManifestItem,
    context: Context,
    onViewPass: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(item.passengerName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PrimaryTealDark)
                    Text("Seats: ${item.seats.sorted().joinToString(", ")}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AccentSaffron)
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (item.status.uppercase() == "CONFIRMED") SuccessGreen.copy(alpha = 0.15f) else Color(0xFFFEF3C7)
                ) {
                    Text(
                        text = if (item.status.uppercase() == "CONFIRMED") "PAID" else "PENDING",
                        color = if (item.status.uppercase() == "CONFIRMED") SuccessGreen else Color(0xFFB45309),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text("PNR: ${item.pnr} • ₹${item.totalFare}", fontSize = 12.sp, color = Color.Gray)

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Dial call
                OutlinedButton(
                    onClick = {
                        val clean = item.passengerMobile.replace(Regex("\\D"), "").takeLast(10)
                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$clean"))
                        context.startActivity(dialIntent)
                    },
                    modifier = Modifier.weight(1f).height(38.dp),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(14.dp), tint = PrimaryTeal)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Call", fontSize = 12.sp, color = PrimaryTeal)
                }

                // WhatsApp
                OutlinedButton(
                    onClick = {
                        val clean = item.passengerMobile.replace(Regex("\\D"), "").takeLast(10)
                        val uri = Uri.parse("https://api.whatsapp.com/send?phone=91$clean&text=Hello%20${item.passengerName}%2C%20this%20is%20your%20Haryana%20Roadways%20bus%20conductor.")
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        } catch (_: Exception) {}
                    },
                    modifier = Modifier.weight(1f).height(38.dp),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("WhatsApp", fontSize = 12.sp, color = SuccessGreen)
                }

                // View ticket
                Button(
                    onClick = onViewPass,
                    modifier = Modifier.weight(1f).height(38.dp),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                ) {
                    Text("View Pass", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
