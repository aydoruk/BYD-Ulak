@file:Suppress("TrailingComma")

package tr.ulak.byd.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Socket
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.time.Duration.Companion.milliseconds

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF10B981),
                    secondary = Color(0xFF06B6D4),
                    background = Color(0xFF060B14),
                    surface = Color(0xFF0F172A),
                ),
            ) {
                UlakS26UltraApp()
            }
        }
    }
}

@Composable
fun UlakS26UltraApp() {
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    var carIp by remember { mutableStateOf("192.168.43.1") }
    var liveConnected by remember { mutableStateOf(value = false) }

    // Telemetri & Kontrol State'leri
    var isLocked by remember { mutableStateOf(value = true) }
    var acActive by remember { mutableStateOf(value = false) }
    var targetTemp by remember { mutableFloatStateOf(21.5f) }
    var cabinTemp by remember { mutableFloatStateOf(23.4f) }
    var sentryActive by remember { mutableStateOf(value = true) }
    var driverSeatVent by remember { mutableIntStateOf(2) }
    var driverSeatHeat by remember { mutableIntStateOf(0) }
    var passengerSeatVent by remember { mutableIntStateOf(1) }
    var socPercent by remember { mutableIntStateOf(84) }
    var fuelPercent by remember { mutableIntStateOf(72) }
    var aux12v by remember { mutableFloatStateOf(13.8f) }
    var speedKmh by remember { mutableIntStateOf(0) }
    var powerKw by remember { mutableFloatStateOf(0.4f) }
    var driveMode by remember { mutableStateOf("EV") }
    var simulating by remember { mutableStateOf(value = false) }
    var selectedCam by remember { mutableStateOf("Ön Geniş (AVM)") }
    var intercomActive by remember { mutableStateOf(value = false) }
    var cabinListen by remember { mutableStateOf(value = false) }

    // UDP 45454 Dinleyici (UlakCar_DiLink3.apk'dan gelen gerçek zamanlı telemetri)
    LaunchedEffect(Unit) {
        launch(Dispatchers.IO) {
            try {
                val socket = DatagramSocket(45454)
                val buffer = ByteArray(4096)
                while (isActive) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)
                    val text = String(packet.data, 0, packet.length, Charsets.UTF_8)
                    val json = JSONObject(text)
                    carIp = packet.address.hostAddress ?: carIp
                    liveConnected = true
                    socPercent = json.optInt("soc_percent", socPercent)
                    fuelPercent = json.optInt("fuel_percent", fuelPercent)
                    aux12v = json.optDouble("aux_12v_voltage", 13.8).toFloat()
                    cabinTemp = json.optDouble("cabin_temp_c", 23.0).toFloat()
                    speedKmh = json.optInt("speed_kmh", 0)
                    driveMode = json.optString("drive_mode", driveMode)
                }
            } catch (_: Exception) { }
        }
    }

    // Sürüş Simülasyonu Döngüsü
    LaunchedEffect(simulating, driveMode) {
        while (simulating && isActive) {
            val t = System.currentTimeMillis() / 900.0
            speedKmh = (72 + (sin(t) * 42)).roundToInt().coerceIn(0, 175)
            powerKw = ((sin(t * 1.3) * 55) + if (driveMode == "Sport") 45 else 18).toFloat()
            delay(800.milliseconds)
        }
        if (!simulating) {
            speedKmh = 0
            powerKw = if (acActive) 1.8f else 0.4f
        }
    }

    fun sendCarCommand(cmd: JSONObject) {
        scope.launch(Dispatchers.IO) {
            try {
                val s = Socket(carIp, 45455)
                val writer = OutputStreamWriter(s.getOutputStream(), Charsets.UTF_8)
                writer.write(cmd.toString() + "\n")
                writer.flush()
                s.close()
            } catch (_: Exception) { }
        }
    }

    val evRange = (socPercent * 1.08f).roundToInt()
    val hevRange = (fuelPercent * 9.8f).roundToInt()
    val combinedRange = evRange + hevRange
    val tabs = listOf("Telemetri", "Kamera", "Konfor", "Yolculuk", "DiLink 3.0")

    Scaffold(
        containerColor = Color(0xFF060B14),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0B1324))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "ULAK",
                                color = Color(0xFF10B981),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF06B6D4).copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    text = if (liveConnected) "DiLink 3.0 CANLI" else "DiLink 3.0 • S26 Ultra",
                                    color = Color(0xFF22D3EE),
                                    fontSize = 11.sp,
                                )
                            }
                        }
                        Text(
                            text = "BYD Seal U DM-i (2025) • ${aux12v}V 12V Akü • $combinedRange km",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                        )
                    }

                    Button(
                        onClick = {
                            isLocked = !isLocked
                            sendCarCommand(JSONObject().put("command", "SET_LOCK").put("locked", isLocked))
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isLocked) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFEF4444).copy(alpha = 0.25f),
                        ),
                    ) {
                        Text(
                            text = if (isLocked) "KİLİTLİ" else "AÇIK",
                            color = if (isLocked) Color(0xFF34D399) else Color(0xFFF87171),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF0B1324)) {
                tabs.forEachIndexed { index, title ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (selectedTab == index) Color(0xFF10B981) else Color(0xFF475569)),
                            )
                        },
                        label = { Text(title, fontSize = 11.sp) },
                    )
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (selectedTab == 0) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "DM-i ÇİFT ENERJİ TELEMETRİSİ (DiLink 3.0 VHAL)",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("18.3 kWh LFP Blade", color = Color(0xFF34D399), fontSize = 12.sp)
                                    Text("%$socPercent • $evRange km EV", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("1.5L Xiaoyun Benzin", color = Color(0xFFF59E0B), fontSize = 12.sp)
                                    Text("%$fuelPercent • $hevRange km HEV", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Slider(
                                value = socPercent.toFloat(),
                                onValueChange = { socPercent = it.roundToInt() },
                                valueRange = 10f..100f
                            )
                            Text(
                                "Toplam Karma Menzil: $combinedRange km • Hücre ΔV: 4 mV • SoH: %99.8",
                                color = Color(0xFF22D3EE),
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("EHS HİBRİT GÜÇ OSİLOSKOPU", color = Color(0xFF22D3EE), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Button(
                                    onClick = { simulating = !simulating },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (simulating) Color(0xFFEF4444) else Color(0xFF10B981)
                                    )
                                ) {
                                    Text(if (simulating) "Durdur" else "Simülasyon", fontSize = 12.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Hız: $speedKmh km/s", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Text("Güç: ${"%.1f".format(powerKw)} kW", color = Color(0xFF34D399), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Text("Mod: $driveMode", color = Color(0xFFF59E0B), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (selectedTab == 1) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("DILINK 3.0 360° AVM KAMERA & ULAK NÖBETÇİ", color = Color(0xFF22D3EE), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Button(
                                    onClick = {
                                        sentryActive = !sentryActive
                                        sendCarCommand(JSONObject().put("command", "SET_SENTRY").put("enable", sentryActive))
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (sentryActive) Color(0xFF10B981) else Color(0xFF1E293B)
                                    )
                                ) {
                                    Text(if (sentryActive) "Nöbetçi Aktif" else "Nöbetçi Kapalı", fontSize = 11.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF060B14)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("CANLI AKIŞ • $selectedCam", color = Color(0xFF34D399), fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("BYD Seal U DM-i • 1080p@30fps WebRTC ($carIp)", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Ön Geniş", "Arka", "360° AVM", "İç Kabin").forEach { cam ->
                                    Button(
                                        onClick = { selectedCam = cam },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                                    ) {
                                        Text(cam, fontSize = 10.sp)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { cabinListen = !cabinListen },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (cabinListen) Color(0xFF10B981) else Color(0xFF1E293B)
                                    )
                                ) {
                                    Text(if (cabinListen) "Kabin Dinleniyor" else "Kabini Dinle", fontSize = 12.sp)
                                }
                                Button(
                                    onClick = { intercomActive = !intercomActive },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (intercomActive) Color(0xFFF59E0B) else Color(0xFF1E293B)
                                    )
                                ) {
                                    Text(if (intercomActive) "Bas-Konuş Açık" else "Bas-Konuş", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            if (selectedTab == 2) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("UZAKTAN İKLİMLENDİRME & KOLTUK KONTROLÜ", color = Color(0xFF34D399), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Hedef: ${"%.1f".format(targetTemp)}°C (Kabin: $cabinTemp°C)", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Button(
                                    onClick = {
                                        acActive = !acActive
                                        sendCarCommand(
                                            JSONObject()
                                                .put("command", "SET_AC")
                                                .put("enable", acActive)
                                                .put("temp", targetTemp.toDouble())
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (acActive) Color(0xFF06B6D4) else Color(0xFF1E293B)
                                    )
                                ) {
                                    Text(if (acActive) "Klima Açık" else "Klimayı Başlat")
                                }
                            }
                            Slider(
                                value = targetTemp,
                                onValueChange = { targetTemp = (it * 2).roundToInt() / 2f },
                                valueRange = 16f..32f
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        driverSeatVent = (driverSeatVent + 1) % 3
                                        sendCarCommand(JSONObject().put("command", "SET_SEAT").put("vent", driverSeatVent).put("heat", driverSeatHeat))
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                                ) {
                                    Text("Sürücü Soğutma: $driverSeatVent", fontSize = 11.sp)
                                }
                                Button(
                                    onClick = {
                                        driverSeatHeat = (driverSeatHeat + 1) % 3
                                        sendCarCommand(JSONObject().put("command", "SET_SEAT").put("vent", driverSeatVent).put("heat", driverSeatHeat))
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                                ) {
                                    Text("Sürücü Isıtma: $driverSeatHeat", fontSize = 11.sp)
                                }
                                Button(
                                    onClick = {
                                        passengerSeatVent = (passengerSeatVent + 1) % 3
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                                ) {
                                    Text("Yolcu Soğutma: $passengerSeatVent", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            if (selectedTab == 3) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("OTOMATİK YOLCULUK KAYITLARI (D/R -> P)", color = Color(0xFF22D3EE), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("• Bugün 18:40 | 28.4 km | %88 EV / %12 HEV | 13.6 kWh/100km", color = Color.White, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("• Dün 09:15 | 44.2 km | %76 EV / %24 HEV | 14.2 kWh/100km", color = Color(0xFF94A3B8), fontSize = 13.sp)
                        }
                    }
                }
            }

            if (selectedTab == 4) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("DILINK 3.0 VHAL KÖPRÜSÜ & BAĞLANTI", color = Color(0xFF10B981), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Araç IP: $carIp (UDP 45454 / TCP 45455)\nPaket: tr.ulak.byd.car (Android 10 API 29)\nMobil: tr.ulak.byd.mobile (S26 Ultra API 35)",
                                color = Color(0xFFE2E8F0),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
