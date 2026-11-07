package com.braveboy.hesabi.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

object HesabiSpacing {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val huge = 48.dp
}

object HesabiRadius {
    val chip = 999.dp
    val input = 12.dp
    val card = 16.dp
    val glassCard = 20.dp
    val sheet = 28.dp
    val hero = 24.dp
    val dock = 32.dp
    val glow = 40.dp
}

val HesabiShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(HesabiRadius.input),
    medium = RoundedCornerShape(HesabiRadius.card),
    large = RoundedCornerShape(HesabiRadius.sheet),
    extraLarge = RoundedCornerShape(32.dp),
)
