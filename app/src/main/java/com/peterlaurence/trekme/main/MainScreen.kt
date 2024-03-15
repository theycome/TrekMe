package com.peterlaurence.trekme.main

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.map.domain.models.MapDownloadAlreadyRunning
import com.peterlaurence.trekme.core.map.domain.models.MapDownloadEvent
import com.peterlaurence.trekme.core.map.domain.models.MapDownloadFinished
import com.peterlaurence.trekme.core.map.domain.models.MapDownloadPending
import com.peterlaurence.trekme.core.map.domain.models.MapDownloadStorageError
import com.peterlaurence.trekme.core.map.domain.models.MapNotRepairable
import com.peterlaurence.trekme.core.map.domain.models.MapUpdateFinished
import com.peterlaurence.trekme.core.map.domain.models.MapUpdatePending
import com.peterlaurence.trekme.core.map.domain.models.MissingApiError
import com.peterlaurence.trekme.events.GenericMessage
import com.peterlaurence.trekme.events.StandardMessage
import com.peterlaurence.trekme.events.WarningMessage
import com.peterlaurence.trekme.events.recording.GpxRecordEvents
import com.peterlaurence.trekme.features.common.presentation.ui.dialogs.WarningDialog
import com.peterlaurence.trekme.features.mapcreate.presentation.ui.navigation.wmtsDestination
import com.peterlaurence.trekme.features.record.app.service.event.NewExcursionEvent
import com.peterlaurence.trekme.main.navigation.MainGraph
import com.peterlaurence.trekme.main.navigation.navigateToAbout
import com.peterlaurence.trekme.main.navigation.navigateToGpsPro
import com.peterlaurence.trekme.main.navigation.navigateToMap
import com.peterlaurence.trekme.main.navigation.navigateToMapCreation
import com.peterlaurence.trekme.main.navigation.navigateToMapImport
import com.peterlaurence.trekme.main.navigation.navigateToMapList
import com.peterlaurence.trekme.main.navigation.navigateToRecord
import com.peterlaurence.trekme.main.navigation.navigateToSettings
import com.peterlaurence.trekme.main.navigation.navigateToShop
import com.peterlaurence.trekme.main.navigation.navigateToTrailSearch
import com.peterlaurence.trekme.main.navigation.navigateToWifiP2p
import com.peterlaurence.trekme.main.viewmodel.RecordingEventHandlerViewModel
import com.peterlaurence.trekme.util.android.activity
import com.peterlaurence.trekme.util.checkInternet
import com.peterlaurence.trekme.util.compose.LaunchedEffectWithLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun MainStateful(
    viewModel: MainActivityViewModel,
    recordingEventHandlerViewModel: RecordingEventHandlerViewModel,
    genericMessages: Flow<GenericMessage>
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    val context = LocalContext.current

    LaunchedEffectWithLifecycle(viewModel.eventFlow) { event ->
        when (event) {
            MainActivityEvent.ShowMap -> navController.navigateToMap()
            MainActivityEvent.ShowMapList -> navController.navigateToMapList()
            MainActivityEvent.ShowRecordings -> navController.navigateToRecord()
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    var isShowingWarningDialog by remember { mutableStateOf<WarningMessage?>(null) }
    isShowingWarningDialog?.also {
        WarningDialog(
            title = it.title,
            contentText = it.msg,
            onDismissRequest = { isShowingWarningDialog = null }
        )
    }

    RecordingEventHandler(
        recordingEventHandlerViewModel.gpxRecordEvents,
        onNewExcursion = recordingEventHandlerViewModel::onNewExcursionEvent
    )

    MapDownloadEventHandler(
        downloadEvents = viewModel.downloadEvents,
        navController = navController,
        snackbarHostState = snackbarHostState,
        scope = scope,
        context = context,
        onGoToMap = { uuid -> viewModel.onGoToMap(uuid) },
        onShowWarningDialog = { isShowingWarningDialog = it }
    )

    HandleGenericMessages(
        genericMessages = genericMessages,
        scope = scope,
        snackbarHostState = snackbarHostState,
        onShowWarningDialog = { isShowingWarningDialog = it }
    )
    HandleBackGesture(drawerState, scope, navController, snackbarHostState)

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Permission should always be granted
    }

    val checkInternetPermission = {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.INTERNET
            ) == PackageManager.PERMISSION_DENIED
        ) {
            launcher.launch(Manifest.permission.INTERNET)
        }
    }

    val noInternet = stringResource(id = R.string.no_internet)
    // TODO: do this in each individual destination and not at this level
    val warnIfNotInternet = {
        scope.launch {
            if (!checkInternet()) {
                snackbarHostState.showSnackbar(noInternet)
            }
        }
    }

    val gpsProPurchased by viewModel.gpsProPurchased.collectAsState()
    val menuItems by remember {
        derivedStateOf {
            if (gpsProPurchased) MenuItem.entries else MenuItem.entries.filter { it != MenuItem.GpsPro }
        }
    }
    val selectedItem = remember { mutableStateOf(menuItems[0]) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                menuItems.forEach { item ->
                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                painterResource(id = getIconForMenu(item)),
                                contentDescription = null
                            )
                        },
                        label = { Text(stringResource(id = getNameForMenu(item))) },
                        selected = item == selectedItem.value,
                        onClick = {
                            scope.launch {
                                drawerState.close()
                            }
                            selectedItem.value = item

                            when (item) {
                                MenuItem.MapList -> navController.navigateToMapList()
                                MenuItem.MapCreate -> {
                                    checkInternetPermission()
                                    warnIfNotInternet()
                                    navController.navigateToMapCreation()
                                }

                                MenuItem.Record -> navController.navigateToRecord()
                                MenuItem.TrailSearch -> {
                                    warnIfNotInternet()
                                    navController.navigateToTrailSearch()
                                }

                                MenuItem.GpsPro -> navController.navigateToGpsPro()
                                MenuItem.MapImport -> navController.navigateToMapImport()
                                MenuItem.WifiP2p -> navController.navigateToWifiP2p()
                                MenuItem.Settings -> navController.navigateToSettings()
                                MenuItem.Shop -> navController.navigateToShop()
                                MenuItem.About -> navController.navigateToAbout()
                            }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        },
        content = {
            Box {
                MainGraph(
                    navController = navController,
                    onMainMenuClick = { scope.launch { drawerState.open() } }
                )
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    )
}

/**
 * If the side menu is opened, just close it.
 * If there's no previous destination, display a confirmation snackbar to back once more before
 * killing the app.
 * Otherwise, navigate up.
 */
@Composable
fun HandleBackGesture(
    drawerState: DrawerState,
    scope: CoroutineScope,
    navController: NavController,
    snackbarHostState: SnackbarHostState
) {
    val activity = LocalContext.current.activity
    val confirmExit = stringResource(id = R.string.confirm_exit)
    BackHandler {
        if (drawerState.isOpen) {
            scope.launch {
                drawerState.close()
            }
        } else {
            if (navController.previousBackStackEntry == null) {
                if (snackbarHostState.currentSnackbarData?.visuals?.message == confirmExit) {
                    activity.finish()
                } else {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            confirmExit,
                            withDismissAction = true,
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            } else {
                navController.navigateUp()
            }
        }
    }
}

@Composable
private fun HandleGenericMessages(
    genericMessages: Flow<GenericMessage>,
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    onShowWarningDialog: (WarningMessage) -> Unit
) {
    LaunchedEffectWithLifecycle(genericMessages) { message ->
        when (message) {
            is StandardMessage -> {
                scope.launch {
                    snackbarHostState.showSnackbar(message = message.msg, isLong = message.showLong)
                }
            }

            is WarningMessage -> {
                onShowWarningDialog(message)
            }
        }
    }
}

@Composable
private fun RecordingEventHandler(
    gpxRecordEvents: GpxRecordEvents,
    onNewExcursion: (NewExcursionEvent) -> Unit
) {
    LaunchedEffectWithLifecycle(gpxRecordEvents.newExcursionEvent) { event ->
        onNewExcursion(event)
    }
}

@Composable
private fun MapDownloadEventHandler(
    downloadEvents: SharedFlow<MapDownloadEvent>,
    navController: NavController,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope,
    context: Context,
    onGoToMap: (UUID) -> Unit,
    onShowWarningDialog: (WarningMessage) -> Unit
) {
    LaunchedEffectWithLifecycle(downloadEvents) { event ->
        when (event) {
            is MapDownloadFinished -> {
                if (wmtsDestination == navController.currentDestination?.route) {
                    navController.navigateToMapList()
                }
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        context.getString(R.string.service_download_finished),
                        actionLabel = context.getString(R.string.ok_dialog),
                        isLong = true
                    )

                    if (result == SnackbarResult.ActionPerformed) {
                        onGoToMap(event.mapId)
                    }
                }
            }

            is MapUpdateFinished -> {
                val result = snackbarHostState.showSnackbar(
                    message = if (event.repairOnly) {
                        context.getString(R.string.service_repair_finished)
                    } else {
                        context.getString(R.string.service_update_finished)
                    },
                    actionLabel = context.getString(R.string.ok_dialog),
                    isLong = true
                )

                if (result == SnackbarResult.ActionPerformed) {
                    onGoToMap(event.mapId)
                }
            }

            MapDownloadAlreadyRunning -> {
                onShowWarningDialog(
                    WarningMessage(
                        title = context.getString(R.string.warning_title),
                        msg = context.getString(
                            R.string.service_download_already_running
                        )
                    )
                )
            }

            MapDownloadStorageError -> {
                onShowWarningDialog(
                    WarningMessage(
                        title = context.getString(R.string.warning_title),
                        msg = context.getString(
                            R.string.service_download_bad_storage
                        )
                    )
                )
            }

            MapNotRepairable -> {
                onShowWarningDialog(
                    WarningMessage(
                        title = context.getString(R.string.warning_title),
                        msg = context.getString(
                            R.string.service_download_repair_error
                        )
                    )
                )
            }

            is MapDownloadPending, is MapUpdatePending -> {
                // Nothing particular to do, the service which fire those events already sends
                // notifications with the progression.
            }

            MissingApiError -> {
                onShowWarningDialog(
                    WarningMessage(
                        title = context.getString(R.string.warning_title),
                        msg = context.getString(
                            R.string.service_download_missing_api
                        )
                    )
                )
            }
        }
    }
}

private suspend fun SnackbarHostState.showSnackbar(
    message: String,
    isLong: Boolean = false,
    actionLabel: String? = null,
): SnackbarResult {
    return showSnackbar(
        message,
        actionLabel = actionLabel,
        duration = if (isLong) SnackbarDuration.Long else SnackbarDuration.Short
    )
}

private fun getNameForMenu(menuItem: MenuItem): Int {
    return when (menuItem) {
        MenuItem.MapList -> R.string.select_map_menu_title
        MenuItem.MapCreate -> R.string.create_menu_title
        MenuItem.Record -> R.string.trails_menu_title
        MenuItem.TrailSearch -> R.string.trail_search_feature_menu
        MenuItem.GpsPro -> R.string.gps_plus_menu_title
        MenuItem.MapImport -> R.string.import_menu_title
        MenuItem.WifiP2p -> R.string.share_menu_title
        MenuItem.Settings -> R.string.settings_menu_title
        MenuItem.Shop -> R.string.shop_menu_title
        MenuItem.About -> R.string.about
    }
}

private fun getIconForMenu(menuItem: MenuItem): Int {
    return when (menuItem) {
        MenuItem.MapList -> R.drawable.ic_menu_gallery
        MenuItem.MapCreate -> R.drawable.ic_terrain_black_24dp
        MenuItem.Record -> R.drawable.folder
        MenuItem.TrailSearch -> R.drawable.ic_baseline_search_24
        MenuItem.GpsPro -> R.drawable.satellite_variant
        MenuItem.MapImport -> R.drawable.import_24dp
        MenuItem.WifiP2p -> R.drawable.ic_share_black_24dp
        MenuItem.Settings -> R.drawable.ic_settings_black_24dp
        MenuItem.Shop -> R.drawable.basket
        MenuItem.About -> R.drawable.help
    }
}

private enum class MenuItem {
    MapList, MapCreate, Record, TrailSearch, GpsPro, MapImport, WifiP2p, Settings, Shop, About
}