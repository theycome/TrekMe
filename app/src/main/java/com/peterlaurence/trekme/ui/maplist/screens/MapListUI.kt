package com.peterlaurence.trekme.ui.maplist.screens

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.findFragment
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.findNavController
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.ui.maplist.MapListFragment
import com.peterlaurence.trekme.ui.maplist.MapListFragmentDirections
import com.peterlaurence.trekme.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.util.pxToDp
import com.peterlaurence.trekme.viewmodel.maplist.*
import com.peterlaurence.trekme.viewmodel.mapsettings.MapSettingsViewModel

@Composable
fun MapListUI(state: MapListState, intents: MapListIntents) {
    when (state) {
        is Loading -> PendingScreen()
        is MapList -> {
            if (state.mapList.isNotEmpty()) {
                LazyColumn(contentPadding = PaddingValues(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.mapList) { map ->
                        MapCard(map, intents)
                    }
                }
            } else {
                GoToMapCreationScreen(intents::navigateToMapCreate)
            }
        }
    }
}

@Composable
private fun MapCard(mapStub: MapStub, intents: MapListIntents) {
    Card {
        Box(Modifier
                .fillMaxWidth()
                .height(155.dp)
                .clickable { intents.onMapClicked(mapStub.mapId) }
        ) {
            Row {
                Column(Modifier
                        .padding(start = 16.dp, top = 16.dp)
                        .weight(1f)) {
                    Text(
                            text = mapStub.title,
                            fontSize = 24.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                    )
                }
                ImagePlaceHolder(mapStub, intents::onSetMapImage)
            }
            ButtonRow(Modifier.align(Alignment.BottomStart), mapStub.isFavorite,
                    { intents::onMapSettings.invoke(mapStub.mapId) },
                    { intents::onMapDelete.invoke(mapStub.mapId) },
                    { intents::onMapFavorite.invoke(mapStub.mapId) }
            )
        }

    }
}

@Composable
private fun ImagePlaceHolder(mapStub: MapStub, onSetMapImage: (Int, Uri) -> Unit) {
    val addImageDialogState = remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            onSetMapImage(mapStub.mapId, uri)
        }
    }

    Box(modifier = Modifier.padding(top = 16.dp, end = 16.dp)) {
        val image = mapStub.image
        if (image != null) {
            Image(modifier = Modifier
                    .clip(RoundedCornerShape(10.dp)),
                    bitmap = image.asImageBitmap(),
                    contentDescription = null
            )
        } else {
            OutlinedButton(modifier = Modifier.size(pxToDp(256).dp),
                    onClick = { addImageDialogState.value = true },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(0.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .alpha(0.5f),
                            painter = painterResource(id = R.drawable.ancient_map_squared),
                            contentDescription = null)
                    Icon(painter = painterResource(id = R.drawable.add_circle),
                            modifier = Modifier.size(50.dp),
                            tint = colorResource(id = R.color.colorDarkGrey),
                            contentDescription = null)
                }

            }
        }
    }

    if (addImageDialogState.value) {
        ConfirmDialog(addImageDialogState, { launcher.launch("image/*") },
                contentText = stringResource(id = R.string.choose_map_thumbnail),
                confirmButtonText = stringResource(id = R.string.ok_dialog),
                cancelButtonText = stringResource(id = R.string.cancel_dialog_string)
        )
    }
}

@Composable
private fun ButtonRow(modifier: Modifier, isFavorite: Boolean,
                      onMapSettings: () -> Unit,
                      onDelete: () -> Unit,
                      onMapFavorite: () -> Unit
) {
    val deleteDialogState = remember { mutableStateOf(false) }

    Row(modifier.padding(start = 8.dp, end = 0.dp, bottom = 0.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        val color = colorResource(id = R.color.colorAccent)
        TextButton(onClick = onMapSettings) {
            Text(text = stringResource(id = R.string.map_manage_btn_string).uppercase(),
                    color = color)
        }
        Spacer(modifier = Modifier.weight(1f))
        if (isFavorite) {
            IconButton(onClick = onMapFavorite) {
                Icon(painter = painterResource(id = R.drawable.ic_baseline_star_24),
                        tint = color,
                        contentDescription = null)
            }
        } else {
            IconButton(onClick = onMapFavorite) {
                Icon(painter = painterResource(id = R.drawable.ic_baseline_star_border_24),
                        tint = color,
                        contentDescription = null)
            }
        }
        IconButton(onClick = {
            deleteDialogState.value = true
        }) {
            Icon(painter = painterResource(id = R.drawable.ic_delete_forever_black_24dp),
                    tint = color,
                    contentDescription = null)
        }
    }

    if (deleteDialogState.value) {
        ConfirmDialog(deleteDialogState, onDelete,
                contentText = stringResource(id = R.string.map_delete_question),
                confirmButtonText = stringResource(id = R.string.delete_dialog),
                cancelButtonText = stringResource(id = R.string.cancel_dialog_string),
                confirmColor = colorResource(id = R.color.colorAccentRed)
        )
    }
}

@Composable
private fun ConfirmDialog(
        openState: MutableState<Boolean>,
        onConfirmPressed: () -> Unit,
        contentText: String,
        confirmButtonText: String,
        cancelButtonText: String,
        confirmColor: Color = colorResource(id = R.color.colorAccent)
) {
    AlertDialog(
            onDismissRequest = { openState.value = false },
            text = {
                Text(contentText)
            },
            confirmButton = {
                OutlinedButton(
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = confirmColor),
                        onClick = {
                            openState.value = false
                            onConfirmPressed()
                        }) {
                    Text(confirmButtonText)
                }
            },
            dismissButton = {
                OutlinedButton(
                        onClick = {
                            openState.value = false
                        }) {
                    Text(cancelButtonText)
                }
            }
    )
}

@Composable
private fun PendingScreen() {
    Column(Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LinearProgressIndicator()
        Text(
                text = stringResource(id = R.string.loading_maps),
                Modifier.padding(top = 16.dp))
    }
}

@Composable
private fun GoToMapCreationScreen(onButtonCLick: () -> Unit) {
    Column(Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
                text = stringResource(id = R.string.no_maps_found_warning),
                Modifier.padding(bottom = 16.dp))
        Button(onClick = onButtonCLick) {
            Text(text = stringResource(id = R.string.go_to_map_creation_btn))
        }
    }
}

class MapListView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0
) : AbstractComposeView(context, attrs, defStyle) {

    @Composable
    override fun Content() {
        val viewModel: MapListViewModel = viewModel(findFragment<MapListFragment>().requireActivity())
        val settingsViewModel: MapSettingsViewModel = viewModel()

        val intents = object : MapListIntents {
            override fun onMapClicked(mapId: Int) {
                val navController = findNavController()
                if (navController.currentDestination?.id == R.id.mapListFragment) {
                    viewModel.setMap(mapId)
                    val action = MapListFragmentDirections.actionMapListFragmentToMapViewFragment()
                    navController.navigate(action)
                }
            }

            override fun onMapFavorite(mapId: Int) {
                viewModel.toggleFavorite(mapId)
            }

            override fun onMapSettings(mapId: Int) {
                viewModel.onMapSettings(mapId)

                /* Navigate to the MapSettingsFragment*/
                val action = MapListFragmentDirections.actionMapListFragmentToMapSettingsFragment(mapId)
                findNavController().navigate(action)
            }

            override fun onSetMapImage(mapId: Int, uri: Uri) {
                settingsViewModel.setMapImage(mapId, uri)
            }

            override fun onMapDelete(mapId: Int) {
                viewModel.deleteMap(mapId)
            }

            override fun navigateToMapCreate() {
                val navController = findNavController()
                navController.navigate(R.id.action_global_mapCreateFragment)
            }
        }

        val mapListState by viewModel.mapState
        TrekMeTheme {
            MapListUI(mapListState, intents)
        }
    }
}

interface MapListIntents {
    fun onMapClicked(mapId: Int)
    fun onMapFavorite(mapId: Int)
    fun onMapSettings(mapId: Int)
    fun onSetMapImage(mapId: Int, uri: Uri)
    fun onMapDelete(mapId: Int)
    fun navigateToMapCreate()
}