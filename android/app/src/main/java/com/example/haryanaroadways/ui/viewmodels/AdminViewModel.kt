package com.example.haryanaroadways.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.haryanaroadways.data.api.Booking
import com.example.haryanaroadways.data.api.Bus
import com.example.haryanaroadways.data.api.CreateBusRequest
import com.example.haryanaroadways.data.api.CreateRouteRequest
import com.example.haryanaroadways.data.api.CreateScheduleRequest
import com.example.haryanaroadways.data.api.DashboardStatsData
import com.example.haryanaroadways.data.api.Route
import com.example.haryanaroadways.data.api.Schedule
import com.example.haryanaroadways.data.api.UpdateBusRequest
import com.example.haryanaroadways.data.api.UpdateRouteRequest
import com.example.haryanaroadways.data.api.UpdateScheduleRequest
import com.example.haryanaroadways.data.api.UserData
import com.example.haryanaroadways.data.repository.AdminRepository
import com.example.haryanaroadways.data.repository.BookingRepository
import com.example.haryanaroadways.data.repository.ScheduleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

data class AdminUiState(
    val isLoadingStats: Boolean = false,
    val stats: DashboardStatsData? = null,
    val statsError: String? = null,

    val isLoadingPending: Boolean = false,
    val pendingBookings: List<Booking> = emptyList(),
    val pendingError: String? = null,

    val isScanning: Boolean = false,
    val scanResult: Booking? = null,
    val scanError: String? = null,

    val isLoadingAllBookings: Boolean = false,
    val allBookings: List<Booking> = emptyList(),
    val allBookingsError: String? = null,

    val isActionLoading: Boolean = false,

    val conductors: List<UserData> = emptyList(),
    val isLoadingConductors: Boolean = false,
    val conductorError: String? = null,

    val buses: List<Bus> = emptyList(),
    val isLoadingBuses: Boolean = false,
    val busError: String? = null,

    val routes: List<Route> = emptyList(),
    val isLoadingRoutes: Boolean = false,
    val routeError: String? = null,

    val schedules: List<Schedule> = emptyList(),
    val isLoadingSchedules: Boolean = false,
    val scheduleError: String? = null,

    val qrCodes: List<com.example.haryanaroadways.data.api.QRCode> = emptyList(),
    val isLoadingQRCodes: Boolean = false,
    val qrError: String? = null
)

class AdminViewModel(
    private val adminRepository: AdminRepository,
    private val bookingRepository: BookingRepository,
    private val scheduleRepository: ScheduleRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    init {
        fetchDashboardStats()
        fetchPendingBookings()
        fetchAllBookings()
        fetchBuses()
        fetchRoutes()
        fetchQRCodes()
    }

    fun fetchDashboardStats() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingStats = true, statsError = null) }
            try {
                val res = adminRepository.getDashboardStats()
                if (res.success && res.data != null) {
                    _uiState.update { it.copy(stats = res.data) }
                } else {
                    _uiState.update { it.copy(statsError = res.message) }
                }
            } catch (e: Throwable) {
                _uiState.update { it.copy(statsError = e.message) }
            } finally {
                _uiState.update { it.copy(isLoadingStats = false) }
            }
        }
    }

    fun fetchPendingBookings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingPending = true, pendingError = null) }
            try {
                val res = adminRepository.getPendingBookings()
                if (res.success && res.data != null) {
                    _uiState.update { it.copy(pendingBookings = res.data) }
                } else {
                    _uiState.update { it.copy(pendingError = res.message) }
                }
            } catch (e: Throwable) {
                _uiState.update { it.copy(pendingError = e.message) }
            } finally {
                _uiState.update { it.copy(isLoadingPending = false) }
            }
        }
    }

    fun fetchAllBookings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingAllBookings = true, allBookingsError = null) }
            try {
                val res = adminRepository.getAllBookings()
                if (res.success && res.data != null) {
                    _uiState.update { it.copy(allBookings = res.data.bookings) }
                } else {
                    _uiState.update { it.copy(allBookingsError = res.message) }
                }
            } catch (e: Throwable) {
                _uiState.update { it.copy(allBookingsError = e.message) }
            } finally {
                _uiState.update { it.copy(isLoadingAllBookings = false) }
            }
        }
    }

    fun approveBooking(bookingId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, pendingError = null) }
            try {
                val res = adminRepository.confirmBooking(bookingId)
                if (res.success) {
                    _uiState.update { state ->
                        state.copy(pendingBookings = state.pendingBookings.filter { it._id != bookingId })
                    }
                    fetchDashboardStats()
                    fetchAllBookings()
                } else {
                    _uiState.update { it.copy(pendingError = res.message.ifEmpty { "Failed to approve booking" }) }
                }
            } catch (e: Throwable) {
                _uiState.update { it.copy(pendingError = e.message ?: "Failed to approve booking") }
            } finally {
                _uiState.update { it.copy(isActionLoading = false) }
            }
        }
    }

    fun rejectBooking(bookingId: String, reason: String = "Payment or booking details could not be verified") {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, pendingError = null) }
            try {
                val res = adminRepository.rejectBooking(bookingId, reason)
                if (res.success) {
                    _uiState.update { state ->
                        state.copy(pendingBookings = state.pendingBookings.filter { it._id != bookingId })
                    }
                    fetchDashboardStats()
                    fetchAllBookings()
                } else {
                    _uiState.update { it.copy(pendingError = res.message.ifEmpty { "Failed to reject booking" }) }
                }
            } catch (e: Throwable) {
                _uiState.update { it.copy(pendingError = e.message ?: "Failed to reject booking") }
            } finally {
                _uiState.update { it.copy(isActionLoading = false) }
            }
        }
    }

    fun scanTicket(pnr: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, scanError = null, scanResult = null) }
            try {
                val res = bookingRepository.getTicketByPNR(pnr)
                if (res.success && res.data != null) {
                    val ticket = res.data
                    val mockBooking = Booking(
                        _id = "",
                        pnr = ticket.pnr,
                        passengerName = ticket.passengerName,
                        seats = ticket.seats,
                        totalFare = ticket.totalFare,
                        status = ticket.status,
                        journeyDate = ticket.journeyDate,
                        schedule = ticket.schedule,
                        referenceCode = ticket.referenceCode
                    )
                    _uiState.update { it.copy(scanResult = mockBooking) }
                } else {
                    _uiState.update { it.copy(scanError = res.message) }
                }
            } catch (e: Throwable) {
                _uiState.update { it.copy(scanError = e.message) }
            } finally {
                _uiState.update { it.copy(isScanning = false) }
            }
        }
    }

    fun resetScanResult() {
        _uiState.update { it.copy(scanResult = null, scanError = null) }
    }

    fun fetchConductors() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingConductors = true, conductorError = null) }
            try {
                val res = adminRepository.getConductors()
                if (res.success && res.data != null) {
                    _uiState.update { it.copy(conductors = res.data) }
                } else {
                    _uiState.update { it.copy(conductorError = res.message) }
                }
            } catch (e: Throwable) {
                _uiState.update { it.copy(conductorError = e.message) }
            } finally {
                _uiState.update { it.copy(isLoadingConductors = false) }
            }
        }
    }

    fun createConductor(name: String, mobile: String, password: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true) }
            try {
                val res = adminRepository.createConductor(mobile, name, password)
                if (res.success) {
                    fetchConductors()
                    onSuccess()
                } else {
                    _uiState.update { it.copy(conductorError = res.message) }
                }
            } catch (e: Throwable) {
                _uiState.update { it.copy(conductorError = e.message) }
            } finally {
                _uiState.update { it.copy(isActionLoading = false) }
            }
        }
    }

    fun deleteConductor(id: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true) }
            try {
                val res = adminRepository.deleteConductor(id)
                if (res.success) {
                    fetchConductors()
                    onSuccess()
                } else {
                    _uiState.update { it.copy(conductorError = res.message) }
                }
            } catch (e: Throwable) {
                _uiState.update { it.copy(conductorError = e.message) }
            } finally {
                _uiState.update { it.copy(isActionLoading = false) }
            }
        }
    }

    // ── Bus Management ──────────────────────────────────────────────────
    fun uploadImage(context: android.content.Context, uri: android.net.Uri, onUploaded: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, busError = null) }
            try {
                val contentResolver = context.contentResolver
                val inputStream = contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes() ?: return@launch
                val mediaType = "image/*".toMediaTypeOrNull()
                val requestFile = bytes.toRequestBody(mediaType)
                val body = MultipartBody.Part.createFormData("image", "photo_${System.currentTimeMillis()}.jpg", requestFile)

                val res = adminRepository.uploadBusImage(body)
                if (res.success && res.data != null) {
                    val fullUrl = if (res.data.url.startsWith("http")) res.data.url else "https://backend.haryanabusinfo.in${res.data.url}"
                    onUploaded(fullUrl)
                } else {
                    _uiState.update { it.copy(busError = res.message.ifEmpty { "Failed to upload photo" }) }
                }
            } catch (e: Throwable) {
                _uiState.update { it.copy(busError = e.message ?: "Failed to upload photo") }
            } finally {
                _uiState.update { it.copy(isActionLoading = false) }
            }
        }
    }

    fun fetchBuses() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingBuses = true, busError = null) }
            try {
                val res = adminRepository.getBuses()
                if (res.success && res.data != null) {
                    _uiState.update { it.copy(buses = res.data) }
                } else {
                    _uiState.update { it.copy(busError = res.message) }
                }
            } catch (e: Throwable) {
                _uiState.update { it.copy(busError = e.message) }
            } finally {
                _uiState.update { it.copy(isLoadingBuses = false) }
            }
        }
    }

    fun createBus(request: CreateBusRequest, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, busError = null) }
            try {
                val res = adminRepository.createBus(request)
                if (res.success) {
                    fetchBuses()
                    onSuccess()
                } else {
                    _uiState.update { it.copy(busError = res.message) }
                }
            } catch (e: Throwable) {
                _uiState.update { it.copy(busError = e.message) }
            } finally {
                _uiState.update { it.copy(isActionLoading = false) }
            }
        }
    }

    fun updateBus(id: String, request: UpdateBusRequest, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, busError = null) }
            try {
                val res = adminRepository.updateBus(id, request)
                if (res.success) {
                    fetchBuses()
                    onSuccess()
                } else {
                    _uiState.update { it.copy(busError = res.message) }
                }
            } catch (e: Throwable) {
                _uiState.update { it.copy(busError = e.message) }
            } finally {
                _uiState.update { it.copy(isActionLoading = false) }
            }
        }
    }

    fun deleteBus(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true) }
            try {
                val res = adminRepository.deleteBus(id)
                if (res.success) {
                    fetchBuses()
                }
            } catch (e: Throwable) {
                // handle error
            } finally {
                _uiState.update { it.copy(isActionLoading = false) }
            }
        }
    }

    // ── Route Management ────────────────────────────────────────────────
    fun fetchRoutes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingRoutes = true, routeError = null) }
            try {
                if (scheduleRepository != null) {
                    val res = scheduleRepository.getRoutes()
                    if (res.success && res.data != null) {
                        _uiState.update { it.copy(routes = res.data) }
                    }
                }
            } catch (e: Throwable) {
                _uiState.update { it.copy(routeError = e.message) }
            } finally {
                _uiState.update { it.copy(isLoadingRoutes = false) }
            }
        }
    }

    fun createRoute(request: CreateRouteRequest, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, routeError = null) }
            try {
                val res = adminRepository.createRoute(request)
                if (res.success) {
                    fetchRoutes()
                    onSuccess()
                } else {
                    _uiState.update { it.copy(routeError = res.message) }
                }
            } catch (e: Throwable) {
                _uiState.update { it.copy(routeError = e.message) }
            } finally {
                _uiState.update { it.copy(isActionLoading = false) }
            }
        }
    }

    fun updateRoute(id: String, request: UpdateRouteRequest, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, routeError = null) }
            try {
                val res = adminRepository.updateRoute(id, request)
                if (res.success) {
                    fetchRoutes()
                    onSuccess()
                } else {
                    _uiState.update { it.copy(routeError = res.message) }
                }
            } catch (e: Throwable) {
                _uiState.update { it.copy(routeError = e.message) }
            } finally {
                _uiState.update { it.copy(isActionLoading = false) }
            }
        }
    }

    fun deleteRoute(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true) }
            try {
                val res = adminRepository.deleteRoute(id)
                if (res.success) {
                    fetchRoutes()
                }
            } catch (e: Throwable) {
                // handle error
            } finally {
                _uiState.update { it.copy(isActionLoading = false) }
            }
        }
    }

    // ── Schedule Management ────────────────────────────────────────────────
    fun fetchSchedules() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingSchedules = true, scheduleError = null) }
            try {
                val res = adminRepository.getSchedules()
                if (res.success && res.data != null) {
                    _uiState.update { it.copy(schedules = res.data) }
                } else {
                    _uiState.update { it.copy(scheduleError = res.message) }
                }
            } catch (e: Throwable) {
                _uiState.update { it.copy(scheduleError = e.message) }
            } finally {
                _uiState.update { it.copy(isLoadingSchedules = false) }
            }
        }
    }

    fun createSchedule(request: CreateScheduleRequest, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, scheduleError = null) }
            try {
                val res = adminRepository.createSchedule(request)
                if (res.success) {
                    fetchSchedules()
                    onSuccess()
                } else {
                    _uiState.update { it.copy(scheduleError = res.message) }
                }
            } catch (e: Throwable) {
                _uiState.update { it.copy(scheduleError = e.message) }
            } finally {
                _uiState.update { it.copy(isActionLoading = false) }
            }
        }
    }

    fun updateSchedule(id: String, request: UpdateScheduleRequest, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, scheduleError = null) }
            try {
                val res = adminRepository.updateSchedule(id, request)
                if (res.success) {
                    fetchSchedules()
                    onSuccess()
                } else {
                    _uiState.update { it.copy(scheduleError = res.message) }
                }
            } catch (e: Throwable) {
                _uiState.update { it.copy(scheduleError = e.message) }
            } finally {
                _uiState.update { it.copy(isActionLoading = false) }
            }
        }
    }

    fun deleteSchedule(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true) }
            try {
                val res = adminRepository.deleteSchedule(id)
                if (res.success) {
                    fetchSchedules()
                }
            } catch (e: Throwable) {
                // handle error
            } finally {
                _uiState.update { it.copy(isActionLoading = false) }
            }
        }
    }

    // ── QR Code Management ────────────────────────────────────────────────
    fun fetchQRCodes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingQRCodes = true, qrError = null) }
            try {
                val res = adminRepository.getQRCodes()
                if (res.success && res.data != null) {
                    _uiState.update { it.copy(qrCodes = res.data) }
                } else {
                    _uiState.update { it.copy(qrError = res.message) }
                }
            } catch (e: Throwable) {
                _uiState.update { it.copy(qrError = e.message) }
            } finally {
                _uiState.update { it.copy(isLoadingQRCodes = false) }
            }
        }
    }

    fun createQRCode(context: android.content.Context, uri: android.net.Uri, upiId: String, upiLabel: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, qrError = null) }
            try {
                val contentResolver = context.contentResolver
                val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
                val extension = if (mimeType.contains("png", ignoreCase = true)) ".png" else ".jpg"
                val inputStream = contentResolver.openInputStream(uri) ?: throw IllegalStateException("Could not read QR image")
                val tempFile = java.io.File(context.cacheDir, "qr_${System.currentTimeMillis()}$extension")
                tempFile.outputStream().use { output ->
                    inputStream.copyTo(output)
                }
                val mediaType = mimeType.toMediaTypeOrNull() ?: "image/jpeg".toMediaTypeOrNull()
                val requestFile = tempFile.asRequestBody(mediaType)
                val body = MultipartBody.Part.createFormData("qrImage", tempFile.name, requestFile)

                val res = adminRepository.createQRCode(upiId, upiLabel, body)
                if (res.success) {
                    fetchQRCodes()
                    onSuccess()
                } else {
                    _uiState.update { it.copy(qrError = res.message.ifEmpty { "Failed to create QR code" }) }
                }
            } catch (e: Throwable) {
                _uiState.update { it.copy(qrError = e.message ?: "Failed to create QR code") }
            } finally {
                _uiState.update { it.copy(isActionLoading = false) }
            }
        }
    }

    fun activateQRCode(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true) }
            try {
                val res = adminRepository.setActiveQRCode(id)
                if (res.success) {
                    fetchQRCodes()
                }
            } catch (e: Throwable) {
                // handle error
            } finally {
                _uiState.update { it.copy(isActionLoading = false) }
            }
        }
    }

    fun deleteQRCode(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true) }
            try {
                val res = adminRepository.deleteQRCode(id)
                if (res.success) {
                    fetchQRCodes()
                }
            } catch (e: Throwable) {
                // handle error
            } finally {
                _uiState.update { it.copy(isActionLoading = false) }
            }
        }
    }
}
