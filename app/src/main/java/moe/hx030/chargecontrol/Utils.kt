package moe.hx030.chargecontrol

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.util.Consumer
import moe.hx030.chargecontrol.Constants.CHARGE_START
import moe.hx030.chargecontrol.Constants.CHARGE_STOP
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.regex.Pattern
import kotlin.math.roundToLong
import kotlin.reflect.KClass


object Utils {
    fun readValue(type: Int): String {
        val path = Constants.PATH_MAP[type]
//        Log.d("030-read", path.toString())
        return path?.let { readValue(it) }.orEmpty()
    }

    private fun readValue(path: String): String {
        val proc = Runtime.getRuntime().exec("su -c cat $path")
        val cmdOutput = readStdout(proc)
        return cmdOutput
    }

    var STATUS: String = "N/A"
    fun isCharging(): Boolean {
        STATUS = readValue(Constants.BATT_STATUS)
        return STATUS.startsWith(Constants.STR_CHARGING)
    }

    var alarmTime = -1L
    var unlimitAt = Long.MAX_VALUE
    fun readLatestAlarmTime(ctx: Context, callback: Consumer<Long>) {
        Thread {
            try {
                val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "dumpsys alarm | grep pendingSend"));
                val result = readStdout(process)
                val pattern = Pattern.compile("time:(\\d+)")
                val matcher = pattern.matcher(result)

                var earliest = Long.MAX_VALUE
                var found = false

                while (matcher.find()) {
                    val ts = matcher.group(1)?.toLong()
                    if (ts != null) {
                        if (ts < earliest) {
                            earliest = ts
                            found = true
                        }
                    }
                }
                if (found) {
                    val k = Constants.PATH_MAP[CHARGE_STOP]?.split("/")?.last()
                    val target = ctx.getSharedPreferences("main", Context.MODE_PRIVATE).getString(k, Constants.DEFAULTS[CHARGE_STOP].toString())
                        ?.let { Integer.parseInt(it) }
                    val battDiff = 100 - Math.max(getBatteryLevel(ctx), target!!)
                    alarmTime = earliest
                    unlimitAt = (earliest - (battDiff * Storage.getUnlimitMultiplier(ctx)!! * 60 * 1000)).roundToLong() // schedule unlimit at 1.5 * (100 - current perc) mins
                    Log.d("030-chg_alarm", "alarm=$alarmTime unlimitAt=$unlimitAt")
                } else {
                    Log.d("030-chg_alarm", "no alarm detected")
                    alarmTime = -1L
                    unlimitAt = Long.MAX_VALUE
                }
                callback.accept(alarmTime)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun readStdout(proc: Process): String {
        val reader = BufferedReader(
            InputStreamReader(proc.inputStream)
        )
        var read: Int
        val buffer = CharArray(512)
        val output = StringBuffer()
        while ((reader.read(buffer).also { read = it }) > 0) {
            output.append(buffer, 0, read)
        }
        reader.close()
        return output.toString().trim()
    }


    var lastIsCharging = false
    var batteryPercentage = 0
    fun getBatteryLevel(context: Context): Int {
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, intentFilter)
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1

        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        lastIsCharging = status == BatteryManager.BATTERY_STATUS_CHARGING
                || status == BatteryManager.BATTERY_STATUS_FULL

        val chargePlug = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB
        val acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC

        batteryPercentage =
            if (level >= 0 && scale > 0) ((level * 100) / scale)
            else -1

        Log.d("030-batt", "$batteryPercentage% status=$status charging=$lastIsCharging plug=$chargePlug usb=$usbCharge ac=$acCharge")
        return batteryPercentage
    }


    fun scheduleAlarm(ctx: Context, triggerAtMillis: Long, receiver: Class<*>) {
        cancelAlarm(ctx, receiver)
        if (triggerAtMillis < System.currentTimeMillis()) {
            Log.w("030-alarm", "attempt to schedule alarm in past ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(triggerAtMillis)}")
            return
        }
        val alarmManager = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(ctx, receiver)
//        intent.action = "moe.hx030.chargecontrol.UNLIMIT_CHARGING"
        val pendingIntent = PendingIntent.getBroadcast(ctx, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
//            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
//        } else {
//            // Fall back for older Android versions that support setExactAndAllowWhileIdle
        }
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        Log.d("030-chg_alarm",
            "scheduled at ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(triggerAtMillis)}")
    }

    fun cancelAlarm(ctx: Context, receiver: Class<*>) {
        val alarmManager = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(ctx, receiver)
        intent.action = "moe.hx030.chargecontrol.UNLIMIT_CHARGING"

        val pendingIntent = PendingIntent.getBroadcast(ctx, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE)

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d("030-chg_alarm", "Cancelled existing alarm")
        } else {
            Log.d("030-chg_alarm", "No existing alarm to cancel")
        }
    }

    fun maybeRestore(context: Context) {
        val t = System.currentTimeMillis()
        readLatestAlarmTime(context) { nextAlarm ->
            // un-limit if in time range
            if (t in unlimitAt ..< nextAlarm) {
                Log.d("030-chg_restore", "in time range, disable limit")
                Storage.writeValue(context, CHARGE_START, "0", true, null)
                Storage.writeValue(context, CHARGE_STOP, "100", true, null)
            } else {
                Log.d("030-chg_restore", "enable limit")
                Storage.writeValue(context, CHARGE_START, null, false, null)
                Storage.writeValue(context, CHARGE_STOP, null, false, null)
            }
        }
    }
}