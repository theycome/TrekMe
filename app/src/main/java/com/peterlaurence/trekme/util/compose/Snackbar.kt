package com.peterlaurence.trekme.util.compose

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult

suspend fun SnackbarHostState.showSnackbar(
    message: String,
    isLongDuration: Boolean = false,
    actionLabel: String? = null,
): SnackbarResult =
    showSnackbar(
        message,
        actionLabel = actionLabel,
        duration = when (isLongDuration) {
            true -> SnackbarDuration.Long
            false -> SnackbarDuration.Short
        }
    )
