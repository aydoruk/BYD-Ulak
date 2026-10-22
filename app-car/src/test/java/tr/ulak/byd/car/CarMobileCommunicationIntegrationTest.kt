package tr.ulak.byd.car

import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class CarMobileCommunicationIntegrationTest {

    @Test
    fun testBidirectionalCarMobileCommunication() = runBlocking {
        val bridge = DiLink3HardwareBridge()

        // 1. CAR MODULE: Create TCP ServerSocket on free port
        val serverSocket = ServerSocket(0)
        val tcpPort = serverSocket.localPort

        val commandReceived = CompletableDeferred<JSONObject>()
        val tcpServerJob = launch(Dispatchers.IO) {
            val clientSocket = serverSocket.accept()
            val reader = BufferedReader(InputStreamReader(clientSocket.getInputStream(), Charsets.UTF_8))
            val line = reader.readLine()
            if (!line.isNullOrBlank()) {
                val cmdJson = JSONObject(line)
                bridge.executeRemoteCommand(cmdJson)
                commandReceived.complete(cmdJson)
            }
            clientSocket.close()
            serverSocket.close()
        }

        // 2. MOBILE MODULE: Start UDP Listener on free port
        val udpSocket = DatagramSocket(0)
        val udpPort = udpSocket.localPort
        val receivedTelemetryDeferred = CompletableDeferred<JSONObject>()

        val mobileUdpListenerJob = launch(Dispatchers.IO) {
            val buffer = ByteArray(4096)
            val packet = DatagramPacket(buffer, buffer.size)
            udpSocket.receive(packet)
            val text = String(packet.data, 0, packet.length, Charsets.UTF_8)
            receivedTelemetryDeferred.complete(JSONObject(text))
            udpSocket.close()
        }

        // Wait a bit to ensure listener is ready
        delay(200)

        // 3. CAR MODULE: Send Telemetry Packet via UDP
        val telemetryPacketSent = bridge.readTelemetrySnapshot()
        val sendSocket = DatagramSocket()
        val data = telemetryPacketSent.toString().toByteArray(Charsets.UTF_8)
        val packet = DatagramPacket(data, data.size, InetAddress.getByName("127.0.0.1"), udpPort)
        sendSocket.send(packet)
        sendSocket.close()

        // Wait for Mobile to receive Car UDP Telemetry
        val receivedTelemetry = withTimeout(2000.milliseconds) { receivedTelemetryDeferred.await() }

        // Assert Mobile received Car's telemetry accurately
        assertEquals("ULAK", receivedTelemetry.getString("app"))
        assertEquals("BYD Seal U DM-i (2025)", receivedTelemetry.getString("vehicle"))
        assertEquals(84, receivedTelemetry.getInt("soc_percent"))
        assertEquals(72, receivedTelemetry.getInt("fuel_percent"))

        // Wait a bit to ensure TCP server is ready
        delay(200)

        // 4. MOBILE MODULE: Send TCP Remote Command to Car
        val mobileCommand = JSONObject()
            .put("command", "SET_AC")
            .put("enable", true)
            .put("temp", 22.5)

        val clientSocket = Socket("127.0.0.1", tcpPort)
        val writer = OutputStreamWriter(clientSocket.getOutputStream(), Charsets.UTF_8)
        writer.write(mobileCommand.toString() + "\n")
        writer.flush()
        clientSocket.close()

        // Wait for Car to receive and execute Mobile's command
        val executedCommand = withTimeout(2000.milliseconds) { commandReceived.await() }

        // Assert Car executed Mobile's command correctly
        assertEquals("SET_AC", executedCommand.getString("command"))
        assertTrue(executedCommand.getBoolean("enable"))
        assertEquals(22.5, executedCommand.getDouble("temp"), 0.01)

        // Verify Car's internal state updated from Mobile's command
        assertTrue(bridge.acActiveFallback)
        assertEquals(22.5, bridge.targetTempFallback, 0.01)

        // Verify next Telemetry snapshot reflects Mobile's command
        val updatedTelemetry = bridge.readTelemetrySnapshot()
        assertTrue(updatedTelemetry.getBoolean("ac_active"))
        assertEquals(22.5, updatedTelemetry.getDouble("target_temp_c"), 0.01)

        // Clean up coroutines
        joinAll(tcpServerJob, mobileUdpListenerJob)
    }

    @Test
    fun testMobileLockUnlockCommandExecution() = runBlocking {
        val bridge = DiLink3HardwareBridge()
        bridge.isLockedFallback = true

        // Mobile sends unlock command
        val lockCmd = JSONObject().put("command", "SET_LOCK").put("locked", false)
        bridge.executeRemoteCommand(lockCmd)

        assertFalse(bridge.isLockedFallback)
        assertFalse(bridge.readTelemetrySnapshot().getBoolean("is_locked"))

        // Mobile sends lock command
        val unlockCmd = JSONObject().put("command", "SET_LOCK").put("locked", true)
        bridge.executeRemoteCommand(unlockCmd)

        assertTrue(bridge.isLockedFallback)
        assertTrue(bridge.readTelemetrySnapshot().getBoolean("is_locked"))
    }
}
