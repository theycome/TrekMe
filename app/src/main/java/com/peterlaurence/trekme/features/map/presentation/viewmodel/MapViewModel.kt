package com.peterlaurence.trekme.features.map.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.core.location.Location
import com.peterlaurence.trekme.core.location.LocationSource
import com.peterlaurence.trekme.events.AppEventBus
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.orientation.OrientationSource
import com.peterlaurence.trekme.core.settings.RotationMode
import com.peterlaurence.trekme.core.settings.Settings
import com.peterlaurence.trekme.core.repositories.map.MapRepository
import com.peterlaurence.trekme.features.map.domain.interactors.MapInteractor
import com.peterlaurence.trekme.features.map.presentation.events.MapFeatureEvents
import com.peterlaurence.trekme.features.map.presentation.viewmodel.controllers.SnackBarController
import com.peterlaurence.trekme.features.map.presentation.viewmodel.layers.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.api.*
import ovh.plrapps.mapcompose.ui.state.MapState
import java.io.File
import java.io.FileInputStream
import javax.inject.Inject
import ovh.plrapps.mapcompose.core.TileStreamProvider as MapComposeTileStreamProvider

@HiltViewModel
class MapViewModel @Inject constructor(
    mapRepository: MapRepository,
    locationSource: LocationSource,
    orientationSource: OrientationSource,
    mapInteractor: MapInteractor,
    private val settings: Settings,
    private val mapFeatureEvents: MapFeatureEvents,
    private val appEventBus: AppEventBus
) : ViewModel() {
    private val mapStateFlow = MutableSharedFlow<MapState>(1, 0, BufferOverflow.DROP_OLDEST)

    private val _uiState = MutableStateFlow<UiState>(Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val locationFlow: Flow<Location> = locationSource.locationFlow
    val orientationFlow: Flow<Double> = orientationSource.orientationFlow

    private val locationLayer: LocationLayer = LocationLayer(
        viewModelScope,
        settings,
        mapRepository.mapFlow.filterNotNull(),
        mapStateFlow
    )

    private val landmarkLayer: LandmarkLayer = LandmarkLayer(
        viewModelScope,
        mapRepository.mapFlow.filterNotNull(),
        mapStateFlow,
        mapInteractor
    )

    private val markerLayer: MarkerLayer = MarkerLayer(
        viewModelScope,
        mapRepository.mapFlow.filterNotNull(),
        mapStateFlow,
        mapFeatureEvents.markerMoved,
        mapInteractor,
        onMarkerEdit = { marker, mapId, markerId ->
            mapFeatureEvents.postMarkerEditEvent(marker, mapId, markerId)
        }
    )
    private val distanceLayer = DistanceLayer(viewModelScope, mapStateFlow)

    val snackBarController = SnackBarController()

    init {
        mapRepository.mapFlow.map {
            if (it != null) {
                onMapChange(it)
            }
        }.launchIn(viewModelScope)

        settings.getRotationMode().combine(mapStateFlow) { rotMode, mapState ->
            applyRotationMode(mapState, rotMode)
        }.launchIn(viewModelScope)
    }

    /* region TopAppBar events */
    fun onMainMenuClick() {
        appEventBus.openDrawer()
    }
    /* endregion */

    /* region Location layer */
    fun onLocationReceived(location: Location) {
        locationLayer.onLocation(location)
    }

    fun toggleShowOrientation() = viewModelScope.launch {
        settings.toggleOrientationVisibility()
    }

    fun setOrientation(intrinsicAngle: Double, displayRotation: Int) {
        locationLayer.onOrientation(intrinsicAngle, displayRotation)
    }
    /* endregion */

    fun addMarker() {
        markerLayer.addMarker()
    }

    fun addLandmark() {
        landmarkLayer.addLandmark()
    }

    fun toggleDistance() {
        distanceLayer.toggleDistance()
    }

    fun isShowingDistanceFlow(): StateFlow<Boolean> = distanceLayer.isVisible

    fun orientationVisibilityFlow(): Flow<Boolean> = settings.getOrientationVisibility()

    /* region map configuration */
    private suspend fun onMapChange(map: Map) {
        /* Shutdown the previous map state, if any */
        mapStateFlow.replayCache.firstOrNull()?.shutdown()

        val tileSize = map.levelList.firstOrNull()?.tileSize?.width ?: run {
            _uiState.value = Error.EmptyMap
            return
        }

        val tileStreamProvider = makeTileStreamProvider(map)

        val magnifyingFactor = settings.getMagnifyingFactor().first()

        val mapState = MapState(
            map.levelList.size,
            map.widthPx,
            map.heightPx,
            tileSize
        ) {
            magnifyingFactor(magnifyingFactor)
        }.apply {
            addLayer(tileStreamProvider)
        }

        /* region Configuration */
        val maxScale = settings.getMaxScale().first()
        mapState.maxScale = maxScale

        val rotationMode = settings.getRotationMode().first()
        applyRotationMode(mapState, rotationMode)

        mapState.shouldLoopScale = true

        mapState.onMarkerClick { id, x, y ->
            landmarkLayer.onMarkerTap(mapState, map.id, id, x, y)
            markerLayer.onMarkerTap(mapState, map.id, id, x, y)
        }
        /* endregion */

        mapStateFlow.tryEmit(mapState)
        val landmarkLinesState = LandmarkLinesState(mapState, map)
        val distanceLineState = DistanceLineState(mapState)
        val mapUiState = MapUiState(
            mapState,
            landmarkLinesState,
            distanceLineState
        )
        _uiState.value = mapUiState
    }

    private fun applyRotationMode(mapState: MapState, rotationMode: RotationMode) {
        when (rotationMode) {
            RotationMode.NONE -> {
                mapState.rotation = 0f
                mapState.disableRotation()
            }
            RotationMode.FOLLOW_ORIENTATION -> {
                // TODO: implement (take into account orientation)
                mapState.disableRotation()
            }
            RotationMode.FREE -> mapState.enableRotation()
        }
    }

    private fun makeTileStreamProvider(map: Map): MapComposeTileStreamProvider {
        return MapComposeTileStreamProvider { row, col, zoomLvl ->
            val relativePathString =
                "$zoomLvl${File.separator}$row${File.separator}$col${map.imageExtension}"

            try {
                FileInputStream(File(map.directory, relativePathString))
            } catch (e: Exception) {
                null
            }
        }
    }
    /* endregion */

    interface MarkerTapListener {
        fun onMarkerTap(mapState: MapState, mapId: Int, id: String, x: Double, y: Double)
    }
}

sealed interface UiState
data class MapUiState(
    val mapState: MapState,
    val landmarkLinesState: LandmarkLinesState,
    val distanceLineState: DistanceLineState
) : UiState

object Loading : UiState
enum class Error : UiState {
    LicenseError, EmptyMap
}

enum class SnackBarEvent {
    CURRENT_LOCATION_OUT_OF_BOUNDS
}