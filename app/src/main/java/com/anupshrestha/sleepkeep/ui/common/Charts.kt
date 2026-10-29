package com.anupshrestha.sleepkeep.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.anupshrestha.sleepkeep.data.MonitoringSegment
import com.anupshrestha.sleepkeep.data.SoundEvent
import com.anupshrestha.sleepkeep.guidance.SleepGuidelines
import com.anupshrestha.sleepkeep.guidance.SleepStats
import com.anupshrestha.sleepkeep.monitor.Coverage
import com.anupshrestha.sleepkeep.ui.theme.LavenderDeep
import com.anupshrestha.sleepkeep.ui.theme.MoonGold
import java.time.format.TextStyle
import java.util.Locale

private const val HOUR = 60 * 60_000f

/**
 * Seven bars, one per day. Days with a check-in are solid lavender; time-in-bed-only days are
 * paler, since they overstate sleep. The dashed line marks the 7-hour adult guideline.
 */
@Composable
fun WeekBars(nights: List<SleepStats.NightBar>, modifier: Modifier = Modifier, height: Dp = 120.dp) {
    val solid = MaterialTheme.colorScheme.primary
    val pale = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
    val track = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val guide = MaterialTheme.colorScheme.secondary
    val maxHours = maxOf(10f, (nights.maxOfOrNull { it.sleepMillis ?: 0 } ?: 0) / HOUR)

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height),
        ) {
            val slot = size.width / nights.size
            val barWidth = slot * 0.46f
            val radius = CornerRadius(barWidth / 2, barWidth / 2)
            nights.forEachIndexed { i, night ->
                val left = slot * i + (slot - barWidth) / 2
                drawRoundRect(track, Offset(left, 0f), Size(barWidth, size.height), radius)
                val sleep = night.sleepMillis ?: return@forEachIndexed
                val barHeight = (sleep / HOUR / maxHours).coerceIn(0.04f, 1f) * size.height
                drawRoundRect(
                    color = if (night.timeInBedOnly) pale else solid,
                    topLeft = Offset(left, size.height - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = radius,
                )
            }
            val guideY = size.height - (SleepGuidelines.MIN_ADULT_SLEEP_MILLIS / HOUR / maxHours) * size.height
            drawLine(
                color = guide,
                start = Offset(0f, guideY),
                end = Offset(size.width, guideY),
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
            )
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            nights.forEach { night ->
                Text(
                    night.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** A thick ring filled to [progress] (0–1), with [content] in the middle. */
@Composable
fun SleepRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 240.dp,
    stroke: Dp = 14.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val track = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    val light = MaterialTheme.colorScheme.primary
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val width = stroke.toPx()
            val inset = width / 2
            val arcSize = Size(this.size.width - width, this.size.height - width)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(width))
            val sweep = 360f * progress.coerceIn(0f, 1f)
            if (sweep > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(listOf(LavenderDeep, light, MoonGold, LavenderDeep)),
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width, cap = StrokeCap.Round),
                )
            }
        }
        content()
    }
}

/**
 * The microphone's recent levels as mirrored bars, newest on the right. It shows that monitoring
 * really hears the room; nothing is recorded.
 */
@Composable
fun LevelBars(levels: List<Float>, modifier: Modifier = Modifier, height: Dp = 56.dp) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
    ) {
        if (levels.isEmpty()) return@Canvas
        val slot = size.width / LEVEL_BARS
        val barWidth = slot * 0.5f
        val mid = size.height / 2
        levels.takeLast(LEVEL_BARS).forEachIndexed { i, db ->
            // -80 dBFS (quiet room) to -20 dBFS (loud) fills the height.
            val fraction = ((db + 80f) / 60f).coerceIn(0.05f, 1f)
            val half = fraction * mid
            val x = size.width - slot * (minOf(levels.size, LEVEL_BARS) - i) + (slot - barWidth) / 2
            drawRoundRect(
                color = color.copy(alpha = 0.35f + 0.65f * fraction),
                topLeft = Offset(x, mid - half),
                size = Size(barWidth, half * 2),
                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2),
            )
        }
    }
}

const val LEVEL_BARS = 48

/**
 * The night from bedtime to wake-up: lavender where it was monitored, grey where it wasn't, and
 * a gold tick for each loud sound.
 */
@Composable
fun NightTimeline(
    start: Long,
    end: Long,
    segments: List<MonitoringSegment>,
    events: List<SoundEvent>,
    modifier: Modifier = Modifier,
) {
    val monitored = MaterialTheme.colorScheme.primary
    val gap = MaterialTheme.colorScheme.outline
    val tick = MaterialTheme.colorScheme.secondary
    val coverage = Coverage.of(start, end, segments)
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp),
    ) {
        val span = (end - start).coerceAtLeast(1).toFloat()
        fun x(t: Long) = ((t - start) / span).coerceIn(0f, 1f) * size.width
        val barTop = size.height * 0.2f
        val barHeight = size.height * 0.6f
        val radius = CornerRadius(barHeight / 2, barHeight / 2)
        drawRoundRect(monitored, Offset(0f, barTop), Size(size.width, barHeight), radius)
        coverage.gaps.forEach { g ->
            drawRect(gap, Offset(x(g.start), barTop), Size(x(g.end) - x(g.start), barHeight))
        }
        events.forEach { e ->
            val cx = x(e.startTime)
            drawLine(tick, Offset(cx, 0f), Offset(cx, size.height), strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
        }
    }
}

/** A coloured dot and label for chart legends. */
@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(Modifier.size(10.dp)) { drawCircle(color) }
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
