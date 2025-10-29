package moe.hx030.chargecontrol

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import java.lang.Integer.parseInt

class QSTileService: TileService() {
    val TAG = "030-batile"
    val INTERVAL = 10000L

    var listening = false
    lateinit var updateThread: Thread

    // Called when the user adds your tile.
    override fun onTileAdded() {
        super.onTileAdded()
        // Set tile as active immediately when added
        Log.d(TAG, "onTileAdded")
        qsTile?.state = Tile.STATE_ACTIVE
        qsTile?.updateTile()
    }

    // Called when your app can update your tile.
    override fun onStartListening() {
        super.onStartListening()
        Log.d(TAG, "onStartListening")
        listening = true

        // Always ensure tile is active regardless of charging state
        qsTile?.state = Tile.STATE_ACTIVE
        qsTile?.updateTile()

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
        Log.d(TAG, "onStopListening")
        listening = false
        if (this::updateThread.isInitialized && updateThread.isAlive) {
            updateThread.interrupt()
        }
    }

    // Called when the user taps on your tile in an active or inactive state.
    override fun onClick() {
        super.onClick()
        // Force tile to active state when clicked
        Log.d(TAG, "onClick")
        qsTile?.state = Tile.STATE_ACTIVE
        qsTile?.updateTile()
        update(true)
    }

    // Called when the user removes your tile.
    override fun onTileRemoved() {
        super.onTileRemoved()
        Log.d(TAG, "onTileRemoved")
        listening = false
        if (this::updateThread.isInitialized && updateThread.isAlive) {
            updateThread.interrupt()
        }
    }

    private fun update(once: Boolean) {
        while (true) {
            try {
                Utils.isCharging()
                val current = try {
                    parseInt(Utils.readValue(Constants.BATT_CURRENT)).toFloat() / 1000
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading battery current: ${e.message}")
                    0.0f
                }
                val temp = try {
                    parseInt(Utils.readValue(Constants.BATT_TEMP)).toFloat() / 10
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading battery temp: ${e.message}")
                    0.0f
                }

                // Always keep tile active
                qsTile.state = Tile.STATE_ACTIVE
                qsTile.label = Utils.STATUS
                qsTile.subtitle = "${current}mA / ${temp} ℃"
                qsTile.updateTile()
                Log.d(TAG, "update -> ${qsTile.label} ${qsTile.state}")
            } catch (e: Exception) {
                Log.e(TAG, "Error in update: ${e.message}")
                // Still keep tile active even if update fails
                qsTile?.state = Tile.STATE_ACTIVE
                qsTile?.label = "Error"
                qsTile?.updateTile()
            }

            if (!listening || once) {
                Log.d(TAG, "not listening")
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