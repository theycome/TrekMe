package com.peterlaurence.trekme.features.common.presentation.ui.dialogs

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.peterlaurence.trekme.features.common.presentation.ui.ext.ButtonColorsExt

@Composable
fun ConfirmDialog(
    contentText: String,
    confirmButtonText: String,
    cancelButtonText: String,
    confirmColorBackground: Color? = null,
    onConfirmPressed: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        text = {
            Text(contentText, fontSize = 16.sp)
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismissRequest()
                    onConfirmPressed()
                },
                colors = ButtonColorsExt.withContainerColorOrPrimary(confirmColorBackground)
            ) {
                Text(confirmButtonText)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismissRequest,
            ) {
                Text(cancelButtonText)
            }
        }
    )
}
