package com.example.haryanaroadways.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.haryanaroadways.data.api.ApiResponse
import com.example.haryanaroadways.data.api.BookingCreatedData
import com.example.haryanaroadways.data.api.CreateBookingRequest
import com.example.haryanaroadways.data.repository.BookingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BookingState(
    val scheduleId: String = "",
    val journeyDate: String = "",
    val farePerSeat: Int = 0,
    
    val totalSeats: Int = 45,
    val unavailableSeats: List<Int> = emptyList(),
    val selectedSeats: List<Int> = emptyList(),
    
    val isFetchingSeats: Boolean = false,
    val seatsError: String? = null,
    
    val passengerName: String = "",
    val passengerMobile: String = "",
    val passengerAge: String = "",
    val passengerGender: String = "",
    
    val isBooking: Boolean = false,
    val bookingResult: ApiResponse<BookingCreatedData>? = null
) {
    val totalFare: Int
        get() = selectedSeats.size * farePerSeat

    val bookingError: String?
        get() = if (bookingResult?.success == false) bookingResult.message else null
}

class BookingViewModel(
    private val bookingRepository: BookingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookingState())
    val uiState: StateFlow<BookingState> = _uiState.asStateFlow()

    fun initBooking(scheduleId: String, date: String, fare: Int) {
        _uiState.update { 
            it.copy(
                scheduleId = scheduleId,
                journeyDate = date,
                farePerSeat = fare,
                selectedSeats = emptyList(),
                bookingResult = null
            )
        }
        fetchSeatAvailability(scheduleId, date)
    }

    fun fetchSeatAvailability(scheduleId: String, date: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isFetchingSeats = true, seatsError = null) }
            try {
                val response = bookingRepository.getSeatAvailability(scheduleId, date)
                if (response.success && response.data != null) {
                    val data = response.data
                    val unavailable = data.unavailableSeats.map { it.seatNumber }
                    _uiState.update { 
                        it.copy(
                            isFetchingSeats = false,
                            totalSeats = data.totalSeats,
                            unavailableSeats = unavailable
                        )
                    }
                } else {
                    _uiState.update { 
                        it.copy(
                            isFetchingSeats = false,
                            seatsError = response.message.ifBlank { "Failed to load seats" }
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isFetchingSeats = false,
                        seatsError = e.message ?: "Network error"
                    )
                }
            }
        }
    }

    fun toggleSeatSelection(seatNumber: Int) {
        _uiState.update { state ->
            if (state.unavailableSeats.contains(seatNumber)) return@update state
            
            val currentSelected = state.selectedSeats.toMutableList()
            if (currentSelected.contains(seatNumber)) {
                currentSelected.remove(seatNumber)
            } else {
                if (currentSelected.size < 6) {
                    currentSelected.add(seatNumber)
                }
            }
            state.copy(selectedSeats = currentSelected)
        }
    }

    fun updatePassengerDetails(
        name: String? = null,
        mobile: String? = null,
        age: String? = null,
        gender: String? = null
    ) {
        _uiState.update { 
            it.copy(
                passengerName = name ?: it.passengerName,
                passengerMobile = mobile ?: it.passengerMobile,
                passengerAge = age ?: it.passengerAge,
                passengerGender = gender ?: it.passengerGender
            )
        }
    }

    fun createBooking() {
        val state = _uiState.value
        if (state.selectedSeats.isEmpty() || state.passengerName.isBlank() || state.passengerMobile.isBlank()) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isBooking = true, bookingResult = null) }
            try {
                val request = CreateBookingRequest(
                    scheduleId = state.scheduleId,
                    journeyDate = state.journeyDate,
                    seats = state.selectedSeats,
                    passengerName = state.passengerName,
                    passengerMobile = state.passengerMobile,
                    passengerAge = state.passengerAge.toIntOrNull(),
                    passengerGender = state.passengerGender.ifBlank { null }?.lowercase()
                )
                val response = bookingRepository.createBooking(request)
                _uiState.update { it.copy(isBooking = false, bookingResult = response) }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isBooking = false,
                        bookingResult = ApiResponse(success = false, message = e.message ?: "Network error")
                    )
                }
            }
        }
    }
    
    fun resetBookingResult() {
        _uiState.update { it.copy(bookingResult = null) }
    }
}
