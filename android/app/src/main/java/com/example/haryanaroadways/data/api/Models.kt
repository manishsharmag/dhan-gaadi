package com.example.haryanaroadways.data.api

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.util.Locale

// ── Generic API response wrapper ─────────────────────────────────────
@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String = ""
)

@Serializable
data class ImageUploadData(val url: String)

// ── Auth ─────────────────────────────────────────────────────────────
@Serializable
data class LoginRequest(val mobile: String, val password: String)

@Serializable
data class RegisterRequest(val mobile: String, val name: String, val password: String)

@Serializable
data class CreateConductorRequest(val mobile: String, val name: String, val password: String)

@Serializable
data class AuthData(
    val token: String,
    val user: UserData
)

@Serializable
data class UserData(
    val id: String = "",
    val _id: String = "",
    val mobile: String = "",
    val name: String = "",
    val email: String? = null,
    val photo: String? = null,
    val role: String = "user"
) {
    val effectiveId: String get() = id.ifBlank { _id }
}

// ── Flexible Deserializers for Unpopulated ObjectId vs Populated Object ─
object FlexibleUserDataSerializer : KSerializer<UserData?> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("FlexibleUserData")

    override fun serialize(encoder: Encoder, value: UserData?) {
        if (value == null) {
            encoder.encodeNull()
        } else {
            encoder.encodeSerializableValue(UserData.serializer(), value)
        }
    }

    override fun deserialize(decoder: Decoder): UserData? {
        val jsonDecoder = decoder as? JsonDecoder ?: return null
        val element = jsonDecoder.decodeJsonElement()
        return when (element) {
            is JsonObject -> {
                try {
                    jsonDecoder.json.decodeFromJsonElement(UserData.serializer(), element)
                } catch (_: Exception) {
                    null
                }
            }
            is JsonPrimitive -> {
                val idStr = element.content
                UserData(id = idStr, _id = idStr, mobile = "")
            }
            else -> null
        }
    }
}

object FlexibleRouteSerializer : KSerializer<Route?> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("FlexibleRoute")

    override fun serialize(encoder: Encoder, value: Route?) {
        if (value == null) {
            encoder.encodeNull()
        } else {
            encoder.encodeSerializableValue(Route.serializer(), value)
        }
    }

    override fun deserialize(decoder: Decoder): Route? {
        val jsonDecoder = decoder as? JsonDecoder ?: return null
        val element = jsonDecoder.decodeJsonElement()
        return when (element) {
            is JsonObject -> {
                try {
                    jsonDecoder.json.decodeFromJsonElement(Route.serializer(), element)
                } catch (_: Exception) {
                    null
                }
            }
            is JsonPrimitive -> {
                Route(_id = element.content, routeNumber = "", name = "", source = "", destination = "", sourceDepot = "")
            }
            else -> null
        }
    }
}

object FlexibleBusSerializer : KSerializer<Bus?> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("FlexibleBus")

    override fun serialize(encoder: Encoder, value: Bus?) {
        if (value == null) {
            encoder.encodeNull()
        } else {
            encoder.encodeSerializableValue(Bus.serializer(), value)
        }
    }

    override fun deserialize(decoder: Decoder): Bus? {
        val jsonDecoder = decoder as? JsonDecoder ?: return null
        val element = jsonDecoder.decodeJsonElement()
        return when (element) {
            is JsonObject -> {
                try {
                    jsonDecoder.json.decodeFromJsonElement(Bus.serializer(), element)
                } catch (_: Exception) {
                    null
                }
            }
            is JsonPrimitive -> {
                Bus(_id = element.content, busNumber = "", depot = "")
            }
            else -> null
        }
    }
}

object FlexibleScheduleSerializer : KSerializer<Schedule?> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("FlexibleSchedule")

    override fun serialize(encoder: Encoder, value: Schedule?) {
        if (value == null) {
            encoder.encodeNull()
        } else {
            encoder.encodeSerializableValue(Schedule.serializer(), value)
        }
    }

    override fun deserialize(decoder: Decoder): Schedule? {
        val jsonDecoder = decoder as? JsonDecoder ?: return null
        val element = jsonDecoder.decodeJsonElement()
        return when (element) {
            is JsonObject -> {
                try {
                    jsonDecoder.json.decodeFromJsonElement(Schedule.serializer(), element)
                } catch (_: Exception) {
                    null
                }
            }
            is JsonPrimitive -> {
                Schedule(_id = element.content)
            }
            else -> null
        }
    }
}

@Serializable
data class UpdateProfileRequest(val name: String)

// ── Route ────────────────────────────────────────────────────────────
@Serializable
data class Stop(
    val name: String,
    val distanceFromSource: Int = 0,
    val order: Int,
    val lat: Double = 0.0,
    val lng: Double = 0.0
)

@Serializable
data class Route(
    val _id: String = "",
    val routeNumber: String = "",
    val name: String = "",
    val source: String = "",
    val destination: String = "",
    val sourceDepot: String = "",
    val destinationDepot: String = "",
    val stops: List<Stop> = emptyList(),
    val totalDistance: Int = 0,
    val baseFare: Int = 0,
    val images: List<String> = emptyList(),
    val status: String = "active"
)

// ── Bus ──────────────────────────────────────────────────────────────
@Serializable
data class Bus(
    val _id: String = "",
    val busNumber: String = "",
    val name: String = "",
    val type: String = "ordinary",
    val totalSeats: Int = 45,
    val depot: String = "",
    val status: String = "active",
    val amenities: List<String> = emptyList(),
    val images: List<String> = emptyList()
)

@Serializable
data class CreateBusRequest(
    val busNumber: String,
    val name: String = "",
    val type: String = "ordinary",
    val totalSeats: Int = 45,
    val depot: String,
    val amenities: List<String> = emptyList(),
    val images: List<String> = emptyList()
)

@Serializable
data class UpdateBusRequest(
    val name: String? = null,
    val type: String? = null,
    val totalSeats: Int? = null,
    val depot: String? = null,
    val status: String? = null,
    val amenities: List<String>? = null,
    val images: List<String>? = null
)

@Serializable
data class CreateRouteRequest(
    val routeNumber: String,
    val name: String,
    val source: String,
    val destination: String,
    val sourceDepot: String,
    val destinationDepot: String = "",
    val stops: List<Stop> = emptyList(),
    val totalDistance: Int,
    val baseFare: Int,
    val images: List<String> = emptyList()
)

@Serializable
data class UpdateRouteRequest(
    val name: String? = null,
    val source: String? = null,
    val destination: String? = null,
    val sourceDepot: String? = null,
    val destinationDepot: String? = null,
    val stops: List<Stop>? = null,
    val totalDistance: Int? = null,
    val baseFare: Int? = null,
    val images: List<String>? = null,
    val status: String? = null
)

// ── Schedule ─────────────────────────────────────────────────────────
@Serializable
data class Schedule(
    val _id: String = "",
    @Serializable(with = FlexibleRouteSerializer::class)
    val route: Route? = null,
    @Serializable(with = FlexibleBusSerializer::class)
    val bus: Bus? = null,
    @Serializable(with = FlexibleUserDataSerializer::class)
    val conductor: UserData? = null,
    val departureTime: String = "",
    val arrivalTime: String = "",
    val daysOfOperation: List<String> = emptyList(),
    val fare: Int? = null,
    val status: String = "active"
) {
    val effectiveFare: Int get() = fare ?: route?.baseFare ?: 0
}

// ── Booking ──────────────────────────────────────────────────────────
@Serializable
data class CreateBookingRequest(
    val scheduleId: String,
    val journeyDate: String,
    val seats: List<Int>,
    val passengerName: String,
    val passengerMobile: String,
    val passengerAge: Int? = null,
    val passengerGender: String? = null,
    val paymentMethod: String = "online"
)

@Serializable
data class BookingCreatedData(
    val booking: BookingBrief,
    val lockExpiresAt: String
)

@Serializable
data class BookingBrief(
    val id: String,
    val pnr: String,
    val referenceCode: String,
    val seats: List<Int>,
    val totalFare: Int,
    val status: String,
    val seatLockExpiry: String? = null
)

@Serializable
data class Booking(
    val _id: String = "",
    val pnr: String = "",
    val referenceCode: String = "",
    @Serializable(with = FlexibleUserDataSerializer::class)
    val user: UserData? = null,
    @Serializable(with = FlexibleScheduleSerializer::class)
    val schedule: Schedule? = null,
    val journeyDate: String = "",
    val seats: List<Int> = emptyList(),
    val passengerName: String = "",
    val passengerMobile: String = "",
    val passengerAge: Int? = null,
    val passengerGender: String? = null,
    val paymentMethod: String = "online",
    @Serializable(with = FlexibleUserDataSerializer::class)
    val bookedBy: UserData? = null,
    val totalFare: Int = 0,
    val status: String = "",
    val paymentScreenshot: String = "",
    val seatLockExpiry: String? = null,
    val confirmedAt: String? = null,
    val rejectedAt: String? = null,
    val rejectionReason: String? = null,
    val ticketUrl: String = "",
    val createdAt: String = ""
)

// ── Seat availability ────────────────────────────────────────────────
@Serializable
data class SeatAvailabilityData(
    val totalSeats: Int,
    val unavailableSeats: List<UnavailableSeat>
)

@Serializable
data class UnavailableSeat(
    val seatNumber: Int,
    val status: String,
    val expiresAt: String? = null
)

// ── QR Code ──────────────────────────────────────────────────────────
@Serializable
data class QRCode(
    val _id: String = "",
    val imagePath: String = "",
    val upiId: String = "",
    val upiLabel: String = "",
    val isActive: Boolean = false
)

// ── Payment submission ───────────────────────────────────────────────
@Serializable
data class PaymentSubmittedData(
    val bookingId: String,
    val referenceCode: String,
    val status: String
)

@Serializable
data class BookingActionData(
    val bookingId: String,
    val pnr: String = "",
    val status: String = "",
    val ticketUrl: String = ""
)

// ── Ticket (PNR lookup) ──────────────────────────────────────────────
@Serializable
data class TicketData(
    val pnr: String,
    val referenceCode: String,
    val passengerName: String,
    val passengerMobile: String = "",
    val seats: List<Int> = emptyList(),
    val totalFare: Int = 0,
    val status: String = "",
    val journeyDate: String = "",
    val rejectionReason: String? = null,
    val ticketUrl: String = "",
    @Serializable(with = FlexibleScheduleSerializer::class)
    val schedule: Schedule? = null
) {
    val scheduleId: String? get() = schedule?._id
}

// ── Date Formatting Utilities ────────────────────────────────────────
fun formatJourneyDate(dateStr: String): String {
    if (dateStr.isBlank()) return "--"
    return try {
        val cleaned = if (dateStr.contains("T")) dateStr.substringBefore("T") else dateStr
        val parts = cleaned.split("-")
        if (parts.size == 3) {
            val year = parts[0].toIntOrNull() ?: return dateStr
            val month = parts[1].toIntOrNull() ?: return dateStr
            val day = parts[2].toIntOrNull() ?: return dateStr
            val months = listOf("", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
            val monthName = if (month in 1..12) months[month] else parts[1]
            String.format(Locale.US, "%02d %s %d", day, monthName, year)
        } else {
            dateStr
        }
    } catch (_: Exception) {
        dateStr
    }
}

val Booking.formattedJourneyDate: String get() = formatJourneyDate(journeyDate)
val TicketData.formattedJourneyDate: String get() = formatJourneyDate(journeyDate)

// ── Admin ────────────────────────────────────────────────────────────
@Serializable
data class DashboardStatsData(
    val totalRevenue: Double = 0.0,
    val totalBookings: Int = 0,
    @SerialName("pendingVerification") val pendingApprovals: Int = 0,
    val confirmedBookings: Int = 0,
    val activeBuses: Int = 0,
    val activeRoutes: Int = 0,
    @SerialName("todayBookings") val todayBookingsCount: Int = 0,
    val todayRevenue: Double = 0.0,
    val uniqueCustomers: Int = 0,
    val onlinePayments: Int = 0,
    val cashPayments: Int = 0,
    val upiPayments: Int = 0
)

@Serializable
data class CreateScheduleRequest(
    val route: String,
    val bus: String,
    val departureTime: String,
    val arrivalTime: String,
    val daysOfOperation: List<String>,
    val fare: Int,
    val conductor: String? = null
)

@Serializable
data class UpdateScheduleRequest(
    val route: String? = null,
    val bus: String? = null,
    val departureTime: String? = null,
    val arrivalTime: String? = null,
    val daysOfOperation: List<String>? = null,
    val fare: Int? = null,
    val conductor: String? = null,
    val status: String? = null
)

@Serializable
data class AllBookingsResponseData(
    val bookings: List<Booking>,
    val pagination: PaginationData
)

@Serializable
data class PaginationData(
    val total: Int,
    val page: Int,
    val pages: Int
)

@Serializable
data class TrackingLocation(
    val lat: Double = 0.0,
    val lng: Double = 0.0
)

@Serializable
data class ActiveTrackingData(
    val _id: String = "",
    val schedule: Schedule? = null,
    val scheduleId: String = "",
    val busId: String = "",
    val routeId: String = "",
    val location: TrackingLocation? = null,
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val speed: Double = 0.0,
    val heading: Double = 0.0,
    val currentStop: String = "",
    val nextStop: String = "",
    val lastUpdated: String = "",
    val isActive: Boolean = true
) {
    val effectiveScheduleId: String get() = schedule?._id ?: scheduleId
    val effectiveLat: Double get() = location?.lat ?: if (lat != 0.0) lat else 0.0
    val effectiveLng: Double get() = location?.lng ?: if (lng != 0.0) lng else 0.0
}

// ── Conductor Duty & Telemetry Broadcast ─────────────────────────────
@Serializable
data class ConductorDutyData(
    @Serializable(with = FlexibleScheduleSerializer::class)
    val schedule: Schedule? = null,
    val passengers: List<PassengerManifestItem> = emptyList(),
    val activeTracking: ActiveTrackingData? = null,
    val message: String? = null
)

@Serializable
data class PassengerManifestItem(
    val pnr: String = "",
    val referenceCode: String = "",
    val passengerName: String = "",
    val passengerMobile: String = "",
    val seats: List<Int> = emptyList(),
    val totalFare: Int = 0,
    val status: String = "",
    val paymentMethod: String = "online",
    val confirmedAt: String? = null
)

@Serializable
data class LocationUpdateRequest(
    val scheduleId: String,
    val lat: Double,
    val lng: Double,
    val speed: Int = 0,
    val heading: Float = 0f,
    val accuracy: Float = 0f
)

@Serializable
data class LocationStopRequest(
    val scheduleId: String
)

@Serializable
data class RejectBookingRequest(
    val reason: String
)
