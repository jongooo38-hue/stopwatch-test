package com.example

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class Lap(val lapNumber: Int, val lapTimeMs: Long, val totalTimeMs: Long)

class StopwatchViewModel : ViewModel() {
    private val _timeMillis = MutableStateFlow(0L)
    val timeMillis: StateFlow<Long> = _timeMillis.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _laps = MutableStateFlow<List<Lap>>(emptyList())
    val laps: StateFlow<List<Lap>> = _laps.asStateFlow()

    private var timerJob: Job? = null
    private var startTime = 0L
    private var timeAtPause = 0L

    fun toggleStartPause() {
        if (_isRunning.value) pause() else start()
    }

    private fun start() {
        if (_isRunning.value) return
        startTime = SystemClock.elapsedRealtime() - timeAtPause
        _isRunning.value = true
        timerJob = viewModelScope.launch {
            while (isActive) {
                _timeMillis.value = SystemClock.elapsedRealtime() - startTime
                delay(10L) // Update every 10ms for smooth UI
            }
        }
    }

    private fun pause() {
        if (!_isRunning.value) return
        _isRunning.value = false
        timerJob?.cancel()
        timeAtPause = SystemClock.elapsedRealtime() - startTime
        _timeMillis.value = timeAtPause
    }

    fun lap() {
        if (!_isRunning.value) return
        val currentTotal = _timeMillis.value
        val previousTotal = _laps.value.firstOrNull()?.totalTimeMs ?: 0L
        val lapTime = currentTotal - previousTotal
        val newLap = Lap(
            lapNumber = _laps.value.size + 1,
            lapTimeMs = lapTime,
            totalTimeMs = currentTotal
        )
        _laps.update { listOf(newLap) + it }
    }

    fun reset() {
        pause()
        timeAtPause = 0L
        _timeMillis.value = 0L
        _laps.value = emptyList()
    }
}

class MainActivity : ComponentActivity() {
    private val viewModel: StopwatchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background
                ) { innerPadding ->
                    StopwatchScreen(viewModel = viewModel, modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun StopwatchScreen(viewModel: StopwatchViewModel, modifier: Modifier = Modifier) {
    val timeMillis by viewModel.timeMillis.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val laps by viewModel.laps.collectAsState()

    val bgColor = Color(0xFF000000)
    val textColor = Color(0xFFFFFFFF)
    val primaryColor = Color(0xFFD0BCFF)
    val ringBgColor = Color(0xFF333333)
    val pillBgColor = Color(0xFF4A4458)
    val pillTextColor = Color(0xFFE8DEF8)
    val btnBorder = Color(0xFF938F99)
    val playBgColor = Color(0xFFD0BCFF)
    val playTextColor = Color(0xFF381E72)
    val lapBg1 = Color(0x33FFFFFF)
    val lapBg2 = Color(0x1AFFFFFF)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(0.2f))
        
        // Circular Time Display
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(24.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = ringBgColor,
                    radius = size.minDimension / 2,
                    style = Stroke(width = 4.dp.toPx())
                )
                drawArc(
                    color = primaryColor,
                    startAngle = -90f,
                    sweepAngle = 120f,
                    useCenter = false,
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val totalSeconds = timeMillis / 1000
                val minutes = totalSeconds / 60
                val seconds = totalSeconds % 60
                val millis = (timeMillis % 1000) / 10

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = String.format("%02d:%02d", minutes, seconds),
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Normal,
                        fontFamily = FontFamily.SansSerif,
                        color = textColor
                    )
                    Text(
                        text = String.format(".%02d", millis),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Normal,
                        fontFamily = FontFamily.SansSerif,
                        color = textColor.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(pillBgColor)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isRunning) "RUNNING" else "PAUSED",
                        color = pillTextColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 2.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Laps List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(laps) { lap ->
                val isEven = lap.lapNumber % 2 == 0
                val itemBgColor = if (isEven) lapBg2 else lapBg1
                val borderModifier = if (isEven) Modifier.border(1.dp, ringBgColor, RoundedCornerShape(16.dp)) else Modifier
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(itemBgColor)
                        .then(borderModifier)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Lap ${lap.lapNumber}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = textColor.copy(alpha = 0.8f)
                    )
                    Text(
                        text = formatLapTime(lap.totalTimeMs),
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        color = textColor
                    )
                    Text(
                        text = if (lap.lapTimeMs > 0) "+${formatLapTime(lap.lapTimeMs)}" else "--",
                        fontSize = 12.sp,
                        color = if (isEven) Color(0xFF4ADE80) else primaryColor
                    )
                }
            }
        }

        // Controls Footer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 48.dp, top = 24.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Reset Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { viewModel.reset() }
                    .padding(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .border(1.dp, btnBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = textColor)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Reset", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = textColor.copy(alpha = 0.7f))
            }

            // Play/Pause Button
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .shadow(8.dp, RoundedCornerShape(28.dp))
                    .clip(RoundedCornerShape(28.dp))
                    .background(playBgColor)
                    .clickable { viewModel.toggleStartPause() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isRunning) "Pause" else "Start",
                    tint = playTextColor,
                    modifier = Modifier.size(48.dp)
                )
            }

            // Lap Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { if (isRunning) viewModel.lap() }
                    .padding(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .border(1.dp, btnBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Flag, contentDescription = "Lap", tint = textColor)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Lap", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = textColor.copy(alpha = 0.7f))
            }
        }
    }
}



fun formatTime(timeMs: Long): String {
    val totalSeconds = timeMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val millis = (timeMs % 1000) / 10 // Get hundredths of a second
    return String.format("%02d:%02d.%02d", minutes, seconds, millis)
}

fun formatLapTime(timeMs: Long): String {
    val totalSeconds = timeMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val millis = (timeMs % 1000) / 10
    return if (minutes > 0) {
        String.format("%02d:%02d.%02d", minutes, seconds, millis)
    } else {
        String.format("%02d.%02d", seconds, millis)
    }
}
