package com.peterlaurence.trekme.features.map.presentation.model

import com.peterlaurence.trekme.core.map.domain.models.Barycenter
import ovh.plrapps.mapcompose.api.BoundingBox
import ovh.plrapps.mapcompose.ui.paths.PathData

data class RouteData(
    val ownerId: String, // this is the id of either the related static route or excursion
    val pathData: PathData,
    val barycenter: Barycenter,
    val boundingBox: BoundingBox,
)