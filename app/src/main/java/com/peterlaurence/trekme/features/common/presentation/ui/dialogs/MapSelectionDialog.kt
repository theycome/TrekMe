package com.peterlaurence.trekme.features.common.presentation.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.features.common.presentation.ui.models.IndexWithSelection
import com.peterlaurence.trekme.features.common.presentation.ui.models.MapUI
import com.peterlaurence.trekme.features.common.presentation.ui.models.of
import com.peterlaurence.trekme.features.common.presentation.ui.models.toMapUI
import com.peterlaurence.trekme.features.common.presentation.viewmodel.MapSelectionDialogViewModel
import kotlinx.coroutines.flow.map
import java.util.UUID

@Composable
fun MapSelectionDialogStateful(
    viewModel: MapSelectionDialogViewModel,
    onMapSelected: (mapUI: MapUI) -> Unit,
    onDismissRequest: () -> Unit,
) {

    val mapUIList by viewModel.mapList.map(List<Map>::toMapUI)
        .collectAsStateWithLifecycle(emptyList())

    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }

    val lazyListState = rememberLazyListState()

    AlertDialog(
        title = { Text(stringResource(id = R.string.choose_a_map)) },
        text = {
            MapSelectionList(
                mapUIList = mapUIList,
                selectedIndex = selectedIndex,
                lazyListState = lazyListState,
                onMapSelection = { selectedIndex = it }
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val map = mapUIList.getOrNull(selectedIndex)
                    if (map != null) {
                        onMapSelected(map)
                    }
                    onDismissRequest()
                }
            ) {
                Text(stringResource(id = R.string.ok_dialog))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(id = R.string.cancel_dialog_string))
            }
        },
        onDismissRequest = onDismissRequest,
    )
}

@Composable
private fun MapSelectionList(
    mapUIList: List<MapUI>,
    selectedIndex: Int,
    lazyListState: LazyListState,
    onMapSelection: (index: Int) -> Unit,
) {
    LazyColumn(state = lazyListState) {
        itemsIndexed(mapUIList, key = { _, mapUI -> mapUI.id }) { index, map ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .background(Color.of(IndexWithSelection(index, selectedIndex)))
                    .clickable { onMapSelection(index) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = map.name, modifier = Modifier.padding(start = 16.dp))
            }
        }
    }
}

@Preview
@Composable
private fun MapSelectionListPreview() {

    val mapUIList = (0..10).map {
        MapUI(UUID.randomUUID(), "Map $it")
    }

    MapSelectionList(
        mapUIList = mapUIList,
        selectedIndex = 2,
        lazyListState = rememberLazyListState(),
        onMapSelection = { }
    )
}
