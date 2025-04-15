package moe.hx030.chargecontrol

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import android.util.Log

class RestoreService : Service() {

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val a = MainActivity()
        a.setContext(this)
        started = true

        val serviceChannel = NotificationChannel("NOTI", "Service Channel", NotificationManager.IMPORTANCE_MIN)
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
        val pi = PendingIntent.getActivity(
            this, 0, Intent(
                this,
                MainActivity::class.java
            ), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification =
            Notification.Builder(this, "NOTI")
                .setContentTitle("ChargeControl")
                .setContentText("Restore service")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.ic_launcher_foreground))
                .setContentIntent(pi)
                .build()
        try {
            if (Build.VERSION.SDK_INT >= 34 && false) {
                startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE)
            } else {
                startForeground(1, notification)
            }
        } catch (e: RuntimeException) {
            Log.e("030-ChargeControl", "failed to start svc", e)
            throw e
        }
        Thread.sleep(5000)
        val ret = super.onStartCommand(intent, flags, startId)
        for (i in 0..1) a.writeValue(i, null)
        stopSelf()
        return ret
    }

    companion object {
        val INTERVAL_MILLIS: Long = (600 * 1000).toLong()
        var started = false
        fun scheduleServiceAlarm(ctx: Context) {
            val alarmManager = ctx.getSystemService(ALARM_SERVICE) as AlarmManager
            val intent = Intent(ctx, RestoreReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(ctx, 1, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

            val triggerAt: Long = System.currentTimeMillis() + INTERVAL_MILLIS

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent
                )
            }

            if (!started) {
                started = true
                val serviceIntent = Intent(ctx, RestoreService::class.java)
                ctx.startForegroundService(serviceIntent)
            }
        }
    }
}