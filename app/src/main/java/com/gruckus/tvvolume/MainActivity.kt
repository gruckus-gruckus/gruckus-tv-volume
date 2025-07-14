package com.gruckus.tvvolume

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
    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GruckusTVVolumeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RectangleShape
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        VolumeDisplay(
                            volume = 15, // Replace with actual volume value
                            visible = true // Replace with actual visibility logic
                        )
                    }
                }
            }
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