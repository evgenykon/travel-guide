package com.evgenykon.travelguide.location

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.evgenykon.travelguide.AppContainer
import com.evgenykon.travelguide.MainActivity
import com.evgenykon.travelguide.R
import com.evgenykon.travelguide.TravelGuideApp
import com.evgenykon.travelguide.data.db.HistoryEntity
import com.evgenykon.travelguide.data.db.PointEntity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class TrackingService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val engine = GeofenceEngine()
    private val speakMutex = Mutex()

    private lateinit var container: AppContainer
    private var pointsCache: List<PointEntity> = emptyList()
    private var visitedCache: Set<Long> = emptySet()
    private var pointsCacheAt = 0L

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            scope.launch { handleLocation(location.latitude, location.longitude) }
        }
    }

    override fun onCreate() {
        super.onCreate()
        container = (application as TravelGuideApp).container
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (!hasLocationPermission()) {
            stopSelf()
            return START_NOT_STICKY
        }

        return try {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                buildNotification(),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                } else {
                    0
                }
            )
            isRunning.value = true
            container.locationProvider.requestUpdates(
                intervalMs = LOCATION_INTERVAL_MS,
                minDistanceMeters = LOCATION_MIN_DISTANCE_M,
                callback = locationCallback
            )
            START_STICKY
        } catch (e: Exception) {
            stopSelf()
            START_NOT_STICKY
        }
    }

    override fun onDestroy() {
        isRunning.value = false
        container.locationProvider.removeUpdates(locationCallback)
        container.ttsRepository.stopPlayback()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private suspend fun handleLocation(lat: Double, lng: Double) {
        val settings = container.settingsStore.settings.first()
        val points = enabledPoints()
            .filter { settings.routeFilterId == null || it.routeId == settings.routeFilterId }
            .filter { it.id !in visitedPointIds() }
        val triggered = engine.onLocation(
            points.map { TrackPoint(it.id, it.lat, it.lng, it.radiusMeters, it.enabled) },
            lat,
            lng
        )

        for (track in triggered) {
            val point = points.firstOrNull { it.id == track.id } ?: continue
            if (point.description.isBlank()) continue

            val routeName = point.routeId?.let { runCatching { container.routeRepository.get(it)?.name }.getOrNull() }
            container.historyRepository.add(
                HistoryEntity(
                    pointId = point.id,
                    pointName = point.name,
                    routeName = routeName,
                    lat = point.lat,
                    lng = point.lng,
                    kind = KIND_ENTER
                )
            )
            visitedCache = visitedCache + point.id

            if (settings.autoPlay) {
                speakMutex.withLock {
                    container.ttsRepository
                        .speak(point.description, settings.voice, settings.speed.toDouble())
                        .onFailure { notifyError(point.name, it.message) }
                }
            }
        }
    }

    private suspend fun enabledPoints(): List<PointEntity> {
        val now = System.currentTimeMillis()
        if (now - pointsCacheAt > POINTS_CACHE_TTL_MS) {
            pointsCache = container.pointRepository.observeEnabled().first()
            visitedCache = container.historyRepository.visitedPointIds().toSet()
            pointsCacheAt = now
        }
        return pointsCache
    }

    private suspend fun visitedPointIds(): Set<Long> {
        enabledPoints()
        return visitedCache
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_tracking),
            NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val stopIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, TrackingService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.notification_tracking_title))
            .setContentText(getString(R.string.notification_tracking_text))
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .addAction(0, getString(R.string.notification_stop), stopIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun notifyError(pointName: String, message: String?) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.notification_error_title, pointName))
            .setContentText(message ?: getString(R.string.notification_error_text))
            .setAutoCancel(true)
            .build()
        val canNotify = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        if (canNotify) {
            NotificationManagerCompat.from(this).notify(ERROR_NOTIFICATION_ID, notification)
        }
    }

    companion object {
        const val ACTION_START = "com.evgenykon.travelguide.action.START_TRACKING"
        const val ACTION_STOP = "com.evgenykon.travelguide.action.STOP_TRACKING"
        const val KIND_ENTER = "ENTER"

        private const val CHANNEL_ID = "tracking"
        private const val NOTIFICATION_ID = 1001
        private const val ERROR_NOTIFICATION_ID = 1002
        private const val LOCATION_INTERVAL_MS = 5_000L
        private const val LOCATION_MIN_DISTANCE_M = 3f
        private const val POINTS_CACHE_TTL_MS = 10_000L

        val isRunning = MutableStateFlow(false)

        fun start(context: Context) {
            val intent = Intent(context, TrackingService::class.java).setAction(ACTION_START)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, TrackingService::class.java).setAction(ACTION_STOP)
            runCatching { context.startService(intent) }
        }
    }
}
