package com.voltwise.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.voltwise.ui.viewmodel.SettingsViewModel

val Ink = Color(0xFF0B1017)
val Panel = Color(0xFF141C27)
val PanelBorder = Color(0xFF26313E)
val Mint = Color(0xFFB6F5CE)
val Muted = Color(0xFF94A3B5)
val Lavender = Color(0xFFC6B9F5)
val Amber = Color(0xFFFFCE93)

@Composable
fun PageHeading(eyebrow: String, title: String, subtitle: String) {
    val text = localizedUiText()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text(eyebrow).uppercase(), color = Mint, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
        Text(text(title), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
        Text(text(subtitle), color = Muted, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun ReadingCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    val text = localizedUiText()
    Surface(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
        color = Panel, border = BorderStroke(1.dp, PanelBorder)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text(title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
fun StatusPill(text: String, color: Color = Mint) {
    val translate = localizedUiText()
    val infiniteTransition = rememberInfiniteTransition(label = "statusPillPulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pillAlpha"
    )

    Surface(color = color.copy(alpha = .10f * alphaAnim), shape = CircleShape) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(6.dp).background(color.copy(alpha = alphaAnim), CircleShape))
            Text(translate(text), color = color.copy(alpha = alphaAnim), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun TabPill(text: String, selected: Boolean, onClick: () -> Unit) {
    val translate = localizedUiText()
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) Mint.copy(alpha = 0.10f) else Color.Transparent,
        border = BorderStroke(1.dp, if (selected) Mint.copy(alpha = 0.75f) else PanelBorder),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = translate(text),
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (selected) Mint else Muted
        )
    }
}

@Composable
fun SensorTile(label: String, value: String, unit: String, note: String, icon: ImageVector,
               modifier: Modifier = Modifier, accent: Color = Mint, indicatorColor: Color? = null) {
    val text = localizedUiText()
    Surface(modifier, color = Panel, shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, PanelBorder)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp))
                if (indicatorColor != null) {
                    Surface(color = indicatorColor.copy(alpha = 0.2f), shape = CircleShape) {
                        Row(Modifier.padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(Modifier.size(6.dp).background(indicatorColor, CircleShape))
                            Text(text(note), color = indicatorColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Text(unit, color = Muted, fontSize = 11.sp)
                }
            }
            Text(value, fontSize = 28.sp, fontWeight = FontWeight.Medium, color = if(value == "—") Muted else Color(0xFFF0F4F8))
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(text(label), style = MaterialTheme.typography.labelLarge)
                if (indicatorColor == null) {
                    Text(text(note), color = Muted, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun EmptyPanel(title: String, detail: String, icon: ImageVector = Icons.Rounded.Bolt) {
    val text = localizedUiText()
    ReadingCard(title) {
        Icon(icon, null, tint = Mint, modifier = Modifier.size(32.dp))
        Text(text(detail), color = Muted, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun localizedUiText(): (String) -> String {
    val settings: SettingsViewModel = viewModel()
    val state by settings.uiState.collectAsState()
    return { value -> appText(state.languageCode, value) }
}

fun measured(value: Float?, unit: String): String = value?.takeIf { it.isFinite() }?.let { "%.2f %s".format(it,unit) } ?: "Not supported"
fun sensorNumber(value: Float?, decimals: Int = 1): String = value?.takeIf { it.isFinite() }?.let { ("%." + decimals + "f").format(it) } ?: "—"
