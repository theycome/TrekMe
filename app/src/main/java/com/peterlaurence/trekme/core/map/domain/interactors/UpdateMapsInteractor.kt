package com.peterlaurence.trekme.core.map.domain.interactors

import com.peterlaurence.trekme.core.map.domain.dao.MapLoaderDao
import com.peterlaurence.trekme.core.repositories.map.MapRepository
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.repositories.map.MapListUpdateRepository
import java.io.File
import javax.inject.Inject

class UpdateMapsInteractor @Inject constructor(
    private val mapLoaderDao: MapLoaderDao,
    private val mapRepository: MapRepository,
    private val mapListUpdateRepository: MapListUpdateRepository
) {

    suspend fun updateMaps(dirs: List<File>) : List<Map> {
        val maps = mapLoaderDao.loadMaps(dirs)
        mapRepository.addMaps(maps)
        mapListUpdateRepository.notifyMapListUpdate()

        return maps
    }
}