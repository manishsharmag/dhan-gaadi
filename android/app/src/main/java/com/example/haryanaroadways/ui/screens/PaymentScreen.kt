package com.example.haryanaroadways.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.haryanaroadways.BuildConfig
import com.example.haryanaroadways.data.api.Booking
import com.example.haryanaroadways.data.api.QRCode
import com.example.haryanaroadways.data.repository.BookingRepository
import com.example.haryanaroadways.theme.*
import com.example.haryanaroadways.ui.AppViewModelProvider
import com.example.haryanaroadways.ui.components.GradientHeader
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

private fun backendAssetUrl(path: String): String {
    if (path.startsWith("http", ignoreCase = true)) return path
    val apiBase = BuildConfig.API_BASE_URL.trimEnd('/')
    val backendBase = apiBase.removeSuffix("/api")
    return "$backendBase/${path.trimStart('/')}"
}

// --- ViewModel ---
data class PaymentUiState(
    val isLoading: Boolean = false,
    val booking: Booking? = null,
    val qrCode: QRCode? = null,
    val error: String? = null,
    val paymentSubmitted: Boolean = false,
    val confirmedPnr: String = ""
)

class PaymentViewModel(private val repository: BookingRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(PaymentUiState())
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    fun loadData(bookingId: String) {
        fetchActiveQR()
        fetchBooking(bookingId)
    }

    private fun fetchActiveQR() {
        viewModelScope.launch {
            try {
                val result = repository.getActiveQR()
                if (result.success && result.data != null) {
                    _uiState.value = _uiState.value.copy(qrCode = result.data)
                }
            } catch (_: Exception) {}
        }
    }

    private fun fetchBooking(bookingId: String) {
        viewModelScope.launch {
            try {
                val result = repository.getBooking(bookingId)
                if (result.success && result.data != null) {
                    _uiState.value = _uiState.value.copy(booking = result.data)
                }
            } catch (_: Exception) {}
        }
    }

    fun submitPayment(bookingId: String, screenshot: MultipartBody.Part?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val result = repository.submitPayment(bookingId, screenshot)
                if (result.success) {
                    val pnr = _uiState.value.booking?.pnr ?: ""
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        paymentSubmitted = true,
                        confirmedPnr = pnr
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message.ifBlank { "Payment submission failed" }
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "An error occurred during submission"
                )
            }
        }
    }
}

fun uriToMultipart(context: Context, uri: Uri?): MultipartBody.Part? {
    if (uri == null) return null
    return try {
        val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
        val extension = when {
            mimeType.contains("png", ignoreCase = true) -> ".png"
            mimeType.contains("webp", ignoreCase = true) -> ".webp"
            else -> ".jpg"
        }
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val tempFile = File(context.cacheDir, "screenshot_${System.currentTimeMillis()}$extension")
        tempFile.outputStream().use { output ->
            inputStream.copyTo(output)
        }
        val mediaType = mimeType.toMediaTypeOrNull() ?: "image/jpeg".toMediaTypeOrNull()
        val requestFile = tempFile.asRequestBody(mediaType)
        MultipartBody.Part.createFormData("screenshot", tempFile.name, requestFile)
    } catch (e: Exception) {
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    bookingId: String,
    onPaymentSuccess: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: PaymentViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(bookingId) {
        viewModel.loadData(bookingId)
    }

    var timeLeft by remember { mutableIntStateOf(600) } // 10 minutes
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var showSourceDialog by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            selectedImageUri = tempCameraUri
        }
    }

    // Sync timer with booking's seatLockExpiry if available
    LaunchedEffect(uiState.booking?.seatLockExpiry) {
        val expiry = uiState.booking?.seatLockExpiry
        if (!expiry.isNullOrBlank()) {
            val expiryMs = try {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.parse(expiry)?.time ?: 0L
            } catch (_: Exception) { 0L }
            val diffSec = ((expiryMs - System.currentTimeMillis()) / 1000).toInt()
            if (diffSec in 1..1800) {
                timeLeft = diffSec
            }
        }
    }

    // Timer Effect
    LaunchedEffect(Unit) {
        while (timeLeft > 0) {
            delay(1000)
            timeLeft--
        }
    }

    // Payment submission success handler
    LaunchedEffect(uiState.paymentSubmitted) {
        if (uiState.paymentSubmitted) {
            val pnr = uiState.confirmedPnr.ifBlank { uiState.booking?.pnr ?: "" }
            onPaymentSuccess(pnr)
        }
    }

    if (showSourceDialog) {
        AlertDialog(
            onDismissRequest = { showSourceDialog = false },
            title = { Text("Attach Payment Screenshot", fontWeight = FontWeight.Bold) },
            text = { Text("Choose a source to attach payment proof:") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSourceDialog = false
                        galleryLauncher.launch("image/*")
                    }
                ) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null, tint = PrimaryTeal)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Gallery", color = PrimaryTeal, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSourceDialog = false
                        val file = File(context.cacheDir, "camera_shot_${System.currentTimeMillis()}.jpg")
                        val uri = androidx.core.content.FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                        tempCameraUri = uri
                        cameraLauncher.launch(uri)
                    }
                ) {
                    Icon(Icons.Filled.PhotoCamera, contentDescription = null, tint = PrimaryTeal)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Camera", color = PrimaryTeal)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Complete UPI Payment", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryTealDark
                )
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = 16.dp,
                color = Color.White,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 16.dp)
                ) {
                    Button(
                        onClick = { 
                            val part = uriToMultipart(context, selectedImageUri)
                            viewModel.submitPayment(bookingId, part) 
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                        enabled = !uiState.isLoading && timeLeft > 0
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("I Have Completed Payment ➔", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .background(BgLight)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Countdown Timer Pill
            val minutes = timeLeft / 60
            val seconds = timeLeft % 60
            val isUrgent = timeLeft < 60
            val timerBg = if (isUrgent) ErrorRedLight else PrimaryTealUltraLight
            val timerColor = if (isUrgent) ErrorRed else PrimaryTealDark
            
            Surface(
                color = timerBg,
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isUrgent) ErrorRed.copy(alpha = 0.3f) else PrimaryTealLight.copy(alpha = 0.4f)),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.AccessTime, contentDescription = "Timer", tint = timerColor, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Seat Reserved: ",
                        color = timerColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = String.format(Locale.US, "%02d:%02d", minutes, seconds),
                        fontWeight = FontWeight.ExtraBold,
                        color = timerColor,
                        fontSize = 15.sp
                    )
                }
            }

            // Booking Summary Card
            if (uiState.booking != null) {
                val b = uiState.booking!!
                val routeSource = b.schedule?.route?.source ?: "Origin"
                val routeDest = b.schedule?.route?.destination ?: "Destination"
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$routeSource ➔ $routeDest",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = PrimaryTealDark
                            )
                            Text(
                                text = "₹${b.totalFare}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = PrimaryTeal
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Seats: ${b.seats.joinToString(", ")}",
                                fontSize = 13.sp,
                                color = TextSecondaryLight
                            )
                            if (b.pnr.isNotBlank()) {
                                Text(
                                    text = "PNR: ${b.pnr}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextMutedLight
                                )
                            }
                        }
                    }
                }
            }

            // QR Code Showcase Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Scan QR to Pay Instantly",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = PrimaryTealDark
                    )
                    Text(
                        text = "Accepts Google Pay, PhonePe, Paytm, BHIM & all banks",
                        fontSize = 12.sp,
                        color = TextSecondaryLight,
                        textAlign = TextAlign.Center
                    )
                    
                    val totalFare = uiState.booking?.totalFare ?: 0
                    val refCode = uiState.booking?.referenceCode?.ifBlank { bookingId.takeLast(6) } ?: bookingId.takeLast(6)
                    val note = Uri.encode("HR-$refCode")
                    val upiId = uiState.qrCode?.upiId?.ifBlank { "9991672565-2@ybl" } ?: "9991672565-2@ybl"
                    val upiLabel = uiState.qrCode?.upiLabel?.ifBlank { "Dhan-Gaadi Fleet" } ?: "Dhan-Gaadi Fleet"
                    val amountParam = if (totalFare > 0) "&am=$totalFare" else ""
                    val upiUriString = "upi://pay?pa=$upiId&pn=${Uri.encode(upiLabel)}$amountParam&cu=INR&tn=$note"

                    val dynamicQrBitmap = remember(upiUriString) {
                        try {
                            val barcodeEncoder = BarcodeEncoder()
                            barcodeEncoder.encodeBitmap(upiUriString, BarcodeFormat.QR_CODE, 450, 450)
                        } catch (_: Exception) {
                            null
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // QR Code Image Box (Dynamic Scannable QR)
                    Box(
                        modifier = Modifier
                            .size(210.dp)
                            .background(Color.White, RoundedCornerShape(18.dp))
                            .border(1.5.dp, PrimaryTealLight.copy(alpha = 0.3f), RoundedCornerShape(18.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (dynamicQrBitmap != null) {
                            Image(
                                bitmap = dynamicQrBitmap.asImageBitmap(),
                                contentDescription = "Scannable UPI QR Code",
                                modifier = Modifier
                                    .size(190.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            )
                        } else if (!uiState.qrCode?.imagePath.isNullOrBlank()) {
                            AsyncImage(
                                model = backendAssetUrl(uiState.qrCode!!.imagePath),
                                contentDescription = "UPI QR Code",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .size(190.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.QrCode2,
                                contentDescription = "QR Code Placeholder",
                                modifier = Modifier.size(130.dp),
                                tint = PrimaryTeal
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = upiLabel,
                        color = TextSecondaryLight,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )

                    // Copy UPI ID Chip
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = PrimaryTealUltraLight,
                        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryTealLight.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .clickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("UPI ID", upiId)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "UPI ID copied: $upiId", Toast.LENGTH_SHORT).show()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = upiId,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = PrimaryTealDark
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Filled.ContentCopy, contentDescription = "Copy", tint = PrimaryTeal, modifier = Modifier.size(14.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 1-Tap "Pay via UPI App" Button
                    OutlinedButton(
                        onClick = {
                            try {
                                val uri = Uri.parse(upiUriString)
                                val intent = Intent(Intent.ACTION_VIEW, uri)
                                context.startActivity(Intent.createChooser(intent, "Pay using UPI App"))
                            } catch (e: Exception) {
                                Toast.makeText(context, "No UPI app found on device", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryTeal),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, PrimaryTeal)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pay via GPay / PhonePe / Paytm", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Screenshot Upload Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showSourceDialog = true },
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
            ) {
                if (selectedImageUri != null) {
                    // Preview attached screenshot
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Screenshot Preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(58.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Payment Proof Attached", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SuccessGreen)
                            Text("Tap to change or replace screenshot", fontSize = 12.sp, color = TextMutedLight)
                        }
                        IconButton(onClick = { selectedImageUri = null }) {
                            Icon(Icons.Filled.Close, contentDescription = "Remove", tint = TextMutedLight)
                        }
                    }
                } else {
                    // Upload Prompt Box
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = PrimaryTealUltraLight,
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.PhotoLibrary, contentDescription = "Upload", tint = PrimaryTeal, modifier = Modifier.size(22.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Attach Payment Screenshot (Optional)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = PrimaryTealDark
                        )
                        Text(
                            text = "Speeds up instant verification by station staff",
                            fontSize = 12.sp,
                            color = TextSecondaryLight
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Trust / Security Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Shield, contentDescription = "Secure", tint = SuccessGreen, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("100% Verified Dhan-Gaadi Payment Portal", fontSize = 12.sp, color = TextSecondaryLight, fontWeight = FontWeight.Medium)
            }

            if (uiState.error != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
