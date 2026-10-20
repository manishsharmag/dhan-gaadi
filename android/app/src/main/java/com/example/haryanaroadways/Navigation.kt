package com.example.haryanaroadways

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.haryanaroadways.ui.AppViewModelProvider
import com.example.haryanaroadways.ui.screens.*
import com.example.haryanaroadways.ui.viewmodels.BookingViewModel

@Composable
fun MainNavigation() {
    val backStack = rememberNavBackStack(Home)

    // Shared BookingViewModel across seat selection and passenger details
    val bookingViewModel: BookingViewModel = viewModel(factory = AppViewModelProvider.Factory)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<Home> {
                HomeScreen(
                    onNavigateToSearch = { src, dst, date -> 
                        backStack.add(SearchResults(src, dst, date)) 
                    },
                    onNavigateToProfile = {
                        backStack.add(Profile)
                    },
                    onNavigateToTicket = { pnr ->
                        backStack.add(Ticket(pnr))
                    },
                    onNavigateToTracking = { scheduleId ->
                        backStack.add(BusTracking(scheduleId))
                    }
                )
            }
            entry<SearchResults> { key ->
                SearchResultsScreen(
                    source = key.source,
                    destination = key.destination,
                    date = key.date,
                    onBack = { backStack.removeLast() },
                    onScheduleSelected = { scheduleId, fare ->
                        backStack.add(SeatSelection(scheduleId, key.date, fare))
                    }
                )
            }
            entry<SeatSelection> { key ->
                LaunchedEffect(key.scheduleId, key.date, key.fare) {
                    bookingViewModel.initBooking(key.scheduleId, key.date, key.fare)
                }
                SeatSelectionScreen(
                    viewModel = bookingViewModel,
                    onBack = { backStack.removeLast() },
                    onNavigateToPassengerDetails = {
                        val seats = bookingViewModel.uiState.value.selectedSeats.joinToString(",")
                        val fare = bookingViewModel.uiState.value.totalFare
                        backStack.add(PassengerDetails(key.scheduleId, key.date, seats, fare))
                    }
                )
            }
            entry<PassengerDetails> { key ->
                PassengerDetailsScreen(
                    viewModel = bookingViewModel,
                    onBack = { backStack.removeLast() },
                    onNavigateToPayment = { bookingId ->
                        backStack.removeLast()
                        backStack.removeLast()
                        backStack.add(Payment(bookingId))
                    }
                )
            }
            entry<Payment> { key ->
                PaymentScreen(
                    bookingId = key.bookingId,
                    onBack = { backStack.removeLast() },
                    onPaymentSuccess = { pnr ->
                        backStack.removeLast()
                        if (pnr.isNotBlank()) {
                            backStack.add(Ticket(pnr))
                        } else {
                            while (backStack.size > 1) { backStack.removeLast() }
                        }
                    }
                )
            }
            entry<Ticket> { key ->
                TicketScreen(
                    pnr = key.pnr,
                    onBack = { backStack.removeLast() },
                    onTrackBus = { scheduleId -> backStack.add(BusTracking(scheduleId)) }
                )
            }
            entry<BusTracking> { key ->
                BusTrackingScreen(
                    scheduleId = key.scheduleId,
                    onBack = { backStack.removeLast() }
                )
            }
            entry<Profile> {
                ProfileScreen(
                    onBack = { backStack.removeLast() },
                    onTicketClick = { pnr -> backStack.add(Ticket(pnr)) },
                    onNavigateToAdminDashboard = { backStack.add(AdminDashboard) },
                    onNavigateToPos = { backStack.add(Pos) },
                    onNavigateToDuty = { backStack.add(ConductorDuty) }
                )
            }
            entry<AdminDashboard> {
                AdminDashboardScreen(
                    onNavigateToPos = { backStack.add(Pos) },
                    onNavigateBack = { backStack.removeLast() },
                    onLogout = { 
                        while(backStack.size > 1) { backStack.removeLast() } 
                    }
                )
            }
            entry<ConductorDuty> {
                ConductorDutyScreen(
                    onBack = { backStack.removeLast() },
                    onNavigateToPos = { backStack.add(Pos) },
                    onNavigateToTicket = { pnr -> backStack.add(Ticket(pnr)) }
                )
            }
            entry<Pos> {
                PosScreen(
                    onNavigateBack = { backStack.removeLast() },
                    onNavigateToTicket = { pnr -> backStack.add(Ticket(pnr)) }
                )
            }
        }
    )
}
