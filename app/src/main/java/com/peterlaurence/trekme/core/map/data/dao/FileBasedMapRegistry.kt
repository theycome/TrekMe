package com.peterlaurence.trekme.core.map.data.dao

import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the correspondence between file based map ids and their root folders.
 */
@Singleton
class FileBasedMapRegistry @Inject constructor() {
    private val fileForId = ConcurrentHashMap<UUID, File>()

    fun getRootFolder(id: UUID): File? = fileForId[id]

    fun setRootFolder(id: UUID, file: File) {
        fileForId[id] = file
    }
}