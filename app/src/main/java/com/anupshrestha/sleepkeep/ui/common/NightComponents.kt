package com.anupshrestha.sleepkeep.ui.common

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.anupshrestha.sleepkeep.ui.theme.NightDeep
import com.anupshrestha.sleepkeep.ui.theme.NightSky

/** The screen-wide galaxy sky behind every screen. */
@Composable
fun NightBackground(content: @Composable BoxScope.() -> Unit) {
    // Text outside cards gets the light on-background colour instead of the default black.
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(NightSky, NightDeep, NightDeep)))
                .galaxy(),
            content = content,
        )
    }
}

/**
 * A rounded card in the app's style; tappable when [onClick] is given. Slightly see-through, with
 * a faint light edge, so the galaxy shows behind it like frosted glass.
 */
@Composable
fun NightCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    color: Color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = color,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
    }
}

/** An icon in a soft lavender circle. */
@Composable
fun IconBadge(
    @DrawableRes icon: Int,
    tint: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 40.dp,
) {
    Box(
        modifier = Modifier
            .size(size)
            .background(tint.copy(alpha = 0.16f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.55f))
    }
}

/** A small rounded label with a coloured dot, e.g. "● Listening". */
@Composable
fun StatusPill(text: String, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(color.copy(alpha = 0.14f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(7.dp).background(color, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = color)
    }
}

/** The big rounded main action, e.g. Start sleep. */
@Composable
fun PillButton(
    text: String,
    @DrawableRes icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.primary,
    content: Color = MaterialTheme.colorScheme.onPrimary,
) {
    Button(
        onClick = onClick,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp),
    ) {
        Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

/** Title of a top-level screen, with its icon. */
@Composable
fun ScreenHeader(title: String, @DrawableRes icon: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(title, style = MaterialTheme.typography.headlineSmall)
    }
}

/** A small label above a value, e.g. "Bedtime" over "10:42 PM". */
@Composable
fun LabeledValue(label: String, value: String, modifier: Modifier = Modifier, @DrawableRes icon: Int? = null) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(
                painterResource(icon), contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
        }
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
}

/** Small grey explanatory text. */
@Composable
fun Hint(text: String, modifier: Modifier = Modifier) {
    Text(
        text, style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier,
    )
}
