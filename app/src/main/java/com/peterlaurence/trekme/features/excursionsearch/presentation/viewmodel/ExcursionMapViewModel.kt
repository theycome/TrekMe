package com.peterlaurence.trekme.features.excursionsearch.presentation.viewmodel

import android.app.Application
import android.content.Intent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.core.billing.domain.model.ExtendedOfferStateOwner
import com.peterlaurence.trekme.core.billing.domain.model.PurchaseState
import com.peterlaurence.trekme.core.excursion.domain.model.ExcursionSearchItem
import com.peterlaurence.trekme.core.excursion.domain.repository.ExcursionRepository
import com.peterlaurence.trekme.core.georecord.domain.model.GeoRecord
import com.peterlaurence.trekme.core.georecord.domain.model.getBoundingBox
import com.peterlaurence.trekme.core.geotools.distanceApprox
import com.peterlaurence.trekme.core.location.domain.model.LatLon
import com.peterlaurence.trekme.core.location.domain.model.Location
import com.peterlaurence.trekme.core.location.domain.model.LocationSource
import com.peterlaurence.trekme.core.map.domain.interactors.GetMapInteractor
import com.peterlaurence.trekme.core.map.domain.interactors.Wgs84ToMercatorInteractor
import com.peterlaurence.trekme.core.map.domain.models.DownloadMapRequest
import com.peterlaurence.trekme.core.map.domain.models.TileResult
import com.peterlaurence.trekme.core.map.domain.models.TileStream
import com.peterlaurence.trekme.core.map.domain.models.intersects
import com.peterlaurence.trekme.core.wmts.domain.dao.TileStreamProviderDao
import com.peterlaurence.trekme.core.wmts.domain.dao.TileStreamReporter
import com.peterlaurence.trekme.core.wmts.domain.model.IgnClassic
import com.peterlaurence.trekme.core.wmts.domain.model.IgnSourceData
import com.peterlaurence.trekme.core.wmts.domain.model.IgnSpainData
import com.peterlaurence.trekme.core.wmts.domain.model.MapSourceData
import com.peterlaurence.trekme.core.wmts.domain.model.OrdnanceSurveyData
import com.peterlaurence.trekme.core.wmts.domain.model.OsmSourceData
import com.peterlaurence.trekme.core.wmts.domain.model.Outdoors
import com.peterlaurence.trekme.core.wmts.domain.model.SwissTopoData
import com.peterlaurence.trekme.core.wmts.domain.model.UsgsData
import com.peterlaurence.trekme.core.wmts.domain.model.WorldStreetMap
import com.peterlaurence.trekme.core.wmts.domain.model.mapSize
import com.peterlaurence.trekme.core.wmts.domain.tools.getMapSpec
import com.peterlaurence.trekme.core.wmts.domain.tools.getNumberOfTiles
import com.peterlaurence.trekme.features.common.domain.interactors.MapExcursionInteractor
import com.peterlaurence.trekme.features.common.domain.util.toMapComposeTileStreamProvider
import com.peterlaurence.trekme.features.common.presentation.ui.mapcompose.Config
import com.peterlaurence.trekme.features.common.presentation.ui.mapcompose.ScaleLimitsConfig
import com.peterlaurence.trekme.features.common.presentation.ui.mapcompose.ignConfig
import com.peterlaurence.trekme.features.common.presentation.ui.mapcompose.ignSpainConfig
import com.peterlaurence.trekme.features.common.presentation.ui.mapcompose.ordnanceSurveyConfig
import com.peterlaurence.trekme.features.common.presentation.ui.mapcompose.osmConfig
import com.peterlaurence.trekme.features.common.presentation.ui.mapcompose.swissTopoConfig
import com.peterlaurence.trekme.features.common.presentation.ui.mapcompose.usgsConfig
import com.peterlaurence.trekme.features.common.presentation.ui.widgets.PositionMarker
import com.peterlaurence.trekme.features.excursionsearch.domain.repository.GeoRecordForSearchItemRepository
import com.peterlaurence.trekme.features.excursionsearch.domain.repository.PendingSearchRepository
import com.peterlaurence.trekme.features.excursionsearch.presentation.viewmodel.layer.MarkerLayer
import com.peterlaurence.trekme.features.excursionsearch.presentation.viewmodel.layer.RouteLayer
import com.peterlaurence.trekme.features.map.domain.models.NormalizedPos
import com.peterlaurence.trekme.features.mapcreate.app.service.download.DownloadService
import com.peterlaurence.trekme.features.mapcreate.domain.repository.DownloadRepository
import com.peterlaurence.trekme.util.ResultL
import com.peterlaurence.trekme.util.checkInternet
import com.peterlaurence.trekme.util.fold
import com.peterlaurence.trekme.util.onFailure
import com.peterlaurence.trekme.util.onLoading
import com.peterlaurence.trekme.util.onSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ovh.plrapps.mapcompose.api.BoundingBox
import ovh.plrapps.mapcompose.api.addLayer
import ovh.plrapps.mapcompose.api.addMarker
import ovh.plrapps.mapcompose.api.centroidX
import ovh.plrapps.mapcompose.api.centroidY
import ovh.plrapps.mapcompose.api.disableFlingZoom
import ovh.plrapps.mapcompose.api.hasMarker
import ovh.plrapps.mapcompose.api.moveMarker
import ovh.plrapps.mapcompose.api.removeAllLayers
import ovh.plrapps.mapcompose.api.scale
import ovh.plrapps.mapcompose.api.scrollTo
import ovh.plrapps.mapcompose.api.setMapBackground
import ovh.plrapps.mapcompose.ui.layout.Fit
import ovh.plrapps.mapcompose.ui.layout.Forced
import ovh.plrapps.mapcompose.ui.state.MapState
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

@HiltViewModel
class ExcursionMapViewModel @Inject constructor(
    locationSource: LocationSource,
    private val getTileStreamProviderDao: TileStreamProviderDao,
    pendingSearchRepository: PendingSearchRepository,
    private val geoRecordForSearchItemRepository: GeoRecordForSearchItemRepository,
    private val wgs84ToMercatorInteractor: Wgs84ToMercatorInteractor,
    private val getMapInteractor: GetMapInteractor,
    private val mapExcursionInteractor: MapExcursionInteractor,
    private val excursionRepository: ExcursionRepository,
    private val downloadRepository: DownloadRepository,
    extendedOfferStateOwner: ExtendedOfferStateOwner,
    private val app: Application
) : ViewModel() {
    val locationFlow: Flow<Location> = locationSource.locationFlow

    private val _uiStateFlow = MutableStateFlow<UiState>(AwaitingSearch)
    val uiStateFlow: StateFlow<UiState> = _uiStateFlow.asStateFlow()
    private val mapStateFlow = _uiStateFlow.filterIsInstance<MapReady>().map { it.mapState }

    val mapSourceDataFlow = MutableStateFlow<MapSourceData>(OsmSourceData(WorldStreetMap))

    private val _excursionSearchFlow = pendingSearchRepository.getSearchResultFlow().stateIn(
        viewModelScope, started = SharingStarted.Eagerly, initialValue = ResultL.loading()
    )

    val geoRecordForSearchFlow =
        geoRecordForSearchItemRepository.getGeoRecordForSearchFlow().stateIn(
            viewModelScope, started = SharingStarted.Eagerly, initialValue = ResultL.loading()
        )

    val extendedOfferFlow = extendedOfferStateOwner.purchaseFlow.map {
        it == PurchaseState.PURCHASED
    }

    private val _events = Channel<Event>(1)
    val event = _events.receiveAsFlow()

    val markerLayer = MarkerLayer(
        scope = viewModelScope,
        excursionItemsFlow = _excursionSearchFlow.mapNotNull { it.getOrNull()?.items },
        mapStateFlow = mapStateFlow,
        wgs84ToMercatorInteractor = wgs84ToMercatorInteractor,
        onExcursionItemClick = { item ->
            viewModelScope.launch {
                geoRecordForSearchItemRepository.postItem(item)
                _events.send(Event.OnMarkerClick)
            }
        }
    )

    val routeLayer = RouteLayer(
        scope = viewModelScope,
        geoRecordFlow = geoRecordForSearchFlow.mapNotNull { it.getOrNull()?.geoRecord },
        mapStateFlow = mapStateFlow,
        wgs84ToMercatorInteractor = wgs84ToMercatorInteractor,
        getMapInteractor = getMapInteractor
    )

    init {
        viewModelScope.launch {
            mapSourceDataFlow.collect {
                changeMapSource(it)
            }
        }

        viewModelScope.launch {
            if (!checkInternet()) {
                _events.send(Event.NoInternet)
            }
        }

        viewModelScope.launch {
            /**
             * This defines the behavior when a new search is done while the map is already displayed.
             * In this case, we want to first show that a search is pending, then automatically
             * scroll to the search location. In case of error, we also inform the user.
             */
            _excursionSearchFlow.collectLatest { searchResult ->
                val previousMapState = _uiStateFlow.value.getMapState()
                if (previousMapState != null) {
                    searchResult.onLoading {
                        setSearchPending(true)
                    }.onFailure {
                        _events.send(Event.SearchError)
                        setSearchPending(false)
                    }.onSuccess { searchData ->
                        val latLon = searchData.location
                        val bb = computeBoundingBox(searchData.items, latLon)
                        if (bb != null) {
                            launch {
                                previousMapState.scrollToBoundingBox(bb)
                            }
                        }
                        setSearchPending(false)
                    }
                }
            }
        }
    }

    fun onMapSourceDataChange(source: MapSourceData) {
        mapSourceDataFlow.value = source
    }

    fun onLocationReceived(location: Location) = viewModelScope.launch {
        /* If there is no MapState, no need to go further */
        val mapState = mapStateFlow.firstOrNull() ?: return@launch

        /* Project lat/lon off UI thread */
        val normalized = withContext(Dispatchers.Default) {
            wgs84ToMercatorInteractor.getNormalized(location.latitude, location.longitude)
        }

        /* Update the position */
        if (normalized != null) {
            updatePosition(mapState, normalized.x, normalized.y)
        }
    }

    fun requiresOffer(mapSourceData: MapSourceData): Boolean {
        return mapSourceData is IgnSourceData && mapSourceData.layer == IgnClassic
    }

    /**
     * Update the position on the map. The first time we update the position, we add the
     * position marker.
     * [x] and [y] are expected to be normalized coordinates.
     */
    private fun updatePosition(mapState: MapState, x: Double, y: Double) {
        if (mapState.hasMarker(positionMarkerId)) {
            mapState.moveMarker(positionMarkerId, x, y)
        } else {
            mapState.addMarker(positionMarkerId, x, y, relativeOffset = Offset(-0.5f, -0.5f)) {
                PositionMarker()
            }
        }
    }

    /**
     * The user has selected a pin, and clicked on the download button in the bottomsheet.
     */
    fun onDownload(withMap: Boolean) = viewModelScope.launch {
        val geoRecordForSearchItem =
            geoRecordForSearchFlow.firstOrNull()?.getOrNull() ?: return@launch

        if (!withMap) {
            _events.send(Event.ExcursionOnlyDownloadStart)
        }

        val result = excursionRepository.putExcursion(
            id = geoRecordForSearchItem.searchItem.id,
            title = geoRecordForSearchItem.searchItem.title,
            type = geoRecordForSearchItem.searchItem.type,
            description = geoRecordForSearchItem.searchItem.description,
            geoRecord = geoRecordForSearchItem.geoRecord
        )

        /* Now that excursion is downloaded, start map download */
        if (withMap) {
            launch {
                scheduleDownload(
                    geoRecord = geoRecordForSearchItem.geoRecord,
                    excursionId = geoRecordForSearchItem.searchItem.id
                )
            }
        } else {
            /* Import the excursion in all maps which intersect the corresponding bounding-box */
            getMapInteractor.getMapList().forEach { map ->
                launch {
                    val excursion = excursionRepository.getExcursion(geoRecordForSearchItem.searchItem.id)
                    val bb = routeLayer.getBoundingBox()
                    if (bb != null && map.intersects(bb) && excursion != null) {
                        mapExcursionInteractor.createExcursionRef(map, excursion)
                    }
                }
            }
        }

        when (result) {
            ExcursionRepository.PutExcursionResult.Ok,
            ExcursionRepository.PutExcursionResult.AlreadyExists,
            ExcursionRepository.PutExcursionResult.Pending -> { /* Do nothing */
            }

            ExcursionRepository.PutExcursionResult.Error -> {
                _events.send(Event.ExcursionDownloadError)
            }
        }
    }

    private suspend fun scheduleDownload(geoRecord: GeoRecord, excursionId: String) {
        val request: DownloadMapRequest = withContext(Dispatchers.Default) {
            val bb = geoRecord.getBoundingBox() ?: return@withContext null
            val topLeft = LatLon(bb.maxLat, bb.minLon)
            val bottomRight = LatLon(bb.minLat, bb.maxLon)
            val p1 = wgs84ToMercatorInteractor.getProjected(topLeft.lat, topLeft.lon)
                ?: return@withContext null
            val p2 = wgs84ToMercatorInteractor.getProjected(bottomRight.lat, bottomRight.lon)
                ?: return@withContext null

            val minLevel = 12
            val maxLevel = 16
            val wmtsConfig = getWmtsConfig(mapSourceDataFlow.value)
            val mapSpec = getMapSpec(minLevel, maxLevel, p1, p2, tileSize = wmtsConfig.tileSize)
            val tileCount = getNumberOfTiles(minLevel, maxLevel, p1, p2)
            val mapSourceData = mapSourceDataFlow.value

            val tileStreamProvider = getTileStreamProviderDao.newTileStreamProvider(
                mapSourceData
            ).getOrNull() ?: return@withContext null

            DownloadMapRequest(
                mapSourceData,
                mapSpec,
                tileCount,
                tileStreamProvider,
                excursionIds = setOf(excursionId)
            )
        } ?: return

        withContext(Dispatchers.Main) {
            downloadRepository.postDownloadMapRequest(request)
            val intent = Intent(app, DownloadService::class.java)
            app.startService(intent)
        }
    }

    /**
     * This is invoked at initialization and when the user changes of layer.
     */
    private suspend fun changeMapSource(mapSourceData: MapSourceData) {
        val previousMapState = _uiStateFlow.value.getMapState()

        /* Shutdown the previous MapState, if any */
        previousMapState?.shutdown()

        val wmtsConfig = getWmtsConfig(mapSourceData)
        val initScaleAndScroll = if (previousMapState != null) {
            ScaleAndScrollConfig(
                scale = previousMapState.scale,
                scroll = NormalizedPos(previousMapState.centroidX, previousMapState.centroidY)
            )
        } else {
            _uiStateFlow.value = AwaitingSearch
            val excursionSearchData = _excursionSearchFlow.first {
                it.fold(
                    onSuccess = { true },
                    onLoading = { false },
                    onFailure = { true }
                )
            }.getOrNull()

            if (excursionSearchData == null) {
                InitConfigError
            } else {
                val latLon = excursionSearchData.location
                val bb = computeBoundingBox(excursionSearchData.items, latLon)
                if (bb != null) {
                    BoundingBoxConfig(bb)
                } else InitConfigError
            }
        }

        /* If no excursions could be found, no need to display the map. */
        if (initScaleAndScroll is InitConfigError) {
            _uiStateFlow.value = Error.NO_EXCURSIONS
            return
        }

        initMapState(mapSourceData, initScaleAndScroll, wmtsConfig)
    }

    private suspend fun initMapState(
        mapSourceData: MapSourceData,
        initScaleAndScroll: InitScaleAndScrollConfig,
        wmtsConfig: WmtsConfig
    ) = coroutineScope {
        /* Display the loading screen while building the new MapState */
        _uiStateFlow.value = LoadingLayer

        val mapState = MapState(
            19, wmtsConfig.mapSize, wmtsConfig.mapSize,
            tileSize = wmtsConfig.tileSize,
            workerCount = 16
        ) {
            /* Apply configuration */
            val mapConfiguration = getScaleAndScrollConfig(mapSourceData)
            mapConfiguration.forEach { conf ->
                when (conf) {
                    is ScaleLimitsConfig -> {
                        val minScale = conf.minScale
                        if (minScale == null) {
                            minimumScaleMode(Fit)
                        } else {
                            minimumScaleMode(Forced(minScale))
                        }
                        conf.maxScale?.also { maxScale -> maxScale(maxScale) }
                    }

                    else -> {} /* Nothing to do */
                }
            }

            if (initScaleAndScroll is ScaleAndScrollConfig) {
                scale(initScaleAndScroll.scale)
                scroll(x = initScaleAndScroll.scroll.x, y = initScaleAndScroll.scroll.y)
            }

            magnifyingFactor(
                if (mapSourceData is OsmSourceData) 1 else 0
            )
        }.apply {
            disableFlingZoom()
            /* Use grey background to contrast with the material 3 top app bar in light mode */
            setMapBackground(Color(0xFFF8F8F8))

            if (initScaleAndScroll is BoundingBoxConfig) {
                launch {
                    scrollToBoundingBox(bb = initScaleAndScroll.bb)
                }
            }
        }

        val tileStreamProvider =
            getTileStreamProviderDao.newTileStreamProvider(mapSourceData, makeReporter())
                .getOrNull()
        _uiStateFlow.value = if (tileStreamProvider != null) {
            mapState.removeAllLayers()
            mapState.addLayer(tileStreamProvider.toMapComposeTileStreamProvider())
            MapReady(mapState, isSearchPending = false)
        } else {
            Error.PROVIDER_OUTAGE
        }
    }

    private suspend fun MapState.scrollToBoundingBox(bb: BoundingBox) {
        scrollTo(bb, padding = Offset(0.2f, 0.2f))
    }

    private suspend fun computeBoundingBox(
        excursionSearchItems: List<ExcursionSearchItem>,
        location: LatLon
    ): BoundingBox? {
        val itemsWithDistance = withContext(Dispatchers.Default) {
            excursionSearchItems.map {
                Pair(it, distanceApprox(it.startLat, it.startLon, location.lat, location.lon))
            }.sortedBy {
                it.second
            }
        }

        val selectedItems = mutableListOf<ExcursionSearchItem>()
        for (itemWithDistance in itemsWithDistance) {
            if (itemWithDistance.second > 2500) {
                if (selectedItems.isNotEmpty()) break
            }
            selectedItems.add(itemWithDistance.first)
        }

        val bb = buildList {
            for (item in selectedItems) {
                add(LatLon(lat = item.startLat, lon = item.startLon))
                add(
                    LatLon(
                        lat = location.lat - (item.startLat - location.lat),
                        lon = location.lon - (item.startLon - location.lon)
                    )
                )
            }
        }

        val normalized = bb.mapNotNull {
            wgs84ToMercatorInteractor.getNormalized(it.lat, it.lon)
        }

        val minX: Double? = normalized.minOfOrNull { it.x }
        val maxX: Double? = normalized.maxOfOrNull { it.x }
        val minY: Double? = normalized.minOfOrNull { it.y }
        val maxY: Double? = normalized.maxOfOrNull { it.y }

        return if (minX != null && maxX != null && minY != null && maxY != null) {
            BoundingBox(minX, minY, maxX, maxY)
        } else null
    }

    private fun makeReporter(): TileStreamReporter {
        return object : TileStreamReporter {
            private var successCnt = AtomicInteger(0)
            private var failureCnt = AtomicInteger(0)

            override fun report(result: TileResult) {
                if (result is TileStream) {
                    if (result.tileStream != null) {
                        successCnt.incrementAndGet()
                    } else {
                        val failCnt = failureCnt.incrementAndGet()
                        val successCnt = successCnt.get()
                        check(failCnt, successCnt)
                    }
                }
            }

            private fun check(failCnt: Int, successCnt: Int) {
                if (failCnt > successCnt) {
                    _uiStateFlow.update {
                        Error.PROVIDER_OUTAGE
                    }
                }
            }
        }
    }

    private fun UiState.getMapState(): MapState? {
        return when (this) {
            is MapReady -> mapState
            else -> null
        }
    }

    private fun getScaleAndScrollConfig(mapSourceData: MapSourceData): List<Config> {
        return when (mapSourceData) {
            is IgnSourceData -> ignConfig
            IgnSpainData -> ignSpainConfig
            OrdnanceSurveyData -> ordnanceSurveyConfig
            is OsmSourceData -> osmConfig
            SwissTopoData -> swissTopoConfig
            UsgsData -> usgsConfig
        }
    }

    private fun getWmtsConfig(mapSourceData: MapSourceData): WmtsConfig {
        return when (mapSourceData) {
            is OsmSourceData -> {
                when (mapSourceData.layer) {
                    Outdoors -> WmtsConfig(512, mapSize * 2)
                    else -> WmtsConfig(256, mapSize)
                }
            }

            else -> WmtsConfig(256, mapSize)
        }
    }

    private fun setSearchPending(pending: Boolean) {
        _uiStateFlow.update { uiState ->
            if (uiState is MapReady) {
                uiState.copy(isSearchPending = pending)
            } else uiState
        }
    }

    private data class WmtsConfig(val tileSize: Int, val mapSize: Int)

    sealed interface Event {
        object OnMarkerClick : Event
        object NoInternet : Event
        object ExcursionOnlyDownloadStart : Event
        object ExcursionDownloadError : Event
        object SearchError : Event
    }
}

private const val positionMarkerId = "position"

private sealed interface InitScaleAndScrollConfig
data class ScaleAndScrollConfig(val scale: Float, val scroll: NormalizedPos): InitScaleAndScrollConfig
data class BoundingBoxConfig(val bb: BoundingBox): InitScaleAndScrollConfig
object InitConfigError : InitScaleAndScrollConfig

sealed interface UiState
object AwaitingSearch : UiState
object LoadingLayer : UiState
data class MapReady(val mapState: MapState, val isSearchPending: Boolean) : UiState
enum class Error : UiState {
    PROVIDER_OUTAGE, NO_EXCURSIONS
}