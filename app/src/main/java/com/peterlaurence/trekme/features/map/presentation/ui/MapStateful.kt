package com.peterlaurence.trekme.features.map.presentation.ui

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import android.view.Surface
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.location.domain.model.Location
import com.peterlaurence.trekme.core.settings.RotationMode
import com.peterlaurence.trekme.features.common.presentation.ui.screens.LoadingScreen
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.features.common.presentation.ui.theme.md_theme_light_background
import com.peterlaurence.trekme.features.map.app.intents.itineraryToMarker
import com.peterlaurence.trekme.features.map.app.service.TrackFollowService
import com.peterlaurence.trekme.features.map.presentation.events.BeaconEditEvent
import com.peterlaurence.trekme.features.map.presentation.events.ExcursionWaypointEditEvent
import com.peterlaurence.trekme.features.map.presentation.events.ItineraryEvent
import com.peterlaurence.trekme.features.map.presentation.events.MarkerEditEvent
import com.peterlaurence.trekme.features.map.presentation.ui.components.*
import com.peterlaurence.trekme.features.map.presentation.ui.screens.ErrorScaffold
import com.peterlaurence.trekme.features.map.presentation.ui.screens.MapScreen
import com.peterlaurence.trekme.features.map.presentation.viewmodel.*
import com.peterlaurence.trekme.features.map.presentation.viewmodel.layers.TrackFollowLayer
import com.peterlaurence.trekme.features.record.presentation.ui.components.dialogs.BatteryOptimSolutionDialog
import com.peterlaurence.trekme.features.record.presentation.ui.components.dialogs.BatteryOptimWarningDialog
import com.peterlaurence.trekme.features.record.presentation.ui.components.dialogs.LocationRationale
import com.peterlaurence.trekme.util.android.requestBackgroundLocationPermission
import com.peterlaurence.trekme.util.android.shouldShowBackgroundLocPermRationale
import com.peterlaurence.trekme.util.compose.LaunchedEffectWithLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.api.rotation
import java.util.*

@Composable
fun MapStateful(
    viewModel: MapViewModel = viewModel(),
    statisticsViewModel: StatisticsViewModel = viewModel(),
    gpxRecordServiceViewModel: GpxRecordServiceViewModel = viewModel(),
    onNavigateToTracksManage: () -> Unit,
    onNavigateToMarkerEdit: (markerId: String, mapId: UUID) -> Unit,
    onNavigateToExcursionWaypointEdit: (waypointId: String, excursionId: String) -> Unit,
    onNavigateToBeaconEdit: (beaconId: String, mapId: UUID) -> Unit,
    onNavigateToShop: () -> Unit,
    onMainMenuClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val purchased by viewModel.purchaseFlow.collectAsState()
    val isShowingOrientation by viewModel.orientationVisibilityFlow()
        .collectAsState(initial = false)
    val isShowingDistance by viewModel.isShowingDistanceFlow().collectAsState()
    val isShowingDistanceOnTrack by viewModel.isShowingDistanceOnTrackFlow().collectAsState()
    val isShowingSpeed by viewModel.isShowingSpeedFlow().collectAsState(initial = false)
    val isLockedOnpPosition by viewModel.isLockedOnPosition()
    val isShowingGpsData by viewModel.isShowingGpsDataFlow().collectAsState(initial = false)
    val isShowingScaleIndicator by viewModel.settings.getShowScaleIndicator()
        .collectAsState(initial = true)
    val stats by statisticsViewModel.stats.collectAsState(initial = null)
    val rotationMode by viewModel.settings.getRotationMode()
        .collectAsState(initial = RotationMode.NONE)

    val lifecycleOwner = LocalLifecycleOwner.current
    val locationFlow = viewModel.locationFlow
    val elevationFix by viewModel.elevationFixFlow.collectAsState()

    LaunchedEffectWithLifecycle {
        viewModel.liveRouteLayer.drawLiveRoute()
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val context = LocalContext.current
    val itineraryError = stringResource(id = R.string.itinerary_error)
    val ok = stringResource(id = R.string.ok_dialog)
    LaunchedEffectWithLifecycle(flow = viewModel.placeableEvents) { event ->
        when(event) {
            is MarkerEditEvent -> onNavigateToMarkerEdit(event.marker.id, event.mapId)
            is ExcursionWaypointEditEvent -> onNavigateToExcursionWaypointEdit(event.waypoint.id, event.excursionId)
            is BeaconEditEvent -> onNavigateToBeaconEdit(event.beacon.id, event.mapId)
            is ItineraryEvent -> {
                val success = itineraryToMarker(event.latitude, event.longitude, context)
                if (!success) {
                    showSnackbar(scope, snackbarHostState, itineraryError, ok)
                }
            }
        }
    }
    LaunchedEffect(lifecycleOwner) {
        launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                locationFlow.collect {
                    viewModel.locationOrientationLayer.onLocation(it)
                }
            }
        }
        launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.checkMapLicense()
            }
        }
        launch {
            viewModel.startTrackFollowEvent.collect {
                val intent = Intent(context, TrackFollowService::class.java)
                context.startService(intent)
            }
        }
    }

    if (uiState is MapUiState) {
        val displayRotation = getDisplayRotation()
        LaunchedEffect(lifecycleOwner, isShowingOrientation) {
            if (!isShowingOrientation) return@LaunchedEffect
            launch {
                lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                    viewModel.orientationFlow.collect {
                        viewModel.locationOrientationLayer.onOrientation(it, displayRotation)
                    }
                }
            }
        }
    }

    val outOfBounds = stringResource(id = R.string.map_screen_loc_outside_map)
    var showTrackFollowDialog by rememberSaveable { mutableStateOf(false) }
    LaunchedEffectWithLifecycle(flow = viewModel.events) { event ->
        fun dismissSnackbar() = scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
        }
        when (event) {
            MapEvent.CURRENT_LOCATION_OUT_OF_BOUNDS -> showSnackbar(scope, snackbarHostState, outOfBounds, ok)

            MapEvent.TRACK_TO_FOLLOW_SELECTED -> dismissSnackbar()
            MapEvent.TRACK_TO_FOLLOW_ALREADY_RUNNING -> {
                showTrackFollowDialog = true
            }
        }
    }

    val activity = context.getActivity()
    val selectTrack = stringResource(id = R.string.select_track_to_follow)
    var isShowingBatteryWarning by rememberSaveable { mutableStateOf(false) }
    var isShowingBatterySolution by rememberSaveable { mutableStateOf(false) }
    var isShowingLocationRationale by rememberSaveable { mutableStateOf(false) }

    LaunchedEffectWithLifecycle(flow = viewModel.trackFollowLayer.events) { event ->
        when (event) {
            TrackFollowLayer.Event.DisableBatteryOptSignal -> isShowingBatteryWarning = true
            TrackFollowLayer.Event.BackgroundLocationNotGranted -> {
                /* In this case, check if we should show a rationale. Whatever the outcome, we'll ask
                 * for the permission. */
                if (activity == null) return@LaunchedEffectWithLifecycle
                if (shouldShowBackgroundLocPermRationale(activity)) {
                    isShowingLocationRationale = true
                } else {
                    /* Request permission anyway */
                    requestBackgroundLocationPermission(activity)
                }
            }

            TrackFollowLayer.Event.SelectTrackToFollow -> {
                showSnackbar(scope, snackbarHostState, selectTrack, ok)
            }
        }
    }

    when (uiState) {
        Loading -> {
            LoadingScreen()
        }

        is MapUiState -> {
            val mapUiState = uiState as MapUiState
            val name by mapUiState.mapNameFlow.collectAsStateWithLifecycle()
            /* Always use the light theme background (dark theme or not). Done this way, it
             * doesn't add a GPU overdraw. */
            TrekMeTheme(darkThemeBackground = md_theme_light_background) {
                Column {
                    MapScaffold(
                        Modifier.weight(1f, true),
                        mapUiState,
                        name,
                        snackbarHostState,
                        isShowingOrientation,
                        isShowingDistance,
                        isShowingDistanceOnTrack,
                        isShowingSpeed,
                        isLockedOnpPosition,
                        isShowingGpsData,
                        isShowingScaleIndicator,
                        rotationMode,
                        locationFlow,
                        elevationFix,
                        hasElevationFix = purchased,
                        hasBeacons = purchased,
                        hasTrackFollow = purchased,
                        onMainMenuClick = onMainMenuClick,
                        onManageTracks = onNavigateToTracksManage,
                        onToggleShowOrientation = viewModel::toggleShowOrientation,
                        onAddMarker = viewModel.markerLayer::addMarker,
                        onAddLandmark = viewModel.landmarkLayer::addLandmark,
                        onAddBeacon = viewModel.beaconLayer::addBeacon,
                        onShowDistance = viewModel.distanceLayer::toggleDistance,
                        onToggleDistanceOnTrack = viewModel.routeLayer::toggleDistanceOnTrack,
                        onToggleSpeed = viewModel::toggleSpeed,
                        onToggleLockOnPosition = viewModel.locationOrientationLayer::toggleLockedOnPosition,
                        onToggleShowGpsData = viewModel::toggleShowGpsData,
                        onFollowTrack = { viewModel.initiateTrackFollow() },
                        onPositionFabClick = viewModel.locationOrientationLayer::centerOnPosition,
                        onCompassClick = viewModel::alignToNorth,
                        onElevationFixUpdate = viewModel::onElevationFixUpdate,
                        onNavigateToShop = onNavigateToShop,
                        recordingButtons = {
                            RecordingFabStateful(gpxRecordServiceViewModel)
                        }
                    )

                    stats?.also {
                        StatsPanel(it)
                    }
                }
            }
        }

        is Error -> ErrorScaffold(
            uiState as Error,
            onMainMenuClick = onMainMenuClick,
            onShopClick = onNavigateToShop
        )
    }

    if (showTrackFollowDialog) {
        AlertDialog(
            title = {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.transit_detour),
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .padding(6.dp)
                            .align(Alignment.Center),
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimary),
                        contentDescription = null
                    )
                }
            },
            text = {
                Text(
                    stringResource(id = R.string.track_follow_already_running),
                    fontSize = 16.sp,
                    style = LocalTextStyle.current.copy(hyphens = Hyphens.Auto)
                )
            },
            confirmButton = {
                TextButton(onClick = { showTrackFollowDialog = false }) {
                    Text(stringResource(id = R.string.cancel_dialog_string))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        /* Stop the service */
                        val intent = Intent(context, TrackFollowService::class.java)
                        intent.action = TrackFollowService.stopAction
                        context.startService(intent)

                        showTrackFollowDialog = false
                    }
                ) {
                    Text(text = stringResource(id = R.string.service_track_follow_stop))
                }
            },
            onDismissRequest = {
                showTrackFollowDialog = false
            }
        )
    }

    if (isShowingBatteryWarning) {
        BatteryOptimWarningDialog(
            text = stringResource(id = R.string.battery_warn_message_track_follow),
            onShowSolution = {
                isShowingBatterySolution = true
                isShowingBatteryWarning = false
            },
            onDismissRequest = {
                isShowingBatteryWarning = false
                viewModel.trackFollowLayer.ackBatteryOptSignal.trySend(Unit)
            },
        )
    }

    if (isShowingBatterySolution) {
        BatteryOptimSolutionDialog(
            onDismissRequest = {
                isShowingBatterySolution = false
                viewModel.trackFollowLayer.ackBatteryOptSignal.trySend(Unit)
            }
        )
    }

    if (isShowingLocationRationale) {
        LocationRationale(
            text = stringResource(id = R.string.background_location_rationale_track_follow),
            onConfirm = {
                if (activity != null) {
                    requestBackgroundLocationPermission(activity)
                }
                isShowingLocationRationale = false
            },
            onIgnore = {
                isShowingLocationRationale = false
            },
        )
    }
}

@Composable
private fun MapScaffold(
    modifier: Modifier = Modifier,
    uiState: MapUiState,
    name: String,
    snackbarHostState: SnackbarHostState,
    isShowingOrientation: Boolean,
    isShowingDistance: Boolean,
    isShowingDistanceOnTrack: Boolean,
    isShowingSpeed: Boolean,
    isLockedOnPosition: Boolean,
    isShowingGpsData: Boolean,
    isShowingScaleIndicator: Boolean,
    rotationMode: RotationMode,
    locationFlow: Flow<Location>,
    elevationFix: Int,
    hasElevationFix: Boolean,
    hasBeacons: Boolean,
    hasTrackFollow: Boolean,
    onMainMenuClick: () -> Unit,
    onManageTracks: () -> Unit,
    onToggleShowOrientation: () -> Unit,
    onAddMarker: () -> Unit,
    onAddLandmark: () -> Unit,
    onAddBeacon: () -> Unit,
    onShowDistance: () -> Unit,
    onToggleDistanceOnTrack: () -> Unit,
    onToggleSpeed: () -> Unit,
    onToggleLockOnPosition: () -> Unit,
    onToggleShowGpsData: () -> Unit,
    onFollowTrack: () -> Unit,
    onPositionFabClick: () -> Unit,
    onCompassClick: () -> Unit,
    onElevationFixUpdate: (Int) -> Unit,
    onNavigateToShop: () -> Unit,
    recordingButtons: @Composable () -> Unit
) {
    Scaffold(
        modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            MapTopAppBar(
                title = name,
                isShowingOrientation = isShowingOrientation,
                isShowingDistance = isShowingDistance,
                isShowingDistanceOnTrack = isShowingDistanceOnTrack,
                isShowingSpeed = isShowingSpeed,
                isLockedOnPosition = isLockedOnPosition,
                isShowingGpsData = isShowingGpsData,
                hasBeacons = hasBeacons,
                hasTrackFollow = hasTrackFollow,
                onMenuClick = onMainMenuClick,
                onManageTracks = onManageTracks,
                onToggleShowOrientation = onToggleShowOrientation,
                onAddMarker = onAddMarker,
                onAddLandmark = onAddLandmark,
                onAddBeacon = onAddBeacon,
                onShowDistance = onShowDistance,
                onToggleDistanceOnTrack = onToggleDistanceOnTrack,
                onToggleSpeed = onToggleSpeed,
                onToggleLockPosition = onToggleLockOnPosition,
                onToggleShowGpsData = onToggleShowGpsData,
                onFollowTrack = onFollowTrack,
                onNavigateToShop = onNavigateToShop
            )
        },
        floatingActionButton = {
            Column {
                if (rotationMode != RotationMode.NONE) {
                    CompassFab(
                        degrees = uiState.mapState.rotation,
                        onClick = if (rotationMode == RotationMode.FREE) onCompassClick else {
                            {}
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                FloatingActionButton(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    onClick = onPositionFabClick,
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_gps_fixed_24dp),
                        contentDescription = stringResource(id = R.string.center_on_position_btn_desc),
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimaryContainer)
                    )
                }
            }
        }
    ) { paddingValues ->
        MapScreen(
            Modifier.padding(paddingValues),
            uiState,
            isShowingDistance,
            isShowingSpeed,
            isShowingGpsData,
            isShowingScaleIndicator,
            locationFlow,
            elevationFix,
            hasElevationFix,
            onElevationFixUpdate,
            recordingButtons = recordingButtons
        )
    }
}

@Composable
private fun RecordingFabStateful(viewModel: GpxRecordServiceViewModel) {
    val gpxRecordState by viewModel.status.collectAsState()
    var isShowingBatteryWarning by rememberSaveable { mutableStateOf(false) }
    var isShowingBatterySolution by rememberSaveable { mutableStateOf(false) }
    var isShowingLocationRationale by rememberSaveable { mutableStateOf(false) }

    LaunchedEffectWithLifecycle(flow = viewModel.events) { event ->
        when (event) {
            GpxRecordServiceViewModel.Event.BackgroundLocationNotGranted -> {
                isShowingLocationRationale = true
            }

            GpxRecordServiceViewModel.Event.DisableBatteryOptSignal -> {
                isShowingBatteryWarning = true
            }
        }
    }

    if (isShowingBatteryWarning) {
        BatteryOptimWarningDialog(
            text = stringResource(id = R.string.battery_warn_message_gpx_recording),
            onShowSolution = {
                isShowingBatterySolution = true
                isShowingBatteryWarning = false
            },
            onDismissRequest = {
                isShowingBatteryWarning = false
                viewModel.ackBatteryOptSignal.trySend(Unit)
            },
        )
    }

    if (isShowingBatterySolution) {
        BatteryOptimSolutionDialog(
            onDismissRequest = {
                isShowingBatterySolution = false
                viewModel.ackBatteryOptSignal.trySend(Unit)
            }
        )
    }

    if (isShowingLocationRationale) {
        LocationRationale(
            text = stringResource(id = R.string.background_location_rationale_gpx_recording),
            onConfirm = {
                /* The background location permission is asked after the rationale is closed. But it doesn't
                 * matter that the recording is already started - it works even when the permission is
                 * granted during the recording. */
                viewModel.requestBackgroundLocationPerm()
                isShowingLocationRationale = false
            },
            onIgnore = {
                isShowingLocationRationale = false
            },
        )
    }

    RecordingButtons(
        gpxRecordState,
        onStartStopClick = viewModel::onStartStopClicked,
        onPauseResumeClick = viewModel::onPauseResumeClicked
    )
}


/**
 * We need to know the display rotation (either 0, 90°, 180°, or 270°) - and not just the
 * portrait / landscape mode.
 * To get that information, we only need a [Context] for Android 11 and up. However, on Android 10
 * and below, we need the [AppCompatActivity].
 *
 * @return The angle in decimal degrees
 */
@Composable
private fun getDisplayRotation(): Int {
    val surfaceRotation: Int = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
        @Suppress("DEPRECATION")
        LocalContext.current.getActivity()?.windowManager?.defaultDisplay?.rotation
            ?: Surface.ROTATION_0
    } else {
        LocalContext.current.display?.rotation ?: Surface.ROTATION_0
    }

    return when (surfaceRotation) {
        Surface.ROTATION_90 -> 90
        Surface.ROTATION_180 -> 180
        Surface.ROTATION_270 -> 270
        else -> 0
    }
}

/**
 * Depending on where the compose tree was originally created, we might have a [ContextWrapper].
 */
private tailrec fun Context.getActivity(): AppCompatActivity? = when (this) {
    is AppCompatActivity -> this
    is ContextWrapper -> baseContext.getActivity()
    else -> null
}

fun showSnackbar(
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    msg: String,
    okString: String
) = scope.launch {
    /* Dismiss the currently showing snackbar, if any */
    snackbarHostState.currentSnackbarData?.dismiss()

    snackbarHostState.showSnackbar(msg, actionLabel = okString)
}
