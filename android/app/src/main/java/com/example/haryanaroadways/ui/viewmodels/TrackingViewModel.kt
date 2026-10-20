package com.example.haryanaroadways.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.haryanaroadways.BuildConfig
import com.example.haryanaroadways.data.api.ActiveTrackingData
import com.example.haryanaroadways.data.api.TrackingLocation
import com.example.haryanaroadways.data.repository.TrackingRepository
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject

data class TrackingUiState(
    val isLoading: Boolean = false,
    val activeTrackings: List<ActiveTrackingData> = emptyList(),
    val currentTracking: ActiveTrackingData? = null,
    val error: String? = null
)

class TrackingViewModel(
    private val trackingRepository: TrackingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrackingUiState())
    val uiState: StateFlow<TrackingUiState> = _uiState.asStateFlow()

    private var socket: Socket? = null
    private var subscribedScheduleId: String? = null

    init {
        connectSocket()
    }

    private fun connectSocket() {
        try {
            val apiBase = BuildConfig.API_BASE_URL.trimEnd('/')
            val backendUrl = if (apiBase.endsWith("/api")) apiBase.removeSuffix("/api") else apiBase
            socket = IO.socket(backendUrl)
            
            socket?.on(Socket.EVENT_CONNECT) {
                println("[SOCKET] Connected to tracking server at $backendUrl")
                subscribedScheduleId?.let { scheduleId ->
                    subscribeToBus(scheduleId)
                }
            }

            // Server broadcasts on 'bus:location'
            socket?.on("bus:location") { args ->
                if (args.isNotEmpty()) {
                    try {
                        val data = args[0] as JSONObject
                        val scheduleId = data.optString("scheduleId", "")
                        val speed = data.optDouble("speed", 0.0)
                        val heading = data.optDouble("heading", 0.0)

                        val locObj = data.optJSONObject("location")
                        val lat = locObj?.optDouble("lat") ?: data.optDouble("lat", 0.0)
                        val lng = locObj?.optDouble("lng") ?: data.optDouble("lng", 0.0)

                        if (lat != 0.0 && lng != 0.0) {
                            _uiState.update { state ->
                                val current = state.currentTracking
                                val updatedCurrent = if (current?.effectiveScheduleId == scheduleId || current?.scheduleId == scheduleId) {
                                    current.copy(
                                        lat = lat,
                                        lng = lng,
                                        speed = speed,
                                        heading = heading,
                                        location = TrackingLocation(lat, lng)
                                    )
                                } else current

                                val updatedList = state.activeTrackings.map {
                                    if (it.effectiveScheduleId == scheduleId || it.scheduleId == scheduleId) {
                                        it.copy(
                                            lat = lat,
                                            lng = lng,
                                            speed = speed,
                                            heading = heading,
                                            location = TrackingLocation(lat, lng)
                                        )
                                    } else it
                                }
                                
                                state.copy(currentTracking = updatedCurrent, activeTrackings = updatedList)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            socket?.connect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun subscribeToBus(scheduleId: String) {
        subscribedScheduleId = scheduleId
        try {
            val json = JSONObject()
            json.put("scheduleId", scheduleId)
            socket?.emit("bus:subscribe", json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun fetchAllActiveTrackings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val response = trackingRepository.getAllActiveTrackings()
            if (response.success && response.data != null) {
                _uiState.update { it.copy(isLoading = false, activeTrackings = response.data) }
            } else {
                _uiState.update { it.copy(isLoading = false, error = response.message) }
            }
        }
    }

    fun fetchTrackingForSchedule(scheduleId: String) {
        subscribeToBus(scheduleId)
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val response = trackingRepository.getTrackingForSchedule(scheduleId)
            if (response.success && response.data != null) {
                _uiState.update { it.copy(isLoading = false, currentTracking = response.data) }
            } else {
                _uiState.update { it.copy(isLoading = false, error = response.message) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        subscribedScheduleId?.let { id ->
            try {
                val json = JSONObject()
                json.put("scheduleId", id)
                socket?.emit("bus:unsubscribe", json)
            } catch (_: Exception) {}
        }
        socket?.disconnect()
    }
}
