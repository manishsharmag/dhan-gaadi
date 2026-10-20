package com.example.haryanaroadways.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.haryanaroadways.R
import com.example.haryanaroadways.data.api.ApiClient
import com.example.haryanaroadways.data.api.LocationStopRequest
import com.example.haryanaroadways.data.api.LocationUpdateRequest
import com.google.android.gms.location.*
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.URI

class LocationTrackingService : Service() {

    companion object {
        const val TAG = "LocationTrackingService"
        const val CHANNEL_ID = "bus_tracking_channel"
        const val NOTIFICATION_ID = 1001
        const val EXTRA_SCHEDULE_ID = "scheduleId"
        const val EXTRA_SERVER_URL = "serverUrl"
        const val EXTRA_TOKEN = "token"

        private val _isRunningFlow = MutableStateFlow(false)
        val isRunningFlow: StateFlow<Boolean> = _isRunningFlow.asStateFlow()

        @Volatile
        var isRunning: Boolean = false
            private set
        @Volatile
        var activeScheduleId: String = ""
            private set
        @Volatile
        var currentSpeedKmh: Int = 0
            private set
        @Volatile
        var lastLatitude: Double = 0.0
            private set
        @Volatile
        var lastLongitude: Double = 0.0
            private set
        @Volatile
        var packetsSentCount: Int = 0
            private set

        fun start(context: Context, scheduleId: String, serverUrl: String, token: String) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                putExtra(EXTRA_SCHEDULE_ID, scheduleId)
                putExtra(EXTRA_SERVER_URL, serverUrl)
                putExtra(EXTRA_TOKEN, token)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, LocationTrackingService::class.java))
        }
    }

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var socket: Socket? = null
    private var scheduleId: String = ""
    private var locationCallback: LocationCallback? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        scheduleId = intent?.getStringExtra(EXTRA_SCHEDULE_ID) ?: ""
        val serverUrl = intent?.getStringExtra(EXTRA_SERVER_URL) ?: ""
        val token = intent?.getStringExtra(EXTRA_TOKEN) ?: ""

        if (scheduleId.isEmpty() || serverUrl.isEmpty()) {
            stopSelf()
            return START_NOT_STICKY
        }

        isRunning = true
        activeScheduleId = scheduleId
        packetsSentCount = 0
        _isRunningFlow.value = true

        // Start foreground with notification
        startForeground(NOTIFICATION_ID, buildNotification("Connecting GPS transmitter..."))

        // Connect to Socket.IO
        connectSocket(serverUrl, token)

        // Start location updates
        startLocationUpdates()

        return START_STICKY
    }

    private fun connectSocket(serverUrl: String, token: String) {
        try {
            val options = IO.Options.builder()
                .setAuth(mapOf("token" to token))
                .build()
            socket = IO.socket(URI.create(serverUrl), options)
            socket?.connect()
            socket?.on(Socket.EVENT_CONNECT) {
                Log.d(TAG, "Socket connected for schedule: $scheduleId")
            }
            socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
                Log.e(TAG, "Socket connection error: ${args.firstOrNull()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect socket: ${e.message}")
        }
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
            .setMinUpdateIntervalMillis(3000L)
            .setMinUpdateDistanceMeters(5f)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    val speed = (location.speed * 3.6).toInt() // m/s to km/h
                    currentSpeedKmh = speed
                    lastLatitude = location.latitude
                    lastLongitude = location.longitude
                    packetsSentCount++

                    val data = JSONObject().apply {
                        put("scheduleId", scheduleId)
                        put("lat", location.latitude)
                        put("lng", location.longitude)
                        put("speed", speed)
                        put("heading", location.bearing)
                        put("accuracy", location.accuracy)
                        put("timestamp", System.currentTimeMillis())
                    }

                    if (socket?.connected() == true) {
                        socket?.emit("bus:location-update", data)
                    } else {
                        // HTTP fallback broadcast
                        serviceScope.launch {
                            try {
                                ApiClient.api.updateLocation(
                                    LocationUpdateRequest(
                                        scheduleId = scheduleId,
                                        lat = location.latitude,
                                        lng = location.longitude,
                                        speed = speed,
                                        heading = location.bearing,
                                        accuracy = location.accuracy
                                    )
                                )
                            } catch (_: Exception) {}
                        }
                    }

                    // Update notification with speed
                    val notification = buildNotification("📍 Broadcasting Bus Location • ${speed} km/h")
                    val nm = getSystemService(NotificationManager::class.java)
                    nm.notify(NOTIFICATION_ID, notification)
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "Location permission not granted: ${e.message}")
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        val currentScheduleId = activeScheduleId
        activeScheduleId = ""
        currentSpeedKmh = 0
        _isRunningFlow.value = false

        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }

        if (currentScheduleId.isNotBlank()) {
            if (socket?.connected() == true) {
                socket?.emit("bus:tracking-stop", JSONObject().put("scheduleId", currentScheduleId))
            }
            serviceScope.launch {
                try {
                    ApiClient.api.stopLocation(LocationStopRequest(currentScheduleId))
                } catch (_: Exception) {}
            }
        }

        socket?.disconnect()
        socket?.close()
        serviceJob.cancel()
        Log.d(TAG, "Service destroyed, tracking stopped")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Bus Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when bus location is being broadcast to passengers"
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Dhan-Gaadi Live GPS")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
