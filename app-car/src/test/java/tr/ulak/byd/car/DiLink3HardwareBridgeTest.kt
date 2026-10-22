package tr.ulak.byd.car

import org.json.JSONObject
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DiLink3HardwareBridgeTest {

    private lateinit var bridge: DiLink3HardwareBridge

    @BeforeTest
    fun setUp() {
        bridge = DiLink3HardwareBridge()
    }

    @Test
    fun testReadTelemetrySnapshot_containsExpectedKeysAndValues() {
        val snapshot: JSONObject = bridge.readTelemetrySnapshot()

        assertEquals("ULAK", snapshot.getString("app"))
        assertEquals("BYD Seal U DM-i (2025)", snapshot.getString("vehicle"))
        assertEquals("DiLink 3.0 (Android 10)", snapshot.getString("system"))
        assertTrue(snapshot.has("soc_percent"))
        assertTrue(snapshot.has("ev_range_km"))
        assertTrue(snapshot.has("fuel_percent"))
        assertTrue(snapshot.has("hev_range_km"))
        assertTrue(snapshot.has("combined_range_km"))
        assertTrue(snapshot.has("aux_12v_voltage"))
        assertTrue(snapshot.has("cabin_temp_c"))
        assertTrue(snapshot.has("target_temp_c"))
        assertTrue(snapshot.has("ac_active"))
        assertTrue(snapshot.has("is_locked"))
        assertTrue(snapshot.has("sentry_active"))
        assertTrue(snapshot.has("driver_vent"))
        assertTrue(snapshot.has("driver_heat"))
        assertTrue(snapshot.has("speed_kmh"))
        assertTrue(snapshot.has("gear"))
        assertTrue(snapshot.has("drive_mode"))
    }

    @Test
    fun testExecuteRemoteCommand_setAc() {
        val cmd = JSONObject()
            .put("command", "SET_AC")
            .put("enable", true)
            .put("temp", 22.0)

        val result = bridge.executeRemoteCommand(cmd)

        assertTrue(result)
        assertTrue(bridge.acActiveFallback)
        assertEquals(22.0, bridge.targetTempFallback, 0.01)

        val snapshot = bridge.readTelemetrySnapshot()
        assertTrue(snapshot.getBoolean("ac_active"))
        assertEquals(22.0, snapshot.getDouble("target_temp_c"), 0.01)
    }

    @Test
    fun testExecuteRemoteCommand_setLock() {
        val cmd = JSONObject()
            .put("command", "SET_LOCK")
            .put("locked", false)

        val result = bridge.executeRemoteCommand(cmd)

        assertTrue(result)
        assertFalse(bridge.isLockedFallback)

        val snapshot = bridge.readTelemetrySnapshot()
        assertFalse(snapshot.getBoolean("is_locked"))
    }

    @Test
    fun testExecuteRemoteCommand_setSentry() {
        val cmd = JSONObject()
            .put("command", "SET_SENTRY")
            .put("enable", false)

        val result = bridge.executeRemoteCommand(cmd)

        assertTrue(result)
        assertFalse(bridge.sentryActiveFallback)

        val snapshot = bridge.readTelemetrySnapshot()
        assertFalse(snapshot.getBoolean("sentry_active"))
    }

    @Test
    fun testExecuteRemoteCommand_setSeat() {
        val cmd = JSONObject()
            .put("command", "SET_SEAT")
            .put("vent", 3)
            .put("heat", 1)

        val result = bridge.executeRemoteCommand(cmd)

        assertTrue(result)
        assertEquals(3, bridge.driverSeatVentFallback)
        assertEquals(1, bridge.driverSeatHeatFallback)

        val snapshot = bridge.readTelemetrySnapshot()
        assertEquals(3, snapshot.getInt("driver_vent"))
        assertEquals(1, snapshot.getInt("driver_heat"))
    }

    @Test
    fun testExecuteRemoteCommand_invalidCommand() {
        val cmd = JSONObject().put("command", "UNKNOWN_ACTION")
        val result = bridge.executeRemoteCommand(cmd)
        assertFalse(result)
    }
}
