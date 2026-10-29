package com.anupshrestha.sleepkeep.ui.welcome

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.anupshrestha.sleepkeep.R
import com.anupshrestha.sleepkeep.ui.common.IconBadge
import com.anupshrestha.sleepkeep.ui.common.MoonLogo
import com.anupshrestha.sleepkeep.ui.common.PillButton
import com.anupshrestha.sleepkeep.ui.theme.MoonGold

/** Shown until the first session exists. Nothing is stored to remember it was seen. */
@Composable
fun WelcomeScreen(onGetStarted: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Edge to edge; everything below it gets side margins.
        NightScene(
            Modifier
                .fillMaxWidth()
                .height(250.dp)
        )
        Column(Modifier.padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.welcome_tagline),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Feature(R.drawable.ic_moon, R.string.welcome_log_title, R.string.welcome_log_text)
            Feature(R.drawable.ic_sound, R.string.welcome_sound_title, R.string.welcome_sound_text)
            Feature(R.drawable.ic_insights, R.string.welcome_week_title, R.string.welcome_week_text)
        }
        Spacer(Modifier.height(36.dp))
        PillButton(stringResource(R.string.welcome_get_started), R.drawable.ic_moon, onGetStarted)
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painterResource(R.drawable.ic_lock), contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                stringResource(R.string.welcome_private),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        }
    }
}

@Composable
private fun Feature(@DrawableRes icon: Int, title: Int, text: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        IconBadge(icon, size = 44.dp)
        Column(Modifier.weight(1f)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(text),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** A crescent moon over layered hills, with a few stars. Drawn, so no image files are needed. */
@Composable
private fun NightScene(modifier: Modifier = Modifier) {
    val far = MaterialTheme.colorScheme.primaryContainer
    val near = MaterialTheme.colorScheme.surfaceContainerHigh
    Box(modifier, contentAlignment = Alignment.TopCenter) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // Stars come from the galaxy background; the moon draws its own glow.
            val back = Path().apply {
                moveTo(0f, h * 0.78f)
                cubicTo(w * 0.25f, h * 0.55f, w * 0.45f, h * 0.70f, w * 0.62f, h * 0.60f)
                cubicTo(w * 0.80f, h * 0.50f, w * 0.92f, h * 0.66f, w, h * 0.62f)
                lineTo(w, h); lineTo(0f, h); close()
            }
            // Hills fade into the sky at the bottom instead of ending in a hard edge.
            drawPath(back, Brush.verticalGradient(listOf(far, Color.Transparent), startY = h * 0.55f, endY = h))
            val front = Path().apply {
                moveTo(0f, h * 0.90f)
                cubicTo(w * 0.30f, h * 0.74f, w * 0.55f, h * 0.92f, w * 0.75f, h * 0.80f)
                cubicTo(w * 0.88f, h * 0.73f, w * 0.95f, h * 0.82f, w, h * 0.80f)
                lineTo(w, h); lineTo(0f, h); close()
            }
            drawPath(front, Brush.verticalGradient(listOf(near, Color.Transparent), startY = h * 0.75f, endY = h))
        }
        MoonLogo(160.dp, Modifier.padding(top = 4.dp))
    }
}
