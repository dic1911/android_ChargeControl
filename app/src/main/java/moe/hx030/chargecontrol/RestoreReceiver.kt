package moe.hx030.chargecontrol

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class RestoreReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (Intent.ACTION_BOOT_COMPLETED == intent!!.action) {
            RestoreService.scheduleServiceAlarm(context)

            // Start the charging monitor service
            context.startForegroundService(Intent(context, ChargeMonitorService::class.java))
        }
    }
}