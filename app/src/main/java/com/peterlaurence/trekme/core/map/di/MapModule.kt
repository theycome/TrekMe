package com.peterlaurence.trekme.core.map.di

import android.app.Application
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.peterlaurence.trekme.core.georecord.data.dao.GeoRecordParserImpl
import com.peterlaurence.trekme.core.georecord.domain.dao.GeoRecordParser
import com.peterlaurence.trekme.core.map.data.dao.*
import com.peterlaurence.trekme.core.map.data.models.RuntimeTypeAdapterFactory
import com.peterlaurence.trekme.core.map.domain.dao.*
import com.peterlaurence.trekme.core.projection.MercatorProjection
import com.peterlaurence.trekme.core.projection.Projection
import com.peterlaurence.trekme.core.projection.UniversalTransverseMercator
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

    @Singleton
    @Provides
    fun provideGson(): Gson {
        val projectionHashMap = object : HashMap<String, Class<out Projection>>() {
            init {
                put(MercatorProjection.NAME, MercatorProjection::class.java)
                put(UniversalTransverseMercator.NAME, UniversalTransverseMercator::class.java)
            }
        }
        val factory = RuntimeTypeAdapterFactory.of(
            Projection::class.java, "projection_name"
        )
        for ((key, value) in projectionHashMap) {
            factory.registerSubtype(value, key)
        }
        return GsonBuilder().serializeNulls().setPrettyPrinting()
            .registerTypeAdapterFactory(factory)
            .create()
    }

    @MapJson
    @Singleton
    @Provides
    fun provideJson(): Json {
        return Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        }
    }

    @Singleton
    @Provides
    fun provideMapSaverDao(
        @MainDispatcher mainDispatcher: CoroutineDispatcher,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        gson: Gson
    ): MapSaverDao {
        return MapSaverDaoImpl(mainDispatcher, ioDispatcher, gson)
    }

    @Singleton
    @Provides
    fun provideMarkerDao(
        @MainDispatcher mainDispatcher: CoroutineDispatcher,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        @MapJson json: Json
    ) : MarkersDao {
        return MarkersDaoImpl(mainDispatcher, ioDispatcher, json)
    }

    @Singleton
    @Provides
    fun provideLandmarkDao(
        @MainDispatcher mainDispatcher: CoroutineDispatcher,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        @MapJson json: Json
    ) : LandmarksDao {
        return LandmarksDaoImpl(mainDispatcher, ioDispatcher, json)
    }

    @Singleton
    @Provides
    fun provideBeaconDao(
        @MainDispatcher mainDispatcher: CoroutineDispatcher,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        @MapJson json: Json
    ): BeaconDao {
        return BeaconsDaoImpl(mainDispatcher, ioDispatcher, json)
    }

    @Singleton
    @Provides
    fun provideMapLoaderDao(
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        mapSaverDao: MapSaverDao,
        gson: Gson,
        @MapJson json: Json
    ): MapLoaderDao {
        return MapLoaderDaoFileBased(ioDispatcher, mapSaverDao, gson, json)
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
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        mapSaverDao: MapSaverDao
    ): MapRenameDao {
        return MapRenameDaoImpl(mainDispatcher, ioDispatcher, mapSaverDao)
    }

    @Singleton
    @Provides
    fun provideMapSetThumbnailDao(
        @DefaultDispatcher defaultDispatcher: CoroutineDispatcher,
        mapSaverDao: MapSaverDao,
        app: Application
    ): MapSetThumbnailDao {
        return MapSetThumbnailDaoImpl(defaultDispatcher, mapSaverDao, app.contentResolver)
    }

    @Singleton
    @Provides
    fun provideMapSizeComputeDao(
        @DefaultDispatcher defaultDispatcher: CoroutineDispatcher,
        @MainDispatcher mainDispatcher: CoroutineDispatcher,
    ): MapSizeComputeDao = MapSizeComputeDaoImpl(defaultDispatcher, mainDispatcher)

    @Singleton
    @Provides
    fun provideArchiveMapDao(
        @DefaultDispatcher defaultDispatcher: CoroutineDispatcher,
        app: Application
    ): ArchiveMapDao {
        return ArchiveMapDaoImpl(defaultDispatcher, app)
    }

    @Singleton
    @Provides
    fun providesGeoRecordParser(@IoDispatcher ioDispatcher: CoroutineDispatcher): GeoRecordParser {
        return GeoRecordParserImpl(ioDispatcher)
    }

    @Singleton
    @Provides
    fun provideMapSource(
        @IoDispatcher dispatcher: CoroutineDispatcher,
        @MapJson json: Json
    ): UpdateElevationFixDao {
        return UpdateElevationFixDaoImpl(dispatcher, json)
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
        @MapJson json: Json
    ): RouteDao {
        return RouteDaoImpl(ioDispatcher, mainDispatcher, json)
    }

    @Singleton
    @Provides
    fun provideMapDownloadDao(
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        settings: Settings,
    ): MapDownloadDao {
        return MapDownloadDaoImpl(ioDispatcher, settings)
    }

    @Singleton
    @Provides
    fun provideExcursionRefDao(
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        @MapJson json: Json
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
    fun provideTagVerifierDao(@IoDispatcher ioDispatcher: CoroutineDispatcher): MapTagDao {
        return MapTagDaoImpl(ioDispatcher)
    }
}

/**
 * To be used for serialization / deserialization of file-based maps
 */
@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class MapJson