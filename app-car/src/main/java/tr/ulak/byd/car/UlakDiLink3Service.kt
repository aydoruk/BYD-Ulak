package tr.ulak.byd.car

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.ServerSocket
import kotlin.time.Duration.Companion.milliseconds

class UlakDiLink3Service : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var hardwareBridge: DiLink3HardwareBridge
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        hardwareBridge = DiLink3HardwareBridge(applicationContext)
        acquireSafeWakeLock()
        startForeground(1001, createNotification())
        startTelemetryBroadcastLoop()
        startCommandServerLoop()
    }

    private fun acquireSafeWakeLock() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "UlakCar::DiLink3TelemetryWakeLock")
        wakeLock?.acquire(12 * 60 * 60 * 1000L)
    }

    private fun startTelemetryBroadcastLoop() {
        serviceScope.launch {
            while (isActive) {
                val snapshot = hardwareBridge.readTelemetrySnapshot()
                val auxVoltage = snapshot.optDouble("aux_12v_voltage", 13.8)

                // 12V Akü Koruma Kalkanı
                if ((auxVoltage < 12.1) && hardwareBridge.sentryActiveFallback) {
                    hardwareBridge.sentryActiveFallback = false
                    snapshot.put("sentry_status", "PAUSED_LOW_12V")
                } else {
                    snapshot.put("sentry_status", if (hardwareBridge.sentryActiveFallback) "ACTIVE" else "STANDBY")
                }

                broadcastToUlakMobile(snapshot.toString())
                delay(1500.milliseconds)
            }
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private fun startCommandServerLoop() {
        serviceScope.launch {
            try {
                val serverSocket = ServerSocket(45455)
                while (isActive) {
                    val client = serverSocket.accept()
                    launch {
                        try {
                            val reader = BufferedReader(InputStreamReader(client.getInputStream()))
                            val line = reader.readLine()
                            if (!line.isNullOrBlank()) {
                                val cmd = JSONObject(line)
                                hardwareBridge.executeRemoteCommand(cmd)
                            }
                        } catch (e: Exception) {
                            Log.w("UlakService", "Komut okuma hatası: ${e.message}")
                        } finally {
                            client.close()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("UlakService", "TCP Komut Sunucusu hatası: ${e.message}")
            }
        }
    }

    private fun broadcastToUlakMobile(jsonPayload: String) {
        try {
            val socket = DatagramSocket()
            socket.broadcast = true
            val data = jsonPayload.toByteArray(Charsets.UTF_8)
            val packet = DatagramPacket(data, data.size, InetAddress.getByName("255.255.255.255"), 45454)
            socket.send(packet)
            socket.close()
        } catch (_: Exception) { }
    }

    private fun createNotification(content: String = "ULAK DiLink 3.0 Köprüsü Aktif • Port 45454/45455"): Notification {
        val channelId = "ulak_dilink3_channel"
        val channel = NotificationChannel(channelId, "Ulak DiLink 3.0 Servisi", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

        return Notification.Builder(this, channelId)
            .setContentTitle("ULAK • BYD Seal U DM-i (DiLink 3.0)")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        if (wakeLock?.isHeld == true) wakeLock?.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
