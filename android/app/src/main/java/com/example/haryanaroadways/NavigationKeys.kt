package com.example.haryanaroadways

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Home : NavKey
@Serializable data class SearchResults(val source: String, val destination: String, val date: String) : NavKey
@Serializable data class SeatSelection(val scheduleId: String, val date: String, val fare: Int = 0) : NavKey
@Serializable data class PassengerDetails(val scheduleId: String, val date: String, val selectedSeats: String, val totalFare: Int) : NavKey
@Serializable data class Payment(val bookingId: String) : NavKey
@Serializable data class Ticket(val pnr: String) : NavKey
@Serializable data class BusTracking(val scheduleId: String) : NavKey
@Serializable data object Profile : NavKey

// Admin & Staff Routes
@Serializable data object AdminDashboard : NavKey
@Serializable data object Pos : NavKey
@Serializable data object ConductorDuty : NavKey
