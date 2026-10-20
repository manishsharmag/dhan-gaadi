package com.example.haryanaroadways.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.haryanaroadways.HRApp
import com.example.haryanaroadways.ui.viewmodels.AuthViewModel
import com.example.haryanaroadways.ui.viewmodels.BookingViewModel
import com.example.haryanaroadways.ui.viewmodels.SearchViewModel
import com.example.haryanaroadways.ui.viewmodels.ProfileViewModel

object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            SearchViewModel(
                hrApp().container.scheduleRepository
            )
        }
        initializer {
            BookingViewModel(
                hrApp().container.bookingRepository
            )
        }
        initializer {
            AuthViewModel(
                hrApp().container.authRepository
            )
        }
        initializer {
            ProfileViewModel(
                hrApp().container.authRepository,
                hrApp().container.bookingRepository
            )
        }
        initializer {
            com.example.haryanaroadways.ui.screens.PaymentViewModel(
                hrApp().container.bookingRepository
            )
        }
        initializer {
            com.example.haryanaroadways.ui.screens.TicketViewModel(
                hrApp().container.bookingRepository
            )
        }
        initializer {
            com.example.haryanaroadways.ui.viewmodels.AdminViewModel(
                hrApp().container.adminRepository,
                hrApp().container.bookingRepository,
                hrApp().container.scheduleRepository
            )
        }
        initializer {
            com.example.haryanaroadways.ui.viewmodels.PosViewModel(
                hrApp().container.bookingRepository,
                hrApp().container.scheduleRepository
            )
        }
        initializer {
            com.example.haryanaroadways.ui.viewmodels.TrackingViewModel(
                hrApp().container.trackingRepository
            )
        }
        initializer {
            com.example.haryanaroadways.ui.screens.ConductorDutyViewModel(
                hrApp().container.scheduleRepository,
                hrApp().container.authRepository,
                hrApp().container.tokenManager
            )
        }
    }
}

fun CreationExtras.hrApp(): HRApp =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as HRApp)
