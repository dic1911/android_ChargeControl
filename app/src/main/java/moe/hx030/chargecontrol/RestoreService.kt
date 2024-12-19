package moe.hx030.chargecontrol

import android.app.Service
import android.content.Intent
import android.os.IBinder

class RestoreService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val a = MainActivity()
        a.setContext(this)
        Thread.sleep(5000)
        for (i in 0..1) a.writeValue(i, null)
        return super.onStartCommand(intent, flags, startId)
    }
}