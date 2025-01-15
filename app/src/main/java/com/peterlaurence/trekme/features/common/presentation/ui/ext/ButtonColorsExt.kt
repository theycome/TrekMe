package com.peterlaurence.trekme.features.common.presentation.ui.ext

import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults.buttonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Created by Ivan Yakushev on 15.01.2025
 *
 * Unable to define extensions directly on ButtonColors somehow...
 */
object ButtonColorsExt {

    @Composable
    fun withContainerColorOrPrimary(color: Color? = null): ButtonColors =
        buttonColors(containerColor = color ?: MaterialTheme.colorScheme.primary)

    @Composable
    fun withContainerColorTertiary(): ButtonColors =
        buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)

}
