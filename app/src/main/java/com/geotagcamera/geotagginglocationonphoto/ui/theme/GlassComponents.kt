package com.geotagcamera.geotagginglocationonphoto.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * "Prism" visual language: a slow-shifting colourful gradient wash behind
 * frosted, translucent panels with a soft light border. Purely additive —
 * it never changes navigation, state or behaviour, only how surfaces are
 * painted. Drop [PrismBackdrop] behind a screen's content and wrap any
 * card/sheet/button surface in [GlassPanel] to pick up the look.
 */

private val PrismColorsDark = listOf(
    Color(0xFF120024),
    Color(0xFF2D0A5C),
    Color(0xFF6A2FBF),
    Color(0xFF00B4D8),
    Color(0xFF120024)
)

private val PrismColorsLight = listOf(
    Color(0xFFEDE3FF),
    Color(0xFFDCEBFF),
    Color(0xFFFFE3F1),
    Color(0xFFE3FFF6),
    Color(0xFFEDE3FF)
)

/**
 * Animated multi-hue gradient background, the "prism" wash. Colours drift
 * slowly and continuously (18s loop) so panels above it always have some
 * movement to refract, without ever being distracting.
 */
@Composable
fun PrismBackdrop(modifier: Modifier = Modifier, dark: Boolean = true) {
    val transition = rememberInfiniteTransition(label = "prismShift")
    val shift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(18000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "prismShiftValue"
    )
    val colors = if (dark) PrismColorsDark else PrismColorsLight
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = colors,
                    start = Offset(0f, 1200f * shift),
                    end = Offset(1200f * (1f - shift), 0f)
                )
            )
    )
}

/**
 * A frosted "glass" surface: translucent tint + blur (real blur on API 31+,
 * a graceful flat-tint fallback below that) + a subtle light-catching border
 * gradient, like a pane of prism glass. Use in place of [androidx.compose.material3.Card]
 * or [androidx.compose.material3.Surface] for any panel that should sit above
 * a [PrismBackdrop].
 */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    tint: Color = Color.White,
    tintAlpha: Float = 0.14f,
    blurRadius: Dp = 18.dp,
    borderAlpha: Float = 0.35f,
    content: @Composable () -> Unit
) {
    val borderBrush = remember(tint, borderAlpha) {
        Brush.linearGradient(
            listOf(
                tint.copy(alpha = borderAlpha),
                tint.copy(alpha = borderAlpha * 0.25f),
                tint.copy(alpha = borderAlpha)
            )
        )
    }
    Box(
        modifier = modifier
            .clip(shape)
            .blur(blurRadius)
            .background(tint.copy(alpha = tintAlpha), shape)
            .border(1.dp, borderBrush, shape)
    ) {
        // Unblurred content layer on top, so text/icons stay crisp while only
        // the panel background reads as frosted.
    }
    Box(
        modifier = modifier
            .clip(shape)
            .background(Color.Transparent, shape)
    ) {
        content()
    }
}

/** Convenience: a full-bleed glass surface with no rounding, for top bars / sheets. */
@Composable
fun GlassBar(modifier: Modifier = Modifier, tint: Color = Color.White, content: @Composable () -> Unit) {
    GlassPanel(modifier = modifier, shape = RectangleShape, tint = tint, blurRadius = 14.dp, content = content)
}
