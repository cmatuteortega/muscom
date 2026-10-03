package com.muscom.app.ui.cards

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import com.muscom.app.engine.Card
import com.muscom.app.engine.Cards
import com.muscom.app.engine.Suit
import com.muscom.app.ui.theme.LocalTableColors
import com.muscom.app.ui.theme.TableColors
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Baraja española de diseño propio dibujada con Canvas (sin assets de terceros).
 */
const val CARD_RATIO = 0.66f

@Composable
fun CardFace(card: Card, width: Dp, modifier: Modifier = Modifier, description: String? = null) {
    val colors = LocalTableColors.current
    val tm = rememberTextMeasurer()
    Canvas(
        modifier
            .width(width)
            .aspectRatio(CARD_RATIO)
            .semantics { if (description != null) contentDescription = description },
    ) { drawCardFace(card, colors, tm) }
}

@Composable
fun CardBack(width: Dp, modifier: Modifier = Modifier) {
    val colors = LocalTableColors.current
    Canvas(modifier.width(width).aspectRatio(CARD_RATIO)) { drawCardBack(colors) }
}

@Composable
fun SuitIcon(suit: Suit, size: Dp, modifier: Modifier = Modifier) {
    val colors = LocalTableColors.current
    Box(modifier) {
        Canvas(Modifier.width(size).aspectRatio(1f)) {
            drawSuit(suit, center, this.size.minDimension * 0.9f, colors.suit[suit.ordinal])
        }
    }
}

// ───────────────────────── dibujo ─────────────────────────

private fun DrawScope.cardOutline(): Path {
    val r = size.width * 0.09f
    return Path().apply {
        addRoundRect(androidx.compose.ui.geometry.RoundRect(0f, 0f, size.width, size.height, CornerRadius(r, r)))
    }
}

fun DrawScope.drawCardBack(colors: TableColors) {
    val outline = cardOutline()
    val w = size.width
    val h = size.height
    clipPath(outline) {
        drawRect(colors.cardBack)
        val step = w / 5f
        val line = colors.cardBackAccent.copy(alpha = 0.35f)
        var x = -h
        while (x < w + h) {
            drawLine(line, Offset(x, 0f), Offset(x + h, h), strokeWidth = w * 0.015f)
            drawLine(line, Offset(x + h, 0f), Offset(x, h), strokeWidth = w * 0.015f)
            x += step
        }
        drawRoundRect(
            colors.cardBackAccent, topLeft = Offset(w * 0.08f, h * 0.06f), size = Size(w * 0.84f, h * 0.88f),
            cornerRadius = CornerRadius(w * 0.06f), style = Stroke(w * 0.025f),
        )
        drawOval(colors.cardBack, topLeft = Offset(w * 0.22f, h * 0.34f), size = Size(w * 0.56f, h * 0.32f))
        drawOval(colors.cardBackAccent, topLeft = Offset(w * 0.22f, h * 0.34f), size = Size(w * 0.56f, h * 0.32f), style = Stroke(w * 0.03f))
        drawSuit(Suit.OROS, center, w * 0.26f, colors.cardBackAccent)
    }
    drawPath(outline, Color.Black.copy(alpha = 0.35f), style = Stroke(w * 0.02f))
}

fun DrawScope.drawCardFace(card: Card, colors: TableColors, tm: TextMeasurer) {
    val suit = Cards.suitOf(card)
    val rank = Cards.rankOf(card)
    val suitColor = colors.suit[suit.ordinal]
    val w = size.width
    val h = size.height
    val outline = cardOutline()
    clipPath(outline) {
        drawRect(colors.cardFace)
        drawRect(
            Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.0f), Color.Black.copy(alpha = 0.06f))),
        )
        // Marco con el color del palo (pinta interrumpida típica de la baraja española).
        drawRoundRect(
            suitColor.copy(alpha = 0.85f), topLeft = Offset(w * 0.07f, h * 0.05f), size = Size(w * 0.86f, h * 0.9f),
            cornerRadius = CornerRadius(w * 0.05f), style = Stroke(w * 0.022f),
        )
    }
    drawPath(outline, Color.Black.copy(alpha = 0.3f), style = Stroke(w * 0.015f))

    // Índices.
    val label = rank.toString()
    val style = TextStyle(
        color = colors.cardInk, fontSize = (w * 0.2f).toSp(), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif,
    )
    val layout = tm.measure(label, style)
    drawText(layout, topLeft = Offset(w * 0.12f, h * 0.06f))
    rotate(180f) { drawText(layout, topLeft = Offset(w * 0.12f, h * 0.06f)) }

    val area = Size(w * 0.7f, h * 0.68f)
    val origin = Offset((w - area.width) / 2f, h * 0.2f)
    if (rank <= 7) {
        val pip = min(area.width, area.height) * (if (rank == 1) 0.62f else 0.3f)
        for ((px, py) in pipLayout(rank)) {
            drawSuit(suit, Offset(origin.x + px * area.width, origin.y + py * area.height * 0.92f), pip, suitColor)
        }
    } else {
        drawFigure(rank, suit, suitColor, colors, origin, area, tm)
    }
}

private fun pipLayout(rank: Int): List<Pair<Float, Float>> = when (rank) {
    1 -> listOf(0.5f to 0.5f)
    2 -> listOf(0.5f to 0.15f, 0.5f to 0.85f)
    3 -> listOf(0.5f to 0.12f, 0.5f to 0.5f, 0.5f to 0.88f)
    4 -> listOf(0.25f to 0.15f, 0.75f to 0.15f, 0.25f to 0.85f, 0.75f to 0.85f)
    5 -> listOf(0.25f to 0.15f, 0.75f to 0.15f, 0.5f to 0.5f, 0.25f to 0.85f, 0.75f to 0.85f)
    6 -> listOf(0.25f to 0.12f, 0.75f to 0.12f, 0.25f to 0.5f, 0.75f to 0.5f, 0.25f to 0.88f, 0.75f to 0.88f)
    else -> listOf(
        0.25f to 0.1f, 0.75f to 0.1f, 0.5f to 0.32f, 0.25f to 0.55f, 0.75f to 0.55f, 0.25f to 0.9f, 0.75f to 0.9f,
    )
}

/** Figuras estilizadas propias: sota (gorro), caballo (herradura) y rey (corona). */
private fun DrawScope.drawFigure(
    rank: Int, suit: Suit, suitColor: Color, colors: TableColors, origin: Offset, area: Size, tm: TextMeasurer,
) {
    val cx = origin.x + area.width / 2f
    val top = origin.y
    val u = area.width
    // Arco de fondo
    drawRoundRect(
        suitColor.copy(alpha = 0.12f), topLeft = Offset(origin.x + u * 0.05f, top), size = Size(u * 0.9f, area.height * 0.95f),
        cornerRadius = CornerRadius(u * 0.4f, u * 0.4f),
    )
    // Cuerpo: túnica trapezoidal
    val body = Path().apply {
        moveTo(cx - u * 0.18f, top + area.height * 0.42f)
        lineTo(cx + u * 0.18f, top + area.height * 0.42f)
        lineTo(cx + u * 0.32f, top + area.height * 0.93f)
        lineTo(cx - u * 0.32f, top + area.height * 0.93f)
        close()
    }
    drawPath(body, suitColor)
    drawPath(body, colors.cardInk.copy(alpha = 0.5f), style = Stroke(u * 0.015f))
    // Cinturón
    drawLine(colors.cardFace, Offset(cx - u * 0.24f, top + area.height * 0.62f), Offset(cx + u * 0.24f, top + area.height * 0.62f), strokeWidth = u * 0.03f)
    // Cabeza
    val headR = u * 0.13f
    val headC = Offset(cx, top + area.height * 0.3f)
    drawCircle(Color(0xFFF2C9A0), headR, headC)
    drawCircle(colors.cardInk.copy(alpha = 0.6f), headR, headC, style = Stroke(u * 0.015f))
    when (rank) {
        10 -> { // gorro de sota
            val cap = Path().apply {
                moveTo(headC.x - headR * 1.1f, headC.y - headR * 0.4f)
                quadraticTo(headC.x, headC.y - headR * 2.2f, headC.x + headR * 1.3f, headC.y - headR * 0.6f)
                close()
            }
            drawPath(cap, suitColor)
        }
        11 -> { // herradura del caballo
            val shoe = Path().apply {
                arcTo(
                    androidx.compose.ui.geometry.Rect(headC.x - headR * 1.1f, headC.y - headR * 2.6f, headC.x + headR * 1.1f, headC.y - headR * 0.6f),
                    150f, 240f, true,
                )
            }
            drawPath(shoe, Color(0xFF6B6B6B), style = Stroke(u * 0.05f, cap = StrokeCap.Round))
        }
        else -> { // corona del rey
            val by = headC.y - headR * 0.8f
            val crown = Path().apply {
                moveTo(headC.x - headR * 1.1f, by)
                lineTo(headC.x - headR * 1.1f, by - headR * 1.2f)
                lineTo(headC.x - headR * 0.55f, by - headR * 0.55f)
                lineTo(headC.x, by - headR * 1.5f)
                lineTo(headC.x + headR * 0.55f, by - headR * 0.55f)
                lineTo(headC.x + headR * 1.1f, by - headR * 1.2f)
                lineTo(headC.x + headR * 1.1f, by)
                close()
            }
            drawPath(crown, Color(0xFFE0A526))
            drawPath(crown, Color(0xFF9C6A10), style = Stroke(u * 0.012f))
        }
    }
    // Emblema del palo sobre la túnica
    drawSuit(suit, Offset(cx, top + area.height * 0.78f), u * 0.3f, colors.cardFace)
    drawSuit(suit, Offset(cx, top + area.height * 0.78f), u * 0.24f, suitColor.copy(alpha = 1f).let { blend(it, Color.Black, 0.15f) })
    // Inicial
    val letter = when (rank) { 10 -> "S"; 11 -> "C"; else -> "R" }
    val l = tm.measure(letter, TextStyle(color = colors.cardInk, fontSize = (u * 0.16f).toSp(), fontWeight = FontWeight.Black, fontFamily = FontFamily.Serif))
    drawText(l, topLeft = Offset(origin.x + u * 0.86f - l.size.width, top + area.height * 0.02f))
}

private fun blend(a: Color, b: Color, t: Float) = Color(
    red = a.red + (b.red - a.red) * t,
    green = a.green + (b.green - a.green) * t,
    blue = a.blue + (b.blue - a.blue) * t,
    alpha = a.alpha,
)

/** Dibuja el símbolo de un palo centrado en [c] con tamaño [s]. */
fun DrawScope.drawSuit(suit: Suit, c: Offset, s: Float, color: Color) {
    when (suit) {
        Suit.OROS -> {
            drawCircle(color, s * 0.45f, c)
            drawCircle(blend(color, Color.Black, 0.35f), s * 0.45f, c, style = Stroke(s * 0.05f))
            drawCircle(blend(color, Color.White, 0.45f), s * 0.28f, c)
            for (i in 0 until 8) {
                val a = Math.toRadians(i * 45.0)
                val p1 = Offset(c.x + cos(a).toFloat() * s * 0.12f, c.y + sin(a).toFloat() * s * 0.12f)
                val p2 = Offset(c.x + cos(a).toFloat() * s * 0.24f, c.y + sin(a).toFloat() * s * 0.24f)
                drawLine(color, p1, p2, strokeWidth = s * 0.04f, cap = StrokeCap.Round)
            }
            drawCircle(color, s * 0.07f, c)
        }
        Suit.COPAS -> {
            val dark = blend(color, Color.Black, 0.3f)
            val bowl = Path().apply {
                moveTo(c.x - s * 0.36f, c.y - s * 0.36f)
                lineTo(c.x + s * 0.36f, c.y - s * 0.36f)
                cubicTo(c.x + s * 0.36f, c.y + s * 0.05f, c.x + s * 0.15f, c.y + s * 0.12f, c.x + s * 0.05f, c.y + s * 0.12f)
                lineTo(c.x - s * 0.05f, c.y + s * 0.12f)
                cubicTo(c.x - s * 0.15f, c.y + s * 0.12f, c.x - s * 0.36f, c.y + s * 0.05f, c.x - s * 0.36f, c.y - s * 0.36f)
                close()
            }
            drawPath(bowl, color)
            drawPath(bowl, dark, style = Stroke(s * 0.04f))
            drawRect(color, topLeft = Offset(c.x - s * 0.05f, c.y + s * 0.1f), size = Size(s * 0.1f, s * 0.22f))
            drawOval(color, topLeft = Offset(c.x - s * 0.25f, c.y + s * 0.3f), size = Size(s * 0.5f, s * 0.14f))
            drawOval(dark, topLeft = Offset(c.x - s * 0.25f, c.y + s * 0.3f), size = Size(s * 0.5f, s * 0.14f), style = Stroke(s * 0.03f))
            drawOval(Color(0xFFE0A526), topLeft = Offset(c.x - s * 0.3f, c.y - s * 0.42f), size = Size(s * 0.6f, s * 0.12f))
        }
        Suit.ESPADAS -> translate(c.x, c.y) {
            rotate(-20f, pivot = Offset.Zero) {
                val blade = Path().apply {
                    moveTo(-s * 0.06f, -s * 0.18f)
                    lineTo(s * 0.06f, -s * 0.18f)
                    lineTo(s * 0.035f, s * 0.42f)
                    lineTo(0f, s * 0.5f)
                    lineTo(-s * 0.035f, s * 0.42f)
                    close()
                }
                drawPath(blade, color)
                drawLine(blend(color, Color.White, 0.5f), Offset(0f, -s * 0.15f), Offset(0f, s * 0.42f), strokeWidth = s * 0.015f)
                drawLine(Color(0xFFE0A526), Offset(-s * 0.2f, -s * 0.2f), Offset(s * 0.2f, -s * 0.2f), strokeWidth = s * 0.06f, cap = StrokeCap.Round)
                drawLine(blend(color, Color.Black, 0.4f), Offset(0f, -s * 0.22f), Offset(0f, -s * 0.4f), strokeWidth = s * 0.07f)
                drawCircle(Color(0xFFE0A526), s * 0.06f, Offset(0f, -s * 0.45f))
            }
        }
        Suit.BASTOS -> translate(c.x, c.y) {
            rotate(18f, pivot = Offset.Zero) {
                val club = Path().apply {
                    moveTo(-s * 0.07f, s * 0.48f)
                    lineTo(s * 0.07f, s * 0.48f)
                    quadraticTo(s * 0.2f, -s * 0.1f, s * 0.14f, -s * 0.45f)
                    quadraticTo(0f, -s * 0.55f, -s * 0.14f, -s * 0.45f)
                    quadraticTo(-s * 0.2f, -s * 0.1f, -s * 0.07f, s * 0.48f)
                    close()
                }
                drawPath(club, color)
                drawPath(club, blend(color, Color.Black, 0.35f), style = Stroke(s * 0.035f))
                for (k in listOf(-0.25f, 0.0f, 0.22f)) {
                    drawCircle(blend(color, Color.Black, 0.35f), s * 0.035f, Offset(s * 0.06f, s * k))
                    drawCircle(blend(color, Color.Black, 0.35f), s * 0.03f, Offset(-s * 0.07f, s * (k + 0.1f)))
                }
                drawLine(Color(0xFF8BC34A), Offset(s * 0.1f, -s * 0.3f), Offset(s * 0.25f, -s * 0.38f), strokeWidth = s * 0.05f, cap = StrokeCap.Round)
            }
        }
    }
}

/** Tapete con degradado radial y anillo central. */
@Composable
fun Felt(modifier: Modifier = Modifier) {
    val colors = LocalTableColors.current
    Canvas(modifier) {
        drawRect(Brush.radialGradient(listOf(colors.feltCenter, colors.feltEdge), center = center, radius = size.maxDimension * 0.7f))
        val r = size.minDimension * 0.28f
        drawCircle(Color.White.copy(alpha = 0.06f), r, center, style = Stroke(size.minDimension * 0.012f))
        drawCircle(Color.White.copy(alpha = 0.04f), r * 1.25f, center, style = Stroke(size.minDimension * 0.004f))
    }
}
