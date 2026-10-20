package com.example.haryanaroadways.data.repository

import com.example.haryanaroadways.data.api.*
import com.example.haryanaroadways.data.local.TokenManager
import kotlinx.coroutines.flow.Flow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

suspend fun <T> safeApiCall(apiCall: suspend () -> ApiResponse<T>): ApiResponse<T> {
    return try {
        apiCall()
    } catch (e: retrofit2.HttpException) {
        val errorBody = e.response()?.errorBody()?.string()
        val parsed = try {
            if (!errorBody.isNullOrBlank()) {
                kotlinx.serialization.json.Json { ignoreUnknownKeys = true }.decodeFromString<ApiResponse<T>>(errorBody)
            } else null
        } catch (_: Exception) { null }

        parsed ?: ApiResponse(
            success = false,
            message = "Server error (${e.code()}): ${e.message()}"
        )
    } catch (e: java.io.IOException) {
        ApiResponse(
            success = false,
            message = "Network error: Please check your internet connection"
        )
    } catch (e: Exception) {
        ApiResponse(
            success = false,
            message = e.message ?: "An unexpected error occurred"
        )
    }
}

interface AppContainer {
    val authRepository: AuthRepository
    val scheduleRepository: ScheduleRepository
    val bookingRepository: BookingRepository
    val adminRepository: AdminRepository
    val trackingRepository: TrackingRepository
    val tokenManager: TokenManager
}

class DefaultAppContainer(
    val apiService: ApiService,
    override val tokenManager: TokenManager
) : AppContainer {
    override val authRepository: AuthRepository by lazy {
        AuthRepository(apiService, tokenManager)
    }
    override val scheduleRepository: ScheduleRepository by lazy {
        ScheduleRepository(apiService)
    }
    override val bookingRepository: BookingRepository by lazy {
        BookingRepository(apiService)
    }
    override val adminRepository: AdminRepository by lazy {
        AdminRepository(apiService)
    }
    override val trackingRepository: TrackingRepository by lazy {
        TrackingRepository(apiService)
    }
}

class AuthRepository(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) {
    val isLoggedInFlow: Flow<Boolean> = tokenManager.isLoggedInFlow
    val userRoleFlow: Flow<String?> = tokenManager.userRoleFlow
    val userNameFlow: Flow<String?> = tokenManager.userNameFlow
    val userMobileFlow: Flow<String?> = tokenManager.userMobileFlow

    suspend fun login(mobile: String, password: String): ApiResponse<AuthData> = safeApiCall {
        val response = apiService.login(LoginRequest(mobile, password))
        if (response.success && response.data != null) {
            val data = response.data
            tokenManager.saveAuth(
                token = data.token,
                id = data.user.id,
                name = data.user.name,
                mobile = data.user.mobile,
                role = data.user.role
            )
        }
        response
    }
    
    suspend fun register(mobile: String, name: String, password: String): ApiResponse<AuthData> = safeApiCall {
        val response = apiService.register(RegisterRequest(mobile, name, password))
        if (response.success && response.data != null) {
            val data = response.data
            tokenManager.saveAuth(
                token = data.token,
                id = data.user.id,
                name = data.user.name,
                mobile = data.user.mobile,
                role = data.user.role
            )
        }
        response
    }
    
    suspend fun getProfile() = safeApiCall { apiService.getProfile() }
    
    suspend fun updateProfile(name: String) = safeApiCall { apiService.updateProfile(UpdateProfileRequest(name)) }
    
    suspend fun logout() {
        tokenManager.clearAuth()
        ApiClient.reset()
    }
}

class ScheduleRepository(private val apiService: ApiService) {
    suspend fun searchSchedules(source: String, destination: String, date: String) = safeApiCall {
        apiService.searchSchedules(source, destination, date)
    }

    suspend fun getSchedules() = safeApiCall { apiService.getSchedules() }
        
    suspend fun getPopularRoutes() = safeApiCall { apiService.getRoutes() }
    suspend fun getRoutes() = safeApiCall { apiService.getRoutes() }

    suspend fun getConductorDuty() = safeApiCall { apiService.getConductorDuty() }
}

class BookingRepository(private val apiService: ApiService) {
    suspend fun getSeatAvailability(scheduleId: String, date: String) = safeApiCall {
        apiService.getSeatAvailability(scheduleId, date)
    }
        
    suspend fun createBooking(request: CreateBookingRequest) = safeApiCall {
        apiService.createBooking(request)
    }
        
    suspend fun getBooking(id: String) = safeApiCall { apiService.getBooking(id) }
    
    suspend fun getActiveQR() = safeApiCall { apiService.getActiveQR() }
    
    suspend fun submitPayment(bookingId: String, screenshot: MultipartBody.Part? = null) = safeApiCall {
        if (screenshot != null) {
            apiService.submitPaymentWithScreenshot(bookingId, screenshot)
        } else {
            apiService.submitPayment(bookingId)
        }
    }
        
    suspend fun getMyBookings() = safeApiCall { apiService.getMyBookings() }
    
    suspend fun getTicketByPNR(pnr: String) = safeApiCall { apiService.getBookingByPNR(pnr) }
}

class AdminRepository(private val apiService: ApiService) {
    suspend fun getDashboardStats() = safeApiCall { apiService.getDashboardStats() }
    suspend fun getPendingBookings() = safeApiCall { apiService.getPendingBookings() }
    suspend fun getAllBookings(page: Int = 1, limit: Int = 100) = safeApiCall { apiService.getAllBookings(page, limit) }
    suspend fun confirmBooking(bookingId: String) = safeApiCall { apiService.confirmBooking(bookingId) }
    suspend fun rejectBooking(bookingId: String, reason: String = "Payment could not be verified") = safeApiCall {
        apiService.rejectBooking(bookingId, RejectBookingRequest(reason))
    }
    suspend fun createConductor(mobile: String, name: String, password: String) = safeApiCall {
        apiService.createConductor(CreateConductorRequest(mobile, name, password))
    }
    suspend fun getConductors() = safeApiCall { apiService.getConductors() }
    suspend fun deleteConductor(id: String) = safeApiCall { apiService.deleteConductor(id) }
    
    suspend fun getBuses() = safeApiCall { apiService.getBuses() }
    suspend fun uploadBusImage(image: MultipartBody.Part) = safeApiCall { apiService.uploadBusImage(image) }
    suspend fun createBus(request: CreateBusRequest) = safeApiCall { apiService.createBus(request) }
    suspend fun updateBus(id: String, request: UpdateBusRequest) = safeApiCall { apiService.updateBus(id, request) }
    suspend fun deleteBus(id: String) = safeApiCall { apiService.deleteBus(id) }
    
    suspend fun createRoute(request: CreateRouteRequest) = safeApiCall { apiService.createRoute(request) }
    suspend fun updateRoute(id: String, request: UpdateRouteRequest) = safeApiCall { apiService.updateRoute(id, request) }
    suspend fun deleteRoute(id: String) = safeApiCall { apiService.deleteRoute(id) }
    
    suspend fun getSchedules() = safeApiCall { apiService.getSchedules() }
    suspend fun createSchedule(request: CreateScheduleRequest) = safeApiCall { apiService.createSchedule(request) }
    suspend fun updateSchedule(id: String, request: UpdateScheduleRequest) = safeApiCall { apiService.updateSchedule(id, request) }
    suspend fun deleteSchedule(id: String) = safeApiCall { apiService.deleteSchedule(id) }

    suspend fun getQRCodes() = safeApiCall { apiService.getQRCodes() }
    suspend fun createQRCode(upiId: String, upiLabel: String, qrImage: MultipartBody.Part) = safeApiCall {
        val idBody = upiId.toRequestBody("text/plain".toMediaTypeOrNull())
        val labelBody = upiLabel.toRequestBody("text/plain".toMediaTypeOrNull())
        apiService.createQRCode(idBody, labelBody, qrImage)
    }
    suspend fun setActiveQRCode(id: String) = safeApiCall { apiService.setActiveQRCode(id) }
    suspend fun deleteQRCode(id: String) = safeApiCall { apiService.deleteQRCode(id) }
}

class TrackingRepository(private val apiService: ApiService) {
    suspend fun getAllActiveTrackings() = safeApiCall { apiService.getAllActiveTrackings() }
    suspend fun getTrackingForSchedule(scheduleId: String) = safeApiCall { apiService.getTrackingForSchedule(scheduleId) }
    suspend fun updateLocation(request: LocationUpdateRequest) = safeApiCall { apiService.updateLocation(request) }
    suspend fun stopLocation(scheduleId: String) = safeApiCall { apiService.stopLocation(LocationStopRequest(scheduleId)) }
}
