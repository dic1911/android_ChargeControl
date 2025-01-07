package moe.hx030.chargecontrol

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import java.lang.Integer.parseInt

class QSTileService: TileService() {
    val INTERVAL = 10000L

    var listening = false
    lateinit var updateThread: Thread

    // Called when the user adds your tile.
//    override fun onTileAdded() {
//        super.onTileAdded()
//    }

    // Called when your app can update your tile.
    override fun onStartListening() {
        super.onStartListening()
        Log.d("030-bat", "onStartListening")
        listening = true

        var needCreateThread = !this::updateThread.isInitialized
        if (!needCreateThread) needCreateThread = !updateThread.isAlive

        if (needCreateThread) {
            updateThread = Thread { update(false) }
            updateThread.start()
        }
    }

    // Called when your app can no longer update your tile.
    override fun onStopListening() {
        super.onStopListening()
        Log.d("030-bat", "onStopListening")
        listening = false
        updateThread.interrupt()
    }

    // Called when the user taps on your tile in an active or inactive state.
    override fun onClick() {
        super.onClick()
        update(true)
    }

    // Called when the user removes your tile.
//    override fun onTileRemoved() {
//        super.onTileRemoved()
//    }

    private fun update(once: Boolean) {
        while (true) {
            Utils.isCharging()
            val current = parseInt(Utils.readValue(Constants.BATT_CURRENT)).toFloat() / 1000
            qsTile.state = Tile.STATE_ACTIVE
            qsTile.label = Utils.STATUS
            qsTile.subtitle =  "${current}mA"
            qsTile.updateTile()
            Log.d("030-bat", "update -> ${qsTile.label} ${qsTile.state}")
            if (!listening or once) {
                Log.d("030-bat", "not listening")
                break
            }
            try {
                Thread.sleep(INTERVAL)
            } catch (ignored: Exception) {
                if (!listening) break
            }
        }
    }

}