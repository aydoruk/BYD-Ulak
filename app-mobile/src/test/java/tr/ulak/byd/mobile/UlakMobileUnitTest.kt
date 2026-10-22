package tr.ulak.byd.mobile

import org.json.JSONObject
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UlakMobileUnitTest {

    @Test
    fun testTelemetryDataParsing() {
        val rawJson = """
            {
                "app": "ULAK",
                "vehicle": "BYD Seal U DM-i (2025)",
                "system": "DiLink 3.0 (Android 10)",
                "soc_percent": 84,
                "fuel_percent": 72,
                "aux_12v_voltage": 13.8,
                "cabin_temp_c": 22.5,
                "speed_kmh": 65,
                "drive_mode": "EV"
            }
        """.trimIndent()

        val json = JSONObject(rawJson)

        val socPercent = json.optInt("soc_percent", 0)
        val fuelPercent = json.optInt("fuel_percent", 0)
        val aux12v = json.optDouble("aux_12v_voltage", 0.0).toFloat()
        val cabinTemp = json.optDouble("cabin_temp_c", 0.0).toFloat()
        val speedKmh = json.optInt("speed_kmh", 0)
        val driveMode = json.optString("drive_mode", "EV")

        assertEquals(84, socPercent)
        assertEquals(72, fuelPercent)
        assertEquals(13.8f, aux12v, 0.01f)
        assertEquals(22.5f, cabinTemp, 0.01f)
        assertEquals(65, speedKmh)
        assertEquals("EV", driveMode)
    }

    @Test
    fun testRangeCalculations() {
        val socPercent = 84
        val fuelPercent = 72

        val evRange = (socPercent * 1.08f).roundToInt()
        val hevRange = (fuelPercent * 9.8f).roundToInt()
        val combinedRange = evRange + hevRange

        assertEquals(91, evRange)
        assertEquals(706, hevRange)
        assertEquals(797, combinedRange)
    }

    @Test
    fun testLockCommandJsonFormat() {
        val cmd = JSONObject().put("command", "SET_LOCK").put("locked", false)

        assertEquals("SET_LOCK", cmd.getString("command"))
        assertFalse(cmd.getBoolean("locked"))
    }

    @Test
    fun testAcCommandJsonFormat() {
        val cmd = JSONObject()
            .put("command", "SET_AC")
            .put("enable", true)
            .put("temp", 21.5)

        assertEquals("SET_AC", cmd.getString("command"))
        assertTrue(cmd.getBoolean("enable"))
        assertEquals(21.5, cmd.getDouble("temp"), 0.01)
    }

    @Test
    fun testSeatCommandJsonFormat() {
        val cmd = JSONObject()
            .put("command", "SET_SEAT")
            .put("vent", 2)
            .put("heat", 1)

        assertEquals("SET_SEAT", cmd.getString("command"))
        assertEquals(2, cmd.getInt("vent"))
        assertEquals(1, cmd.getInt("heat"))
    }
}
