package moe.hx030.chargecontrol

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class ChargingReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("030-chgrecv", "context: ${context != null}, intent: ${intent != null}, act: ${intent?.action}}")
        if (intent?.action == "moe.hx030.chargecontrol.UNLIMIT_CHARGING") {
            Log.d("030-chg_alarm", "Alarm triggered, unlimiting charging")
            Storage.writeValue(context!!, Constants.CHARGE_START, "90", true, null)
            Storage.writeValue(context, Constants.CHARGE_STOP, "100", true, null)
        } else if (Intent.ACTION_POWER_CONNECTED == intent?.action) {
            // check if theres an alarm
            Utils.readLatestAlarmTime(context!!) {
                if (it != -1L) context.let { ctx ->
                    Utils.scheduleAlarm(ctx, Utils.unlimitAt, this::class.java)
                }
            }

        } else if (Intent.ACTION_POWER_DISCONNECTED == intent?.action) {
            // restore values when unplugged
            for (i in 0..1)
                Storage.writeValue(context!!, i, null, false, null)
        } else if (Intent.ACTION_BATTERY_CHANGED.equals(intent?.action)) {
            Utils.getBatteryLevel(context!!)
        } else {
            Log.d("030-charge_recv", "action=${intent?.action.toString()}")
            Utils.maybeRestore(context!!)
        }
    }
}