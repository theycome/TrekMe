package com.peterlaurence.trekme.features.maplist.presentation.ui.screens

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.findFragment
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.findNavController
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.features.common.presentation.ui.scrollbar.drawVerticalScrollbar
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.features.common.presentation.ui.theme.backgroundColor
import com.peterlaurence.trekme.features.maplist.presentation.ui.MapListFragment
import com.peterlaurence.trekme.features.maplist.presentation.ui.MapListFragmentDirections
import com.peterlaurence.trekme.features.maplist.presentation.ui.components.GoToMapCreationScreen
import com.peterlaurence.trekme.features.maplist.presentation.ui.components.MapCard
import com.peterlaurence.trekme.features.maplist.presentation.ui.components.PendingScreen
import com.peterlaurence.trekme.features.maplist.presentation.viewmodel.MapListState
import com.peterlaurence.trekme.features.maplist.presentation.viewmodel.MapListViewModel
import com.peterlaurence.trekme.features.maplist.presentation.viewmodel.MapSettingsViewModel
import com.peterlaurence.trekme.features.maplist.presentation.model.MapItem
import com.peterlaurence.trekme.features.maplist.presentation.ui.components.DownloadCard
import java.util.*

@Composable
fun MapListStateful(state: MapListState, intents: MapListIntents) {
    val listState = rememberLazyListState()
    Column {
        if (state.isDownloadPending) {
            DownloadCard(
                Modifier.background(backgroundColor()).padding(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
                state.downloadItem,
                intents::onCancelDownload
            )
        }

        if (state.isMapListLoading && !state.isDownloadPending) {
            PendingScreen()
        } else {
            if (state.mapItems.isNotEmpty()) {
                LazyColumn(
                    Modifier
                        .background(backgroundColor())
                        .drawVerticalScrollbar(listState),
                    state = listState,
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.mapItems, key = { it.mapId }) { map ->
                        AnimatedMapCard(map, intents)
                    }
                }
            } else {
                GoToMapCreationScreen(intents::navigateToMapCreate)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LazyItemScope.AnimatedMapCard(mapItem: MapItem, intents: MapListIntents) {
    MapCard(Modifier.animateItemPlacement(), mapItem, intents)
}


class MapListView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : AbstractComposeView(context, attrs, defStyle) {

    @Composable
    override fun Content() {
        val viewModel: MapListViewModel =
            viewModel(findFragment<MapListFragment>().requireActivity())
        val settingsViewModel: MapSettingsViewModel = viewModel()

        val intents = object : MapListIntents {
            override fun onMapClicked(mapId: UUID) {
                val navController = findNavController()
                if (navController.currentDestination?.id == R.id.mapListFragment) {
                    viewModel.setMap(mapId)
                    navController.navigate(R.id.action_global_mapFragment)
                }
            }

            override fun onMapFavorite(mapId: UUID) {
                viewModel.toggleFavorite(mapId)
            }

            override fun onMapSettings(mapId: UUID) {
                viewModel.onMapSettings(mapId)

                /* Navigate to the MapSettingsFragment*/
                val action =
                    MapListFragmentDirections.actionMapListFragmentToMapSettingsGraph()
                findNavController().navigate(action)
            }

            override fun onSetMapImage(mapId: UUID, uri: Uri) {
                settingsViewModel.setMapImage(mapId, uri)
            }

            override fun onMapDelete(mapId: UUID) {
                viewModel.deleteMap(mapId)
            }

            override fun navigateToMapCreate(showOnBoarding: Boolean) {
                /* First, let the app globally know about the user choice */
                viewModel.onNavigateToMapCreate(showOnBoarding)

                /* Then, navigate */
                val navController = findNavController()
                navController.navigate(R.id.action_global_mapCreateFragment)
            }

            override fun onCancelDownload() {
                viewModel.onCancelDownload()
            }
        }

        val mapListState by viewModel.mapListState
        TrekMeTheme {
            MapListStateful(mapListState, intents)
        }
    }
}

interface MapListIntents {
    fun onMapClicked(mapId: UUID)
    fun onMapFavorite(mapId: UUID)
    fun onMapSettings(mapId: UUID)
    fun onSetMapImage(mapId: UUID, uri: Uri)
    fun onMapDelete(mapId: UUID)
    fun navigateToMapCreate(showOnBoarding: Boolean)
    fun onCancelDownload()
}

@Preview
@Composable
private fun MapCardPreview() {
    val mapList = listOf(
        MapItem(UUID.randomUUID()).apply {
            title = "A map 1"
        },
        MapItem(UUID.randomUUID()).apply {
            title = "A map 2"
        },
        MapItem(UUID.randomUUID()).apply {
            title = "A map 3"
        }
    )

    val intents = object : MapListIntents {
        override fun onMapClicked(mapId: UUID) {
        }

        override fun onMapFavorite(mapId: UUID) {
            mapList.firstOrNull { it.mapId == mapId }?.apply {
                isFavorite = !isFavorite
            }
        }

        override fun onMapSettings(mapId: UUID) {
        }

        override fun onSetMapImage(mapId: UUID, uri: Uri) {
        }

        override fun onMapDelete(mapId: UUID) {
        }

        override fun navigateToMapCreate(showOnBoarding: Boolean) {
        }

        override fun onCancelDownload() {
        }
    }

    TrekMeTheme {
        MapListStateful(MapListState(mapList, false), intents)
    }
}