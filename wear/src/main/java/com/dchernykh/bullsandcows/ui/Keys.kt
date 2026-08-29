package com.dchernykh.bullsandcows.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material3.Text
import com.dchernykh.bullsandcows.layout.Box as LayoutBox

/**
 * One key of the ring.
 *
 * A key whose digit is already in the guess being composed goes dim and stops
 * taking taps. Every code is distinct digits, so a digit once placed is spent for
 * the rest of that guess, and a key that looked live and did nothing would be the
 * worst of both.
 */
@Composable
fun DigitKey(
    box: LayoutBox,
    digit: Int,
    taken: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val background =
        when {
            taken -> ColorKeyTaken
            pressed -> ColorKeyPressed
            else -> ColorKey
        }
    Box(
        modifier =
            Modifier
                .absoluteBox(box)
                .clip(CircleShape)
                .background(background)
                .pressable(interactionSource, label, enabled = !taken, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = digit.toString(),
            color = if (taken) ColorKeyTextTaken else ColorText,
            fontSize = with(LocalDensity.current) { (box.h * 0.5f).toSp() },
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

/**
 * One slot of the guess being composed: empty, filled, or the one the next digit
 * goes into. The three look different so that where the next tap lands is visible
 * without counting.
 */
@Composable
fun GuessSlot(
    box: LayoutBox,
    digit: Int?,
    isNext: Boolean,
) {
    val background =
        when {
            digit != null -> ColorSlotFilled
            isNext -> ColorSlotNext
            else -> ColorSlot
        }
    Box(
        modifier =
            Modifier
                .absoluteBox(box)
                .clip(RoundedCornerShape(percent = 30))
                .background(background),
        contentAlignment = Alignment.Center,
    ) {
        if (digit != null) {
            Text(
                text = digit.toString(),
                color = ColorText,
                fontSize = with(LocalDensity.current) { (box.h * 0.55f).toSp() },
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
        }
    }
}

/**
 * A pill button. [accented] is the one that plays a guess, and it only lights up
 * once the guess is complete - so the button says whether the guess is playable
 * before the finger arrives.
 */
@Composable
fun PillButton(
    box: LayoutBox,
    text: String,
    onClick: () -> Unit,
    accented: Boolean = false,
    enabled: Boolean = true,
    label: String = text,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val background =
        when {
            accented && enabled -> if (pressed) ColorAccentPressed else ColorAccent
            pressed -> ColorButtonPressed
            else -> ColorButton
        }
    Box(
        modifier =
            Modifier
                .absoluteBox(box)
                .clip(RoundedCornerShape(percent = 50))
                .background(background)
                .pressable(interactionSource, label, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        FittedText(
            text = text,
            color = if (enabled) ColorText else ColorMuted,
            boxHeight = box.h,
            boxWidth = box.w,
            fraction = 0.46f,
        )
    }
}

/**
 * Text sized to fit the box it is in.
 *
 * Zepp OS drew text at exactly the size it was given and clipped the rest, so the
 * original had to guess a size from the glyph count. Compose can measure, so it
 * measures: [fraction] of the height is what the label wants, and it shrinks only
 * as far as it has to, which is what keeps the seven letters of a Russian button
 * label on a pill cut for the two of "OK".
 */
@Composable
fun FittedText(
    text: String,
    color: Color,
    boxHeight: Int,
    boxWidth: Int,
    fraction: Float,
    weight: FontWeight = FontWeight.Normal,
) {
    val density = LocalDensity.current
    val wanted = boxHeight * fraction
    // The same generous estimate the original used, but as a floor to shrink
    // towards rather than as the answer: a label only pays for its own length.
    val byWidth = (boxWidth * 0.86f) / maxOf(1, text.length) / 0.6f
    Text(
        text = text,
        color = color,
        fontSize = with(density) { minOf(wanted, byWidth).coerceAtLeast(MIN_TEXT_PX).toSp() },
        fontWeight = weight,
        maxLines = 1,
        textAlign = TextAlign.Center,
    )
}

/** Below this a label is unreadable on a watch, so a tight box clips rather than shrink further. */
const val MIN_TEXT_PX = 12f
