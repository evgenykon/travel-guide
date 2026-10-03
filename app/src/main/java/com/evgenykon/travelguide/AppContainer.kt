package com.evgenykon.travelguide

import android.content.Context
import androidx.room.Room
import com.evgenykon.travelguide.auth.OpenRouterOAuth
import com.evgenykon.travelguide.auth.YandexAuthManager
import com.evgenykon.travelguide.data.backup.BackupRepository
import com.evgenykon.travelguide.data.db.AppDatabase
import com.evgenykon.travelguide.data.prefs.SecureStore
import com.evgenykon.travelguide.data.prefs.SettingsStore
import com.evgenykon.travelguide.data.repo.AiRepository
import com.evgenykon.travelguide.data.repo.HistoryRepository
import com.evgenykon.travelguide.data.repo.PointRepository
import com.evgenykon.travelguide.data.repo.RouteRepository
import com.evgenykon.travelguide.location.LocationProvider
import com.evgenykon.travelguide.network.Network
import com.evgenykon.travelguide.tts.AudioPlayer
import com.evgenykon.travelguide.tts.TtsRepository
import kotlinx.coroutines.flow.MutableStateFlow

class AppContainer(context: Context) {

    val db: AppDatabase = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "travel-guide.db"
    ).fallbackToDestructiveMigration().build()

    val secureStore = SecureStore(context.applicationContext)
    val settingsStore = SettingsStore(context.applicationContext)

    private val network = Network()

    val yandexAuth = YandexAuthManager(secureStore, network.yandexIamApi)
    val openRouterOAuth = OpenRouterOAuth(network.openRouterApi)
    val aiRepository = AiRepository(secureStore, network.openRouterApi)

    val pointRepository = PointRepository(db.pointDao())
    val routeRepository = RouteRepository(db.routeDao(), db.pointDao())
    val historyRepository = HistoryRepository(db.historyDao())
    val backupRepository = BackupRepository(db)

    val audioPlayer = AudioPlayer(context.applicationContext)
    val ttsRepository = TtsRepository(
        context = context.applicationContext,
        auth = yandexAuth,
        api = network.yandexTtsApi,
        player = audioPlayer
    )

    val locationProvider = LocationProvider(context.applicationContext)

    val pendingSharedKey = MutableStateFlow<String?>(null)
    val pendingRouteId = MutableStateFlow<Long?>(null)
}
