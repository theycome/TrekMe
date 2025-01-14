package com.peterlaurence.trekme.features.common.presentation.ui.models

import com.peterlaurence.trekme.core.map.domain.models.Map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * Created by Ivan Yakushev on 12.01.2025
 */
data class MapUI(
    val id: UUID,
    val name: String,
) {

    companion object {
        suspend operator fun invoke(map: Map): MapUI =
            map.name.map { name ->
                MapUI(id = map.id, name = name)
            }.first()
    }

}

suspend fun List<Map>.toMapUI() =
    map { MapUI(it) }
