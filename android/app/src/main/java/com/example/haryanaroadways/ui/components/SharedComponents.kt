package com.example.haryanaroadways.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.haryanaroadways.R
import com.example.haryanaroadways.theme.*

/**
 * Ultra-Modern Gradient Header with decorative glow rings, transport crest, and official emblem.
 */
@Composable
fun GradientHeader(
    title: String? = null,
    subtitle: String? = null,
    badgeText: String? = "Govt. of Haryana • Transport Dept",
    modifier: Modifier = Modifier,
    topPadding: Dp = 0.dp,
    bottomPadding: Dp = 40.dp,
    showLogo: Boolean = true,
    content: (@Composable ColumnScope.() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(BrandHeroGradient)
            .padding(top = topPadding, bottom = bottomPadding)
            .padding(horizontal = 22.dp)
    ) {
        // Subtle background decorative radial glow
        Box(
            modifier = Modifier
                .size(160.dp)
                .align(Alignment.TopEnd)
                .offset(x = 40.dp, y = (-20).dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.04f))
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (badgeText != null) {
                        Surface(
                            color = Color.White.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(20.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
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
                                    text = badgeText.uppercase(),
                                    color = Color.White.copy(alpha = 0.95f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    if (title != null) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = (-0.5).sp
                        )
                    }

                    if (subtitle != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.85f),
                            lineHeight = 20.sp
                        )
                    }
                }

                if (showLogo) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 6.dp,
                        border = androidx.compose.foundation.BorderStroke(2.dp, Color.White.copy(alpha = 0.85f)),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.app_logo_round),
                            contentDescription = "Official Dhan-Gaadi Logo",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    }
                }
            }

            content?.invoke(this)
        }
    }
}

/**
 * Production-grade elevated AppTextField with custom placeholder, clear leading icon and focus border.
 */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    val actualLeadingIcon: (@Composable () -> Unit)? = leadingIcon ?: icon?.let {
        { Icon(it, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(20.dp)) }
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        shadowElevation = 1.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    label,
                    color = TextMutedLight,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            },
            leadingIcon = actualLeadingIcon,
            trailingIcon = trailingIcon,
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                focusedTextColor = TextPrimaryLight,
                unfocusedTextColor = TextPrimaryLight
            ),
            singleLine = true,
            keyboardOptions = keyboardOptions,
            readOnly = readOnly,
            enabled = enabled
        )
    }
}

/**
 * High-definition status badge chip with crisp borders and colors.
 */
@Composable
fun StatusChip(status: String) {
    val clean = status.trim().uppercase()
    val (bgColor, borderColor, textColor, iconEmoji) = when (clean) {
        "CONFIRMED" -> listOf(Color(0xFFECFDF5), Color(0xFFA7F3D0), Color(0xFF059669), "✓")
        "PENDING", "PENDING_VERIFICATION" -> listOf(Color(0xFFFFFBEB), Color(0xFFFDE68A), Color(0xFFD97706), "⏳")
        "REJECTED", "CANCELLED" -> listOf(Color(0xFFFEF2F2), Color(0xFFFECACA), Color(0xFFDC2626), "✕")
        "SEAT_LOCKED", "LOCKED" -> listOf(Color(0xFFEFF6FF), Color(0xFFBFDBFE), Color(0xFF2563EB), "🔒")
        else -> listOf(Color(0xFFF8FAFC), Color(0xFFE2E8F0), Color(0xFF64748B), "•")
    }

    Surface(
        color = bgColor as Color,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor as Color)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "${iconEmoji as String} ${clean.replace("_", " ")}",
                fontWeight = FontWeight.Bold,
                color = textColor as Color,
                fontSize = 11.sp,
                letterSpacing = 0.4.sp
            )
        }
    }
}

/**
 * Reusable Luxury Price Pill Tag.
 */
@Composable
fun PriceTag(
    amount: Number,
    modifier: Modifier = Modifier,
    subText: String? = null
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = PrimaryTeal.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryTeal.copy(alpha = 0.2f)),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = "₹${amount.toInt()}",
                color = PrimaryTealDark,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp
            )
            if (subText != null) {
                Text(
                    text = subText,
                    color = TextSecondaryLight,
                    fontSize = 10.sp
                )
            }
        }
    }
}
