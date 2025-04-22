package moe.hx030.chargecontrol

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.util.Log
import androidx.core.app.NotificationCompat

class ChargeMonitorService : JobService() {
    private lateinit var chargingReceiver: ChargingReceiver

    override fun onCreate() {
        super.onCreate()
        // Create notification channel for Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "charging_service",
                "Charge Control Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        // Create foreground notification
        val notification = NotificationCompat.Builder(this, "charging_service")
            .setContentTitle("Charge Control Active")
            .setContentText("Monitoring charge levels")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        // Start as foreground service
        startForeground(1, notification)

        // Register receiver
        chargingReceiver = ChargingReceiver()
        registerReceiver(chargingReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
//            addAction(Intent.ACTION_BATTERY_CHANGED)
        })
        // temp
        ChargingReceiver.handle(this, null)
//        Utils.scheduleAlarm(this, System.currentTimeMillis() + (15*60*1000), ChargingReceiver::class.java)
        ChargingReceiver.maybeScheduleAlarm(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        scheduleJob(this)
        return START_STICKY
    }

//    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartJob(params: JobParameters?): Boolean {
        Utils.maybeRestore(this)
        return false
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        return false
    }

    override fun onDestroy() {
        unregisterReceiver(chargingReceiver)
        super.onDestroy()
    }

    companion object {
        fun scheduleJob(context: Context) {
            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler

            val jobInfo = JobInfo.Builder(101, ComponentName(context, ChargeMonitorService::class.java))
                .setPeriodic(15 * 60 * 1000) // Every 15 minutes (minimum allowed)
                .setRequiresCharging(true) // Only run when charging
                .build()

            jobScheduler.schedule(jobInfo)
        }
    }
}