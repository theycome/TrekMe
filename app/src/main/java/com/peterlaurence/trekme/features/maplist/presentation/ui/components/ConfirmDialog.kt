package com.peterlaurence.trekme.features.maplist.presentation.ui.components

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.peterlaurence.trekme.features.common.presentation.ui.ext.ButtonColorsExt
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme

@Composable
fun ConfirmDialog(
    openState: MutableState<Boolean>,
    onConfirmPressed: () -> Unit,
    contentText: String,
    confirmButtonText: String,
    cancelButtonText: String,
    confirmColorBackground: Color? = null,
) {
    AlertDialog(
        onDismissRequest = { openState.value = false },
        text = {
            Text(contentText, fontSize = 16.sp)
        },
        confirmButton = {
            Button(
                onClick = {
                    openState.value = false
                    onConfirmPressed()
                },
                colors = ButtonColorsExt.withContainerColorOrPrimary(confirmColorBackground)
            ) {
                Text(confirmButtonText)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = {
                    openState.value = false
                },
            ) {
                Text(cancelButtonText)
            }
        }
    )
}

@Preview(uiMode = UI_MODE_NIGHT_YES)
@Preview(showBackground = true)
@Composable
private fun ConfirmDialogPreview() {
    TrekMeTheme {
        val state = remember { mutableStateOf(true) }
        Box {
            ConfirmDialog(
                openState = state,
                onConfirmPressed = { },
                contentText = "Do you really want to delete?",
                confirmButtonText = "Delete",
                cancelButtonText = "Cancel",
                confirmColorBackground = MaterialTheme.colorScheme.error
            )
        }
    }
}
