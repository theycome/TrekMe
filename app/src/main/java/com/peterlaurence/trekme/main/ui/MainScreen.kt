package com.peterlaurence.trekme.main.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.events.AppEventBus
import com.peterlaurence.trekme.events.GenericMessage.FatalMessage
import com.peterlaurence.trekme.events.GenericMessage.WarningMessage
import com.peterlaurence.trekme.events.gpspro.GpsProEvents
import com.peterlaurence.trekme.events.maparchive.MapArchiveEvents
import com.peterlaurence.trekme.features.common.presentation.ui.dialogs.WarningDialog
import com.peterlaurence.trekme.main.eventhandler.BillingEventHandler
import com.peterlaurence.trekme.main.eventhandler.HandleGenericMessages
import com.peterlaurence.trekme.main.eventhandler.MapArchiveEventHandler
import com.peterlaurence.trekme.main.eventhandler.MapDownloadEventHandler
import com.peterlaurence.trekme.main.eventhandler.RecordingEventHandler
import com.peterlaurence.trekme.main.permission.PermissionRequestHandler
import com.peterlaurence.trekme.main.ui.component.DrawerHeader
import com.peterlaurence.trekme.main.ui.component.HandleBackGesture
import com.peterlaurence.trekme.main.ui.component.MainActivityLifecycleObserver
import com.peterlaurence.trekme.main.ui.navigation.MainGraph
import com.peterlaurence.trekme.main.ui.navigation.navigateToAbout
import com.peterlaurence.trekme.main.ui.navigation.navigateToGpsPro
import com.peterlaurence.trekme.main.ui.navigation.navigateToMapCreation
import com.peterlaurence.trekme.main.ui.navigation.navigateToMapImport
import com.peterlaurence.trekme.main.ui.navigation.navigateToMapList
import com.peterlaurence.trekme.main.ui.navigation.navigateToRecord
import com.peterlaurence.trekme.main.ui.navigation.navigateToSettings
import com.peterlaurence.trekme.main.ui.navigation.navigateToShop
import com.peterlaurence.trekme.main.ui.navigation.navigateToTrailSearch
import com.peterlaurence.trekme.main.ui.navigation.navigateToWifiP2p
import com.peterlaurence.trekme.main.viewmodel.MainActivityViewModel
import com.peterlaurence.trekme.main.viewmodel.RecordingEventHandlerViewModel
import com.peterlaurence.trekme.util.android.activity
import com.peterlaurence.trekme.util.compose.LaunchedEffectWithLifecycle
import kotlinx.coroutines.launch

@Composable
fun MainStateful(
    viewModel: MainActivityViewModel,
    recordingEventHandlerViewModel: RecordingEventHandlerViewModel,
    appEventBus: AppEventBus,
    gpsProEvents: GpsProEvents,
    mapArchiveEvents: MapArchiveEvents,
) {

    val context = LocalContext.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    val mapsInitializing by viewModel.mapsInitializing.collectAsState()

    LaunchedEffectWithLifecycle(viewModel.eventFlow) {
        navController.let(it.navigateAction)
    }

    val snackbarHostState = remember { SnackbarHostState() }

    var warningMessage by remember { mutableStateOf<WarningMessage?>(null) }
    warningMessage?.also {
        WarningDialog(
            title = it.title ?: stringResource(id = R.string.warning_title),
            contentText = it.msg,
            onDismissRequest = { warningMessage = null }
        )
    }

    var fatalMessage by remember { mutableStateOf<FatalMessage?>(null) }
    fatalMessage?.also {
        WarningDialog(
            title = it.title,
            contentText = it.msg,
            onDismissRequest = { context.activity.finish() }
        )
    }

    HandleBackGesture(
        drawerState = drawerState,
        scope = scope,
        navController = navController,
        snackbarHostState = snackbarHostState
    )

    PermissionRequestHandler(
        appEventBus = appEventBus,
        gpsProEvents = gpsProEvents,
        snackbarHostState = snackbarHostState,
        scope = scope
    )

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
        onShowWarningDialog = { warningMessage = it }
    )

    HandleGenericMessages(
        genericMessages = appEventBus.genericMessageEvents,
        scope = scope,
        snackbarHostState = snackbarHostState,
        onShowWarningDialog = { warningMessage = it },
        onShowErrorDialog = { fatalMessage = it }
    )

    MapArchiveEventHandler(
        appEventBus = appEventBus,
        mapArchiveEvents = mapArchiveEvents
    )

    BillingEventHandler(appEventBus)

    val gpsProPurchased by viewModel.gpsProPurchased.collectAsState()
    val menuItems by remember {
        derivedStateOf {
            if (gpsProPurchased) {
                MenuItem.entries
            } else {
                MenuItem.entries.filter { it != MenuItem.GpsPro }
            }
        }
    }

    val selectedItem = remember { mutableStateOf(menuItems[0]) }

    val onMenuItemClick: (MenuItem) -> Unit = {
        scope.launch {
            drawerState.close()
        }
        selectedItem.value = it
        navController.let(it.navigateAction)
    }

    NavigationDrawer(
        drawerState = drawerState,
        snackbarHostState = snackbarHostState,
        navController = navController,
        menuItems = menuItems,
        selectedItem = selectedItem.value,
        mapsInitializing = mapsInitializing,
        onMenuItemClick = onMenuItemClick,
        onMainMenuClick = { scope.launch { drawerState.open() } }
    )

    MainActivityLifecycleObserver(viewModel)

}

@Composable
private fun NavigationDrawer(
    drawerState: DrawerState,
    snackbarHostState: SnackbarHostState,
    navController: NavHostController,
    menuItems: List<MenuItem>,
    selectedItem: MenuItem,
    mapsInitializing: Boolean,
    onMenuItemClick: (MenuItem) -> Unit,
    onMainMenuClick: () -> Unit,
) {

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !mapsInitializing,
        drawerContent = {
            ModalDrawerSheet(
                Modifier
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
            ) {

                DrawerHeader()

                menuItems.forEach { item ->
                    NavigationDrawerItem(
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                        icon = {
                            Icon(
                                painterResource(id = item.drawableId),
                                contentDescription = null
                            )
                        },
                        label = { Text(stringResource(id = item.stringId)) },
                        selected = item == selectedItem,
                        onClick = { onMenuItemClick(item) },
                    )
                }
            }
        },
        content = {
            Box {
                if (!LocalInspectionMode.current) {
                    MainGraph(
                        navController = navController,
                        onMainMenuClick = onMainMenuClick
                    )
                }
                SnackbarHost(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    hostState = snackbarHostState,
                )
            }
        }
    )
}

@Preview
@Composable
private fun NavigationDrawerPreview() {
    NavigationDrawer(
        drawerState = rememberDrawerState(DrawerValue.Open),
        snackbarHostState = remember { SnackbarHostState() },
        navController = rememberNavController(),
        menuItems = MenuItem.entries,
        selectedItem = MenuItem.MapCreate,
        mapsInitializing = true,
        onMenuItemClick = {},
        onMainMenuClick = {},
    )
}

private enum class MenuItem(
    @StringRes val stringId: Int,
    @DrawableRes val drawableId: Int,
    val navigateAction: NavHostController.() -> Unit,
) {
    MapList(
        stringId = R.string.select_map_menu_title,
        drawableId = R.drawable.ic_menu_gallery,
        navigateAction = NavHostController::navigateToMapList
    ),
    MapCreate(
        stringId = R.string.create_menu_title,
        drawableId = R.drawable.ic_terrain_black_24dp,
        navigateAction = NavHostController::navigateToMapCreation
    ),
    Record(
        stringId = R.string.trails_menu_title,
        drawableId = R.drawable.folder,
        navigateAction = NavHostController::navigateToRecord
    ),
    TrailSearch(
        stringId = R.string.trail_search_feature_menu,
        drawableId = R.drawable.ic_baseline_search_24,
        navigateAction = NavHostController::navigateToTrailSearch
    ),
    GpsPro(
        stringId = R.string.gps_plus_menu_title,
        drawableId = R.drawable.satellite_variant,
        navigateAction = NavHostController::navigateToGpsPro
    ),
    MapImport(
        stringId = R.string.import_menu_title,
        drawableId = R.drawable.import_24dp,
        navigateAction = NavHostController::navigateToMapImport
    ),
    WifiP2p(
        stringId = R.string.share_menu_title,
        drawableId = R.drawable.ic_share_black_24dp,
        navigateAction = NavHostController::navigateToWifiP2p
    ),
    Settings(
        stringId = R.string.settings_menu_title,
        drawableId = R.drawable.ic_settings_black_24dp,
        navigateAction = NavHostController::navigateToSettings
    ),
    Shop(
        stringId = R.string.shop_menu_title,
        drawableId = R.drawable.basket,
        navigateAction = NavHostController::navigateToShop
    ),
    About(
        stringId = R.string.about,
        drawableId = R.drawable.help,
        navigateAction = NavHostController::navigateToAbout
    ),
}
