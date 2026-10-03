package com.evgenykon.travelguide.location

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat

enum class ReadinessAction {
    REQUEST_PERMISSION,
    OPEN_APP_SETTINGS,
    OPEN_LOCATION_SETTINGS,
    REQUEST_BATTERY_OPTIMIZATION,
    OPEN_MIUI_AUTOSTART,
    OPEN_MIUI_BATTERY
}

data class ReadinessItem(
    val id: String,
    val title: String,
    val description: String,
    val satisfied: Boolean,
    val required: Boolean,
    val action: ReadinessAction,
    val manual: Boolean = false
)

object TrackingReadiness {

    const val ID_LOCATION = "location"
    const val ID_BACKGROUND_LOCATION = "background_location"
    const val ID_LOCATION_ENABLED = "location_enabled"
    const val ID_NOTIFICATIONS = "notifications"
    const val ID_BATTERY = "battery"
    const val ID_XIAOMI_AUTOSTART = "xiaomi_autostart"
    const val ID_XIAOMI_BATTERY = "xiaomi_battery"

    fun isXiaomi(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return manufacturer.contains("xiaomi") ||
            manufacturer.contains("redmi") ||
            manufacturer.contains("poco")
    }

    fun evaluate(
        context: Context,
        manualAutostart: Boolean,
        manualBattery: Boolean
    ): List<ReadinessItem> {
        val items = mutableListOf<ReadinessItem>()

        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        items += ReadinessItem(
            id = ID_LOCATION,
            title = "Точная геолокация",
            description = "Нужна для отслеживания зон радиусом 10 м",
            satisfied = fine,
            required = true,
            action = ReadinessAction.REQUEST_PERMISSION
        )

        val background = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        items += ReadinessItem(
            id = ID_BACKGROUND_LOCATION,
            title = "Геолокация «Всегда»",
            description = "Доступ к позиции в фоне и при выключенном экране",
            satisfied = background,
            required = true,
            action = ReadinessAction.OPEN_APP_SETTINGS
        )

        val locationEnabled = runCatching {
            val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                manager.isLocationEnabled
            } else {
                manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            }
        }.getOrDefault(false)
        items += ReadinessItem(
            id = ID_LOCATION_ENABLED,
            title = "Геолокация включена",
            description = "GPS или определение по сети должны быть включены",
            satisfied = locationEnabled,
            required = true,
            action = ReadinessAction.OPEN_LOCATION_SETTINGS
        )

        val notifications = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        items += ReadinessItem(
            id = ID_NOTIFICATIONS,
            title = "Уведомления",
            description = "Постоянное уведомление сервиса слежения",
            satisfied = notifications,
            required = true,
            action = ReadinessAction.REQUEST_PERMISSION
        )

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val battery = powerManager.isIgnoringBatteryOptimizations(context.packageName)
        items += ReadinessItem(
            id = ID_BATTERY,
            title = "Батарея без ограничений",
            description = "Система не должна останавливать сервис в фоне",
            satisfied = battery,
            required = true,
            action = ReadinessAction.REQUEST_BATTERY_OPTIMIZATION
        )

        if (isXiaomi()) {
            items += ReadinessItem(
                id = ID_XIAOMI_AUTOSTART,
                title = "Автозапуск (MIUI)",
                description = "Настройки MIUI → Приложения → Разрешения → Автозапуск. " +
                    "Отметьте вручную, если не удалось проверить автоматически",
                satisfied = manualAutostart,
                required = true,
                action = ReadinessAction.OPEN_MIUI_AUTOSTART,
                manual = true
            )
            items += ReadinessItem(
                id = ID_XIAOMI_BATTERY,
                title = "Экономия батареи MIUI",
                description = "MIUI → Батарея → Без ограничений для приложения",
                satisfied = manualBattery,
                required = true,
                action = ReadinessAction.OPEN_MIUI_BATTERY,
                manual = true
            )
        }

        return items
    }

    fun canStart(items: List<ReadinessItem>): Boolean =
        items.filter { it.required }.all { it.satisfied }

    fun openAction(context: Context, item: ReadinessItem) {
        when (item.action) {
            ReadinessAction.OPEN_APP_SETTINGS -> openAppDetails(context)
            ReadinessAction.OPEN_LOCATION_SETTINGS -> openLocationSettings(context)
            ReadinessAction.REQUEST_BATTERY_OPTIMIZATION -> openBatterySettings(context)
            ReadinessAction.OPEN_MIUI_AUTOSTART -> if (!openMiuiAutostart(context)) {
                openAppDetails(context)
            }
            ReadinessAction.OPEN_MIUI_BATTERY -> if (!openMiuiBattery(context)) {
                openAppDetails(context)
            }
            ReadinessAction.REQUEST_PERMISSION -> Unit
        }
    }

    fun openAppDetails(context: Context) {
        runCatching {
            context.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:${context.packageName}")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    private fun openLocationSettings(context: Context) {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    private fun openBatterySettings(context: Context) {
        val direct = Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (runCatching { context.startActivity(direct) }.isFailure) {
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    }

    private fun openMiuiAutostart(context: Context): Boolean {
        val intents = listOf(
            Intent().setComponent(
                ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                )
            ),
            Intent("miui.intent.action.OP_AUTO_START").addCategory(Intent.CATEGORY_DEFAULT)
        )
        return intents.any { intent ->
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(intent) }.isSuccess
        }
    }

    private fun openMiuiBattery(context: Context): Boolean {
        val intents = listOf(
            Intent().setComponent(
                ComponentName(
                    "com.miui.powerkeeper",
                    "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"
                )
            ).apply {
                putExtra("package_name", context.packageName)
                putExtra("package_label", "Eff Travel Guide")
            },
            Intent().setComponent(
                ComponentName(
                    "com.miui.powerkeeper",
                    "com.miui.powerkeeper.ui.HiddenAppsContainerManagementActivity"
                )
            )
        )
        return intents.any { intent ->
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(intent) }.isSuccess
        }
    }
}
