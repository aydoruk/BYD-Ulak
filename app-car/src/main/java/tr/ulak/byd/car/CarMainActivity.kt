package tr.ulak.byd.car

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.net.NetworkInterface

class CarMainActivity : Activity() {

    private lateinit var bridge: DiLink3HardwareBridge
    private lateinit var telemetryText: TextView
    private val uiHandler = Handler(Looper.getMainLooper())

    private val refreshRunnable = object : Runnable {
        override fun run() {
            telemetryText.text = bridge.readTelemetrySnapshot().toString(2)
            uiHandler.postDelayed(this, 2000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bridge = DiLink3HardwareBridge(applicationContext)
        startUlakService()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#060B14"))
            setPadding(48, 36, 48, 36)
            gravity = Gravity.TOP
        }

        val title = TextView(this).apply {
            text = "ULAK ARAÇ • BYD Seal U DM-i (2025) • DiLink 3.0"
            textSize = 24f
            setTextColor(Color.parseColor("#10B981"))
            typeface = Typeface.DEFAULT_BOLD
        }

        val ipAddress = getLocalIpAddress()
        val subtitle = TextView(this).apply {
            text = "Durum: BYD VHAL Köprüsü Aktif | Araç IP: $ipAddress | UDP: 45454 | TCP Komut: 45455"
            textSize = 15f
            setTextColor(Color.parseColor("#22D3EE"))
            setPadding(0, 8, 0, 20)
        }

        telemetryText = TextView(this).apply {
            text = bridge.readTelemetrySnapshot().toString(2)
            textSize = 14f
            setTextColor(Color.parseColor("#E2E8F0"))
            typeface = Typeface.MONOSPACE
            setBackgroundColor(Color.parseColor("#0F172A"))
            setPadding(32, 24, 32, 24)
        }

        val scroll = ScrollView(this).apply {
            addView(telemetryText)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }

        val refreshBtn = Button(this).apply {
            text = "Telemetriyi Yenile ve DiLink 3.0 Servisini Tetikle"
            setOnClickListener {
                startUlakService()
                telemetryText.text = bridge.readTelemetrySnapshot().toString(2)
            }
        }

        root.addView(title)
        root.addView(subtitle)
        root.addView(scroll)
        root.addView(refreshBtn)

        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        uiHandler.post(refreshRunnable)
    }

    override fun onPause() {
        super.onPause()
        uiHandler.removeCallbacks(refreshRunnable)
    }

    private fun startUlakService() {
        val serviceIntent = Intent(this, UlakDiLink3Service::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun getLocalIpAddress(): String {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                val addrs = intf.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (!addr.isLoopbackAddress && addr.hostAddress?.contains(':') == false) {
                        return addr.hostAddress ?: "192.168.43.1"
                    }
                }
            }
            "192.168.43.1"
        } catch (_: Exception) {
            "192.168.43.1"
        }
    }
}
