package moe.hx030.chargecontrol

object Constants {
    const val CHARGE_START = 0
    const val CHARGE_STOP = 1
    const val BATT_CAPACITY = 2
    const val BATT_CAPACITY_DESIGN = 3
    const val BATT_CYCLES = 4
    const val BATT_STATUS = 5
    const val BATT_TEMP = 6
    const val BATT_CURRENT = 7
    const val BATT_CURRENT_AVG = 8
    const val BATT_VOLTAGE = 9
    const val BATT_VOLTAGE_AVG = 10
    const val CHARGE_TYPE = 101
//    val CHARGE_DONE = 102

    val STR_CHARGING = "Charging"

    val DEFAULTS = HashMap<Int, Int>()
    const val PATH_BASE = "/sys/devices/platform/google,charger"
    const val BATT_PATH_BASE = "/sys/devices/platform/google,battery/power_supply/battery"
    val PATH_MAP = HashMap<Int, String>()
    init {
        DEFAULTS[0] = 0
        DEFAULTS[1] = 100
        PATH_MAP[CHARGE_START] = "$PATH_BASE/charge_start_level"
        PATH_MAP[CHARGE_STOP] = "$PATH_BASE/charge_stop_level"
        PATH_MAP[BATT_CAPACITY] = "$BATT_PATH_BASE/charge_full"
        PATH_MAP[BATT_CAPACITY_DESIGN] = "$BATT_PATH_BASE/charge_full_design"
        PATH_MAP[BATT_CYCLES] = "$BATT_PATH_BASE/cycle_count"
        PATH_MAP[BATT_STATUS] = "$BATT_PATH_BASE/status"
        PATH_MAP[BATT_TEMP] = "$BATT_PATH_BASE/temp"
        PATH_MAP[BATT_CURRENT] = "$BATT_PATH_BASE/current_now"
        PATH_MAP[BATT_CURRENT_AVG] = "$BATT_PATH_BASE/current_avg"
        PATH_MAP[CHARGE_TYPE] = "$BATT_PATH_BASE/charge_type"
//        PATH_MAP[CHARGE_DONE] = "$BATT_PATH_BASE/charge_done"
        PATH_MAP[BATT_VOLTAGE] = "$BATT_PATH_BASE/voltage_now"
        PATH_MAP[BATT_VOLTAGE_AVG] = "$BATT_PATH_BASE/voltage_avg"
    }
}