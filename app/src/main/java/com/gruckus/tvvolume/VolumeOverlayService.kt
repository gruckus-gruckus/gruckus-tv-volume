package com.gruckus.tvvolume

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.media.AudioManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat

class VolumeOverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private var volumeReceiver: BroadcastReceiver? = null
    private lateinit var audioManager: AudioManager
    private lateinit var volumeText: TextView
    private val handler = Handler(Looper.getMainLooper())
    private var hideRunnable: Runnable? = null
    private var lastVolume: Int = -1

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        createNotificationChannel() // Ensure channel exists before startForeground
        // Promote to a foreground service first so we satisfy the 5-second
        // startForeground() deadline even if overlay setup bails out below.
        startForegroundCompat()
        showOverlay()
        showOverlayInstantly()
        scheduleHideOverlay()
        registerVolumeReceiver()
        lastVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val currentVolume = lastVolume
        Log.d("LOGASDF", "Current volume at start: $currentVolume")
    }

    private fun startForegroundCompat() {
        val notification = NotificationCompat.Builder(this, "volume_overlay_channel")
            .setContentTitle("Volume Overlay")
            .setContentText("Overlay is running")
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, notification)
        }
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
        // Check SYSTEM_ALERT_WINDOW permission before showing overlay.
        if (!android.provider.Settings.canDrawOverlays(this)) {
            // Just stop. Do NOT launch MainActivity here: the activity restarts
            // this service, which would bounce back here and loop. MainActivity
            // is responsible for prompting the user to grant the permission.
            Log.e("VolumeOverlayService", "Overlay permission not granted; stopping service.")
            stopSelf()
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

    private fun showOverlayInstantly() {
        // If the overlay never attached (e.g. permission missing), do nothing
        // rather than re-running showOverlay() and risking another loop.
        val view = overlayView ?: return
        // Cancel any in-flight fade-out animator; otherwise it keeps driving
        // alpha back to 0 and hides the view right after we show it, which
        // looks like the overlay lagging or not appearing on a volume change.
        view.animate().cancel()
        view.clearAnimation()
        view.visibility = View.VISIBLE
        view.alpha = 1f
    }

    private fun fadeOutOverlay() {
        overlayView?.let { view ->
            view.clearAnimation() // Ensure previous animation is cleared
            view.animate()
                .alpha(0f)
                .setDuration(750)
                .withEndAction {
                    view.visibility = View.GONE
                    view.alpha = 1f // Reset alpha for next show
                }
                .start()
        }
    }

    private fun scheduleHideOverlay() {
        hideRunnable?.let { handler.removeCallbacks(it) }
        hideRunnable = Runnable {
            fadeOutOverlay()
        }
        handler.postDelayed(hideRunnable!!, 1000) // 1 second
    }

    private fun onVolumeChanged() {
        val newVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        if (newVolume == lastVolume) return
        lastVolume = newVolume
        updateVolumeText()
        showOverlayInstantly()
        scheduleHideOverlay()
    }

    private fun registerVolumeReceiver() {
        volumeReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                onVolumeChanged()
                val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                Log.d("Gruckus volume", "Current volume on event: $currentVolume")
            }
        }
        val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        registerReceiver(volumeReceiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        hideRunnable?.let { handler.removeCallbacks(it) }
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
