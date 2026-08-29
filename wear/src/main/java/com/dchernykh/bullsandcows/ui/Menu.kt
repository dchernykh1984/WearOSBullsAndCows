package com.dchernykh.bullsandcows.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dchernykh.bullsandcows.layout.centeredBox
import kotlin.math.roundToInt
import com.dchernykh.bullsandcows.layout.Box as LayoutBox

// The menus: a stack of lines and buttons on a panel over the whole round face.
// Unlike the board, a menu is not confined to the circle the keypad ring leaves -
// the ring is gone while a menu is up, so the menu gets the screen.

sealed interface MenuItem {
    val height: Int

    data class Line(
        override val height: Int,
        val color: Color,
        val text: String,
    ) : MenuItem

    data class Action(
        override val height: Int,
        val text: String,
        val onClick: () -> Unit,
    ) : MenuItem

    data class Gap(
        override val height: Int,
    ) : MenuItem
}

/** The type scale of the menus, derived from the screen so it holds at any size. */
class MenuMetrics(
    screenSize: Int,
) {
    val big = (screenSize * 0.1f).roundToInt()
    val row = (screenSize * 0.082f).roundToInt()
    val small = (screenSize * 0.062f).roundToInt()
    val button = (screenSize * 0.11f).roundToInt()
    val gap = (screenSize * 0.022f).roundToInt()
    val maxWidth = screenSize * 0.82f
}

@Composable
fun MenuOverlay(
    screenSize: Int,
    metrics: MenuMetrics,
    items: List<MenuItem>,
) {
    // Opaque, and over everything: a menu here replaces the board rather than
    // pausing over it, because the keypad ring would otherwise show around it and
    // invite taps that do nothing.
    Box(modifier = Modifier.absoluteBox(LayoutBox(0, 0, screenSize, screenSize)).background(ColorBackground))

    val stackHeight = items.sumOf { it.height }
    var y = (screenSize / 2f).roundToInt() - (stackHeight / 2f).roundToInt()
    for (item in items) {
        val box =
            centeredBox(screenSize, screenSize / 2f, y, item.height, metrics.maxWidth, SCREEN_PADDING)
        when (item) {
            is MenuItem.Gap -> Unit
            is MenuItem.Line -> MenuLine(box, item.color, item.text)
            is MenuItem.Action ->
                PillButton(box = box, text = item.text, onClick = item.onClick)
        }
        y += item.height
    }
}

@Composable
fun MenuLine(
    box: LayoutBox,
    color: Color,
    text: String,
) {
    Box(modifier = Modifier.absoluteBox(box), contentAlignment = Alignment.Center) {
        FittedText(text = text, color = color, boxHeight = box.h, boxWidth = box.w, fraction = 0.76f)
    }
}

/** A row of the records table: the level on the left, its two figures on the right. */
@Composable
fun RecordRow(
    box: LayoutBox,
    level: String,
    attempts: String,
    time: String,
) {
    Box(
        modifier =
            Modifier
                .absoluteBox(box)
                .clip(RoundedCornerShape(4.dp))
                .background(ColorPanel),
        contentAlignment = Alignment.Center,
    ) {
        FittedText(
            text = "$level   $attempts   $time",
            color = ColorText,
            boxHeight = box.h,
            boxWidth = box.w,
            fraction = 0.62f,
        )
    }
}
