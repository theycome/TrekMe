package com.peterlaurence.trekme.features.map.presentation.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.excursion.domain.model.ExcursionPhoto
import com.peterlaurence.trekme.core.excursion.domain.model.ExcursionWaypoint
import com.peterlaurence.trekme.core.map.domain.models.ExcursionRef
import com.peterlaurence.trekme.core.map.domain.models.Marker
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.features.map.presentation.ui.components.Marker
import com.peterlaurence.trekme.features.map.presentation.viewmodel.MarkersManageViewModel
import com.peterlaurence.trekme.util.darkenColor
import com.peterlaurence.trekme.util.parseColor
import com.peterlaurence.trekme.util.randomString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.random.Random

@Composable
fun MarkersManageStateful(
    viewModel: MarkersManageViewModel = hiltViewModel(),
    onNavigateToMap: () -> Unit,
    onBackClick: () -> Unit
) {
    val markers by viewModel.getMarkersFlow().collectAsState()
    val excursionWaypoints by viewModel.excursionWaypointFlow.collectAsState()
    // if changing excursion waypoint color causes recomposition of the whole screen and maybe a flicker,
    // then try observing each excursion using the state below.
//    val waypoints by viewModel.getExcursionWaypointsFlow2().collectAsState()
    var search by remember { mutableStateOf("") }

    var searchJob1: Job? = null

    val filteredMarkers by produceState(
        initialValue = markers,
        key1 = search,
        key2 = markers
    ) {
        searchJob1?.cancel()
        searchJob1 = launch(Dispatchers.Default) {
            val lowerCase = search.lowercase().trim()
            value = markers.filter {
                it.name.lowercase().contains(lowerCase)
            }
        }
    }

    var searchJob2: Job? = null
    val filteredWaypoints by produceState(
        initialValue = excursionWaypoints,
        key1 = search,
        key2 = excursionWaypoints
    ) {
        searchJob2?.cancel()
        searchJob2 = launch(Dispatchers.Default) {
            value = if (search.isNotEmpty()) {
                val lowerCase = search.lowercase().trim()
                excursionWaypoints.mapValues {
                    it.value.filter { wpt ->
                        wpt.name.lowercase().contains(lowerCase)
                    }
                }
            } else excursionWaypoints
        }
    }

    MarkersManageScreen(
        markers = filteredMarkers,
        excursionWaypoints = filteredWaypoints,
        hasMarkers = markers.isNotEmpty(),
        onNewSearch = { search = it },
        onGoToMarker = {},
        onGoToExcursionWaypoint = {},
        onBackClick = onBackClick
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MarkersManageScreen(
    markers: List<Marker>,
    excursionWaypoints: Map<ExcursionRef, List<ExcursionWaypoint>>,
    hasMarkers: Boolean,
    onNewSearch: (String) -> Unit,
    onGoToMarker: (Marker) -> Unit,
    onGoToExcursionWaypoint: (ExcursionWaypoint) -> Unit,
    onBackClick: () -> Unit
) {

    Scaffold(
        topBar = {
            MarkersTopAppBar(
                hasAtLeastOneNotSelected = true, // TODO
                onToggleSelectAll = {}, // TODO
                onBackClick
            )
        }
    ) { paddingValues ->
        if (hasMarkers) {
            Column(
                Modifier.padding(paddingValues)
            ) {
                SearchBar(onNewSearch = onNewSearch)

                LazyColumn(
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(markers, key = { it.id }) {
                        PinCard(
                            modifier = Modifier.animateItemPlacement(),
                            name = it.name,
                            color = it.color,
                            onGoToPin = { onGoToMarker(it) }
                        )
                    }
                    for (excursion in excursionWaypoints.keys) {
                        val name = excursion.name.value
                        val wpts = excursionWaypoints[excursion]
                        if (wpts.isNullOrEmpty()) continue
                        item(key = excursion.id) {
                            Text(
                                text = name,
                                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                                fontWeight = FontWeight.Medium
                            )
                        }
                        items(wpts, key = { it.id }) {
                            val excursionColor by excursion.color.collectAsState()
                            PinCard(
                                modifier = Modifier.animateItemPlacement(),
                                name = it.name,
                                color = it.color ?: excursionColor,
                                onGoToPin = { onGoToExcursionWaypoint(it) }
                            )
                        }
                    }
                }
            }
        } else {
            Text(
                stringResource(id = R.string.no_markers_message),
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(16.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MarkersTopAppBar(
    hasAtLeastOneNotSelected: Boolean,
    onToggleSelectAll: () -> Unit,
    onBackClick: () -> Unit
) {
    TopAppBar(
        title = { Text(text = stringResource(id = R.string.markers_manage_title)) },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "")
            }
        },
        actions = {
            var expanded by remember { mutableStateOf(false) }

            IconButton(
                onClick = { expanded = true },
                modifier = Modifier.width(36.dp)
            ) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = null,
                )
            }
            Box(
                Modifier
                    .height(24.dp)
                    .wrapContentSize(Alignment.BottomEnd, true)
            ) {
                DropdownMenu(
                    modifier = Modifier.wrapContentSize(Alignment.TopEnd),
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    offset = DpOffset(0.dp, 0.dp)
                ) {
                    DropdownMenuItem(
                        onClick = onToggleSelectAll,
                        text = {
                            if (hasAtLeastOneNotSelected) {
                                Text(stringResource(id = R.string.markers_manage_select_all))
                            } else {
                                Text(stringResource(id = R.string.markers_manage_deselect_all))
                            }
                        }
                    )
                }
            }
        }
    )
}

@Composable
private fun SearchBar(
    modifier: Modifier = Modifier,
    onNewSearch: (String) -> Unit,
) {
    var searchText by rememberSaveable {
        mutableStateOf("")
    }
    Surface(
        Modifier
            .padding(start = 8.dp, end = 8.dp)
            .fillMaxWidth()
            .padding(top = 12.dp)
            .height(40.dp)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(50)
            ),
        shape = RoundedCornerShape(50),
    ) {
        BasicTextField(
            value = searchText,
            onValueChange = {
                searchText = it
                onNewSearch(it)
            },
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
        ) { innerTextField ->
            Row(
                modifier,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Leading icon
                Icon(
                    painter = painterResource(id = R.drawable.ic_baseline_search_24),
                    modifier = Modifier.padding(horizontal = 8.dp),
                    contentDescription = stringResource(id = R.string.search_hint)
                )

                Box(
                    Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (searchText.isEmpty()) {
                        Text(
                            stringResource(id = R.string.excursion_search_button),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.67f),
                            fontSize = 16.sp
                        )
                    }
                    innerTextField()
                }

                // Trailing icon
                if (searchText.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            searchText = ""
                            onNewSearch("")
                        }
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.close_circle_outline),
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
                            contentDescription = null
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PinCard(
    modifier: Modifier = Modifier,
    name: String,
    color: String,
    onGoToPin: () -> Unit
) {
    var expandedMenu by remember { mutableStateOf(false) }

    ElevatedCard(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val backgroundColor = parseColor(color)
            val strokeColor = darkenColor(backgroundColor, 0.15f)

            Marker(
                modifier = Modifier
                    .graphicsLayer {
                        clip = true
                        translationY = 12.dp.toPx()
                    }
                    .padding(horizontal = 8.dp),
                backgroundColor = Color(backgroundColor),
                strokeColor = Color(strokeColor),
                isStatic = true
            )

            Spacer(modifier = Modifier.width(8.dp))

            if (name.isNotEmpty()) {
                Text(text = name)
            } else {
                Text(
                    text = stringResource(id = R.string.markers_manage_no_name),
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Spacer(modifier = Modifier.weight(1f))

            IconButton(
                onClick = { expandedMenu = true },
            ) {
                Icon(
                    Icons.Default.MoreVert,
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
                    DropdownMenuItem(
                        onClick = {
                            expandedMenu = false
                            onGoToPin()
                        },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(id = R.string.markers_manage_goto))
                                Spacer(Modifier.weight(1f))
                                IconButton(onClick = onGoToPin ) {
                                    Icon(
                                        painterResource(id = R.drawable.ic_gps_fixed_24dp),
                                        contentDescription = stringResource(id = R.string.open_dialog)
                                    )
                                }
                            }
                        }
                    )

                    DropdownMenuItem(
                        onClick = {
                            expandedMenu = false
                            // TODO
                        },
                        text = {
                            Text(stringResource(id = R.string.markers_manage_edit))
                            Spacer(Modifier.weight(1f))
                        }
                    )

                    DropdownMenuItem(
                        onClick = {
                            expandedMenu = false
                            // TODO
                        },
                        text = {
                            Text(stringResource(id = R.string.delete_dialog))
                            Spacer(Modifier.weight(1f))
                        }
                    )
                }
            }
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MarkersManagePreview() {
    TrekMeTheme {
        val markers = buildList {
            repeat(3) {
                add(Marker(lat = 12.6, lon = 2.57, name = "marker-$it"))
                add(Marker(lat = 12.6, lon = 2.57))
            }
        }

        val ref1: ExcursionRef = object : ExcursionRef {
            override val id: String = "id"
            override val name: MutableStateFlow<String> = MutableStateFlow("Excursion name")
            override val visible: MutableStateFlow<Boolean> = MutableStateFlow(true)
            override val color: MutableStateFlow<String> = MutableStateFlow("#2196f3")
        }

        fun makeExcursionWaypoint(): ExcursionWaypoint {
            return object : ExcursionWaypoint {
                override val id: String = UUID.randomUUID().toString()
                override val name: String = randomString(6)
                override val latitude: Double = 0.0
                override val longitude: Double = 0.0
                override val elevation: Double? = null
                override val comment: String = randomString(10)
                override val photos: List<ExcursionPhoto> = emptyList()
                override val color: String? = if (Random.nextBoolean()) "#ffffff" else null
            }
        }
        val excursionWaypoints = buildMap {
            put(ref1, buildList {
                repeat(5) {
                    add(makeExcursionWaypoint())
                }
            })
        }

        MarkersManageScreen(
            markers = markers,
            excursionWaypoints = excursionWaypoints,
            hasMarkers = true,
            onNewSearch = {},
            onGoToMarker = {},
            onGoToExcursionWaypoint = {},
            onBackClick = {}
        )
    }
}
