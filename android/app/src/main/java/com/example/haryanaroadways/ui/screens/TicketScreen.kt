package com.example.haryanaroadways.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.EventSeat
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.haryanaroadways.BuildConfig
import com.example.haryanaroadways.R
import com.example.haryanaroadways.data.api.TicketData
import com.example.haryanaroadways.data.api.formattedJourneyDate
import com.example.haryanaroadways.data.repository.BookingRepository
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.example.haryanaroadways.theme.*
import com.example.haryanaroadways.ui.AppViewModelProvider
import com.example.haryanaroadways.ui.components.StatusChip
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TicketUiState(
    val isLoading: Boolean = true,
    val ticket: TicketData? = null,
    val error: String? = null
)

class TicketViewModel(private val repository: BookingRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(TicketUiState())
    val uiState: StateFlow<TicketUiState> = _uiState.asStateFlow()

    fun fetchTicket(pnr: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val response = repository.getTicketByPNR(pnr)
                if (response.success && response.data != null) {
                    _uiState.value = TicketUiState(isLoading = false, ticket = response.data)
                } else {
                    _uiState.value = TicketUiState(isLoading = false, error = response.message.ifBlank { "Ticket not found" })
                }
            } catch (e: Exception) {
                _uiState.value = TicketUiState(isLoading = false, error = e.message ?: "Failed to fetch ticket")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketScreen(
    pnr: String,
    onBack: () -> Unit,
    onTrackBus: (String) -> Unit = {},
    viewModel: TicketViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(pnr) {
        viewModel.fetchTicket(pnr)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("e-Boarding Pass", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.fetchTicket(pnr) }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryTealDark
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BgLight)
        ) {
            when {
                state.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = PrimaryTeal)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Loading boarding pass...", color = TextSecondaryLight, fontSize = 14.sp)
                        }
                    }
                }
                state.error != null -> {
                    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = state.error ?: "Unknown error",
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.fetchTicket(pnr) },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }
                state.ticket != null -> {
                    val ticket = state.ticket!!
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                // Top Header Gradient Banner
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(BrandHeroGradient)
                                        .padding(22.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                color = Color.White.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(16.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Shield,
                                                        contentDescription = null,
                                                        tint = AccentSaffron,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "DHAN-GAADI FLEET OFFICIAL",
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 10.sp,
                                                        letterSpacing = 0.8.sp
                                                    )
                                                }
                                            }

                                            Surface(
                                                shape = CircleShape,
                                                color = Color.White,
                                                shadowElevation = 4.dp,
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Image(
                                                    painter = painterResource(id = R.drawable.app_logo_round),
                                                    contentDescription = "Dhan-Gaadi Fleet Logo",
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .clip(CircleShape)
                                                )
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.height(16.dp))
                                        
                                        // Route Source ➔ Destination
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val source = ticket.schedule?.route?.source ?: "SRC"
                                            val dest = ticket.schedule?.route?.destination ?: "DST"

                                            Column(horizontalAlignment = Alignment.Start) {
                                                Text(
                                                    text = source.take(3).uppercase(),
                                                    color = Color.White,
                                                    fontSize = 28.sp,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                                Text(
                                                    text = source,
                                                    color = Color.White.copy(alpha = 0.85f),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }

                                            Surface(
                                                shape = CircleShape,
                                                color = Color.White.copy(alpha = 0.15f),
                                                modifier = Modifier.size(42.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Filled.DirectionsBus,
                                                        contentDescription = "Bus",
                                                        tint = AccentSaffron,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }
                                            }

                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = dest.take(3).uppercase(),
                                                    color = Color.White,
                                                    fontSize = 28.sp,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                                Text(
                                                    text = dest,
                                                    color = Color.White.copy(alpha = 0.85f),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }

                                // Body Details
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Passenger Name", color = TextMutedLight, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                            Text(
                                                text = ticket.passengerName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                color = TextPrimaryLight
                                            )
                                        }
                                        StatusChip(status = ticket.status)
                                    }

                                    if (ticket.status.equals("PENDING_VERIFICATION", ignoreCase = true)) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color(0xFFFEF3C7),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("⏳", fontSize = 18.sp)
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        "Payment Verification In Progress",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF92400E)
                                                    )
                                                    Text(
                                                        "Your seats are locked. The conductor or admin will verify receipt shortly. Click the refresh button on top to check live status.",
                                                        fontSize = 11.sp,
                                                        color = Color(0xFFB45309),
                                                        lineHeight = 14.sp
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (ticket.status.equals("REJECTED", ignoreCase = true)) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color(0xFFFEF2F2),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("✕", fontSize = 20.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        "Booking Rejected",
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF991B1B)
                                                    )
                                                    val reason = ticket.rejectionReason?.takeIf { it.isNotBlank() }
                                                        ?: "Payment receipt or seat allocation could not be verified."
                                                    Text(
                                                        "Reason: $reason",
                                                        fontSize = 12.sp,
                                                        color = Color(0xFFB91C1C),
                                                        lineHeight = 15.sp
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        "If your account was debited, your payment will be reconciled. Please contact Dhan-Gaadi helpline.",
                                                        fontSize = 11.sp,
                                                        color = Color.DarkGray
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("Journey Date", color = TextMutedLight, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                            Text(
                                                text = ticket.formattedJourneyDate,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp,
                                                color = TextPrimaryLight
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("Assigned Seats", color = TextMutedLight, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                            Text(
                                                text = ticket.seats.sorted().joinToString(", "),
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 16.sp,
                                                color = PrimaryTealDark
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("Departure Time", color = TextMutedLight, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                            Text(
                                                text = ticket.schedule?.departureTime ?: "--",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp,
                                                color = TextPrimaryLight
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("Total Fare Paid", color = TextMutedLight, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                            Text(
                                                text = "₹${ticket.totalFare}",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 18.sp,
                                                color = PrimaryTeal
                                            )
                                        }
                                    }

                                    // Assigned Conductor Trust Card
                                    val conductor = ticket.schedule?.conductor
                                    if (conductor != null) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = Color(0xFFF8FAFC),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = PrimaryTeal.copy(alpha = 0.15f),
                                                        modifier = Modifier.size(46.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            if (!conductor.photo.isNullOrBlank()) {
                                                                AsyncImage(
                                                                    model = conductor.photo,
                                                                    contentDescription = conductor.name,
                                                                    contentScale = ContentScale.Crop,
                                                                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                                                                )
                                                            } else {
                                                                Icon(
                                                                    Icons.Filled.Person,
                                                                    contentDescription = null,
                                                                    tint = PrimaryTealDark,
                                                                    modifier = Modifier.size(26.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = conductor.name,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 14.sp,
                                                            color = TextPrimaryLight
                                                        )
                                                        Text(
                                                            text = "Assigned Bus Conductor • HR Roadways",
                                                            fontSize = 11.sp,
                                                            color = TextMutedLight
                                                        )
                                                        Text(
                                                            text = "📞 +91 ${conductor.mobile}",
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = PrimaryTealDark
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(10.dp))

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    OutlinedButton(
                                                        onClick = {
                                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${conductor.mobile}"))
                                                            context.startActivity(intent)
                                                        },
                                                        modifier = Modifier.weight(1f).height(38.dp),
                                                        shape = RoundedCornerShape(10.dp),
                                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryTealDark),
                                                        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryTeal)
                                                    ) {
                                                        Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text("Call Staff", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                    }

                                                    val cleanMobile = conductor.mobile.replace(Regex("\\D"), "").takeLast(10)
                                                    Button(
                                                        onClick = {
                                                            val waMsg = "Hello Conductor ${conductor.name}, I am a passenger on bus (${ticket.schedule?.bus?.busNumber ?: ""}) with PNR: ${ticket.pnr}."
                                                            val uri = Uri.parse("https://api.whatsapp.com/send?phone=91$cleanMobile&text=${Uri.encode(waMsg)}")
                                                            val waIntent = Intent(Intent.ACTION_VIEW, uri)
                                                            context.startActivity(waIntent)
                                                        },
                                                        modifier = Modifier.weight(1f).height(38.dp),
                                                        shape = RoundedCornerShape(10.dp),
                                                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                                                    ) {
                                                        Icon(Icons.Filled.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text("WhatsApp", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Scalloped Notch Divider with Dashed Line
                                val notchBgColor = BgLight
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(28.dp)
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val radius = 14.dp.toPx()
                                        drawCircle(color = notchBgColor, radius = radius, center = Offset(0f, size.height / 2))
                                        drawCircle(color = notchBgColor, radius = radius, center = Offset(size.width, size.height / 2))
                                        drawLine(
                                            color = Color(0xFFCBD5E1),
                                            start = Offset(radius + 12f, size.height / 2),
                                            end = Offset(size.width - radius - 12f, size.height / 2),
                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f),
                                            strokeWidth = 2f
                                        )
                                    }
                                }

                                // PNR & Scannable QR Section
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("BOOKING PNR (CLICK TO COPY)", color = TextMutedLight, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                    
                                    Spacer(modifier = Modifier.height(6.dp))

                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = PrimaryTealUltraLight,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryTealLight.copy(alpha = 0.4f)),
                                        modifier = Modifier.clickable {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("PNR", ticket.pnr)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, "PNR copied: ${ticket.pnr}", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = ticket.pnr,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 22.sp,
                                                letterSpacing = 2.sp,
                                                color = PrimaryTealDark
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Icon(Icons.Filled.ContentCopy, contentDescription = "Copy PNR", tint = PrimaryTeal, modifier = Modifier.size(16.dp))
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Dynamic Local Offline Ticket QR Code
                                    val qrBitmap = remember(ticket.pnr) {
                                        try {
                                            val barcodeEncoder = BarcodeEncoder()
                                            barcodeEncoder.encodeBitmap(ticket.pnr, BarcodeFormat.QR_CODE, 400, 400)
                                        } catch (_: Exception) {
                                            null
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(18.dp),
                                        color = Color(0xFFF8FAFC),
                                        border = androidx.compose.foundation.BorderStroke(1.5.dp, BorderSubtle),
                                        modifier = Modifier.size(160.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(10.dp)) {
                                            if (qrBitmap != null) {
                                                Image(
                                                    bitmap = qrBitmap.asImageBitmap(),
                                                    contentDescription = "Offline Verified QR Code",
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Filled.ConfirmationNumber,
                                                    contentDescription = "Ticket PNR",
                                                    modifier = Modifier.size(80.dp),
                                                    tint = PrimaryTeal
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Scan QR with Conductor POS Terminal (Offline Verified)",
                                        fontSize = 11.sp,
                                        color = TextMutedLight,
                                        fontWeight = FontWeight.Medium
                                    )

                                    Spacer(modifier = Modifier.height(18.dp))

                                    // WhatsApp & Direct Share Button
                                    Button(
                                        onClick = {
                                            val webBaseUrl = BuildConfig.API_BASE_URL.trimEnd('/').removeSuffix("/api").trimEnd('/')
                                            val ticketUrl = if (webBaseUrl.isNotBlank()) "$webBaseUrl/ticket/${ticket.pnr}" else "https://haryanabusinfo.in/ticket/${ticket.pnr}"
                                            val shareMessage = """
                                                🚌 *DHAN-GAADI FLEET e-TICKET*
                                                ━━━━━━━━━━━━━━━━━━━━
                                                🎫 *PNR:* ${ticket.pnr}
                                                👤 *Passenger:* ${ticket.passengerName}
                                                📍 *Route:* ${ticket.schedule?.route?.source ?: "Origin"} ➔ ${ticket.schedule?.route?.destination ?: "Destination"}
                                                📅 *Date:* ${ticket.formattedJourneyDate} (${ticket.schedule?.departureTime ?: ""})
                                                💺 *Seats:* ${ticket.seats.sorted().joinToString(", ")}
                                                💵 *Total Fare:* Rs. ${ticket.totalFare}
                                                📲 *View Official Ticket:*
                                                $ticketUrl
                                                ━━━━━━━━━━━━━━━━━━━━
                                                _Valid for travel on Dhan-Gaadi Fleet._
                                            """.trimIndent()

                                            try {
                                                val uri = Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(shareMessage)}")
                                                val waIntent = Intent(Intent.ACTION_VIEW, uri)
                                                context.startActivity(waIntent)
                                            } catch (e: Exception) {
                                                val shareIntent = Intent().apply {
                                                    action = Intent.ACTION_SEND
                                                    putExtra(Intent.EXTRA_TEXT, shareMessage)
                                                    type = "text/plain"
                                                }
                                                context.startActivity(Intent.createChooser(shareIntent, "Share e-Ticket"))
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().height(50.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                                    ) {
                                        Icon(Icons.Filled.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Share Ticket on WhatsApp", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    }

                                    val schedId = ticket.scheduleId
                                    if (!schedId.isNullOrEmpty() && ticket.status != "CANCELLED") {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        OutlinedButton(
                                            onClick = { onTrackBus(schedId) },
                                            modifier = Modifier.fillMaxWidth().height(50.dp),
                                            shape = RoundedCornerShape(14.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryTeal),
                                            border = androidx.compose.foundation.BorderStroke(1.5.dp, PrimaryTeal)
                                        ) {
                                            Icon(Icons.Filled.DirectionsBus, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Track Live Bus GPS", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}
