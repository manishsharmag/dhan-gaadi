package com.example.haryanaroadways.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.haryanaroadways.R
import com.example.haryanaroadways.theme.*
import com.example.haryanaroadways.ui.viewmodels.AuthMode
import com.example.haryanaroadways.ui.viewmodels.AuthUiState
import com.example.haryanaroadways.ui.viewmodels.AuthViewModel

@Composable
fun AuthBottomSheet(
    authViewModel: AuthViewModel,
    onDismiss: () -> Unit
) {
    val uiState by authViewModel.uiState.collectAsState()
    AuthBottomSheet(
        uiState = uiState,
        onMobileChange = { authViewModel.updateMobile(it) },
        onNameChange = { authViewModel.updateName(it) },
        onPasswordChange = { authViewModel.updatePassword(it) },
        onToggleMode = { authViewModel.toggleMode() },
        onSubmit = { authViewModel.submit() },
        onDismiss = onDismiss
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthBottomSheet(
    uiState: AuthUiState,
    onMobileChange: (String) -> Unit,
    onNameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onToggleMode: () -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = Color.White,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = Color.LightGray.copy(alpha = 0.6f)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp, top = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Official Logo Emblem
            Surface(
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 6.dp,
                border = androidx.compose.foundation.BorderStroke(2.dp, PrimaryTealLight.copy(alpha = 0.5f)),
                modifier = Modifier.size(64.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_logo_round),
                    contentDescription = "Dhan-Gaadi Official Logo",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            }

            // Government Brand Pill
            Surface(
                color = PrimaryTealUltraLight,
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryTealLight.copy(alpha = 0.4f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = null,
                        tint = AccentSaffronDark,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "DHAN-GAADI FLEET ACCOUNT",
                        color = PrimaryTealDark,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Text(
                if (uiState.mode == AuthMode.LOGIN) "Welcome Back" else "Create Rider Account",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = PrimaryTealDark
            )

            Text(
                text = if (uiState.mode == AuthMode.LOGIN) "Log in to view your tickets & manage bookings" else "Register once to access quick bookings and live tracking",
                fontSize = 13.sp,
                color = TextSecondaryLight,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            if (uiState.error != null) {
                Surface(
                    color = ErrorRedLight,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = uiState.error,
                        color = ErrorRed,
                        modifier = Modifier.padding(12.dp),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (uiState.isSuccess) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 12.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = SuccessGreenLight,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = "Success", tint = SuccessGreen, modifier = Modifier.size(36.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        "Authentication Successful!",
                        color = PrimaryTealDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                    ) {
                        Text("Continue ➔", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                if (uiState.mode == AuthMode.REGISTER) {
                    AppTextField(
                        value = uiState.name,
                        onValueChange = onNameChange,
                        label = "Full Name",
                        leadingIcon = { Icon(Icons.Filled.Person, contentDescription = "Name", tint = PrimaryTeal) }
                    )
                }

                AppTextField(
                    value = uiState.mobile,
                    onValueChange = { if (it.length <= 10 && it.all { c -> c.isDigit() }) onMobileChange(it) },
                    label = "10-Digit Mobile Number",
                    leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = "Phone", tint = PrimaryTeal) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextField(
                        value = uiState.password,
                        onValueChange = onPasswordChange,
                        placeholder = { Text("Password", color = TextMutedLight, fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = "Password", tint = PrimaryTeal, modifier = Modifier.size(20.dp)) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = onSubmit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    enabled = !uiState.isLoading,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryTeal,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text(
                            if (uiState.mode == AuthMode.LOGIN) "Sign In to Account" else "Create Free Account",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                TextButton(onClick = onToggleMode) {
                    Text(
                        if (uiState.mode == AuthMode.LOGIN) "Don't have an account? Sign Up" else "Already registered? Sign In",
                        fontSize = 13.sp,
                        color = PrimaryTealDark,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
