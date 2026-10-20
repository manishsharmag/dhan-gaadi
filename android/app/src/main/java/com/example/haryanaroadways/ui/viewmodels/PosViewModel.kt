package com.example.haryanaroadways.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.haryanaroadways.data.api.CreateBookingRequest
import com.example.haryanaroadways.data.api.Schedule
import com.example.haryanaroadways.data.repository.BookingRepository
import com.example.haryanaroadways.data.repository.ScheduleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PosUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val successPnr: String? = null,
    val schedules: List<Schedule> = emptyList(),
    val selectedScheduleId: String = "",
    val totalSeats: Int = 45,
    val unavailableSeats: Set<Int> = emptySet(),
    val selectedSeats: List<Int> = emptyList(),
    val isFetchingSeats: Boolean = false,
    val passengerName: String = "",
    val passengerMobile: String = "",
    val paymentMethod: String = "cash"
)

class PosViewModel(
    private val bookingRepository: BookingRepository,
    private val scheduleRepository: ScheduleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PosUiState())
    val uiState: StateFlow<PosUiState> = _uiState.asStateFlow()

    init {
        fetchTodaySchedules()
    }

    private fun getTodayDate(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return formatter.format(Date())
    }

    private fun fetchTodaySchedules() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val res = scheduleRepository.getSchedules()
                if (res.success && res.data != null) {
                    val todayDay = SimpleDateFormat("EEE", Locale.ENGLISH).format(Date())
                    val filtered = res.data.filter { schedule ->
                        schedule.status == "active" &&
                        (schedule.daysOfOperation.isEmpty() || schedule.daysOfOperation.any { it.equals(todayDay, ignoreCase = true) })
                    }
                    _uiState.update { it.copy(schedules = filtered) }
                } else {
                    _uiState.update { it.copy(error = res.message.ifBlank { "Failed to load schedules" }) }
                }
            } catch (e: Throwable) {
                _uiState.update { it.copy(error = e.message ?: "Failed to load schedules") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun updateForm(
        scheduleId: String = _uiState.value.selectedScheduleId,
        name: String = _uiState.value.passengerName,
        mobile: String = _uiState.value.passengerMobile,
        paymentMethod: String = _uiState.value.paymentMethod
    ) {
        if (scheduleId != _uiState.value.selectedScheduleId) {
            fetchSeatAvailability(scheduleId)
        }
        _uiState.update { 
            it.copy(
                selectedScheduleId = scheduleId,
                passengerName = name,
                passengerMobile = mobile,
                paymentMethod = paymentMethod,
                error = null
            )
        }
    }

    private fun fetchSeatAvailability(scheduleId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isFetchingSeats = true, selectedSeats = emptyList()) }
            try {
                val today = getTodayDate()
                val res = bookingRepository.getSeatAvailability(scheduleId, today)
                if (res.success && res.data != null) {
                    _uiState.update {
                        it.copy(
                            totalSeats = res.data.totalSeats,
                            unavailableSeats = res.data.unavailableSeats.map { u -> u.seatNumber }.toSet()
                        )
                    }
                }
            } catch (e: Throwable) {
                // Ignore error, seats will remain default
            } finally {
                _uiState.update { it.copy(isFetchingSeats = false) }
            }
        }
    }

    fun toggleSeatSelection(seatNumber: Int) {
        _uiState.update { state ->
            val current = state.selectedSeats.toMutableList()
            if (current.contains(seatNumber)) {
                current.remove(seatNumber)
            } else {
                current.add(seatNumber)
            }
            state.copy(selectedSeats = current, error = null)
        }
    }

    fun toggleSeat(seatNumber: Int) = toggleSeatSelection(seatNumber)

    fun createWalkInBooking() {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.selectedScheduleId.isEmpty() || state.passengerName.isBlank() || state.passengerMobile.length < 10) {
                _uiState.update { it.copy(error = "Please fill all details correctly") }
                return@launch
            }
            if (state.selectedSeats.isEmpty()) {
                _uiState.update { it.copy(error = "Please select at least one seat") }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val today = getTodayDate()
                
                val request = CreateBookingRequest(
                    scheduleId = state.selectedScheduleId,
                    journeyDate = today,
                    seats = state.selectedSeats,
                    passengerName = state.passengerName,
                    passengerMobile = state.passengerMobile,
                    paymentMethod = state.paymentMethod
                )
                
                val response = bookingRepository.createBooking(request)
                if (response.success && response.data != null) {
                    _uiState.update { it.copy(successPnr = response.data.booking.pnr) }
                } else {
                    _uiState.update { it.copy(error = response.message) }
                }
            } catch (e: Throwable) {
                _uiState.update { it.copy(error = e.message ?: "Failed to book ticket") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun submitBooking() = createWalkInBooking()

    fun reset() {
        _uiState.update { PosUiState(schedules = it.schedules) }
    }
}
