package com.gruckus.tvvolume

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.media.AudioManager
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class VolumeOverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private var volumeReceiver: BroadcastReceiver? = null
    private lateinit var audioManager: AudioManager
    private lateinit var volumeText: TextView

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        createNotificationChannel() // Ensure channel exists before startForeground
        showOverlay()
        registerVolumeReceiver()
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        Log.d("LOGASDF", "Current volume at start: $currentVolume")
        startForeground(1, NotificationCompat.Builder(this, "volume_overlay_channel")
            .setContentTitle("Volume Overlay")
            .setContentText("Overlay is running")
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode)
            .build())
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "volume_overlay_channel",
                "Volume Overlay",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun showOverlay() {
        // Check SYSTEM_ALERT_WINDOW permission before showing overlay
        if (!android.provider.Settings.canDrawOverlays(this)) {
            val intent = Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:" + packageName))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            Log.e("VolumeOverlayService", "SYSTEM_ALERT_WINDOW permission not granted. Requesting permission.")
            return
        }
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        overlayView = inflater.inflate(R.layout.overlay_volume, null)
        volumeText = overlayView!!.findViewById(R.id.volumeText)
        updateVolumeText()
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.BOTTOM or Gravity.END
        params.x = 32
        params.y = 32
        windowManager.addView(overlayView, params)
    }

    private fun updateVolumeText() {
        val volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        volumeText.text = "Volume: $volume"
    }

    private fun registerVolumeReceiver() {
        volumeReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                updateVolumeText()
                val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                Log.d("LOGASDF", "Current volume on event: $currentVolume")
            }
        }
        val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        registerReceiver(volumeReceiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (overlayView != null) {
            windowManager.removeView(overlayView)
            overlayView = null
        }
        if (volumeReceiver != null) {
            unregisterReceiver(volumeReceiver)
            volumeReceiver = null
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
