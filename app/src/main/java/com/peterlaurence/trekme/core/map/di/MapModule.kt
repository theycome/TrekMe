package com.peterlaurence.trekme.core.map.di

import android.app.Application
import com.peterlaurence.trekme.core.georecord.data.dao.GeoRecordParserImpl
import com.peterlaurence.trekme.core.georecord.domain.dao.GeoRecordParser
import com.peterlaurence.trekme.core.map.data.dao.ArchiveMapDaoImpl
import com.peterlaurence.trekme.core.map.data.dao.BeaconsDaoImpl
import com.peterlaurence.trekme.core.map.data.dao.CheckTileStreamProviderDaoImpl
import com.peterlaurence.trekme.core.map.data.dao.ExcursionRefDaoImpl
import com.peterlaurence.trekme.core.map.data.dao.LandmarksDaoImpl
import com.peterlaurence.trekme.core.map.data.dao.MapDeleteDaoImpl
import com.peterlaurence.trekme.core.map.data.dao.MapDownloadDaoImpl
import com.peterlaurence.trekme.core.map.data.dao.MapLoaderDaoFileBased
import com.peterlaurence.trekme.core.map.data.dao.MapRenameDaoImpl
import com.peterlaurence.trekme.core.map.data.dao.MapSaverDaoImpl
import com.peterlaurence.trekme.core.map.data.dao.MapSeekerDaoImpl
import com.peterlaurence.trekme.core.map.data.dao.MapSetThumbnailDaoImpl
import com.peterlaurence.trekme.core.map.data.dao.MapTagDaoImpl
import com.peterlaurence.trekme.core.map.data.dao.MapUpdateDataDaoImpl
import com.peterlaurence.trekme.core.map.data.dao.MarkersDaoImpl
import com.peterlaurence.trekme.core.map.data.dao.RouteDaoImpl
import com.peterlaurence.trekme.core.map.data.dao.UpdateElevationFixDaoImpl
import com.peterlaurence.trekme.core.map.data.dao.UpdateMapSizeInBytesDaoImpl
import com.peterlaurence.trekme.core.map.domain.dao.ArchiveMapDao
import com.peterlaurence.trekme.core.map.domain.dao.BeaconDao
import com.peterlaurence.trekme.core.map.domain.dao.CheckTileStreamProviderDao
import com.peterlaurence.trekme.core.map.domain.dao.ExcursionRefDao
import com.peterlaurence.trekme.core.map.domain.dao.LandmarksDao
import com.peterlaurence.trekme.core.map.domain.dao.MapDeleteDao
import com.peterlaurence.trekme.core.map.domain.dao.MapDownloadDao
import com.peterlaurence.trekme.core.map.domain.dao.MapLoaderDao
import com.peterlaurence.trekme.core.map.domain.dao.MapRenameDao
import com.peterlaurence.trekme.core.map.domain.dao.MapSaverDao
import com.peterlaurence.trekme.core.map.domain.dao.MapSeekerDao
import com.peterlaurence.trekme.core.map.domain.dao.MapSetThumbnailDao
import com.peterlaurence.trekme.core.map.domain.dao.MapTagDao
import com.peterlaurence.trekme.core.map.domain.dao.MapUpdateDataDao
import com.peterlaurence.trekme.core.map.domain.dao.MarkersDao
import com.peterlaurence.trekme.core.map.domain.dao.RouteDao
import com.peterlaurence.trekme.core.map.domain.dao.UpdateElevationFixDao
import com.peterlaurence.trekme.core.map.domain.dao.UpdateMapSizeInBytesDao
import com.peterlaurence.trekme.core.settings.Settings
import com.peterlaurence.trekme.di.DefaultDispatcher
import com.peterlaurence.trekme.di.IoDispatcher
import com.peterlaurence.trekme.di.MainDispatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.json.Json
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MapModule {

    @MapJson
    @Singleton
    @Provides
    fun provideJson(): Json {
        return Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }

    @Singleton
    @Provides
    fun provideMapSaverDao(
        @MainDispatcher mainDispatcher: CoroutineDispatcher,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        @MapJson json: Json,
    ): MapSaverDao {
        return MapSaverDaoImpl(mainDispatcher, ioDispatcher, json)
    }

    @Singleton
    @Provides
    fun provideMarkerDao(
        @MainDispatcher mainDispatcher: CoroutineDispatcher,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        @MapJson json: Json,
    ): MarkersDao {
        return MarkersDaoImpl(mainDispatcher, ioDispatcher, json)
    }

    @Singleton
    @Provides
    fun provideLandmarkDao(
        @MainDispatcher mainDispatcher: CoroutineDispatcher,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        @MapJson json: Json,
    ): LandmarksDao {
        return LandmarksDaoImpl(mainDispatcher, ioDispatcher, json)
    }

    @Singleton
    @Provides
    fun provideBeaconDao(
        @MainDispatcher mainDispatcher: CoroutineDispatcher,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        @MapJson json: Json,
    ): BeaconDao {
        return BeaconsDaoImpl(mainDispatcher, ioDispatcher, json)
    }

    @Singleton
    @Provides
    fun provideMapLoaderDao(
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        mapSaverDao: MapSaverDao,
        @MapJson json: Json,
    ): MapLoaderDao {
        return MapLoaderDaoFileBased(ioDispatcher, mapSaverDao, json)
    }

    @Singleton
    @Provides
    fun provideMapDeleteDao(
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): MapDeleteDao = MapDeleteDaoImpl(ioDispatcher)

    @Singleton
    @Provides
    fun provideMapRenameDao(
        @MainDispatcher mainDispatcher: CoroutineDispatcher,
        mapSaverDao: MapSaverDao,
    ): MapRenameDao {
        return MapRenameDaoImpl(mainDispatcher, mapSaverDao)
    }

    @Singleton
    @Provides
    fun provideMapSetThumbnailDao(
        @DefaultDispatcher defaultDispatcher: CoroutineDispatcher,
        mapSaverDao: MapSaverDao,
        app: Application,
    ): MapSetThumbnailDao {
        return MapSetThumbnailDaoImpl(defaultDispatcher, mapSaverDao, app.contentResolver)
    }

    @Singleton
    @Provides
    fun provideMapSizeComputeDao(
        @DefaultDispatcher defaultDispatcher: CoroutineDispatcher,
        @MapJson json: Json,
    ): UpdateMapSizeInBytesDao = UpdateMapSizeInBytesDaoImpl(defaultDispatcher, json)

    @Singleton
    @Provides
    fun provideArchiveMapDao(
        @DefaultDispatcher defaultDispatcher: CoroutineDispatcher,
        app: Application,
    ): ArchiveMapDao {
        return ArchiveMapDaoImpl(defaultDispatcher, app)
    }

    @Singleton
    @Provides
    fun providesGeoRecordParser(
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): GeoRecordParser {
        return GeoRecordParserImpl(ioDispatcher)
    }

    @Singleton
    @Provides
    fun provideUpdateElevationFixDao(
        @IoDispatcher dispatcher: CoroutineDispatcher,
        @MapJson json: Json,
    ): UpdateElevationFixDao {
        return UpdateElevationFixDaoImpl(dispatcher, json)
    }

    @Singleton
    @Provides
    fun provideMapUpdateDataDao(
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        @MapJson json: Json,
    ): MapUpdateDataDao {
        return MapUpdateDataDaoImpl(ioDispatcher, json)
    }

    @Singleton
    @Provides
    fun provideMapSeekerDao(
        mapLoaderDao: MapLoaderDao,
        mapSaverDao: MapSaverDao,
    ): MapSeekerDao {
        return MapSeekerDaoImpl(mapLoaderDao, mapSaverDao)
    }

    @Singleton
    @Provides
    fun provideRouteDao(
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        @MainDispatcher mainDispatcher: CoroutineDispatcher,
        @MapJson json: Json,
    ): RouteDao {
        return RouteDaoImpl(ioDispatcher, mainDispatcher, json)
    }

    @Singleton
    @Provides
    fun provideMapDownloadDao(
        settings: Settings,
    ): MapDownloadDao {
        return MapDownloadDaoImpl(settings)
    }

    @Singleton
    @Provides
    fun provideExcursionRefDao(
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        @MapJson json: Json,
    ): ExcursionRefDao {
        return ExcursionRefDaoImpl(ioDispatcher, json)
    }

    @Singleton
    @Provides
    fun provideCheckTileStreamProviderDao(): CheckTileStreamProviderDao {
        return CheckTileStreamProviderDaoImpl()
    }

    @Singleton
    @Provides
    fun provideTagVerifierDao(
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): MapTagDao {
        return MapTagDaoImpl(ioDispatcher)
    }
}

/**
 * To be used for serialization / deserialization of file-based maps
 */
@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class MapJson