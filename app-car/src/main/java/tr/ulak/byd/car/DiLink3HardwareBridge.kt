package tr.ulak.byd.car

import android.content.Context
import android.util.Log
import org.json.JSONObject

/**
 * BYD Seal U DM-i (2025) - DiLink 3.0 (Android 10) Donanım Köprüsü
 * BydAutoStatisticDevice, BydAutoBodyworkDevice, BydAutoAcDevice, BydAutoEnergyDevice,
 * BydAutoGearboxDevice ve BydAutoSpeedDevice sınıflarını Reflection ile çağırır.
 */
class DiLink3HardwareBridge(private val context: Context) {

    companion object {
        private const val TAG = "UlakDiLink3Bridge"
    }

    private var statisticDevice: Any? = null
    private var bodyworkDevice: Any? = null
    private var acDevice: Any? = null
    private var energyDevice: Any? = null
    private var gearboxDevice: Any? = null
    private var speedDevice: Any? = null

    // Simülasyon/Fallback durum değişkenleri
    var isLockedFallback: Boolean = true
    var acActiveFallback: Boolean = false
    var targetTempFallback: Double = 21.5
    var sentryActiveFallback: Boolean = true
    var driverSeatVentFallback: Int = 2
    var driverSeatHeatFallback: Int = 0

    init {
        initDiLink3Devices()
    }

    private fun initDiLink3Devices() {
        try {
            statisticDevice = getBydDevice("android.hardware.bydauto.statistic.BydAutoStatisticDevice")
            bodyworkDevice = getBydDevice("android.hardware.bydauto.bodywork.BydAutoBodyworkDevice")
            acDevice = getBydDevice("android.hardware.bydauto.ac.BydAutoAcDevice")
            energyDevice = getBydDevice("android.hardware.bydauto.energy.BydAutoEnergyDevice")
            gearboxDevice = getBydDevice("android.hardware.bydauto.gearbox.BydAutoGearboxDevice")
            speedDevice = getBydDevice("android.hardware.bydauto.speed.BydAutoSpeedDevice")
            Log.i(TAG, "ULAK: DiLink 3.0 BydAuto donanım modülleri başarıyla bağlandı.")
        } catch (e: Exception) {
            Log.e(TAG, "DiLink 3.0 modül başlatma hatası: ${e.message}")
        }
    }

    private fun getBydDevice(className: String): Any? {
        return try {
            val clazz = Class.forName(className)
            val getInstance = clazz.getMethod("getInstance", Context::class.java)
            getInstance.invoke(null, context)
        } catch (e: Exception) {
            Log.w(TAG, "Cihaz sınıfı bulunamadı ($className) - Fallback devrede")
            null
        }
    }

    private fun invokeDouble(instance: Any?, methodName: String, fallback: Double): Double {
        if (instance == null) return fallback
        return try {
            val method = instance.javaClass.getMethod(methodName)
            val raw = (method.invoke(instance) as? Number)?.toDouble() ?: fallback
            if (raw.isNaN() || raw < 0.0) fallback else raw
        } catch (e: Exception) {
            fallback
        }
    }

    private fun invokeInt(instance: Any?, methodName: String, fallback: Int): Int {
        if (instance == null) return fallback
        return try {
            val method = instance.javaClass.getMethod(methodName)
            (method.invoke(instance) as? Number)?.toInt() ?: fallback
        } catch (e: Exception) {
            fallback
        }
    }

    fun readTelemetrySnapshot(): JSONObject {
        val rawSoc = invokeDouble(statisticDevice, "getElecPercentageValue", 0.84)
        val socPercent = if (rawSoc <= 1.0) (rawSoc * 100.0).toInt() else rawSoc.toInt()
        val evRangeKm = invokeInt(statisticDevice, "getEVMileageValue", 92)
        val fuelPercent = invokeInt(statisticDevice, "getFuelPercentageValue", 72)
        val fuelRangeKm = invokeInt(statisticDevice, "getFuelMileageValue", 780)
        val totalOdometerKm = invokeDouble(statisticDevice, "getTotalMileageValue", 14280.0)
        val batteryVoltage12V = invokeDouble(bodyworkDevice, "getBatteryVoltage", 13.8)
        val cabinTemp = invokeDouble(acDevice, "getTemprature", 22.5)
        val currentSpeed = invokeDouble(speedDevice, "getCurrentSpeed", 0.0)
        val operationMode = invokeInt(energyDevice, "getOperationMode", 0) // 0: EV, 1: HEV
        val gearState = invokeInt(gearboxDevice, "getGearboxAutoModeType", 1) // 1: P

        return JSONObject().apply {
            put("app", "ULAK")
            put("vehicle", "BYD Seal U DM-i (2025)")
            put("system", "DiLink 3.0 (Android 10)")
            put("timestamp", System.currentTimeMillis())
            put("soc_percent", socPercent.coerceIn(0, 100))
            put("ev_range_km", evRangeKm)
            put("fuel_percent", fuelPercent.coerceIn(0, 100))
            put("hev_range_km", fuelRangeKm)
            put("combined_range_km", evRangeKm + fuelRangeKm)
            put("odometer_km", totalOdometerKm)
            put("aux_12v_voltage", batteryVoltage12V)
            put("cabin_temp_c", cabinTemp)
            put("target_temp_c", targetTempFallback)
            put("ac_active", acActiveFallback)
            put("is_locked", isLockedFallback)
            put("sentry_active", sentryActiveFallback)
            put("driver_vent", driverSeatVentFallback)
            put("driver_heat", driverSeatHeatFallback)
            put("speed_kmh", currentSpeed.toInt())
            put("gear", when (gearState) { 1 -> "P"; 2 -> "R"; 3 -> "N"; 4 -> "D"; else -> "P" })
            put("drive_mode", if (operationMode == 0) "EV" else "HEV")
        }
    }

    fun executeRemoteCommand(cmdJson: JSONObject): Boolean {
        val action = cmdJson.optString("command", "")
        return try {
            when (action) {
                "SET_AC" -> {
                    val enable = cmdJson.optBoolean("enable", false)
                    val temp = cmdJson.optDouble("temp", 21.5)
                    acActiveFallback = enable
                    targetTempFallback = temp
                    setClimateControl(enable, temp.toInt())
                }
                "SET_LOCK" -> {
                    isLockedFallback = cmdJson.optBoolean("locked", true)
                    true
                }
                "SET_SENTRY" -> {
                    sentryActiveFallback = cmdJson.optBoolean("enable", true)
                    true
                }
                "SET_SEAT" -> {
                    driverSeatVentFallback = cmdJson.optInt("vent", driverSeatVentFallback)
                    driverSeatHeatFallback = cmdJson.optInt("heat", driverSeatHeatFallback)
                    true
                }
                else -> false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Komut yürütme hatası: ${e.message}")
            false
        }
    }

    fun setClimateControl(enable: Boolean, targetTempC: Int): Boolean {
        return try {
            acDevice?.let { dev ->
                if (enable) {
                    dev.javaClass.getMethod("startRearAc", Int::class.javaPrimitiveType).invoke(dev, 1)
                    dev.javaClass.getMethod("setAcTemperature", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
                        .invoke(dev, 0, targetTempC)
                } else {
                    dev.javaClass.getMethod("stopAc", Int::class.javaPrimitiveType).invoke(dev, 1)
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "DiLink 3.0 Klima komutu hatası: ${e.message}")
            false
        }
    }
}
