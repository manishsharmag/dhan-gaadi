package com.example.haryanaroadways.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.haryanaroadways.theme.PrimaryTeal
import com.example.haryanaroadways.ui.viewmodels.TrackingViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun LiveFleetTab(
    trackingViewModel: TrackingViewModel
) {
    val context = LocalContext.current
    val uiState by trackingViewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(
            context,
            context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        )
        trackingViewModel.fetchAllActiveTrackings()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        if (uiState.isLoading && uiState.activeTrackings.isEmpty()) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = PrimaryTeal
            )
        } else if (uiState.activeTrackings.isEmpty()) {
            Text(
                "No active buses on the road currently.",
                modifier = Modifier.align(Alignment.Center),
                color = Color.Gray
            )
        } else {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    MapView(ctx).apply {
                        setMultiTouchControls(true)
                        controller.setZoom(10.0)
                        
                        // Center on Haryana (rough coordinates)
                        controller.setCenter(GeoPoint(29.0588, 76.0856))
                    }
                },
                update = { mapView ->
                    mapView.overlays.clear()
                    
                    uiState.activeTrackings.forEach { tracking ->
                        val geoPoint = GeoPoint(tracking.lat, tracking.lng)
                        val marker = Marker(mapView)
                        marker.position = geoPoint
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        marker.title = "Bus: ${tracking.busId}\nSpeed: ${tracking.speed} km/h"
                        mapView.overlays.add(marker)
                    }
                    mapView.invalidate()
                }
            )
        }
    }
}
