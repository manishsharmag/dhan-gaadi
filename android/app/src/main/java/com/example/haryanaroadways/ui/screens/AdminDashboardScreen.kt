package com.example.haryanaroadways.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.haryanaroadways.BuildConfig
import com.example.haryanaroadways.theme.*
import com.example.haryanaroadways.data.api.Booking
import com.example.haryanaroadways.data.api.formattedJourneyDate
import com.example.haryanaroadways.data.api.Bus
import com.example.haryanaroadways.data.api.CreateBusRequest
import com.example.haryanaroadways.data.api.CreateRouteRequest
import com.example.haryanaroadways.data.api.CreateScheduleRequest
import com.example.haryanaroadways.data.api.Route
import com.example.haryanaroadways.data.api.Schedule
import com.example.haryanaroadways.data.api.UpdateBusRequest
import com.example.haryanaroadways.data.api.UpdateRouteRequest
import com.example.haryanaroadways.data.api.UpdateScheduleRequest
import com.example.haryanaroadways.ui.AppViewModelProvider
import com.example.haryanaroadways.ui.viewmodels.AdminUiState
import com.example.haryanaroadways.ui.viewmodels.AdminViewModel
import com.example.haryanaroadways.ui.viewmodels.AuthViewModel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

private fun backendAssetUrl(path: String): String {
    if (path.startsWith("http", ignoreCase = true)) return path
    val apiBase = BuildConfig.API_BASE_URL.trimEnd('/')
    val backendBase = apiBase.removeSuffix("/api")
    return "$backendBase/${path.trimStart('/')}"
}

private fun formatFare(amount: Number): String = "Rs. ${amount.toInt()}"

private fun extractTicketPnr(rawValue: String): String {
    val value = rawValue.trim()
    val ticketPathMatch = Regex("""/ticket/([^/?#]+)""", RegexOption.IGNORE_CASE).find(value)
    if (ticketPathMatch != null) return ticketPathMatch.groupValues[1]
    val pnrQueryMatch = Regex("""[?&]pnr=([^&#]+)""", RegexOption.IGNORE_CASE).find(value)
    if (pnrQueryMatch != null) return pnrQueryMatch.groupValues[1]
    val jsonPnrMatch = Regex(""""pnr"\s*:\s*"([^"]+)"""", RegexOption.IGNORE_CASE).find(value)
    if (jsonPnrMatch != null) return jsonPnrMatch.groupValues[1]
    return value
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    adminViewModel: AdminViewModel = viewModel(factory = AppViewModelProvider.Factory),
    authViewModel: AuthViewModel = viewModel(factory = AppViewModelProvider.Factory),
    trackingViewModel: com.example.haryanaroadways.ui.viewmodels.TrackingViewModel = viewModel(factory = AppViewModelProvider.Factory),
    onNavigateToPos: () -> Unit,
    onNavigateBack: () -> Unit = {},
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val uiState by adminViewModel.uiState.collectAsState()
    val userRole by authViewModel.userRole.collectAsState()
    var showCameraPermissionDialog by remember { mutableStateOf(false) }

    val scanLauncher = rememberLauncherForActivityResult(
        contract = ScanContract(),
        onResult = { result ->
            if (result.contents != null) {
                val pnr = extractTicketPnr(result.contents)
                adminViewModel.scanTicket(pnr)
            }
        }
    )

    fun launchScanner() {
        val options = ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setPrompt("Scan passenger's e-Ticket QR code")
            setBeepEnabled(true)
            setOrientationLocked(true)
        }
        scanLauncher.launch(options)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                launchScanner()
            } else {
                showCameraPermissionDialog = true
            }
        }
    )

    var selectedTab by remember { mutableIntStateOf(0) }
    var showCreateBusDialog by remember { mutableStateOf(false) }
    var editingBus by remember { mutableStateOf<Bus?>(null) }
    var showCreateRouteDialog by remember { mutableStateOf(false) }
    var editingRoute by remember { mutableStateOf<Route?>(null) }
    var showCreateScheduleDialog by remember { mutableStateOf(false) }
    var editingSchedule by remember { mutableStateOf<Schedule?>(null) }
    var showCreateQRDialog by remember { mutableStateOf(false) }
    var showAddConductorDialog by remember { mutableStateOf(false) }
    var rejectingBooking by remember { mutableStateOf<Booking?>(null) }

    LaunchedEffect(Unit) {
        adminViewModel.fetchDashboardStats()
        adminViewModel.fetchPendingBookings()
        adminViewModel.fetchBuses()
        adminViewModel.fetchRoutes()
        adminViewModel.fetchSchedules()
        adminViewModel.fetchConductors()
    }

    if (uiState.isScanning) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Verifying Ticket...") },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryTeal)
                }
            },
            confirmButton = {}
        )
    }

    if (uiState.scanResult != null) {
        val booking = uiState.scanResult!!
        AlertDialog(
            onDismissRequest = { adminViewModel.resetScanResult() },
            title = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = "Valid", tint = Color(0xFF4CAF50))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Valid Ticket ✓", color = Color(0xFF4CAF50))
                }
            },
            text = {
                Column {
                    Text("PNR: ${booking.pnr}", fontWeight = FontWeight.Bold)
                    Text("Passenger: ${booking.passengerName}")
                    Text("Seats: ${booking.seats.joinToString(", ")}")
                    Text("Status: ${booking.status}")
                }
            },
            confirmButton = {
                Button(onClick = { adminViewModel.resetScanResult() }) {
                    Text("OK")
                }
            }
        )
    }

    if (uiState.scanError != null) {
        AlertDialog(
            onDismissRequest = { adminViewModel.resetScanResult() },
            title = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Error, contentDescription = "Error", tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Invalid Ticket ✗", color = MaterialTheme.colorScheme.error)
                }
            },
            text = { Text(uiState.scanError!!) },
            confirmButton = {
                Button(onClick = { adminViewModel.resetScanResult() }) {
                    Text("OK")
                }
            }
        )
    }

    if (showCameraPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showCameraPermissionDialog = false },
            icon = { Icon(Icons.Filled.PhotoCamera, contentDescription = null, tint = PrimaryTeal) },
            title = { Text("Camera Permission Required", fontWeight = FontWeight.Bold) },
            text = { Text("Camera access is required to scan passenger e-Tickets. Please allow camera permission in App Settings.") },
            confirmButton = {
                Button(
                    onClick = {
                        showCameraPermissionDialog = false
                        try {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Fallback
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                ) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCameraPermissionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                            colors = listOf(PrimaryTealDark, PrimaryTeal)
                        )
                    )
            ) {
                TopAppBar(
                    title = { Text(if (userRole == "conductor") "Conductor Portal" else "Admin Portal", color = Color.White, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    actions = {
                        IconButton(onClick = {
                            adminViewModel.fetchDashboardStats()
                            adminViewModel.fetchPendingBookings()
                            adminViewModel.fetchBuses()
                            adminViewModel.fetchRoutes()
                            adminViewModel.fetchSchedules()
                            adminViewModel.fetchAllBookings()
                            adminViewModel.fetchConductors()
                        }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = Color.White)
                        }
                        IconButton(onClick = { 
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED

                            if (hasPermission) {
                                launchScanner()
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }) {
                            Icon(Icons.Filled.QrCodeScanner, contentDescription = "Scan QR", tint = Color.White)
                        }
                        IconButton(onClick = onNavigateToPos) {
                            Icon(Icons.Filled.PointOfSale, contentDescription = "POS", tint = Color.White)
                        }
                        IconButton(onClick = {
                            authViewModel.logout()
                            onLogout()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = Color.White)
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            when (selectedTab) {
                1 -> FloatingActionButton(
                    onClick = { showCreateBusDialog = true },
                    containerColor = AccentSaffron,
                    contentColor = Color.White
                ) { Icon(Icons.Filled.DirectionsBus, contentDescription = "Add Bus") }

                2 -> FloatingActionButton(
                    onClick = { showCreateRouteDialog = true },
                    containerColor = AccentSaffron,
                    contentColor = Color.White
                ) { Icon(Icons.Filled.AltRoute, contentDescription = "Add Route") }

                3 -> FloatingActionButton(
                    onClick = { showCreateScheduleDialog = true },
                    containerColor = AccentSaffron,
                    contentColor = Color.White
                ) { Icon(Icons.Filled.Schedule, contentDescription = "Add Schedule") }

                4 -> { /* No FAB for Bookings list */ }

                5 -> FloatingActionButton(
                    onClick = { showCreateQRDialog = true },
                    containerColor = AccentSaffron,
                    contentColor = Color.White
                ) { Icon(Icons.Filled.QrCode, contentDescription = "Add QR Code") }

                6 -> FloatingActionButton(
                    onClick = { showAddConductorDialog = true },
                    containerColor = AccentSaffron,
                    contentColor = Color.White
                ) { Icon(Icons.Filled.PersonAdd, contentDescription = "Add Conductor") }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = PrimaryTealDark,
                contentColor = Color.White,
                edgePadding = 8.dp
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Overview") },
                    icon = { Icon(Icons.Filled.Dashboard, contentDescription = "Overview") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Buses (${uiState.buses.size})") },
                    icon = { Icon(Icons.Filled.DirectionsBus, contentDescription = "Buses") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Routes (${uiState.routes.size})") },
                    icon = { Icon(Icons.Filled.AltRoute, contentDescription = "Routes") }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Timings (${uiState.schedules.size})") },
                    icon = { Icon(Icons.Filled.Schedule, contentDescription = "Timings") }
                )
                Tab(
                    selected = selectedTab == 4,
                    onClick = {
                        selectedTab = 4
                        adminViewModel.fetchAllBookings()
                        adminViewModel.fetchPendingBookings()
                    },
                    text = {
                        val count = uiState.pendingBookings.size
                        Text(if (count > 0) "Bookings ($count)" else "Bookings")
                    },
                    icon = { Icon(Icons.Filled.ListAlt, contentDescription = "Bookings") }
                )
                Tab(
                    selected = selectedTab == 5,
                    onClick = { selectedTab = 5 },
                    text = { Text("Payment QR") },
                    icon = { Icon(Icons.Filled.QrCode, contentDescription = "Payment QR") }
                )
                Tab(
                    selected = selectedTab == 6,
                    onClick = {
                        selectedTab = 6
                        adminViewModel.fetchConductors()
                    },
                    text = { Text("Staff (${uiState.conductors.size})") },
                    icon = { Icon(Icons.Filled.Badge, contentDescription = "Staff") }
                )
                Tab(
                    selected = selectedTab == 7,
                    onClick = { selectedTab = 7 },
                    text = { Text("Analytics") },
                    icon = { Icon(Icons.Filled.BarChart, contentDescription = "Analytics") }
                )
                Tab(
                    selected = selectedTab == 8,
                    onClick = { selectedTab = 8 },
                    text = { Text("Bus View") },
                    icon = { Icon(Icons.Filled.EventSeat, contentDescription = "Bus View") }
                )
                Tab(
                    selected = selectedTab == 9,
                    onClick = { selectedTab = 9 },
                    text = { Text("Live Fleet") },
                    icon = { Icon(Icons.Filled.Map, contentDescription = "Live Fleet") }
                )
            }

            when (selectedTab) {
                0 -> OverviewTab(
                    uiState = uiState,
                    onApprove = { adminViewModel.approveBooking(it) },
                    onReject = { bookingId ->
                        rejectingBooking = uiState.pendingBookings.find { it._id == bookingId }
                    },
                    onTabChange = { selectedTab = it }
                )
                1 -> BusesTab(
                    buses = uiState.buses,
                    isLoading = uiState.isLoadingBuses,
                    onEdit = { editingBus = it },
                    onDelete = { adminViewModel.deleteBus(it) }
                )
                2 -> RoutesTab(
                    routes = uiState.routes,
                    isLoading = uiState.isLoadingRoutes,
                    onEdit = { editingRoute = it },
                    onDelete = { adminViewModel.deleteRoute(it) }
                )
                3 -> SchedulesTab(
                    schedules = uiState.schedules,
                    isLoading = uiState.isLoadingSchedules,
                    onEdit = { editingSchedule = it },
                    onDelete = { adminViewModel.deleteSchedule(it) }
                )
                4 -> AllBookingsTab(
                    uiState = uiState
                )
                5 -> QRCodesTab(
                    qrCodes = uiState.qrCodes,
                    isLoading = uiState.isLoadingQRCodes,
                    onActivate = { adminViewModel.activateQRCode(it) },
                    onDelete = { adminViewModel.deleteQRCode(it) }
                )
                6 -> ConductorsTab(
                    conductors = uiState.conductors,
                    isLoading = uiState.isLoadingConductors,
                    onAdd = { showAddConductorDialog = true },
                    onDelete = { conductor -> adminViewModel.deleteConductor(conductor.id) }
                )
                7 -> AnalyticsTab(uiState = uiState)
                8 -> BusViewTab(
                    uiState = uiState,
                    onFetchBookings = { scheduleId -> }
                )
                9 -> LiveFleetTab(trackingViewModel = trackingViewModel)
            }
        }

        // Dialogs
        if (showCreateBusDialog) {
            BusDialog(
                bus = null,
                error = uiState.busError,
                isLoading = uiState.isActionLoading,
                onDismiss = { showCreateBusDialog = false },
                onUploadPhoto = { context, uri, onUploaded ->
                    adminViewModel.uploadImage(context, uri, onUploaded)
                },
                onSaveBus = { request ->
                    adminViewModel.createBus(request) { showCreateBusDialog = false }
                }
            )
        }

        if (editingBus != null) {
            BusDialog(
                bus = editingBus,
                error = uiState.busError,
                isLoading = uiState.isActionLoading,
                onDismiss = { editingBus = null },
                onUploadPhoto = { context, uri, onUploaded ->
                    adminViewModel.uploadImage(context, uri, onUploaded)
                },
                onSaveBus = { request ->
                    adminViewModel.updateBus(editingBus!!._id, UpdateBusRequest(
                        name = request.name,
                        type = request.type,
                        totalSeats = request.totalSeats,
                        depot = request.depot,
                        amenities = request.amenities,
                        images = request.images
                    )) { editingBus = null }
                }
            )
        }

        if (showCreateRouteDialog) {
            RouteDialog(
                route = null,
                error = uiState.routeError,
                isLoading = uiState.isActionLoading,
                onDismiss = { showCreateRouteDialog = false },
                onSaveRoute = { request ->
                    adminViewModel.createRoute(request) { showCreateRouteDialog = false }
                }
            )
        }

        if (editingRoute != null) {
            RouteDialog(
                route = editingRoute,
                error = uiState.routeError,
                isLoading = uiState.isActionLoading,
                onDismiss = { editingRoute = null },
                onSaveRoute = { request ->
                    adminViewModel.updateRoute(editingRoute!!._id, UpdateRouteRequest(
                        name = request.name,
                        source = request.source,
                        destination = request.destination,
                        sourceDepot = request.sourceDepot,
                        destinationDepot = request.destinationDepot,
                        stops = request.stops,
                        totalDistance = request.totalDistance,
                        baseFare = request.baseFare,
                        images = request.images
                    )) { editingRoute = null }
                }
            )
        }

        if (showCreateScheduleDialog) {
            ScheduleDialog(
                schedule = null,
                buses = uiState.buses,
                routes = uiState.routes,
                conductors = uiState.conductors,
                error = uiState.scheduleError,
                isLoading = uiState.isActionLoading,
                onDismiss = { showCreateScheduleDialog = false },
                onSaveSchedule = { request ->
                    adminViewModel.createSchedule(request) { showCreateScheduleDialog = false }
                }
            )
        }

        if (editingSchedule != null) {
            ScheduleDialog(
                schedule = editingSchedule,
                buses = uiState.buses,
                routes = uiState.routes,
                conductors = uiState.conductors,
                error = uiState.scheduleError,
                isLoading = uiState.isActionLoading,
                onDismiss = { editingSchedule = null },
                onSaveSchedule = { request ->
                    adminViewModel.updateSchedule(editingSchedule!!._id, UpdateScheduleRequest(
                        route = request.route,
                        bus = request.bus,
                        departureTime = request.departureTime,
                        arrivalTime = request.arrivalTime,
                        daysOfOperation = request.daysOfOperation,
                        fare = request.fare,
                        conductor = request.conductor
                    )) { editingSchedule = null }
                }
            )
        }

        if (showCreateQRDialog) {
            QRDialog(
                error = uiState.qrError,
                isLoading = uiState.isActionLoading,
                onDismiss = { showCreateQRDialog = false },
                onSave = { context, uri, upiId, upiLabel ->
                    adminViewModel.createQRCode(context, uri, upiId, upiLabel) {
                        showCreateQRDialog = false
                    }
                }
            )
        }

        if (showAddConductorDialog) {
            AddConductorDialog(
                error = uiState.conductorError,
                isLoading = uiState.isActionLoading,
                onDismiss = { showAddConductorDialog = false },
                onSave = { name, mobile, password ->
                    adminViewModel.createConductor(name, mobile, password) {
                        showAddConductorDialog = false
                    }
                }
            )
        }

        if (rejectingBooking != null) {
            RejectBookingDialog(
                booking = rejectingBooking!!,
                onDismiss = { rejectingBooking = null },
                onConfirmReject = { reason ->
                    adminViewModel.rejectBooking(rejectingBooking!!._id, reason)
                    rejectingBooking = null
                }
            )
        }
    }
}

@Composable
fun AnalyticsTab(uiState: AdminUiState) {
    val bookings = uiState.allBookings
    if (bookings.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No data available for analytics", color = Color.Gray)
        }
        return
    }

    val confirmed = bookings.count { it.status.equals("CONFIRMED", ignoreCase = true) }
    val pending = bookings.count { it.status.equals("PENDING_VERIFICATION", ignoreCase = true) }
    val rejected = bookings.count { it.status.equals("REJECTED", ignoreCase = true) || it.status.equals("EXPIRED", ignoreCase = true) }

    val revenueByDate = bookings
        .filter { it.status.equals("CONFIRMED", ignoreCase = true) }
        .groupBy { if (it.createdAt.length >= 10) it.createdAt.substring(0, 10) else "Unknown" }
        .mapValues { entry -> entry.value.sumOf { it.totalFare.toDouble() } }
        .entries.sortedBy { it.key }.takeLast(7)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Booking Status Distribution", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = PrimaryTealDark)
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Confirmed Bookings", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                    Text(confirmed.toString(), fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Pending Verification", color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold)
                    Text(pending.toString(), fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Rejected / Expired", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    Text(rejected.toString(), fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(16.dp))

                val total = confirmed + pending + rejected
                if (total > 0) {
                    Row(modifier = Modifier.fillMaxWidth().height(24.dp).clip(RoundedCornerShape(12.dp))) {
                        Box(modifier = Modifier.weight(if(confirmed>0) confirmed.toFloat() else 0.01f).fillMaxHeight().background(Color(0xFF4CAF50)))
                        Box(modifier = Modifier.weight(if(pending>0) pending.toFloat() else 0.01f).fillMaxHeight().background(Color(0xFFF59E0B)))
                        Box(modifier = Modifier.weight(if(rejected>0) rejected.toFloat() else 0.01f).fillMaxHeight().background(MaterialTheme.colorScheme.error))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Recent Revenue (Confirmed)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = PrimaryTealDark)
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (revenueByDate.isEmpty()) {
                    Text("No confirmed revenue data yet.", color = Color.Gray)
                } else {
                    val maxRevenue = revenueByDate.maxOfOrNull { it.value } ?: 1.0

                    revenueByDate.forEach { (date, revenue) ->
                        val fraction = (revenue / maxRevenue).toFloat().coerceIn(0.01f, 1f)
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(date, modifier = Modifier.width(100.dp), fontSize = 12.sp)
                            Box(modifier = Modifier.weight(1f).height(16.dp)) {
                                Box(modifier = Modifier.fillMaxWidth(fraction).fillMaxHeight().background(PrimaryTeal, RoundedCornerShape(4.dp)))
                            }
                            Text("₹${revenue.toInt()}", modifier = Modifier.width(60.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusViewTab(
    uiState: AdminUiState,
    onFetchBookings: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedSchedule by remember { mutableStateOf<Schedule?>(null) }

    val schedules = uiState.schedules
    val allBookings = uiState.allBookings

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Select Schedule to View Seats", fontWeight = FontWeight.Bold, color = PrimaryTealDark)
        Spacer(modifier = Modifier.height(8.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedSchedule?.let { "${it.route?.source} to ${it.route?.destination} (${it.departureTime})" } ?: "Select Schedule",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                schedules.forEach { schedule ->
                    val label = "${schedule.route?.source} to ${schedule.route?.destination} (${schedule.departureTime})"
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            selectedSchedule = schedule
                            expanded = false
                            onFetchBookings(schedule._id)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedSchedule != null) {
            val totalSeats = selectedSchedule!!.bus?.totalSeats ?: 45
            val scheduleBookings = allBookings.filter {
                it.schedule?._id == selectedSchedule!!._id &&
                (it.status.equals("CONFIRMED", ignoreCase = true) || it.status.equals("PENDING_VERIFICATION", ignoreCase = true))
            }
            val bookedSeatsMap = mutableMapOf<Int, Booking>()
            scheduleBookings.forEach { b ->
                b.seats.forEach { s -> bookedSeatsMap[s] = b }
            }

            Text("Total Seats: $totalSeats", fontWeight = FontWeight.Bold)
            Text("Booked/Pending: ${bookedSeatsMap.size}", color = Color.Gray)
            Text("Available: ${totalSeats - bookedSeatsMap.size}", color = Color(0xFF4CAF50))

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp)).padding(8.dp)
            ) {
                val rows = (totalSeats + 4) / 5

                items(rows) { rowIndex ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (colIndex in 0 until 5) {
                            val seatNumber = rowIndex * 5 + colIndex + 1
                            if (colIndex == 2) {
                                Spacer(modifier = Modifier.width(32.dp))
                            } else {
                                if (seatNumber <= totalSeats) {
                                    val booking = bookedSeatsMap[seatNumber]
                                    val seatColor = when {
                                        booking == null -> Color.White
                                        booking.status.equals("CONFIRMED", ignoreCase = true) -> Color(0xFF4CAF50)
                                        else -> Color(0xFFF59E0B)
                                    }
                                    val textColor = if (booking == null) Color.Black else Color.White

                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(seatColor, RoundedCornerShape(8.dp))
                                            .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(seatNumber.toString(), color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Spacer(modifier = Modifier.size(40.dp))
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Select a schedule to view its seat layout.", color = Color.Gray)
            }
        }
    }
}

@Composable
fun OverviewTab(
    uiState: AdminUiState,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit,
    onTabChange: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            if (uiState.statsError != null) {
                Text(
                    text = uiState.statsError,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            AdminStatsGrid(uiState = uiState, onTabChange = onTabChange)
            AdminCustomerInsights(uiState = uiState, onBookingsClick = { onTabChange(4) })
        }

        item {
            Text(
                text = "Pending Payment Approvals",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        if (uiState.pendingError != null) {
            item {
                Text(
                    text = uiState.pendingError,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        when {
            uiState.isLoadingPending -> item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryTeal)
                }
            }

            uiState.pendingBookings.isEmpty() -> item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No pending bookings to approve", color = Color.Gray)
                }
            }

            else -> items(uiState.pendingBookings) { booking ->
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    PendingBookingCard(booking, onApprove, onReject)
                }
            }
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    subText: String? = null
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = color.copy(alpha = 0.12f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = title, tint = color, modifier = Modifier.size(20.dp))
                    }
                }
                if (subText != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = BgLight
                    ) {
                        Text(
                            text = subText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondaryLight,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimaryLight,
                letterSpacing = (-0.5).sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondaryLight
            )
        }
    }
}

@Composable
fun AdminStatsGrid(uiState: AdminUiState, onTabChange: (Int) -> Unit) {
    val stats = uiState.stats
    val totalBookings = stats?.totalBookings ?: uiState.allBookings.size
    val pending = stats?.pendingApprovals ?: uiState.pendingBookings.size
    val confirmed = stats?.confirmedBookings ?: uiState.allBookings.count { it.status.equals("CONFIRMED", ignoreCase = true) }
    val totalRevenue = stats?.totalRevenue ?: uiState.allBookings
        .filter { it.status.equals("CONFIRMED", ignoreCase = true) }
        .sumOf { it.totalFare }

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            StatCard(modifier = Modifier.weight(1f).clickable { onTabChange(4) }, title = "Total Bookings", value = totalBookings.toString(), icon = Icons.Filled.ConfirmationNumber, color = Color(0xFF2563EB), subText = "All Time")
            StatCard(modifier = Modifier.weight(1f).clickable { onTabChange(6) }, title = "Net Revenue", value = formatFare(totalRevenue), icon = Icons.Filled.CurrencyRupee, color = Color(0xFF059669), subText = "Gross")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            StatCard(modifier = Modifier.weight(1f).clickable { onTabChange(4) }, title = "Pending Approvals", value = pending.toString(), icon = Icons.Filled.PendingActions, color = Color(0xFFD97706), subText = "Action Req.")
            StatCard(modifier = Modifier.weight(1f), title = "Confirmed Trips", value = confirmed.toString(), icon = Icons.Filled.Verified, color = Color(0xFF16A34A), subText = "Active")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            StatCard(modifier = Modifier.weight(1f), title = "Active Fleet", value = (stats?.activeBuses ?: uiState.buses.count { it.status == "active" }).toString(), icon = Icons.Filled.DirectionsBus, color = Color(0xFF7C3AED), subText = "Buses")
            StatCard(modifier = Modifier.weight(1f), title = "State Routes", value = (stats?.activeRoutes ?: uiState.routes.count { it.status == "active" }).toString(), icon = Icons.Filled.AltRoute, color = Color(0xFF0891B2), subText = "Corridors")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            StatCard(modifier = Modifier.weight(1f), title = "Today Bookings", value = "${stats?.todayBookingsCount ?: 0} trips", icon = Icons.Filled.Today, color = Color(0xFFDB2777), subText = "Today")
            StatCard(modifier = Modifier.weight(1f), title = "Today Revenue", value = formatFare(stats?.todayRevenue ?: 0), icon = Icons.Filled.Payments, color = Color(0xFF0F766E), subText = "Today")
        }
    }
}

@Composable
fun AdminCustomerInsights(uiState: AdminUiState, onBookingsClick: () -> Unit) {
    val bookings = uiState.allBookings
    val uniqueCustomers = uiState.stats?.uniqueCustomers
        ?: bookings.map { it.passengerMobile }.filter { it.isNotBlank() }.distinct().size
    val onlinePayments = uiState.stats?.onlinePayments ?: bookings.count { it.paymentMethod.equals("online", ignoreCase = true) }
    val cashPayments = uiState.stats?.cashPayments ?: bookings.count { it.paymentMethod.equals("cash", ignoreCase = true) }
    val upiPayments = uiState.stats?.upiPayments ?: bookings.count { it.paymentMethod.equals("upi_direct", ignoreCase = true) || it.paymentMethod.equals("UPI", ignoreCase = true) }
    val topCustomers = bookings
        .filter { it.passengerMobile.isNotBlank() }
        .groupBy { it.passengerMobile }
        .map { entry ->
            val latest = entry.value.maxByOrNull { it.createdAt } ?: entry.value.first()
            Triple(latest.passengerName.ifBlank { "Customer" }, entry.key, entry.value.size)
        }
        .sortedByDescending { it.third }
        .take(3)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onBookingsClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Customer Activity", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimaryLight)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = PrimaryTealUltraLight
                ) {
                    Text(
                        text = "$uniqueCustomers Unique Riders",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryTealDark,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = BgLight,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Online", fontSize = 11.sp, color = TextSecondaryLight)
                        Text("$onlinePayments", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PrimaryTealDark)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = BgLight,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Cash / POS", fontSize = 11.sp, color = TextSecondaryLight)
                        Text("$cashPayments", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AccentSaffronDark)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = BgLight,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("UPI Direct", fontSize = 11.sp, color = TextSecondaryLight)
                        Text("$upiPayments", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                    }
                }
            }

            if (topCustomers.isNotEmpty()) {
                HorizontalDivider(color = BorderSubtle)
                topCustomers.forEach { customer ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(customer.first, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrimaryLight)
                            Text(customer.second, color = TextMutedLight, fontSize = 11.sp)
                        }
                        Text("${customer.third} trip${if (customer.third == 1) "" else "s"}", fontWeight = FontWeight.Bold, color = PrimaryTeal, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun PendingBookingCard(
    booking: Booking,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit
) {
    var showScreenshot by remember { mutableStateOf(false) }
    val route = booking.schedule?.route
    val bus = booking.schedule?.bus

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = PrimaryTealUltraLight
                ) {
                    Text(
                        text = "PNR: ${booking.pnr}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = PrimaryTealDark,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Text(formatFare(booking.totalFare), fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = PrimaryTeal)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text("👤 Passenger: ${booking.passengerName} (${booking.passengerMobile})", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimaryLight)
            Text("📍 Route: ${route?.source ?: "Unknown"} ➔ ${route?.destination ?: "Unknown"}", fontSize = 13.sp, color = TextSecondaryLight)
            Text("🚍 Bus: ${bus?.busNumber ?: "Unassigned"} (${bus?.type ?: "-"})", fontSize = 12.sp, color = TextSecondaryLight)
            Text("📅 Journey: ${booking.formattedJourneyDate} • Seats: ${booking.seats.joinToString()}", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextPrimaryLight)

            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = BgLight,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                ) {
                    Text(
                        text = "Method: ${booking.paymentMethod.ifBlank { "online" }.uppercase()}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondaryLight,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                if (booking.referenceCode.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = AccentSaffronLight
                    ) {
                        Text(
                            text = "Ref: ${booking.referenceCode}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentSaffronDark,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            if (booking.paymentScreenshot.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { showScreenshot = !showScreenshot },
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (showScreenshot) "Hide Payment Screenshot" else "View Payment Screenshot", fontSize = 12.sp)
                }
                if (showScreenshot) {
                    Spacer(modifier = Modifier.height(8.dp))
                    AsyncImage(
                        model = backendAssetUrl(booking.paymentScreenshot),
                        contentDescription = "Payment screenshot",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { onReject(booking._id) },
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Reject", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reject", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { onApprove(booking._id) },
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = "Approve", tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Approve", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AllBookingsTab(
    uiState: AdminUiState
) {
    if (uiState.isLoadingAllBookings) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PrimaryTeal)
        }
    } else if (uiState.allBookingsError != null) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(uiState.allBookingsError, color = MaterialTheme.colorScheme.error)
        }
    } else if (uiState.allBookings.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No bookings found.", color = Color.Gray)
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(uiState.allBookings) { booking ->
                BookingLogCard(booking = booking)
            }
        }
    }
}

@Composable
fun BookingLogCard(booking: Booking) {
    var showDetailsDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { showDetailsDialog = true },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "PNR: ${booking.pnr}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                val statusColor = when (booking.status.uppercase()) {
                    "CONFIRMED" -> Color(0xFF4CAF50)
                    "PENDING_VERIFICATION" -> Color(0xFFF59E0B)
                    "EXPIRED", "REJECTED" -> MaterialTheme.colorScheme.error
                    else -> Color.Gray
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = booking.status,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Passenger: ${booking.passengerName} (${booking.passengerMobile})",
                fontSize = 14.sp
            )
            val routeStr = booking.schedule?.route?.let { "${it.source} ➔ ${it.destination}" } ?: "Unknown Route"
            Text(
                text = "Route: $routeStr",
                fontSize = 14.sp,
                color = Color.DarkGray
            )
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "Seats: ${booking.seats.size} (${booking.seats.joinToString()})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "₹${booking.totalFare} (${booking.paymentMethod})",
                    fontWeight = FontWeight.Bold,
                    color = PrimaryTeal
                )
            }
        }
    }

    if (showDetailsDialog) {
        AlertDialog(
            onDismissRequest = { showDetailsDialog = false },
            title = { Text("Booking Details - ${booking.pnr}") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("Passenger: ${booking.passengerName}", fontWeight = FontWeight.Bold)
                    Text("Mobile: ${booking.passengerMobile}")
                    Text("Gender: ${booking.passengerGender}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Status: ${booking.status}", fontWeight = FontWeight.Bold)
                    Text("Payment Method: ${booking.paymentMethod}")
                    Text("Total Fare: ${formatFare(booking.totalFare)}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Seats booked (${booking.seats.size}):", fontWeight = FontWeight.Bold)
                    Text(booking.seats.joinToString(", "))
                    Spacer(modifier = Modifier.height(8.dp))
                    if (booking.schedule != null) {
                        Text("Bus details:", fontWeight = FontWeight.Bold)
                        val r = booking.schedule.route
                        if (r != null) {
                            Text("Route: ${r.source} to ${r.destination}")
                        }
                        Text("Departure: ${booking.schedule.departureTime}")
                    }
                    if (!booking.paymentScreenshot.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Payment Screenshot:", fontWeight = FontWeight.Bold)
                        AsyncImage(
                            model = backendAssetUrl(booking.paymentScreenshot),
                            contentDescription = "Screenshot",
                            modifier = Modifier.fillMaxWidth().height(150.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDetailsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun BusesTab(
    buses: List<Bus>,
    isLoading: Boolean,
    onEdit: (Bus) -> Unit,
    onDelete: (String) -> Unit
) {
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PrimaryTeal)
        }
    } else if (buses.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No buses registered yet. Click + to add a bus.")
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(buses) { bus ->
                BusCard(bus = bus, onEdit = { onEdit(bus) }, onDelete = { onDelete(bus._id) })
            }
        }
    }
}

@Composable
fun BusCard(
    bus: Bus,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (bus.images.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().height(120.dp)
                ) {
                    items(bus.images) { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = "Bus Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .width(180.dp)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(bus.busNumber, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PrimaryTealDark)
                    Text(bus.name.ifEmpty { "Dhan-Gaadi Fleet Bus" }, fontSize = 14.sp, color = Color.Gray)
                }
                AssistChip(
                    onClick = {},
                    label = { Text(bus.type.uppercase()) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = PrimaryTeal.copy(alpha = 0.1f))
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Depot: ${bus.depot}", fontSize = 14.sp)
                Text("Seats: ${bus.totalSeats}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryTeal)
            }

            if (bus.amenities.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(bus.amenities) { amenity ->
                        SuggestionChip(
                            onClick = {},
                            label = { Text(amenity, fontSize = 11.sp) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit Bus", tint = PrimaryTeal)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete Bus", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun RoutesTab(
    routes: List<Route>,
    isLoading: Boolean,
    onEdit: (Route) -> Unit,
    onDelete: (String) -> Unit
) {
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PrimaryTeal)
        }
    } else if (routes.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No routes created yet. Click + to add a route.")
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(routes) { route ->
                RouteCard(route = route, onEdit = { onEdit(route) }, onDelete = { onDelete(route._id) })
            }
        }
    }
}

@Composable
fun RouteCard(
    route: Route,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (route.images.isNotEmpty()) {
                AsyncImage(
                    model = route.images.first(),
                    contentDescription = "Route Banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("${route.routeNumber} — ${route.name}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PrimaryTealDark)
                Text("₹${route.baseFare}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AccentSaffron)
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text("${route.source} ➔ ${route.destination} (${route.totalDistance} km)", fontSize = 14.sp, color = Color.Gray)

            if (route.stops.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Stops: ${route.stops.joinToString(" ➔ ") { it.name }}", fontSize = 12.sp, color = Color.DarkGray)
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Edit Route", tint = PrimaryTeal) }
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Delete Route", tint = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
fun SchedulesTab(
    schedules: List<Schedule>,
    isLoading: Boolean,
    onEdit: (Schedule) -> Unit,
    onDelete: (String) -> Unit
) {
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PrimaryTeal)
        }
    } else if (schedules.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No timings created yet. Click + to add a bus schedule.")
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(schedules) { schedule ->
                ScheduleCard(schedule = schedule, onEdit = { onEdit(schedule) }, onDelete = { onDelete(schedule._id) })
            }
        }
    }
}

@Composable
fun ScheduleCard(
    schedule: Schedule,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = schedule.route?.name ?: "Route Timings",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = PrimaryTealDark
                    )
                    Text(
                        text = "Bus: ${schedule.bus?.busNumber ?: "Bus"} (${schedule.bus?.name ?: ""})",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
                Text("₹${schedule.fare ?: schedule.route?.baseFare ?: 0}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AccentSaffron)
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = PrimaryTeal.copy(alpha = 0.1f),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Schedule, contentDescription = "Departure", tint = PrimaryTeal, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Dep: ${schedule.departureTime}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PrimaryTealDark)
                    }
                }

                if (schedule.arrivalTime.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AccentSaffron.copy(alpha = 0.1f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AccessTime, contentDescription = "Arrival", tint = AccentSaffron, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Arr: ${schedule.arrivalTime}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFD84315))
                        }
                    }
                }
            }

            if (schedule.daysOfOperation.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Operating Days: ${schedule.daysOfOperation.joinToString(", ")}", fontSize = 12.sp, color = Color.Gray)
            }

            if (schedule.conductor != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFEFF6FF),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Badge, contentDescription = null, tint = Color(0xFF1D4ED8), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Conductor: ${schedule.conductor.name} (${schedule.conductor.mobile})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E40AF)
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(6.dp))
                Text("Conductor: Unassigned", fontSize = 11.sp, color = Color.Gray, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Edit Schedule", tint = PrimaryTeal) }
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Delete Schedule", tint = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusDialog(
    bus: Bus?,
    error: String?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onUploadPhoto: (android.content.Context, Uri, (String) -> Unit) -> Unit = { _, _, _ -> },
    onSaveBus: (CreateBusRequest) -> Unit
) {
    var busNumber by remember { mutableStateOf(bus?.busNumber ?: "") }
    var name by remember { mutableStateOf(bus?.name ?: "") }
    var type by remember { mutableStateOf(bus?.type ?: "ordinary") }
    var totalSeatsStr by remember { mutableStateOf((bus?.totalSeats ?: 45).toString()) }
    var depot by remember { mutableStateOf(bus?.depot ?: "Hisar") }
    var amenitiesStr by remember { mutableStateOf(bus?.amenities?.joinToString(", ") ?: "AC, WiFi, Charging") }
    var imagesStr by remember { mutableStateOf(bus?.images?.joinToString("\n") ?: "") }

    val context = LocalContext.current
    val photoLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            onUploadPhoto(context, it) { uploadedUrl ->
                val list = imagesStr.split("\n", ",").map { s -> s.trim() }.filter { s -> s.isNotEmpty() }
                imagesStr = (list + uploadedUrl).joinToString("\n")
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (bus == null) "Add New Bus" else "Edit Bus Details") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (error != null) {
                    Text(text = error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                OutlinedTextField(
                    value = busNumber,
                    onValueChange = { busNumber = it },
                    label = { Text("Bus Number (Required e.g. HR-38-A-9999)") },
                    enabled = bus == null,
                    isError = busNumber.isBlank(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Bus Name / Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = type,
                    onValueChange = { type = it },
                    label = { Text("Type (ordinary/deluxe/volvo-ac/super-deluxe)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = totalSeatsStr,
                    onValueChange = { totalSeatsStr = it },
                    label = { Text("Total Seats Count (e.g. 40, 45, 52)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = depot,
                    onValueChange = { depot = it },
                    label = { Text("Home Depot") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amenitiesStr,
                    onValueChange = { amenitiesStr = it },
                    label = { Text("Amenities (Comma separated: AC, WiFi, Charging)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedButton(
                    onClick = { photoLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.AddAPhoto, contentDescription = "Upload Photo")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Upload Photo from Device")
                }

                OutlinedTextField(
                    value = imagesStr,
                    onValueChange = { imagesStr = it },
                    label = { Text("Bus Photo URLs (1 per line or uploaded)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val seats = totalSeatsStr.toIntOrNull() ?: 45
                    val amenities = amenitiesStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    val images = imagesStr.split("\n", ",").map { it.trim() }.filter { it.isNotEmpty() }
                    onSaveBus(
                        CreateBusRequest(
                            busNumber = busNumber,
                            name = name,
                            type = type,
                            totalSeats = seats,
                            depot = depot,
                            amenities = amenities,
                            images = images
                        )
                    )
                },
                enabled = !isLoading && busNumber.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                } else {
                    Text("Save Bus")
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteDialog(
    route: Route?,
    error: String?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSaveRoute: (CreateRouteRequest) -> Unit
) {
    var routeNumber by remember { mutableStateOf(route?.routeNumber ?: "") }
    var name by remember { mutableStateOf(route?.name ?: "") }
    var source by remember { mutableStateOf(route?.source ?: "Hisar") }
    var destination by remember { mutableStateOf(route?.destination ?: "Delhi") }
    var sourceDepot by remember { mutableStateOf(route?.sourceDepot ?: "Hisar Bus Stand") }
    var destinationDepot by remember { mutableStateOf(route?.destinationDepot ?: "ISBT Kashmere Gate") }
    var distanceStr by remember { mutableStateOf((route?.totalDistance ?: 220).toString()) }
    var fareStr by remember { mutableStateOf((route?.baseFare ?: 350).toString()) }
    var imagesStr by remember { mutableStateOf(route?.images?.joinToString("\n") ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (route == null) "Create Route" else "Edit Route Details") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (error != null) {
                    Text(text = error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                OutlinedTextField(
                    value = routeNumber,
                    onValueChange = { routeNumber = it },
                    label = { Text("Route Number (Required e.g. HR-72)") },
                    isError = routeNumber.isBlank(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Route Name (e.g. Hisar - Delhi Direct)") },
                    isError = name.isBlank(),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = source, onValueChange = { source = it }, label = { Text("Source") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = destination, onValueChange = { destination = it }, label = { Text("Destination") }, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = sourceDepot, onValueChange = { sourceDepot = it }, label = { Text("Source Depot") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = destinationDepot, onValueChange = { destinationDepot = it }, label = { Text("Destination Depot") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = distanceStr, onValueChange = { distanceStr = it }, label = { Text("Distance (km)") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = fareStr, onValueChange = { fareStr = it }, label = { Text("Base Fare (₹)") }, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(
                    value = imagesStr,
                    onValueChange = { imagesStr = it },
                    label = { Text("Banner Image URLs") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val dist = distanceStr.toIntOrNull() ?: 200
                    val fare = fareStr.toIntOrNull() ?: 300
                    val images = imagesStr.split("\n", ",").map { it.trim() }.filter { it.isNotEmpty() }
                    onSaveRoute(
                        CreateRouteRequest(
                            routeNumber = routeNumber,
                            name = name,
                            source = source,
                            destination = destination,
                            sourceDepot = sourceDepot,
                            destinationDepot = destinationDepot,
                            totalDistance = dist,
                            baseFare = fare,
                            images = images
                        )
                    )
                },
                enabled = !isLoading && routeNumber.isNotBlank() && name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                } else {
                    Text("Save Route")
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleDialog(
    schedule: Schedule?,
    buses: List<Bus>,
    routes: List<Route>,
    conductors: List<com.example.haryanaroadways.data.api.UserData> = emptyList(),
    error: String?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSaveSchedule: (CreateScheduleRequest) -> Unit
) {
    var selectedRouteId by remember { mutableStateOf(schedule?.route?._id ?: routes.firstOrNull()?._id ?: "") }
    var selectedBusId by remember { mutableStateOf(schedule?.bus?._id ?: buses.firstOrNull()?._id ?: "") }
    var selectedConductorId by remember { mutableStateOf(schedule?.conductor?._id ?: "") }
    var departureTime by remember { mutableStateOf(schedule?.departureTime ?: "06:30 AM") }
    var arrivalTime by remember { mutableStateOf(schedule?.arrivalTime ?: "11:00 AM") }
    var fareStr by remember { mutableStateOf((schedule?.fare ?: 350).toString()) }
    var selectedDays by remember { mutableStateOf(schedule?.daysOfOperation?.toSet() ?: setOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")) }

    val allDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (schedule == null) "Add Bus Timings & Schedule" else "Edit Bus Schedule") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (error != null) {
                    Text(text = error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Text("Select Route", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(routes) { r ->
                        FilterChip(
                            selected = selectedRouteId == r._id,
                            onClick = { selectedRouteId = r._id },
                            label = { Text("${r.routeNumber}: ${r.source} ➔ ${r.destination}") }
                        )
                    }
                }

                Text("Select Bus", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(buses) { b ->
                        FilterChip(
                            selected = selectedBusId == b._id,
                            onClick = { selectedBusId = b._id },
                            label = { Text("${b.busNumber} (${b.type})") }
                        )
                    }
                }

                Text("Assign Bus Staff / Conductor", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        FilterChip(
                            selected = selectedConductorId.isEmpty(),
                            onClick = { selectedConductorId = "" },
                            label = { Text("None (Unassigned)") }
                        )
                    }
                    items(conductors) { c ->
                        FilterChip(
                            selected = selectedConductorId == c._id,
                            onClick = { selectedConductorId = c._id },
                            label = { Text("👮 ${c.name} (${c.mobile})") }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = departureTime,
                        onValueChange = { departureTime = it },
                        label = { Text("Departure Time (e.g. 06:30 AM)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = arrivalTime,
                        onValueChange = { arrivalTime = it },
                        label = { Text("Arrival Time (e.g. 11:00 AM)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = fareStr,
                    onValueChange = { fareStr = it },
                    label = { Text("Ticket Fare (₹)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Operating Days", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(allDays) { day ->
                        FilterChip(
                            selected = selectedDays.contains(day),
                            onClick = {
                                selectedDays = if (selectedDays.contains(day)) selectedDays - day else selectedDays + day
                            },
                            label = { Text(day, fontSize = 12.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val fareVal = fareStr.toIntOrNull() ?: 350
                    onSaveSchedule(
                        CreateScheduleRequest(
                            route = selectedRouteId,
                            bus = selectedBusId,
                            departureTime = departureTime,
                            arrivalTime = arrivalTime,
                            daysOfOperation = selectedDays.toList(),
                            fare = fareVal,
                            conductor = selectedConductorId.ifBlank { null }
                        )
                    )
                },
                enabled = !isLoading && selectedRouteId.isNotBlank() && selectedBusId.isNotBlank() && departureTime.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                } else {
                    Text("Save Timings")
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun QRCodesTab(
    qrCodes: List<com.example.haryanaroadways.data.api.QRCode>,
    isLoading: Boolean,
    onActivate: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PrimaryTeal)
        }
    } else if (qrCodes.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No QR codes added yet. Click + to add.")
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(qrCodes) { qrCode ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(qrCode.upiLabel, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            if (qrCode.isActive) {
                                AssistChip(
                                    onClick = {},
                                    label = { Text("Active") },
                                    colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFF4CAF50).copy(alpha = 0.2f))
                                )
                            }
                        }
                        Text(qrCode.upiId, color = Color.Gray, fontSize = 14.sp)

                        val imgUrl = backendAssetUrl(qrCode.imagePath)
                        Spacer(modifier = Modifier.height(12.dp))
                        AsyncImage(
                            model = imgUrl,
                            contentDescription = "QR Code",
                            modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(12.dp))
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            if (!qrCode.isActive) {
                                Button(onClick = { onActivate(qrCode._id) }) {
                                    Text("Set Active")
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                            IconButton(onClick = { onDelete(qrCode._id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRDialog(
    error: String?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSave: (context: android.content.Context, uri: Uri, upiId: String, upiLabel: String) -> Unit
) {
    var upiId by remember { mutableStateOf("") }
    var upiLabel by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val context = LocalContext.current
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        selectedImageUri = uri
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Payment QR Code") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (error != null) {
                    Text(text = error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Button(onClick = { imagePicker.launch("image/*") }) {
                    Text(if (selectedImageUri == null) "Select QR Image" else "Image Selected")
                }

                OutlinedTextField(
                    value = upiLabel,
                    onValueChange = { upiLabel = it },
                    label = { Text("UPI Label (e.g. Rahul Travels)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = upiId,
                    onValueChange = { upiId = it },
                    label = { Text("UPI ID") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedImageUri != null) {
                        onSave(context, selectedImageUri!!, upiId, upiLabel)
                    }
                },
                enabled = !isLoading && selectedImageUri != null && upiLabel.isNotBlank() && upiId.isNotBlank()
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("Upload & Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) { Text("Cancel") }
        }
    )
}

@Composable
fun ConductorsTab(
    conductors: List<com.example.haryanaroadways.data.api.UserData>,
    isLoading: Boolean,
    onAdd: () -> Unit,
    onDelete: (com.example.haryanaroadways.data.api.UserData) -> Unit
) {
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PrimaryTeal)
        }
    } else if (conductors.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.PeopleOutline,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("No staff conductors found", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Add conductors to manage on-board POS ticketing and live tracking.",
                    color = Color.Gray,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onAdd,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                ) {
                    Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add New Conductor")
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(conductors, key = { it.id }) { conductor ->
                var showDeleteConfirm by remember { mutableStateOf(false) }

                if (showDeleteConfirm) {
                    AlertDialog(
                        onDismissRequest = { showDeleteConfirm = false },
                        title = { Text("Remove Conductor") },
                        text = { Text("Are you sure you want to remove conductor ${conductor.name} (+91 ${conductor.mobile})?") },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showDeleteConfirm = false
                                    onDelete(conductor)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Delete")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
                        }
                    )
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = PrimaryTeal.copy(alpha = 0.1f),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = conductor.name.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    color = com.example.haryanaroadways.theme.PrimaryTealDark,
                                    fontSize = 20.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = conductor.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                AssistChip(
                                    onClick = {},
                                    label = { Text(conductor.role.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = PrimaryTeal.copy(alpha = 0.12f),
                                        labelColor = com.example.haryanaroadways.theme.PrimaryTealDark
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Phone, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "+91 ${conductor.mobile}",
                                    fontSize = 14.sp,
                                    color = Color.DarkGray
                                )
                            }
                        }

                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Delete Conductor",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddConductorDialog(
    error: String?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, mobile: String, password: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val isFormValid = name.trim().length >= 3 && mobile.trim().length == 10 && password.length >= 6

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Conductor / Staff") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (error != null) {
                    Text(text = error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = mobile,
                    onValueChange = { if (it.length <= 10 && it.all { c -> c.isDigit() }) mobile = it },
                    label = { Text("10-Digit Mobile Number") },
                    leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Login Password (min 6 chars)") },
                    leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name.trim(), mobile.trim(), password) },
                enabled = !isLoading && isFormValid,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("Create Account")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) { Text("Cancel") }
        }
    )
}

@Composable
fun RejectBookingDialog(
    booking: Booking,
    onDismiss: () -> Unit,
    onConfirmReject: (String) -> Unit
) {
    var selectedReason by remember { mutableStateOf("Payment UTR or screenshot could not be verified") }
    var customReason by remember { mutableStateOf("") }
    val commonReasons = listOf(
        "Payment UTR or screenshot could not be verified",
        "Payment amount mismatch with ticket fare",
        "Duplicate UTR or reference submitted",
        "Seat unavailable on this schedule",
        "Suspected invalid transaction",
        "Other (Type custom reason below)"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Cancel, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reject Booking - ${booking.pnr}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Passenger: ${booking.passengerName} (${booking.passengerMobile})",
                    fontSize = 13.sp,
                    color = Color.DarkGray,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "Please select or enter the reason for rejection. This will be shown to the passenger in 'My Bookings' and sent via SMS.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                HorizontalDivider(color = BorderSubtle)

                commonReasons.forEach { reason ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedReason = reason }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selectedReason == reason),
                            onClick = { selectedReason = reason },
                            colors = RadioButtonDefaults.colors(selectedColor = ErrorRed)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(reason, fontSize = 13.sp, color = TextPrimaryLight)
                    }
                }

                if (selectedReason.startsWith("Other")) {
                    OutlinedTextField(
                        value = customReason,
                        onValueChange = { customReason = it },
                        label = { Text("Specify Rejection Reason") },
                        placeholder = { Text("Enter reason for passenger...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalReason = if (selectedReason.startsWith("Other")) {
                        customReason.ifBlank { "Payment or booking details could not be verified" }
                    } else {
                        selectedReason
                    }
                    onConfirmReject(finalReason)
                },
                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
            ) {
                Text("Confirm Rejection", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

