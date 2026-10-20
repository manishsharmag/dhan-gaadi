package com.example.haryanaroadways.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.haryanaroadways.theme.AccentSaffron
import com.example.haryanaroadways.theme.PrimaryTeal
import com.example.haryanaroadways.theme.PrimaryTealDark
import com.example.haryanaroadways.theme.SuccessGreen
import com.example.haryanaroadways.ui.AppViewModelProvider
import com.example.haryanaroadways.ui.viewmodels.TrackingViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusTrackingScreen(
    scheduleId: String,
    onBack: () -> Unit,
    viewModel: TrackingViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    
    var mapViewInstance by remember { mutableStateOf<MapView?>(null) }
    var busMarkerInstance by remember { mutableStateOf<Marker?>(null) }
    var hasCenteredOnBus by remember { mutableStateOf(false) }

    // Initialize OSMDroid config
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(
            context,
            context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        )
        viewModel.fetchTrackingForSchedule(scheduleId)
    }

    DisposableEffect(mapViewInstance) {
        mapViewInstance?.onResume()
        onDispose {
            mapViewInstance?.onPause()
            mapViewInstance?.onDetach()
        }
    }

    val tracking = uiState.currentTracking

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Live Bus GPS Tracking", color = Color.White, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryTealDark),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.fetchTrackingForSchedule(scheduleId) }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = Color.White)
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
        ) {
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryTeal)
                }
            } else if (tracking == null) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.LightGray.copy(alpha = 0.3f),
                        modifier = Modifier.size(80.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.DirectionsBus, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(40.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Bus GPS is Currently Offline",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Live telemetry will stream automatically once the conductor starts their journey broadcast.",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        MapView(ctx).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            controller.setZoom(16.0)
                            mapViewInstance = this
                        }
                    },
                    update = { mapView ->
                        val lat = tracking.effectiveLat
                        val lng = tracking.effectiveLng
                        if (lat != 0.0 && lng != 0.0) {
                            val geoPoint = GeoPoint(lat, lng)
                            if (!hasCenteredOnBus) {
                                mapView.controller.animateTo(geoPoint)
                                hasCenteredOnBus = true
                            }

                            if (busMarkerInstance == null) {
                                val marker = Marker(mapView).apply {
                                    position = geoPoint
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                    title = "Dhan-Gaadi Fleet Express"
                                }
                                mapView.overlays.add(marker)
                                busMarkerInstance = marker
                            } else {
                                busMarkerInstance?.position = geoPoint
                            }

                            mapView.invalidate()
                        }
                    }
                )

                // Floating Recenter Button
                FloatingActionButton(
                    onClick = {
                        val lat = tracking.effectiveLat
                        val lng = tracking.effectiveLng
                        if (lat != 0.0 && lng != 0.0) {
                            mapViewInstance?.controller?.animateTo(GeoPoint(lat, lng))
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 130.dp, end = 20.dp),
                    containerColor = PrimaryTeal,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Navigation, contentDescription = "Recenter Bus")
                }

                // Telemetry HUD Card
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                                    color = PrimaryTeal.copy(alpha = 0.1f),
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Filled.Speed, contentDescription = "Speed", tint = PrimaryTeal, modifier = Modifier.size(24.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Speed", color = Color.Gray, fontSize = 12.sp)
                                    val speedKmh = (tracking.speed * 3.6).roundToInt()
                                    Text("$speedKmh km/h", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = PrimaryTealDark)
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = SuccessGreen.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        "● LIVE EN ROUTE",
                                        color = SuccessGreen,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                val formattedUpdatedTime = remember(tracking.lastUpdated) {
                                    try {
                                        if (tracking.lastUpdated.contains("T")) {
                                            tracking.lastUpdated.substringAfter("T").take(8)
                                        } else {
                                            tracking.lastUpdated.takeLast(8).ifEmpty { "Just now" }
                                        }
                                    } catch (e: Exception) {
                                        "Just now"
                                    }
                                }
                                Text(
                                    "Updated $formattedUpdatedTime",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        val conductor = tracking.schedule?.conductor
                        if (conductor != null) {
                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Surface(
                                        shape = CircleShape,
                                        color = PrimaryTeal.copy(alpha = 0.12f),
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            if (!conductor.photo.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = conductor.photo,
                                                    contentDescription = conductor.name,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                                                )
                                            } else {
                                                Icon(Icons.Filled.Person, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(22.dp))
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(conductor.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PrimaryTealDark)
                                        Text("Assigned Conductor", fontSize = 11.sp, color = Color.Gray)
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = PrimaryTeal.copy(alpha = 0.12f),
                                    modifier = Modifier.clickable {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${conductor.mobile}"))
                                        context.startActivity(intent)
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Filled.Phone, contentDescription = "Call", tint = PrimaryTealDark, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Call", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PrimaryTealDark)
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
