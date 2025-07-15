package com.gruckus.tvvolume

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.gruckus.tvvolume.ui.theme.GruckusTVVolumeTheme

class MainActivity : ComponentActivity() {
    private var volumeReceiver: BroadcastReceiver? = null

    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // For sideloaded use: show a message if overlay permission is not granted
        if (!android.provider.Settings.canDrawOverlays(this)) {
            // Show a simple message or toast to instruct the user
            android.widget.Toast.makeText(this, "Please grant overlay permission in system settings for volume overlay to work.", android.widget.Toast.LENGTH_LONG).show()
            val intent = Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:" + packageName))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
        // Start the overlay service
        val intent = Intent(this, VolumeOverlayService::class.java)
        startForegroundService(intent)
        // Finish MainActivity so only overlay runs
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (volumeReceiver != null) {
            unregisterReceiver(volumeReceiver)
            volumeReceiver = null
        }
    }
}

@Composable
fun VolumeDisplay(volume: Int, visible: Boolean, modifier: Modifier = Modifier) {
    val targetAlpha = if (visible) 1f else 0f
    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = 500), label = "fade"
    )
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        Box(
            modifier = Modifier
                .border(2.dp, Color.White)
                .background(Color(0xAA222222))
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .alpha(alpha)
        ) {
            Text(
                text = "Volume: $volume",
                color = Color.White,
                fontSize = 32.sp
            )
        }
    }
}