package moe.hx030.chargecontrol

import java.io.BufferedReader
import java.io.InputStreamReader

object Utils {
    fun readValue(type: Int): String {
        val path = Constants.PATH_MAP[type]
        val proc = Runtime.getRuntime().exec("su -c cat $path")
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
        val cmdOutput = output.toString().trim()
        return cmdOutput
    }

    var STATUS: String = "N/A"
    fun isCharging(): Boolean {
        STATUS = readValue(Constants.BATT_STATUS)
        return STATUS.startsWith(Constants.STR_CHARGING)
    }
}