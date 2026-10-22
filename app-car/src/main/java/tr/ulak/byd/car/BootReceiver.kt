package tr.ulak.byd.car

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if ((action == Intent.ACTION_BOOT_COMPLETED) || (action == "com.byd.action.DILINK_READY")) {
            Log.i("UlakBootReceiver", "DiLink 3.0 Başlatıldı -> UlakDiLink3Service tetikleniyor...")
            val serviceIntent = Intent(context, UlakDiLink3Service::class.java)
            context.startForegroundService(serviceIntent)
        }
    }
}
