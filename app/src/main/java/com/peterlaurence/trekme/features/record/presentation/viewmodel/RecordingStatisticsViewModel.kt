@file:Suppress("BlockingMethodInNonBlockingContext")

package com.peterlaurence.trekme.features.record.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.core.georecord.domain.interactors.GeoRecordInteractor
import com.peterlaurence.trekme.features.common.domain.interactors.RemoveRouteInteractor
import com.peterlaurence.trekme.features.common.domain.model.RecordingDataStateOwner
import com.peterlaurence.trekme.features.common.domain.model.RecordingsAvailable
import com.peterlaurence.trekme.features.common.domain.model.RecordingsState
import com.peterlaurence.trekme.features.record.domain.interactors.ImportRecordingsInteractor
import com.peterlaurence.trekme.features.record.domain.model.RecordingData
import com.peterlaurence.trekme.features.record.presentation.events.RecordEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject


/**
 * This view-model exposes a [recordingDataFlow] flow which holds the the state of the list of
 * recordings.
 *
 * @since 2019/04/21
 */
@HiltViewModel
class RecordingStatisticsViewModel @Inject constructor(
    private val removeRouteInteractor: RemoveRouteInteractor,
    recordingDataStateOwner: RecordingDataStateOwner,
    private val geoRecordInteractor: GeoRecordInteractor,
    private val importRecordingsInteractor: ImportRecordingsInteractor,
    private val eventBus: RecordEventBus,
) : ViewModel() {

    val recordingDataFlow: StateFlow<RecordingsState> = recordingDataStateOwner.recordingDataFlow

    private val newRecordingEventChannel = Channel<Unit>(1)
    val newRecordingEventFlow = newRecordingEventChannel.receiveAsFlow()

    init {
        /* Emit an event when there's a new element in the list */
        viewModelScope.launch {
            recordingDataFlow.filterIsInstance<RecordingsAvailable>().scan(0) { s, l ->
                if (l.recordings.size > s) {
                    newRecordingEventChannel.send(Unit)
                }
                l.recordings.size
            }.collect()
        }
    }

    /**
     * Imports all [Uri]s, and notifies the user when either all imports succeeded, or one of the
     * imports failed.
     */
    fun importRecordings(uriList: List<Uri>) = viewModelScope.launch {
        importRecordingsInteractor.importRecordings(uriList)
    }

    fun getRecordingUri(recordingData: RecordingData): Uri? {
        return geoRecordInteractor.getRecordUri(recordingData.id)
    }

    fun renameRecording(id: UUID, newName: String) {
        viewModelScope.launch {
            geoRecordInteractor.rename(id, newName)
        }
    }

    fun onRequestDeleteRecordings(recordingDataList: List<RecordingData>) = viewModelScope.launch {
        /* Remove recordings */
        launch {
            val success = geoRecordInteractor.delete(recordingDataList.map { it.id })
            /* If only one removal failed, notify the user */
            if (!success) {
                eventBus.postRecordingDeletionFailed()
            }
        }

        /* Remove corresponding routes on existing maps */
        launch {
            val routeIds = recordingDataList.flatMap { it.routeIds }
            removeRouteInteractor.removeRoutesOnMaps(routeIds)
        }
    }
}
