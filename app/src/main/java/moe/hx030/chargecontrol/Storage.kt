package moe.hx030.chargecontrol

import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import com.google.android.material.snackbar.Snackbar
import java.util.function.Consumer

object Storage {

    fun writeValue(ctx: Context, type: Int?, value: String?, temp: Boolean = false, callback: Consumer<Number>?) {
        val prefs = if (temp) null else ctx.getSharedPreferences("main", MODE_PRIVATE)
        val path = Constants.PATH_MAP[type]
        val key = if (type != null) path?.split("/")?.last() else Constants.UNLIMIT_CHARGE_MULTIPLIER
        var target = value
        if (target == null) {
            target = prefs?.getString(key, Constants.DEFAULTS[type].toString())
        }
        if (target == null) {
            callback?.accept(Int.MIN_VALUE)
            return
        }
        if (type == null) {
            prefs?.edit()?.putString(key, value)?.apply()
            return
        }
        val proc = Runtime.getRuntime().exec("su -c bash -c \"echo $target > $path\"")
        proc.waitFor()

        val ret = proc.exitValue()
        if (ret == 0) {
            Log.d("030-chargectl", "$key set to $target")
            prefs?.edit()?.putString(path?.split("/")?.last(), target)?.apply()
        } else {
            val current = Utils.readValue(type)
            Log.d("030-chargectl", "failed to set value for $key, current=$current")
        }
        callback?.accept(ret)
    }

    fun getUnlimitMultiplier(ctx: Context): Float? {
        return ctx.getSharedPreferences("main", MODE_PRIVATE)
            .getString(Constants.UNLIMIT_CHARGE_MULTIPLIER, "1.5")
            ?.toFloatOrNull()
    }

    fun isLimitOverridden(): Boolean {
        val startLevel = Utils.readValue(Constants.CHARGE_START)
        val stopLevel = Utils.readValue(Constants.CHARGE_STOP)
        return startLevel == "0" && stopLevel == "100"
    }
}