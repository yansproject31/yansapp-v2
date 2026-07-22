package com.yansproject.app.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Forced Premium Luxury Dark Theme
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("yans_appearance_prefs", Context.MODE_PRIVATE) }

    var themeVariant by remember { mutableStateOf(prefs.getString("theme_variant", "YANSPROJECT.ID Classic") ?: "YANSPROJECT.ID Classic") }
    var accentColorName by remember { mutableStateOf(prefs.getString("accent_color", "Aged Gold") ?: "Aged Gold") }
    var canvasStyleName by remember { mutableStateOf(prefs.getString("canvas_style", "Shadow Black (#0A0A0A)") ?: "Shadow Black (#0A0A0A)") }

    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
            if (key == "theme_variant" || key == "accent_color" || key == "canvas_style") {
                themeVariant = p.getString("theme_variant", "YANSPROJECT.ID Classic") ?: "YANSPROJECT.ID Classic"
                accentColorName = p.getString("accent_color", "Aged Gold") ?: "Aged Gold"
                canvasStyleName = p.getString("canvas_style", "Shadow Black (#0A0A0A)") ?: "Shadow Black (#0A0A0A)"
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    val primaryAccent = when(accentColorName) {
        "Aged Gold" -> AgedGold
        "Soft Cyan" -> HighlightSoftCyan
        "Emerald Green" -> Color(0xFF2ECC71)
        "Imperial Amber" -> Color(0xFFFFB300)
        "Sapphire Blue" -> Color(0xFF3B82F6)
        "Rose Gold" -> Color(0xFFE5A186)
        else -> AgedGold
    }

    val canvasBackground = when(canvasStyleName) {
        "Pure Obsidian Black (#000000)" -> Color(0xFF000000)
        "Dark Slate Teal (#081F20)" -> Color(0xFF081F20)
        else -> Color(0xFF0A0A0A)
    }

    val surfaceBg = when(themeVariant) {
        "Royal Emerald Imperial" -> Color(0xFF0B2B26)
        "Midnight Sapphire Luxury" -> Color(0xFF0A192F)
        "Onyx Platinum Edition" -> Color(0xFF1E293B)
        "Ruby Imperial Velvet" -> Color(0xFF2B0B14)
        else -> DarkTealSurface
    }

    val dynamicColorScheme = darkColorScheme(
        primary = primaryAccent,
        onPrimary = Color.Black,
        primaryContainer = surfaceBg,
        onPrimaryContainer = primaryAccent,
        secondary = HighlightSoftCyan,
        onSecondary = Color.Black,
        tertiary = HighlightSoftCyan,
        background = canvasBackground,
        onBackground = YansTextPrimary,
        surface = surfaceBg,
        onSurface = YansTextPrimary,
        surfaceVariant = DarkTealSurfaceVariant,
        onSurfaceVariant = YansTextSecondary,
        error = YansError,
        onError = Color.White
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            var ctx = view.context
            while (ctx is ContextWrapper) {
                if (ctx is Activity) break
                ctx = ctx.baseContext
            }
            val activity = ctx as? Activity
            activity?.window?.let { window ->
                window.statusBarColor = canvasBackground.toArgb()
                window.navigationBarColor = Color(0xFF0F3D3E).toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = false
                insetsController.isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = dynamicColorScheme,
        typography = YansTypography,
        content = content
    )
}

/**
 * SharedPremiumCard - Reusable Premium Non-Blurred Card Component
 */
@Composable
fun SharedPremiumCard(
    modifier: Modifier = Modifier,
    padding: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    borderGlowColor: Color = AgedGold.copy(alpha = 0.2f),
    content: @Composable () -> Unit
) {
    val cardShape = RoundedCornerShape(20.dp) // Card corner radius constraint: 20dp
    
    val cardModifier = if (onClick != null) {
        modifier
            .shadow(
                elevation = 6.dp,
                shape = cardShape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.5f),
                spotColor = Color.Black.copy(alpha = 0.5f)
            )
            .clip(cardShape)
            .clickable(onClick = onClick)
    } else {
        modifier
            .shadow(
                elevation = 6.dp,
                shape = cardShape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.5f),
                spotColor = Color.Black.copy(alpha = 0.5f)
            )
            .clip(cardShape)
    }

    Card(
        modifier = cardModifier,
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    borderGlowColor,
                    Color.Transparent
                )
            )
        )
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            DarkTealSurface,
                            DarkTealBase
                        )
                    )
                )
                .padding(padding)
        ) {
            content()
        }
    }
}
