package com.example.haryanaroadways.data.api

import retrofit2.http.*
import okhttp3.MultipartBody

interface ApiService {

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): ApiResponse<AuthData>

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): ApiResponse<AuthData>

    @GET("auth/profile")
    suspend fun getProfile(): ApiResponse<UserData>

    @PUT("auth/profile")
    suspend fun updateProfile(@Body body: UpdateProfileRequest): ApiResponse<UserData>

    // ── Schedules / Search ───────────────────────────────────────────
    @GET("schedules/search")
    suspend fun searchSchedules(
        @Query("source") source: String,
        @Query("destination") destination: String,
        @Query("date") date: String
    ): ApiResponse<List<Schedule>>

    @GET("schedules/{id}")
    suspend fun getSchedule(@Path("id") id: String): ApiResponse<Schedule>

    // ── Routes ───────────────────────────────────────────────────────
    @GET("routes/search")
    suspend fun searchRoutes(
        @Query("source") source: String,
        @Query("destination") destination: String
    ): ApiResponse<List<Route>>

    @GET("routes")
    suspend fun getRoutes(): ApiResponse<List<Route>>

    // ── Bookings ─────────────────────────────────────────────────────
    @GET("bookings/seats")
    suspend fun getSeatAvailability(
        @Query("scheduleId") scheduleId: String,
        @Query("date") date: String
    ): ApiResponse<SeatAvailabilityData>

    @POST("bookings")
    suspend fun createBooking(@Body body: CreateBookingRequest): ApiResponse<BookingCreatedData>

    @POST("bookings/{bookingId}/payment")
    suspend fun submitPayment(
        @Path("bookingId") bookingId: String
    ): ApiResponse<PaymentSubmittedData>

    @Multipart
    @POST("bookings/{bookingId}/payment")
    suspend fun submitPaymentWithScreenshot(
        @Path("bookingId") bookingId: String,
        @Part screenshot: MultipartBody.Part
    ): ApiResponse<PaymentSubmittedData>

    @GET("bookings/my-bookings")
    suspend fun getMyBookings(): ApiResponse<List<Booking>>

    @GET("bookings/{id}")
    suspend fun getBooking(@Path("id") id: String): ApiResponse<Booking>

    @GET("bookings/pnr/{pnr}")
    suspend fun getBookingByPNR(@Path("pnr") pnr: String): ApiResponse<TicketData>

    // ── QR Code ──────────────────────────────────────────────────────
    @GET("qr/active")
    suspend fun getActiveQR(): ApiResponse<QRCode>

    @GET("qr")
    suspend fun getQRCodes(): ApiResponse<List<QRCode>>

    @Multipart
    @POST("qr")
    suspend fun createQRCode(
        @Part("upiId") upiId: okhttp3.RequestBody,
        @Part("upiLabel") upiLabel: okhttp3.RequestBody,
        @Part qrImage: MultipartBody.Part
    ): ApiResponse<QRCode>

    @PUT("qr/{id}/activate")
    suspend fun setActiveQRCode(@Path("id") id: String): ApiResponse<QRCode>

    @DELETE("qr/{id}")
    suspend fun deleteQRCode(@Path("id") id: String): ApiResponse<Unit>

    // ── Admin ────────────────────────────────────────────────────────
    @GET("bookings/admin/pending")
    suspend fun getPendingBookings(): ApiResponse<List<Booking>>

    @GET("bookings/admin/all")
    suspend fun getAllBookings(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 100
    ): ApiResponse<AllBookingsResponseData>

    @GET("bookings/admin/dashboard")
    suspend fun getDashboardStats(): ApiResponse<DashboardStatsData>

    @PUT("bookings/admin/{bookingId}/confirm")
    suspend fun confirmBooking(@Path("bookingId") bookingId: String): ApiResponse<BookingActionData>

    @PUT("bookings/admin/{bookingId}/reject")
    suspend fun rejectBooking(
        @Path("bookingId") bookingId: String,
        @Body body: RejectBookingRequest = RejectBookingRequest("Payment could not be verified")
    ): ApiResponse<BookingActionData>

    @POST("auth/admin/conductors")
    suspend fun createConductor(@Body body: CreateConductorRequest): ApiResponse<UserData>

    @GET("auth/admin/conductors")
    suspend fun getConductors(): ApiResponse<List<UserData>>

    @DELETE("auth/admin/conductors/{id}")
    suspend fun deleteConductor(@Path("id") id: String): ApiResponse<Unit>

    // ── Tracking ─────────────────────────────────────────────────────
    @GET("tracking/active/all")
    suspend fun getAllActiveTrackings(): ApiResponse<List<ActiveTrackingData>>

    @GET("tracking/{scheduleId}")
    suspend fun getTrackingForSchedule(@Path("scheduleId") scheduleId: String): ApiResponse<ActiveTrackingData>

    @POST("tracking/update")
    suspend fun updateLocation(@Body body: LocationUpdateRequest): ApiResponse<Unit>

    @POST("tracking/stop")
    suspend fun stopLocation(@Body body: LocationStopRequest): ApiResponse<Unit>

    // ── Conductor Duty ────────────────────────────────────────────────
    @GET("schedules/conductor/my-duty")
    suspend fun getConductorDuty(): ApiResponse<ConductorDutyData>

    // ── Bus CRUD ──────────────────────────────────────────────────────
    @GET("buses")
    suspend fun getBuses(): ApiResponse<List<Bus>>

    @Multipart
    @POST("buses/upload")
    suspend fun uploadBusImage(@Part image: MultipartBody.Part): ApiResponse<ImageUploadData>

    @POST("buses")
    suspend fun createBus(@Body body: CreateBusRequest): ApiResponse<Bus>

    @PUT("buses/{id}")
    suspend fun updateBus(@Path("id") id: String, @Body body: UpdateBusRequest): ApiResponse<Bus>

    @DELETE("buses/{id}")
    suspend fun deleteBus(@Path("id") id: String): ApiResponse<Unit>

    // ── Route CRUD ────────────────────────────────────────────────────
    @POST("routes")
    suspend fun createRoute(@Body body: CreateRouteRequest): ApiResponse<Route>

    @PUT("routes/{id}")
    suspend fun updateRoute(@Path("id") id: String, @Body body: UpdateRouteRequest): ApiResponse<Route>

    @DELETE("routes/{id}")
    suspend fun deleteRoute(@Path("id") id: String): ApiResponse<Unit>

    // ── Schedule CRUD ──────────────────────────────────────────────────
    @GET("schedules")
    suspend fun getSchedules(): ApiResponse<List<Schedule>>

    @POST("schedules")
    suspend fun createSchedule(@Body body: CreateScheduleRequest): ApiResponse<Schedule>

    @PUT("schedules/{id}")
    suspend fun updateSchedule(@Path("id") id: String, @Body body: UpdateScheduleRequest): ApiResponse<Schedule>

    @DELETE("schedules/{id}")
    suspend fun deleteSchedule(@Path("id") id: String): ApiResponse<Unit>
}
