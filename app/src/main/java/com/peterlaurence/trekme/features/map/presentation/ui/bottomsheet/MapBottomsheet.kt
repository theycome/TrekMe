@file:OptIn(ExperimentalFoundationApi::class)

package com.peterlaurence.trekme.features.map.presentation.ui.bottomsheet

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.georecord.domain.model.GeoStatistics
import com.peterlaurence.trekme.core.georecord.domain.model.hasMeaningfulElevation
import com.peterlaurence.trekme.core.location.domain.model.LatLon
import com.peterlaurence.trekme.core.map.domain.models.ExcursionRef
import com.peterlaurence.trekme.features.common.presentation.ui.bottomsheet.BottomSheetCustom
import com.peterlaurence.trekme.features.common.presentation.ui.bottomsheet.DragHandle
import com.peterlaurence.trekme.features.common.presentation.ui.bottomsheet.States
import com.peterlaurence.trekme.features.common.presentation.ui.component.TrackStats
import com.peterlaurence.trekme.features.common.presentation.ui.dialogs.ConfirmDialog
import com.peterlaurence.trekme.features.map.presentation.ui.components.ColorIndicator
import com.peterlaurence.trekme.features.map.presentation.ui.components.ColorPicker
import com.peterlaurence.trekme.features.map.presentation.viewmodel.layers.BottomSheetState
import com.peterlaurence.trekme.features.map.presentation.viewmodel.layers.TrackType
import com.peterlaurence.trekme.features.map.presentation.viewmodel.layers.hasElevation
import com.peterlaurence.trekme.features.trailsearch.presentation.ui.component.ElevationGraph
import com.peterlaurence.trekme.util.parseColorL
import kotlinx.coroutines.flow.StateFlow

@Composable
fun BottomSheet(
    anchoredDraggableState: AnchoredDraggableState<States>,
    screenHeightDp: Dp,
    screenHeightPx: Float,
    bottomSheetState: BottomSheetState,
    expandedRatio: Float,
    peakedRatio: Float,
    onCursorMove: (latLon: LatLon, d: Double, ele: Double) -> Unit,
    onColorChange: (Long, TrackType) -> Unit,
    onTitleChange: (String, TrackType) -> Unit,
    onEditPath: (ExcursionRef) -> Unit,
    onSharePath: (ExcursionRef) -> Unit,
    onDelete: (TrackType) -> Unit
) {
    val anchors = remember {
        DraggableAnchors {
            States.EXPANDED at screenHeightPx * (1 - expandedRatio)
            States.PEAKED at screenHeightPx * (1 - peakedRatio)
            States.COLLAPSED at screenHeightPx
        }
    }

    SideEffect {
        anchoredDraggableState.updateAnchors(anchors)
    }

    BottomSheetCustom(
        state = anchoredDraggableState,
        fullHeight = screenHeightDp * expandedRatio,
        header = {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                DragHandle()
            }
        },
        content = {
            when (bottomSheetState) {
                BottomSheetState.Loading -> {
                    item {
                        Box(Modifier.fillMaxWidth()) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .padding(vertical = 32.dp)
                                    .align(Alignment.Center)
                            )
                        }
                    }
                }

                is BottomSheetState.BottomSheetData -> {
                    titleSection(
                        bottomSheetState.title,
                        bottomSheetState.color,
                        bottomSheetState.shareLoading,
                        onColorChange = { color ->
                            onColorChange(color, bottomSheetState.type)
                        },
                        onTitleChange = { title ->
                            onTitleChange(title, bottomSheetState.type)
                        },
                        onEditPath = if (bottomSheetState.type is TrackType.ExcursionType && bottomSheetState.type.isPathEditable) {
                            { onEditPath(bottomSheetState.type.excursionRef) }
                        } else null,
                        onShare = if (bottomSheetState.type is TrackType.ExcursionType) {
                            { onSharePath(bottomSheetState.type.excursionRef) }
                        } else null,
                        onDelete = { onDelete(bottomSheetState.type) }
                    )
                    statsSection(bottomSheetState.stats, bottomSheetState.hasElevation)
                    elevationGraphSection(bottomSheetState, onCursorMove)
                }
            }
        }
    )
}

private fun LazyListScope.titleSection(
    titleFlow: StateFlow<String>,
    colorFlow: StateFlow<String>,
    shareLoadingFlow: StateFlow<Boolean>,
    onColorChange: (Long) -> Unit,
    onTitleChange: (String) -> Unit,
    onEditPath: (() -> Unit)?,
    onShare: (() -> Unit)?,
    onDelete: () -> Unit
) {
    stickyHeader("title") {
        val title by titleFlow.collectAsState()
        val color by colorFlow.collectAsState()
        val shareLoading by shareLoadingFlow.collectAsState()
        var isShowingColorPicker by remember { mutableStateOf(false) }
        var isShowingTitleEdit by remember { mutableStateOf(false) }
        var expandedMenu by remember { mutableStateOf(false) }
        var showDeleteConfirmation by remember { mutableStateOf(false) }

        Row(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ColorIndicator(
                radius = 16.dp,
                color = color,
                onClick = { isShowingColorPicker = true }
            )
            Spacer(Modifier.width(8.dp))
            Text(
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        isShowingTitleEdit = true
                    },
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            IconButton(
                onClick = { expandedMenu = true },
                modifier = Modifier.width(36.dp)
            ) {
                Icon(
                    Icons.Default.MoreVert,
                    tint = MaterialTheme.colorScheme.onSurface,
                    contentDescription = null,
                )
            }
            Box(
                Modifier
                    .height(24.dp)
                    .wrapContentSize(Alignment.BottomEnd, true)
            ) {
                DropdownMenu(
                    expanded = expandedMenu,
                    onDismissRequest = { expandedMenu = false },
                    offset = DpOffset(0.dp, 0.dp)
                ) {
                    if (onEditPath != null) {
                        DropdownMenuItem(
                            onClick = {
                                expandedMenu = false
                                onEditPath()
                            },
                            text = {
                                Text(stringResource(id = R.string.edit_track_path))
                                Spacer(Modifier.weight(1f))
                            },
                            leadingIcon = {
                                Icon(
                                    painterResource(id = R.drawable.ic_edit_black_30dp),
                                    modifier = Modifier.size(24.dp),
                                    contentDescription = stringResource(
                                        id = R.string.edit_track_path
                                    )
                                )
                            }
                        )
                    }
                    if (onShare != null) {
                        DropdownMenuItem(
                            enabled = !shareLoading,
                            onClick = {
                                expandedMenu = false
                                onShare()
                            },
                            text = {
                                Text(stringResource(id = R.string.track_share))
                                Spacer(Modifier.weight(1f))
                            },
                            leadingIcon = {
                                Icon(
                                    painterResource(id = R.drawable.ic_share_black_24dp),
                                    modifier = Modifier.size(24.dp),
                                    contentDescription = stringResource(
                                        id = R.string.track_share
                                    )
                                )
                            }
                        )
                    }
                    DropdownMenuItem(
                        onClick = {
                            expandedMenu = false
                            showDeleteConfirmation = true
                        },
                        text = {
                            Text(stringResource(id = R.string.delete_dialog))
                            Spacer(Modifier.weight(1f))
                        },
                        leadingIcon = {
                            Icon(
                                painterResource(id = R.drawable.ic_delete_forever_black_24dp),
                                contentDescription = stringResource(
                                    id = R.string.delete_dialog
                                )
                            )
                        }
                    )
                }
            }
        }

        if (isShowingColorPicker) {
            ColorPicker(
                initColor = parseColorL(color),
                onColorPicked = { c ->
                    onColorChange(c)
                    isShowingColorPicker = false
                },
                onCancel = { isShowingColorPicker = false }
            )
        }

        if (isShowingTitleEdit) {
            EditTitleDialog(
                title = title,
                onTitleChange = onTitleChange,
                onDismissRequest = { isShowingTitleEdit = false }
            )
        }

        if (showDeleteConfirmation) {
            ConfirmDialog(
                contentText = stringResource(R.string.delete_track_confirm),
                onConfirmPressed = onDelete,
                cancelButtonText = stringResource(R.string.cancel_dialog_string),
                confirmButtonText = stringResource(R.string.delete_dialog),
                onDismissRequest = { showDeleteConfirmation = false }
            )
        }
    }
}

private fun LazyListScope.statsSection(geoStatistics: GeoStatistics, hasElevation: Boolean) {
    item("stats") {
        Column {
            Spacer(Modifier.height(16.dp))
            TrackStats(Modifier.padding(horizontal = 16.dp), geoStatistics)
            if (!hasElevation || !geoStatistics.hasMeaningfulElevation) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.information),
                        tint = MaterialTheme.colorScheme.primary,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(
                            if (!hasElevation) {
                                R.string.no_elevation_track
                            } else {
                                R.string.elevation_stats_not_significant
                            }
                        ),
                        fontSize = 13.sp,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

private fun LazyListScope.elevationGraphSection(
    data: BottomSheetState.BottomSheetData,
    onCursorMove: (latLon: LatLon, d: Double, ele: Double) -> Unit
) {
    if (data.elevationGraphPoints != null) {
        item(key = "elevation-graph") {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding()
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    stringResource(id = R.string.elevation_profile),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card {
                    ElevationGraph(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        points = data.elevationGraphPoints,
                        verticalSpacingY = 20.dp,
                        verticalPadding = 16.dp,
                        onCursorMove = onCursorMove
                    )
                }
            }
        }
    }
}

@Composable
private fun EditTitleDialog(
    title: String,
    onTitleChange: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                title,
                selection = TextRange(title.length)
            )
        )
    }
    val focusRequester = remember { FocusRequester() }

    AlertDialog(
        title = {
            Text(
                text = stringResource(R.string.track_name_change),
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            TextField(
                value = textFieldValue,
                onValueChange = { textFieldValue = it },
                modifier = Modifier.focusRequester(focusRequester)
            )
        },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    onTitleChange(textFieldValue.text)
                    onDismissRequest()
                }
            ) {
                Text(text = stringResource(id = R.string.ok_dialog))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    textFieldValue = textFieldValue.copy(text = title)
                    onDismissRequest()
                }
            ) {
                Text(text = stringResource(id = R.string.cancel_dialog_string))
            }
        }
    )

    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }
}