package com.anupshrestha.sleepkeep.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.min
import kotlin.random.Random

// Galaxy colours: kept here, since only the backdrop and the logo use them.
private val NebulaViolet = Color(0xFF6E5CF0)
private val NebulaTeal = Color(0xFF2BB3C0)
private val NebulaMagenta = Color(0xFFB05CF0)
private val StarWhite = Color(0xFFF3F1FF)
private val MoonLight = Color(0xFFFFF3CC)
private val MoonMid = Color(0xFFF2D891)
private val MoonShadow = Color(0xFFD9A54A)

private data class Star(val x: Float, val y: Float, val radius: Float, val alpha: Float)

/** Fixed positions (a seeded random), so the sky looks the same on every screen and launch. */
private val stars: List<Star> = Random(42).let { r ->
    List(150) {
        val big = r.nextFloat() < 0.08f
        Star(
            x = r.nextFloat(),
            y = r.nextFloat(),
            radius = if (big) 1.6f + r.nextFloat() else 0.5f + r.nextFloat() * 0.9f,
            alpha = if (big) 0.9f else 0.25f + r.nextFloat() * 0.55f,
        )
    }
}

/**
 * Stars, a faint Milky Way band and soft nebula glows over the base gradient. Static: drawn
 * once per size and cached, with no animation.
 */
fun Modifier.galaxy(): Modifier = drawWithCache {
    val w = size.width
    val h = size.height
    val nebulae = listOf(
        Triple(Offset(w * 0.88f, h * 0.10f), w * 0.75f, NebulaViolet.copy(alpha = 0.32f)),
        Triple(Offset(w * 0.05f, h * 0.42f), w * 0.65f, NebulaTeal.copy(alpha = 0.14f)),
        Triple(Offset(w * 0.70f, h * 0.80f), w * 0.70f, NebulaMagenta.copy(alpha = 0.12f)),
    ).map { (center, radius, color) ->
        Triple(center, radius, Brush.radialGradient(listOf(color, Color.Transparent), center, radius))
    }
    val bandWidth = w * 0.55f
    val band = Brush.horizontalGradient(
        listOf(Color.Transparent, StarWhite.copy(alpha = 0.05f), NebulaViolet.copy(alpha = 0.06f), Color.Transparent),
        startX = w / 2 - bandWidth / 2,
        endX = w / 2 + bandWidth / 2,
    )
    val density = density
    onDrawBehind {
        nebulae.forEach { (center, radius, brush) -> drawCircle(brush, radius, center) }
        rotate(degrees = 28f) {
            drawRect(band, Offset(w / 2 - bandWidth / 2, -h), Size(bandWidth, h * 3))
        }
        stars.forEach { s ->
            val center = Offset(s.x * w, s.y * h)
            val r = s.radius * density
            // Brightest at the top and fading lower down, like a real sky, so stars don't compete
            // with the cards and buttons further down.
            val fade = 1f - 0.7f * s.y
            if (s.radius > 1.5f && s.y < 0.55f) {
                drawCircle(Brush.radialGradient(listOf(StarWhite.copy(alpha = 0.35f * fade), Color.Transparent), center, r * 4), r * 4, center)
            }
            drawCircle(StarWhite.copy(alpha = s.alpha * fade), if (s.y < 0.55f) r else r * 0.7f, center)
        }
    }
}

/**
 * The app's moon: a gold crescent lit from the upper left, with a soft glow and two sparkles.
 * Drawn, not an image, so it stays sharp at any size.
 */
@Composable
fun MoonLogo(size: Dp, modifier: Modifier = Modifier, sparkles: Boolean = true) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) { drawMoon(sparkles) }
    }
}

private fun DrawScope.drawMoon(sparkles: Boolean) {
    val s = min(size.width, size.height)
    val c = Offset(size.width / 2, size.height / 2)

    // Glow behind the moon.
    drawCircle(
        Brush.radialGradient(listOf(MoonMid.copy(alpha = 0.35f), MoonMid.copy(alpha = 0.08f), Color.Transparent), c, s * 0.5f),
        radius = s * 0.5f,
        center = c,
    )

    // Crescent: a disc with an offset disc cut out of it.
    val r = s * 0.30f
    val disc = Path().apply { addOval(androidx.compose.ui.geometry.Rect(c, r)) }
    val cut = Path().apply {
        addOval(androidx.compose.ui.geometry.Rect(Offset(c.x + r * 0.55f, c.y - r * 0.38f), r * 0.86f))
    }
    val crescent = Path().apply { op(disc, cut, PathOperation.Difference) }
    drawPath(
        crescent,
        Brush.linearGradient(
            listOf(MoonLight, MoonMid, MoonShadow),
            start = Offset(c.x - r, c.y - r),
            end = Offset(c.x + r * 0.4f, c.y + r),
        ),
    )
    // A thin bright rim on the lit edge for depth.
    drawPath(
        crescent,
        Brush.radialGradient(listOf(Color.Transparent, Color.Transparent, Color.White.copy(alpha = 0.25f)), Offset(c.x + r * 0.2f, c.y), r * 1.05f),
    )

    if (sparkles) {
        drawSparkle(Offset(c.x + s * 0.30f, c.y - s * 0.28f), s * 0.07f)
        drawSparkle(Offset(c.x + s * 0.40f, c.y + s * 0.02f), s * 0.04f)
    }
}

/** A four-pointed star. */
private fun DrawScope.drawSparkle(center: Offset, radius: Float) {
    val inner = radius * 0.28f
    val path = Path().apply {
        moveTo(center.x, center.y - radius)
        quadraticTo(center.x + inner, center.y - inner, center.x + radius, center.y)
        quadraticTo(center.x + inner, center.y + inner, center.x, center.y + radius)
        quadraticTo(center.x - inner, center.y + inner, center.x - radius, center.y)
        quadraticTo(center.x - inner, center.y - inner, center.x, center.y - radius)
        close()
    }
    drawPath(path, StarWhite.copy(alpha = 0.95f))
}
