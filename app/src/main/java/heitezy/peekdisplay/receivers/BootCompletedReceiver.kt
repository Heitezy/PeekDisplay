package heitezy.peekdisplay.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import heitezy.peekdisplay.helpers.Global
import heitezy.peekdisplay.services.ForegroundService

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (
            intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            if (Global.currentAlwaysOnState(context)) {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, ForegroundService::class.java),
                )
            }
        }
    }
}
