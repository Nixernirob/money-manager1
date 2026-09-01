package com.example.ui.components

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role

enum class HapticStyle {
    LIGHT,       // Standard button / tab press (KEYBOARD_TAP / CLOCK_TICK)
    MEDIUM,      // Card clicks, switches, toggles (VIRTUAL_KEY)
    HEAVY,       // Major actions, Save, Delete (CONTEXT_CLICK)
    SUCCESS,     // Completed transaction, unlocked (CONFIRM)
    ERROR        // Invalid input, wrong PIN (REJECT)
}

class AppHapticFeedback(private val view: View) {
    fun perform(style: HapticStyle = HapticStyle.LIGHT) {
        try {
            when (style) {
                HapticStyle.LIGHT -> {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                }
                HapticStyle.MEDIUM -> {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                }
                HapticStyle.HEAVY -> {
                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                }
                HapticStyle.SUCCESS -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    } else {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    }
                }
                HapticStyle.ERROR -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        view.performHapticFeedback(HapticFeedbackConstants.REJECT)
                    } else {
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    }
                }
            }
        } catch (_: Exception) {
            // Graceful fallback
        }
    }
}

@Composable
fun rememberAppHaptics(): AppHapticFeedback {
    val view = LocalView.current
    return remember(view) { AppHapticFeedback(view) }
}

fun Modifier.hapticClickable(
    enabled: Boolean = true,
    style: HapticStyle = HapticStyle.LIGHT,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit
): Modifier = composed {
    val haptics = rememberAppHaptics()
    val interactionSource = remember { MutableInteractionSource() }
    
    this.clickable(
        enabled = enabled,
        onClickLabel = onClickLabel,
        role = role,
        interactionSource = interactionSource,
        indication = ripple(),
        onClick = {
            haptics.perform(style)
            onClick()
        }
    )
}
