package moe.hx030.chargecontrol

object Constants {
    val CHARGE_START = 0
    val CHARGE_STOP = 1
    val BATT_CAPACITY = 2
    val BATT_CAPACITY_DESIGN = 3
    val BATT_CYCLES = 4
    val BATT_STATUS = 5
    val BATT_TEMP = 6
    val BATT_CURRENT = 7
    val BATT_CURRENT_AVG = 8
    val BATT_VOLTAGE = 9
    val BATT_VOLTAGE_AVG = 10
    val CHARGE_TYPE = 101
    val CHARGE_DONE = 102

    val STR_CHARGING = "Charging"

    val PATH_MAP = HashMap<Int, String>()
    init {
        PATH_MAP[CHARGE_START] = "/sys/devices/platform/soc/soc:google,charger/charge_start_level"
        PATH_MAP[CHARGE_STOP] = "/sys/devices/platform/soc/soc:google,charger/charge_stop_level"
        PATH_MAP[BATT_CAPACITY] = "/sys/devices/platform/soc/soc:google,battery/power_supply/battery/charge_full"
        PATH_MAP[BATT_CAPACITY_DESIGN] = "/sys/devices/platform/soc/soc:google,battery/power_supply/battery/charge_full_design"
        PATH_MAP[BATT_CYCLES] = "/sys/devices/platform/soc/soc:google,battery/power_supply/battery/cycle_count"
        PATH_MAP[BATT_STATUS] = "/sys/devices/platform/soc/soc:google,battery/power_supply/battery/status"
        PATH_MAP[BATT_TEMP] = "/sys/devices/platform/soc/soc:google,battery/power_supply/battery/temp"
        PATH_MAP[BATT_CURRENT] = "/sys/devices/platform/soc/soc:google,battery/power_supply/battery/current_now"
        PATH_MAP[BATT_CURRENT_AVG] = "/sys/devices/platform/soc/soc:google,battery/power_supply/battery/current_avg"
        PATH_MAP[CHARGE_TYPE] = "/sys/devices/platform/soc/soc:google,battery/power_supply/battery/charge_type"
        PATH_MAP[CHARGE_DONE] = "/sys/devices/platform/soc/soc:google,battery/power_supply/battery/charge_done"
        PATH_MAP[BATT_VOLTAGE] = "/sys/devices/platform/soc/soc:google,battery/power_supply/battery/voltage_now"
        PATH_MAP[BATT_VOLTAGE_AVG] = "/sys/devices/platform/soc/soc:google,battery/power_supply/battery/voltage_avg"
    }
}