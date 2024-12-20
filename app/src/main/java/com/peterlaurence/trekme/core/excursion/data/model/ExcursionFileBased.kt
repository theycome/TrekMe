package com.peterlaurence.trekme.core.excursion.data.model

import com.peterlaurence.trekme.core.excursion.data.mapper.toDomain
import com.peterlaurence.trekme.core.excursion.domain.model.Excursion
import com.peterlaurence.trekme.core.excursion.domain.model.ExcursionPhoto
import com.peterlaurence.trekme.core.excursion.domain.model.ExcursionType
import com.peterlaurence.trekme.core.excursion.domain.model.ExcursionWaypoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File

class ExcursionFileBased(
    val root: File,
    val config: ExcursionConfig
) : Excursion {
    val waypointsFlow = MutableStateFlow<List<Waypoint>>(emptyList())

    override val id: String
        get() = config.id
    override val title = MutableStateFlow(config.title)
    override val type: ExcursionType
        get() = config.type.toDomain()
    override val description: String
        get() = config.description
    override val waypoints: StateFlow<List<ExcursionWaypoint>>
        get() = waypointsFlow
    override val photos: List<ExcursionPhoto>
        get() = config.photos
    override val isPathEditable: Boolean
        get() = config.isPathEditable
}

@Serializable
data class ExcursionConfig(
    val id: String,
    val title: String,
    @SerialName("description")
    val description: String,
    val type: Type,
    @SerialName("photos")
    val photos: List<Photo> = emptyList(),
    @SerialName("is-path-editable")
    val isPathEditable: Boolean = false
)

@Serializable
enum class Type {
    @SerialName("hike")
    Hike,

    @SerialName("running")
    Running,

    @SerialName("mountain-bike")
    MountainBike,

    @SerialName("travel-bike")
    TravelBike,

    @SerialName("horse-riding")
    HorseRiding,

    @SerialName("aerial")
    Aerial,

    @SerialName("nautical")
    Nautical,

    @SerialName("motorised-vehicle")
    MotorisedVehicle
}
