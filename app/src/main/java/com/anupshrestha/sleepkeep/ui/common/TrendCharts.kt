package com.anupshrestha.sleepkeep.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anupshrestha.sleepkeep.guidance.SleepGuidelines
import com.anupshrestha.sleepkeep.guidance.SleepStats
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import kotlin.math.floor

private const val HOUR = 60 * 60_000f
private val shortDate = DateTimeFormatter.ofPattern("d MMM")

/**
 * A month or more of nights as dots, with the rolling average as a line and the 7-hour guideline
 * dashed. The hour axis is zoomed to the data and labelled; dots rather than bars, since bars
 * that don't start at zero would exaggerate the differences. Missing nights leave a gap.
 */
@Composable
fun TrendBars(
    nights: List<SleepStats.NightBar>,
    rolling: List<Long?>,
    modifier: Modifier = Modifier,
    height: Dp = 170.dp,
) {
    val solid = MaterialTheme.colorScheme.primary
    val pale = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
    val guide = MaterialTheme.colorScheme.secondary
    val line = MaterialTheme.colorScheme.onSurface
    val grid = MaterialTheme.colorScheme.outlineVariant
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

    val hours = nights.mapNotNull { it.sleepMillis?.let { ms -> ms / HOUR } }
    val low = floor((hours.minOrNull() ?: 5f) - 0.5f).coerceIn(0f, 6f)
    val high = ceil((hours.maxOrNull() ?: 9f) + 0.5f).coerceAtLeast(8f)

    Column(modifier.fillMaxWidth()) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(height)
        ) {
            if (nights.isEmpty()) return@Canvas
            val labelWidth = 28.dp.toPx()
            val chartWidth = size.width - labelWidth
            val slot = chartWidth / nights.size
            fun x(i: Int) = labelWidth + slot * i + slot / 2
            fun y(millis: Long) = size.height - ((millis / HOUR - low) / (high - low)).coerceIn(0f, 1f) * size.height

            // Hour labels and faint grid lines.
            var h = low.toInt()
            while (h <= high) {
                if ((h - low.toInt()) % 2 == 0 || h == 7) {
                    val gy = y((h * HOUR).toLong())
                    drawLine(grid, Offset(labelWidth, gy), Offset(size.width, gy), strokeWidth = 1f)
                    val text = measurer.measure("${h}h", labelStyle)
                    drawText(text, topLeft = Offset(0f, (gy - text.size.height / 2).coerceIn(0f, size.height - text.size.height)))
                }
                h++
            }

            val guideY = y(SleepGuidelines.MIN_ADULT_SLEEP_MILLIS)
            drawLine(
                guide, Offset(labelWidth, guideY), Offset(size.width, guideY),
                strokeWidth = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
            )

            val dot = minOf(slot * 0.42f, 4.dp.toPx()).coerceAtLeast(1.5.dp.toPx())
            nights.forEachIndexed { i, night ->
                val sleep = night.sleepMillis ?: return@forEachIndexed
                drawCircle(if (night.timeInBedOnly) pale else solid, dot, Offset(x(i), y(sleep)))
            }

            // The rolling average, broken where there isn't enough data.
            var path: Path? = null
            rolling.forEachIndexed { i, value ->
                val point = value?.let { Offset(x(i), y(it)) }
                when {
                    point == null -> {
                        path?.let { drawPath(it, line, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)) }
                        path = null
                    }
                    path == null -> path = Path().apply { moveTo(point.x, point.y) }
                    else -> path!!.lineTo(point.x, point.y)
                }
            }
            path?.let { drawPath(it, line, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)) }
        }
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(28.dp))
            Column(Modifier.weight(1f)) { DateEnds(nights) }
        }
    }
}

/**
 * When each night happened: a bar from bedtime down to wake-up on a clock axis, so drifting or
 * irregular schedules are visible at a glance.
 */
@Composable
fun BedtimeRangeChart(
    nights: List<SleepStats.NightBar>,
    modifier: Modifier = Modifier,
    height: Dp = 180.dp,
) {
    val bar = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 10.sp, color = labelColor)
    val use24h = is24HourClock()

    // Hours after 18:00, so a night from 23:00 to 07:00 runs 5 → 13 without wrapping.
    fun offset(minute: Int) = ((minute - 18 * 60 + 24 * 60) % (24 * 60)) / 60f
    val beds = nights.mapNotNull { it.bedMinute?.let(::offset) }
    val wakes = nights.mapNotNull { it.wakeMinute?.let(::offset) }
    val top = if (beds.isEmpty()) 3f else floor(beds.min() - 0.5f).coerceAtLeast(0f)
    val bottom = if (wakes.isEmpty()) 15f else ceil(wakes.max() + 0.5f).coerceAtMost(20f)
    val span = (bottom - top).coerceAtLeast(1f)

    Column(modifier.fillMaxWidth()) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(height)
        ) {
            val labelWidth = 40.dp.toPx()
            val chartWidth = size.width - labelWidth
            fun y(hours: Float) = (hours - top) / span * size.height

            // Grid every 2 or 3 hours, labelled with clock times.
            val step = if (span > 10) 3 else 2
            var h = ceil(top).toInt()
            while (h <= bottom) {
                if (h % step == 0) {
                    val gy = y(h.toFloat())
                    drawLine(grid, Offset(labelWidth, gy), Offset(size.width, gy), strokeWidth = 1f)
                    val clock = (18 + h) % 24
                    val label = if (use24h) "%02d:00".format(clock) else "${(clock + 11) % 12 + 1} ${if (clock < 12) "am" else "pm"}"
                    val text = measurer.measure(label, labelStyle)
                    drawText(text, topLeft = Offset(0f, (gy - text.size.height / 2).coerceIn(0f, size.height - text.size.height)))
                }
                h++
            }

            val slot = chartWidth / nights.size.coerceAtLeast(1)
            val barWidth = maxOf(slot * 0.55f, 3f)
            nights.forEachIndexed { i, night ->
                val bed = night.bedMinute ?: return@forEachIndexed
                val wake = night.wakeMinute ?: return@forEachIndexed
                val y1 = y(offset(bed))
                val y2 = y(offset(wake))
                if (y2 <= y1) return@forEachIndexed
                drawRoundRect(
                    color = if (night.timeInBedOnly) bar.copy(alpha = 0.45f) else bar,
                    topLeft = Offset(labelWidth + slot * i + (slot - barWidth) / 2, y1),
                    size = Size(barWidth, y2 - y1),
                    cornerRadius = CornerRadius(barWidth / 2, barWidth / 2),
                )
            }
        }
        // Dates start after the hour labels, under the bars.
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(40.dp))
            Column(Modifier.weight(1f)) { DateEnds(nights) }
        }
    }
}

/** First and last date under a chart. */
@Composable
private fun DateEnds(nights: List<SleepStats.NightBar>) {
    if (nights.isEmpty()) return
    Row(Modifier.fillMaxWidth()) {
        Hint(shortDate.format(nights.first().date), Modifier.weight(1f))
        Hint(shortDate.format(nights.last().date))
    }
}
