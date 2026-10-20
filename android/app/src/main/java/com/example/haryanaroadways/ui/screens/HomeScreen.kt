package com.example.haryanaroadways.ui.screens

import android.content.Intent
import android.net.Uri
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.haryanaroadways.theme.*
import com.example.haryanaroadways.ui.AppViewModelProvider
import com.example.haryanaroadways.ui.components.AppTextField
import com.example.haryanaroadways.ui.components.AuthBottomSheet
import com.example.haryanaroadways.ui.components.GradientHeader
import com.example.haryanaroadways.ui.components.PriceTag
import com.example.haryanaroadways.ui.viewmodels.AuthViewModel
import com.example.haryanaroadways.ui.viewmodels.SearchViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSearch: (String, String, String) -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToTicket: (String) -> Unit = {},
    onNavigateToTracking: (String) -> Unit = {},
    searchViewModel: SearchViewModel = viewModel(factory = AppViewModelProvider.Factory),
    authViewModel: AuthViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val searchState by searchViewModel.uiState.collectAsState()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
    val view = LocalView.current
    val context = LocalContext.current

    var searchMode by remember { mutableStateOf("book") } // "book", "track", "pnr"
    var pnrInput by remember { mutableStateOf("") }
    var trackScheduleInput by remember { mutableStateOf("") }

    var showAuthBottomSheet by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var showDatePicker by remember { mutableStateOf(false) }
    var swapRotated by remember { mutableStateOf(false) }

    val rotationAngle by animateFloatAsState(
        targetValue = if (swapRotated) 180f else 0f,
        label = "swapRotation"
    )

    val datePickerState = rememberDatePickerState()

    // Initialize default date to today if empty
    LaunchedEffect(Unit) {
        if (searchState.searchDate.isEmpty()) {
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            searchViewModel.updateSearchQuery(
                source = searchState.searchSource,
                destination = searchState.searchDestination,
                date = todayStr
            )
        }
    }

    // Sync date from date picker
    LaunchedEffect(datePickerState.selectedDateMillis) {
        datePickerState.selectedDateMillis?.let { millis ->
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dateStr = formatter.format(Date(millis))
            searchViewModel.updateSearchQuery(
                source = searchState.searchSource,
                destination = searchState.searchDestination,
                date = dateStr
            )
            showDatePicker = false
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Select Date", color = PrimaryTeal, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp,
                modifier = Modifier.border(width = 1.dp, color = BorderSubtle)
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home", modifier = Modifier.size(24.dp)) },
                    label = { Text("Book Tickets", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryTeal,
                        selectedTextColor = PrimaryTeal,
                        indicatorColor = PrimaryTealUltraLight
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile", modifier = Modifier.size(24.dp)) },
                    label = { Text("My Account", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) },
                    selected = selectedTab == 1,
                    onClick = {
                        if (!isLoggedIn) {
                            showAuthBottomSheet = true
                        } else {
                            selectedTab = 1
                            onNavigateToProfile()
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryTeal,
                        selectedTextColor = PrimaryTeal,
                        indicatorColor = PrimaryTealUltraLight
                    )
                )
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
            // Hero Header
            GradientHeader(
                title = "Where to next?",
                subtitle = "Dhan-Gaadi Fleet e-Booking & Live Tracking",
                badgeText = "Enterprise Ground Transit • Live Highway Radar",
                modifier = Modifier.fillMaxWidth()
            )

            // Search Card Container
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .offset(y = (-28).dp),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Multi-Mode Selector Tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(PrimaryTealUltraLight, RoundedCornerShape(14.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf(
                            Triple("book", "🚌 Book Bus", "Book Ticket"),
                            Triple("track", "📍 Live GPS", "Track Bus"),
                            Triple("pnr", "🎫 Find Ticket", "Search PNR")
                        ).forEach { (mode, label, _) ->
                            val isSelected = searchMode == mode
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Color.White else Color.Transparent,
                                shadowElevation = if (isSelected) 2.dp else 0.dp,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { searchMode = mode }
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) PrimaryTealDark else TextSecondaryLight
                                    )
                                }
                            }
                        }
                    }

                    if (searchMode == "pnr") {
                        // Find Ticket by PNR Form
                        Column(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Find & View e-Ticket",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryTealDark
                            )
                            Text(
                                text = "Enter the 8-digit PNR from your booking SMS to view, download, or show your verified boarding pass to the conductor.",
                                fontSize = 12.sp,
                                color = TextSecondaryLight
                            )
                            AppTextField(
                                value = pnrInput,
                                onValueChange = { pnrInput = it.uppercase() },
                                label = "Booking PNR (e.g. HR-B871B)",
                                leadingIcon = {
                                    Icon(Icons.Filled.ConfirmationNumber, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(20.dp))
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = {
                                    if (pnrInput.isNotBlank()) {
                                        onNavigateToTicket(pnrInput.trim().uppercase())
                                    }
                                },
                                enabled = pnrInput.trim().length >= 4,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                            ) {
                                Text("Open Boarding Pass ➔", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    } else if (searchMode == "track") {
                        // Live Fleet Tracking
                        Column(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Live Fleet Radar & GPS",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryTealDark
                            )
                            Text(
                                text = "Track Dhan-Gaadi fleet buses in real-time with live highway telemetry, route ETA countdowns, and upcoming stops.",
                                fontSize = 12.sp,
                                color = TextSecondaryLight
                            )
                            AppTextField(
                                value = trackScheduleInput,
                                onValueChange = { trackScheduleInput = it },
                                label = "Bus Schedule ID (Optional)",
                                leadingIcon = {
                                    Icon(Icons.Filled.DirectionsBus, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(20.dp))
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = {
                                    val target = trackScheduleInput.trim().ifBlank {
                                        searchState.popularRoutes.firstOrNull()?._id ?: "active"
                                    }
                                    onNavigateToTracking(target)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                            ) {
                                Text("Open Live Bus Radar ➔", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    } else {
                        // Standard Book Bus Form
                        // Origin Input
                        AppTextField(
                            value = searchState.searchSource,
                            onValueChange = {
                                searchViewModel.updateSearchQuery(it, searchState.searchDestination, searchState.searchDate)
                            },
                            label = "From (Source City / Depot)",
                            leadingIcon = {
                                Icon(Icons.Filled.LocationOn, contentDescription = "From", tint = PrimaryTeal, modifier = Modifier.size(20.dp))
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                    // Swap Locations Button Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = PrimaryTealUltraLight,
                            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryTealLight.copy(alpha = 0.4f)),
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .size(40.dp)
                                .clickable {
                                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                    swapRotated = !swapRotated
                                    searchViewModel.updateSearchQuery(
                                        source = searchState.searchDestination,
                                        destination = searchState.searchSource,
                                        date = searchState.searchDate
                                    )
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.SwapVert,
                                    contentDescription = "Swap Locations",
                                    tint = PrimaryTeal,
                                    modifier = Modifier
                                        .size(22.dp)
                                        .rotate(rotationAngle)
                                )
                            }
                        }
                    }

                    // Destination Input
                    AppTextField(
                        value = searchState.searchDestination,
                        onValueChange = {
                            searchViewModel.updateSearchQuery(searchState.searchSource, it, searchState.searchDate)
                        },
                        label = "To (Destination City / Depot)",
                        leadingIcon = {
                            Icon(Icons.Filled.PinDrop, contentDescription = "To", tint = AccentSaffronDark, modifier = Modifier.size(20.dp))
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Date Input Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { showDatePicker = true }
                    ) {
                        AppTextField(
                            value = formatHumanDate(searchState.searchDate),
                            onValueChange = { },
                            label = "Journey Date",
                            leadingIcon = {
                                Icon(Icons.Filled.CalendarMonth, contentDescription = "Date", tint = PrimaryTeal, modifier = Modifier.size(20.dp))
                            },
                            enabled = false,
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Quick Date Chips: Today, Tomorrow, Day After
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
                        val tomorrowStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
                        val cal2 = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 2) }
                        val dayAfterStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal2.time)

                        val isToday = searchState.searchDate == todayStr
                        val isTomorrow = searchState.searchDate == tomorrowStr
                        val isDayAfter = searchState.searchDate == dayAfterStr

                        FilterChip(
                            selected = isToday,
                            onClick = {
                                searchViewModel.updateSearchQuery(searchState.searchSource, searchState.searchDestination, todayStr)
                            },
                            label = { Text("Today", fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryTealUltraLight,
                                selectedLabelColor = PrimaryTealDark
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        FilterChip(
                            selected = isTomorrow,
                            onClick = {
                                searchViewModel.updateSearchQuery(searchState.searchSource, searchState.searchDestination, tomorrowStr)
                            },
                            label = { Text("Tomorrow", fontWeight = if (isTomorrow) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryTealUltraLight,
                                selectedLabelColor = PrimaryTealDark
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        FilterChip(
                            selected = isDayAfter,
                            onClick = {
                                searchViewModel.updateSearchQuery(searchState.searchSource, searchState.searchDestination, dayAfterStr)
                            },
                            label = { Text("+2 Days", fontWeight = if (isDayAfter) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryTealUltraLight,
                                selectedLabelColor = PrimaryTealDark
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Search Button
                    Button(
                        onClick = {
                            searchViewModel.searchSchedules()
                            onNavigateToSearch(
                                searchState.searchSource,
                                searchState.searchDestination,
                                searchState.searchDate
                            )
                        },
                        enabled = searchState.searchSource.isNotBlank() && searchState.searchDestination.isNotBlank() && searchState.searchDate.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryTeal,
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Icon(Icons.Filled.Search, contentDescription = "Search", tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Search Express Buses", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    } // end else
                }
            }

            // Trust & Features Badge Strip (Interactive)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .offset(y = (-14).dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FeatureBadge(icon = Icons.Filled.ElectricBolt, text = "Instant Ticket", onClick = { searchMode = "book" })
                FeatureBadge(icon = Icons.Filled.DirectionsBus, text = "Live GPS Track", onClick = { searchMode = "track" })
                FeatureBadge(icon = Icons.Filled.QrCodeScanner, text = "E-Verification", onClick = { searchMode = "pnr" })
                FeatureBadge(icon = Icons.Filled.Shield, text = "Govt. Official", onClick = { searchMode = "book" })
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Popular Routes Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Popular Express Routes",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryLight
                    )
                    Text(
                        text = "Frequent state roadways departures",
                        fontSize = 13.sp,
                        color = TextSecondaryLight
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (searchState.isLoading) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryTeal)
                }
            } else if (searchState.error != null) {
                Text(
                    text = searchState.error ?: "",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
            } else if (searchState.popularRoutes.isEmpty()) {
                Text(
                    text = "No popular routes available right now.",
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    searchState.popularRoutes.forEach { route ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val dateStr = searchState.searchDate.ifEmpty {
                                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                    }
                                    searchViewModel.updateSearchQuery(route.source, route.destination, dateStr)
                                    onNavigateToSearch(route.source, route.destination, dateStr)
                                },
                            shape = RoundedCornerShape(20.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                if (route.images.isNotEmpty()) {
                                    Box(modifier = Modifier.fillMaxWidth().height(110.dp)) {
                                        AsyncImage(
                                            model = route.images.first(),
                                            contentDescription = "Route Image",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        // Gradient Scrim
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    Brush.verticalGradient(
                                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f))
                                                    )
                                                )
                                        )
                                        // Route Number Pill
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color.Black.copy(alpha = 0.6f),
                                            modifier = Modifier
                                                .align(Alignment.BottomStart)
                                                .padding(10.dp)
                                        ) {
                                            Text(
                                                text = "Route ${route.routeNumber}",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "${route.source} ➔ ${route.destination}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp,
                                            color = PrimaryTealDark
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "${route.totalDistance} km • Daily Express Service",
                                            fontSize = 12.sp,
                                            color = TextSecondaryLight
                                        )
                                    }

                                    PriceTag(amount = route.baseFare, subText = "per seat")
                                }
                            }
                        }
                    }
                }

                // 24/7 Passenger Safety & Emergency Helpline Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Shield, contentDescription = "Safety", tint = AccentSaffron, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("24/7 Passenger Safety & Helplines", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryTealDark)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                Triple("112", "🚨 Police 112", "Police Emergency"),
                                Triple("1091", "🛡️ Women 1091", "Women Safety"),
                                Triple("18001802057", "📞 Roadways Help", "Toll Free")
                            ).forEach { (number, label, _) ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = PrimaryTealUltraLight,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryTealLight.copy(alpha = 0.4f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            try {
                                                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
                                                context.startActivity(dialIntent)
                                            } catch (_: Exception) {}
                                        }
                                ) {
                                    Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryTealDark)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
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

@Composable
private fun FeatureBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector, 
    text: String,
    onClick: () -> Unit = {}
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .clickable { onClick() }
    ) {
        Surface(
            shape = CircleShape,
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
            shadowElevation = 2.dp,
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = TextSecondaryLight)
    }
}

private fun formatHumanDate(dateStr: String): String {
    if (dateStr.isBlank()) return "Select Journey Date"
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = parser.parse(dateStr)
        if (date != null) {
            val formatter = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())
            formatter.format(date)
        } else {
            dateStr
        }
    } catch (e: Exception) {
        dateStr
    }
}
