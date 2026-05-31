package heitezy.peekdisplay.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.service.quicksettings.TileService
import androidx.core.app.NotificationCompat
import heitezy.peekdisplay.R
import heitezy.peekdisplay.receivers.CombinedServiceReceiver

class ForegroundService : Service() {
    private val combinedServiceReceiver = CombinedServiceReceiver()

    override fun onCreate() {
        super.onCreate()
        CombinedServiceReceiver.compat = packageManager.getInstallerPackageName(packageName)
            ?.hashCode()
            ?: 0
        CombinedServiceReceiver.helper = packageName.hashCode()
        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_POWER_CONNECTED)
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED)
        filter.addAction(Intent.ACTION_SCREEN_OFF)
        filter.addAction(Intent.ACTION_SCREEN_ON)
        registerReceiver(combinedServiceReceiver, filter)
        TileService.requestListeningState(
            this,
            ComponentName(this, AlwaysOnTileService::class.java),
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(combinedServiceReceiver)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentText(resources.getString(R.string.service_text))
            .setSmallIcon(R.drawable.ic_always_on_black)
            .setShowWhen(false)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, notification)
        }
        return START_REDELIVER_INTENT
    }

    override fun onBind(intent: Intent): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    resources.getString(R.string.service_channel),
                    NotificationManager.IMPORTANCE_LOW,
                )
            channel.setShowBadge(false)
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID: String = "service_channel"
    }
}
