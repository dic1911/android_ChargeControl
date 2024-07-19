package moe.hx030.chargecontrol

import android.os.Bundle
import android.view.Menu
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.children
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.snackbar.Snackbar
import moe.hx030.chargecontrol.databinding.ActivityMainBinding
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var fragment: MainFragment
    var startLevel = ""
    var stopLevel = ""
    var fullCapacity = "4049000" // div by 1k
    var fullCapacityDesign = "4049000"
    var cycles = "69"
    var status = "Discharging"
    var isCharging = false
    var temp = "300" // div by 10
    var current = "-300000" // div by 1k
    var currentAvg = "-300000" // div by 1k
    var voltage = "4269167" // div by 1m
    var voltageAvg = "4269167" // div by 1m
    var chargeType = "Unknown"
    var chargeDone = "0"
    var hasSUAccess = false
    var autoRefresh = true
    var autoRefreshBlocked = false
    var autoRefreshThread: Thread? = null
    val autoRefreshRunnable = {
        try {
            while (autoRefresh) {
                while (autoRefreshBlocked) Thread.sleep(3000)
                Thread.sleep(3000)
                runOnUiThread {
                    refresh(true)
                }
            }
        } catch (ignore: Exception) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.fab.setOnClickListener { _ ->
            if (!hasSUAccess) {
                snack(getString(R.string.no_root))
                return@setOnClickListener
            }
            refresh(false)
            snack("Refreshed")
        }

        hasSUAccess = hasSU()
    }

    override fun onResume() {
        super.onResume()
        autoRefreshBlocked = false
        supportActionBar!!.displayOptions = supportActionBar!!.displayOptions or ActionBar.DISPLAY_SHOW_CUSTOM or ActionBar.DISPLAY_SHOW_TITLE
        if (hasSUAccess) {
            refresh(false)
            runAutoRefresh()
        } else {
            val dlg = AlertDialog.Builder(this).setMessage(R.string.no_root)
            dlg.setOnDismissListener { dlg.show() }
            dlg.show()
            snack(getString(R.string.no_root))
        }
    }

    override fun onPause() {
        super.onPause()
        autoRefreshBlocked = true
        if (autoRefreshThread != null && autoRefreshThread!!.state != Thread.State.TERMINATED) {
            autoRefreshThread!!.interrupt()
            autoRefreshThread = null
        }
    }

    override fun onDestroy() {
        if (autoRefreshThread != null && autoRefreshThread!!.state != Thread.State.TERMINATED) {
            autoRefreshThread!!.interrupt()
        }
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        
        menu.children.iterator().forEach { it ->
            if (it.itemId == R.id.toggle_auto_refresh) {
                val sw = it.actionView!!.findViewById<SwitchCompat>(R.id.toggle_auto_refresh_sw)
                sw.setOnCheckedChangeListener { _, value ->
                    autoRefresh = value
                    runAutoRefresh()
                    return@setOnCheckedChangeListener
                }
            }
        }
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    fun setFragment(fm: MainFragment) {
        fragment = fm
    }

    fun hasSU(): Boolean {
        try {
            val proc = Runtime.getRuntime().exec("su -v")
            proc.waitFor()
            return true
        } catch (ex: Exception) {
            return false
        }
    }

    fun runAutoRefresh() {
        if (autoRefreshThread == null || (autoRefresh && autoRefreshThread!!.state != Thread.State.NEW)) {
            autoRefreshThread = Thread(autoRefreshRunnable)
        }
        if (autoRefresh) autoRefreshThread!!.start()
        else autoRefreshThread!!.interrupt()
    }

    fun refresh(auto: Boolean) {
        val start = if (auto) 2 else 0
        for (i in Constants.PATH_MAP.keys) {
            if (i < start) continue
            readValue(i)
        }
    }

    fun readValue(type: Int) {
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
        when (type) {
            Constants.CHARGE_START -> startLevel = cmdOutput
            Constants.CHARGE_STOP -> stopLevel = cmdOutput
            Constants.BATT_CAPACITY -> fullCapacity = cmdOutput
            Constants.BATT_CAPACITY_DESIGN -> fullCapacityDesign = cmdOutput
            Constants.BATT_CYCLES -> cycles = cmdOutput
            Constants.BATT_STATUS -> status = cmdOutput
            Constants.BATT_TEMP -> temp = cmdOutput
            Constants.BATT_CURRENT -> current = cmdOutput
            Constants.BATT_CURRENT_AVG -> currentAvg = cmdOutput
            Constants.BATT_VOLTAGE -> voltage = cmdOutput
            Constants.BATT_VOLTAGE_AVG -> voltageAvg = cmdOutput
            Constants.CHARGE_TYPE -> chargeType = cmdOutput
            Constants.CHARGE_DONE -> chargeDone = cmdOutput
        }
        proc.waitFor()

        if (type >= 8) {
            fragment.refresh()
        }
        isCharging = status.startsWith(Constants.STR_CHARGING)
        if (isCharging && type == Constants.BATT_STATUS) {
            for (i in 101..102) {
                readValue(i)
            }
        }
    }

    fun writeValue(type: Int, value: String) {
        val path = Constants.PATH_MAP[type]
        val proc = Runtime.getRuntime().exec("su -c bash -c \"echo $value > $path\"")
        proc.waitFor()
        Snackbar.make(binding.fab, "returned ${proc.exitValue()}", Snackbar.LENGTH_LONG)
            .setAnchorView(R.id.fab).show()
        readValue(type)
    }

    fun snack(str: String) {
        Snackbar.make(binding.fab, str, Snackbar.LENGTH_LONG)
            .setAnchorView(R.id.fab).show()
    }
}