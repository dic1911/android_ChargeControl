package moe.hx030.chargecontrol

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        intent?.action
        context.startForegroundService(Intent(context, RestoreService::class.java))
    }
}