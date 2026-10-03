package com.muscom.app.ui.game

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muscom.app.R
import com.muscom.app.engine.Card
import com.muscom.app.engine.HandSummary
import com.muscom.app.engine.Lance
import com.muscom.app.engine.LanceOutcome
import com.muscom.app.engine.MusAction
import com.muscom.app.engine.MusView
import com.muscom.app.engine.Phase
import com.muscom.app.engine.Sena
import com.muscom.app.engine.teamOf
import com.muscom.app.ui.cards.CardBack
import com.muscom.app.ui.cards.CardFace
import com.muscom.app.ui.cards.Felt
import com.muscom.app.ui.theme.LocalTableColors

// Asientos en pantalla: 0 abajo (tú), 1 derecha, 2 arriba (compañero), 3 izquierda.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    ui: GameUiState,
    onAction: (MusAction) -> Unit,
    onToggleCard: (Card) -> Unit,
    onDiscard: () -> Unit,
    onEnvido: (Int) -> Unit,
    onSena: (Sena) -> Unit,
    onNextHand: () -> Unit,
    onRematch: () -> Unit,
    onExit: () -> Unit,
) {
    val view = ui.view
    var showHistory by rememberSaveable { mutableStateOf(false) }
    var showSenas by rememberSaveable { mutableStateOf(false) }
    var confirmExit by rememberSaveable { mutableStateOf(false) }
    val colors = LocalTableColors.current

    BackHandler { if (view?.phase == Phase.FIN_PARTIDA) onExit() else confirmExit = true }

    Box(Modifier.fillMaxSize().background(colors.feltEdge)) {
        Felt(Modifier.fillMaxSize())
        if (view == null) return@Box
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            TopBar(view, ui, onBack = { confirmExit = true }, onHistory = { showHistory = true })
            LanceTrack(view)
            Box(Modifier.weight(1f).fillMaxWidth()) {
                SeatView(ui, 2, Modifier.align(Alignment.TopCenter).padding(top = 6.dp))
                SeatView(ui, 3, Modifier.align(Alignment.CenterStart).padding(start = 6.dp))
                SeatView(ui, 1, Modifier.align(Alignment.CenterEnd).padding(end = 6.dp))
                CenterInfo(ui, Modifier.align(Alignment.Center))
            }
            MySeatHeader(ui)
            MyHand(ui, onToggleCard)
            ActionBar(ui, onAction, onDiscard, onEnvido, onOpenSenas = { showSenas = true })
        }
        val summary = view.summary
        if ((view.phase == Phase.FIN_MANO || view.phase == Phase.FIN_PARTIDA) && summary != null) {
            SummaryOverlay(view, summary, ui.names, onNextHand, onRematch, onExit)
        }
    }

    if (showHistory && view != null) {
        ModalBottomSheet(onDismissRequest = { showHistory = false }) { HistorySheet(view, ui) }
    }
    if (showSenas && view != null) {
        ModalBottomSheet(onDismissRequest = { showSenas = false }) {
            SenasSheet(view, ui.names, onSena = { onSena(it); showSenas = false })
        }
    }
    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text(stringResource(R.string.exit_title)) },
            text = { Text(stringResource(R.string.exit_body)) },
            confirmButton = { TextButton(onClick = { confirmExit = false; onExit() }) { Text(stringResource(R.string.exit_confirm)) } },
            dismissButton = { TextButton(onClick = { confirmExit = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

// ───────────────────────── barra superior ─────────────────────────

@Composable
private fun TopBar(view: MusView, ui: GameUiState, onBack: () -> Unit, onHistory: () -> Unit) {
    val colors = LocalTableColors.current
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), tint = colors.onFelt)
        }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ScoreBox(stringResource(R.string.team_us), view.scores[0], view.config.target, colors.teamUs, Modifier.weight(1f))
            ScoreBox(stringResource(R.string.team_them), view.scores[1], view.config.target, colors.teamThem, Modifier.weight(1f))
        }
        IconButton(onClick = onHistory) {
            Icon(Icons.AutoMirrored.Filled.List, stringResource(R.string.history), tint = colors.onFelt)
        }
    }
}

@Composable
private fun ScoreBox(label: String, score: Int, target: Int, color: Color, modifier: Modifier) {
    val desc = stringResource(R.string.score_desc, label, score, target)
    Column(modifier.semantics(mergeDescendants = true) { contentDescription = desc }) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(label, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f), maxLines = 1)
            Text("$score", color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp)
            Text("/$target", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
        }
        LinearProgressIndicator(
            progress = { score / target.toFloat() },
            color = color,
            trackColor = Color.White.copy(alpha = 0.15f),
            strokeCap = StrokeCap.Round,
            modifier = Modifier.fillMaxWidth().height(5.dp),
        )
    }
}

@Composable
private fun LanceTrack(view: MusView) {
    val colors = LocalTableColors.current
    val order = listOf(Lance.GRANDE, Lance.CHICA, Lance.PARES, if (view.phase == Phase.PUNTO || view.lances.any { it.lance == Lance.PUNTO }) Lance.PUNTO else Lance.JUEGO)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        for (l in order) {
            val rec = view.lances.lastOrNull { it.lance == l }
            val current = view.phase.lance == l
            val bg = when {
                current -> colors.highlight
                rec != null -> Color.White.copy(alpha = 0.18f)
                else -> Color.Black.copy(alpha = 0.18f)
            }
            val fg = if (current) Color.Black else Color.White
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(bg).padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(lanceName(l), color = fg, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(
                    rec?.let { shortOutcome(it.outcome, it.amount, it.ordago) } ?: if (current) "•" else " ",
                    color = fg.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall, maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun shortOutcome(o: LanceOutcome, amount: Int, ordago: Boolean): String = when {
    ordago && o == LanceOutcome.QUERIDO -> stringResource(R.string.short_ordago)
    o == LanceOutcome.PASO -> stringResource(R.string.short_paso)
    o == LanceOutcome.QUERIDO -> stringResource(R.string.short_querido, amount)
    o == LanceOutcome.NO_QUERIDO -> stringResource(R.string.short_no_querido, amount)
    o == LanceOutcome.SIN_APUESTA -> stringResource(R.string.short_sin_apuesta)
    else -> stringResource(R.string.short_no_hay)
}

// ───────────────────────── asientos ─────────────────────────

@Composable
fun rememberTimerProgress(timer: TurnTimer?): Float {
    val p by produceState(initialValue = 1f, key1 = timer) {
        if (timer == null) {
            value = 0f
            return@produceState
        }
        while (true) {
            withFrameMillis { }
            val el = SystemClock.elapsedRealtime() - timer.startedAt
            value = (1f - el.toFloat() / timer.durationMs).coerceIn(0f, 1f)
            if (value <= 0f) break
        }
    }
    return p
}

@Composable
private fun Avatar(name: String, seat: Int, active: Boolean, timer: TurnTimer?, size: Dp = 46.dp) {
    val colors = LocalTableColors.current
    val teamColor = if (teamOf(seat) == 0) colors.teamUs else colors.teamThem
    val progress = if (timer?.seat == seat) rememberTimerProgress(timer) else 0f
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size + 8.dp)) {
        Canvas(Modifier.size(size + 8.dp)) {
            if (active) {
                drawCircle(colors.highlight.copy(alpha = 0.25f))
                val warn = progress < 0.25f
                drawArc(
                    color = if (warn) Color(0xFFFF5252) else colors.highlight,
                    startAngle = -90f, sweepAngle = 360f * progress, useCenter = false,
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
                    topLeft = Offset(2.dp.toPx(), 2.dp.toPx()),
                    size = Size(this.size.width - 4.dp.toPx(), this.size.height - 4.dp.toPx()),
                )
            }
        }
        Box(
            Modifier.size(size).clip(CircleShape).background(teamColor).border(2.dp, Color.White.copy(alpha = 0.8f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
        }
    }
}

@Composable
private fun Badges(view: MusView, seat: Int) {
    val colors = LocalTableColors.current
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        if (view.mano == seat) Badge(stringResource(R.string.badge_mano), colors.highlight, Color.Black)
        if (view.dealer == seat) Badge(stringResource(R.string.badge_dealer), Color.White.copy(alpha = 0.25f), Color.White)
        view.declarations.pares?.let { Badge(stringResource(if (it[seat]) R.string.badge_pares_si else R.string.badge_pares_no), Color.Black.copy(alpha = 0.3f), Color.White) }
        view.declarations.juego?.let { Badge(stringResource(if (it[seat]) R.string.badge_juego_si else R.string.badge_juego_no), Color.Black.copy(alpha = 0.3f), Color.White) }
    }
}

@Composable
private fun Badge(text: String, bg: Color, fg: Color) {
    Text(
        text, color = fg, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1,
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(bg).padding(horizontal = 5.dp, vertical = 1.dp),
    )
}

@Composable
private fun SpeechBubble(bubble: Bubble?, modifier: Modifier = Modifier) {
    AnimatedVisibility(bubble != null, modifier = modifier, enter = scaleIn() + fadeIn(), exit = fadeOut() + scaleOut()) {
        val b = bubble ?: return@AnimatedVisibility
        val isSena = b.action is MusAction.SenaAction
        val isOrdago = b.action is MusAction.Ordago
        val bg = when {
            isOrdago -> Color(0xFFD32F2F)
            isSena -> Color(0xFF263238)
            else -> Color.White
        }
        val fg = if (isOrdago || isSena) Color.White else Color.Black
        Text(
            if (isSena) "👁 " + actionText(b.action) else actionText(b.action),
            color = fg, fontWeight = FontWeight.Bold, fontSize = if (isOrdago) 18.sp else 14.sp,
            modifier = Modifier
                .shadow(4.dp, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .background(bg)
                .padding(horizontal = 10.dp, vertical = 5.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

@Composable
private fun SeatView(ui: GameUiState, seat: Int, modifier: Modifier) {
    val view = ui.view ?: return
    val active = seat in GameUiState.actorsOf(view) && view.phase != Phase.FIN_MANO
    val vertical = seat == 2
    Column(modifier.widthIn(max = 150.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (!vertical) SpeechBubble(ui.bubbles[seat])
        Avatar(ui.names[seat], seat, active, ui.timer)
        Text(ui.names[seat], color = Color.White, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Badges(view, seat)
        Spacer(Modifier.height(4.dp))
        OpponentCards(view, seat)
        if (view.musRound > 0 && view.phase == Phase.MUS && view.lastDiscardCounts[seat] > 0) {
            Text(stringResource(R.string.discarded_n, view.lastDiscardCounts[seat]), color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
        }
        if (view.phase == Phase.DESCARTE) {
            Text(
                stringResource(if (seat in view.pendingDiscard) R.string.discarding else R.string.discard_done),
                color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp,
            )
        }
        if (vertical) SpeechBubble(ui.bubbles[seat], Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun OpponentCards(view: MusView, seat: Int) {
    val revealed = view.revealed?.get(seat)
    Row(horizontalArrangement = Arrangement.spacedBy((-12).dp)) {
        if (revealed != null) {
            revealed.forEach { CardFace(it, 30.dp, description = cardName(it)) }
        } else {
            repeat(4) { CardBack(24.dp, Modifier.graphicsLayer { rotationZ = (it - 1.5f) * 6f }) }
        }
    }
}

// ───────────────────────── centro ─────────────────────────

@Composable
private fun CenterInfo(ui: GameUiState, modifier: Modifier) {
    val view = ui.view ?: return
    val colors = LocalTableColors.current
    Column(modifier.widthIn(max = 190.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            phaseName(view.phase).uppercase(), color = colors.highlight, fontWeight = FontWeight.Black, fontSize = 22.sp,
            letterSpacing = 2.sp, modifier = Modifier.semantics { heading() },
        )
        val b = view.betting
        when {
            b?.ordago == true -> Text(stringResource(R.string.ordago_on_table), color = Color(0xFFFF5252), fontWeight = FontWeight.Black, fontSize = 20.sp)
            b != null && b.proposed > 0 -> {
                Text(stringResource(R.string.bet_on_table, b.proposed), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(stringResource(R.string.deje_info, b.deje), color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            }
            view.phase == Phase.MUS && view.musRound > 0 ->
                Text(stringResource(R.string.mus_round, view.musRound + 1), color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
            view.phase == Phase.MUS ->
                Text(stringResource(R.string.hand_n, view.handNumber), color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
        }
        Spacer(Modifier.height(8.dp))
        AnimatedVisibility(ui.banner != null, enter = fadeIn() + scaleIn(), exit = fadeOut()) {
            val r = ui.banner ?: return@AnimatedVisibility
            Surface(shape = RoundedCornerShape(12.dp), color = Color.Black.copy(alpha = 0.55f)) {
                Text(
                    "${lanceName(r.lance)}: ${outcomeText(r)}",
                    color = Color.White, textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp).semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
        }
    }
}

// ───────────────────────── mi mano ─────────────────────────

@Composable
private fun MySeatHeader(ui: GameUiState) {
    val view = ui.view ?: return
    val active = HUMAN in GameUiState.actorsOf(view) && view.phase != Phase.FIN_MANO
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Avatar(ui.names[HUMAN], HUMAN, active, ui.timer, size = 36.dp)
        Column(Modifier.weight(1f)) {
            Text(ui.names[HUMAN], color = Color.White, style = MaterialTheme.typography.labelLarge, maxLines = 1)
            Badges(view, HUMAN)
        }
        SpeechBubble(ui.bubbles[HUMAN])
        val pts = com.muscom.app.engine.Evaluate.handPoints(view.hand, view.config.reyes)
        Text(stringResource(R.string.my_points, pts), color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
    }
}

@Composable
private fun MyHand(ui: GameUiState, onToggle: (Card) -> Unit) {
    val view = ui.view ?: return
    val discarding = view.phase == Phase.DESCARTE && HUMAN in view.pendingDiscard
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
    ) {
        view.hand.forEachIndexed { i, card ->
            val selected = card in ui.selected
            val lift by animateDpAsState(if (selected) (-22).dp else 0.dp, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "lift")
            val inspection = androidx.compose.ui.platform.LocalInspectionMode.current
            val anim = remember(ui.dealKey, card) { Animatable(if (inspection) 1f else 0f) }
            LaunchedEffect(ui.dealKey, card) {
                kotlinx.coroutines.delay(i * 90L)
                anim.animateTo(1f, tween(320, easing = FastOutSlowInEasing))
            }
            val name = cardName(card)
            val desc = if (selected) stringResource(R.string.card_selected, name) else name
            Box(
                Modifier
                    .weight(1f, fill = false)
                    .offset(y = lift)
                    .graphicsLayer {
                        translationY = (1f - anim.value) * -600f
                        alpha = anim.value
                        rotationZ = (i - 1.5f) * 2.5f * anim.value
                    }
                    .then(
                        if (discarding) Modifier.clickable(role = Role.Checkbox) { onToggle(card) } else Modifier,
                    )
                    .semantics { this.selected = selected; contentDescription = desc },
            ) {
                CardFace(card, 82.dp, Modifier.shadow(if (selected) 10.dp else 3.dp, RoundedCornerShape(8.dp)))
                if (selected) {
                    Box(
                        Modifier.matchParentSize().clip(RoundedCornerShape(8.dp))
                            .border(3.dp, LocalTableColors.current.highlight, RoundedCornerShape(8.dp)),
                    )
                }
            }
        }
    }
}

// ───────────────────────── acciones ─────────────────────────

@Composable
private fun ActionBar(
    ui: GameUiState,
    onAction: (MusAction) -> Unit,
    onDiscard: () -> Unit,
    onEnvido: (Int) -> Unit,
    onOpenSenas: () -> Unit,
) {
    val view = ui.view ?: return
    val myTurn = HUMAN in GameUiState.actorsOf(view)
    Surface(color = Color.Black.copy(alpha = 0.35f), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp).heightIn(min = 64.dp), verticalArrangement = Arrangement.Center) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val canSena = view.phase != Phase.FIN_MANO && view.phase != Phase.FIN_PARTIDA &&
                    view.senas.count { it.seat == HUMAN } < view.config.maxSenasPerHand
                IconButton(onClick = onOpenSenas, enabled = canSena) {
                    Icon(Icons.Filled.Face, stringResource(R.string.senas), tint = if (canSena) Color.White else Color.White.copy(alpha = 0.3f))
                }
                Box(Modifier.weight(1f)) {
                    if (!myTurn || view.phase == Phase.FIN_MANO || view.phase == Phase.FIN_PARTIDA) {
                        val waiting = GameUiState.actorsOf(view).firstOrNull()
                        val text = when {
                            view.phase == Phase.DESCARTE -> stringResource(R.string.waiting_discards)
                            waiting != null && view.phase != Phase.FIN_MANO -> stringResource(R.string.waiting_for, ui.names[waiting])
                            else -> ""
                        }
                        Text(text, color = Color.White.copy(alpha = 0.85f), modifier = Modifier.padding(8.dp))
                    } else {
                        TurnActions(view, ui, onAction, onDiscard, onEnvido)
                    }
                }
            }
        }
    }
}

@Composable
private fun TurnActions(
    view: MusView,
    ui: GameUiState,
    onAction: (MusAction) -> Unit,
    onDiscard: () -> Unit,
    onEnvido: (Int) -> Unit,
) {
    val b = view.betting
    val pad = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 8.dp)
    val outlined = ButtonDefaults.outlinedButtonColors(contentColor = Color.White, disabledContentColor = Color.White.copy(alpha = 0.3f))
    val outlinedBorder = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.6f))
    val ordagoColors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828), contentColor = Color.White)
    val primary = ButtonDefaults.buttonColors(containerColor = LocalTableColors.current.highlight, contentColor = Color.Black)
    when {
        view.phase == Phase.MUS -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onAction(MusAction.Mus) }, colors = primary, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.act_mus)) }
            FilledTonalButton(onClick = { onAction(MusAction.NoMus) }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.act_no_mus)) }
        }
        view.phase == Phase.DESCARTE -> Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.discard_hint), color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Button(onClick = onDiscard, enabled = ui.selected.isNotEmpty(), colors = primary) {
                Text(stringResource(R.string.discard_n, ui.selected.size))
            }
        }
        b == null -> {}
        b.ordago -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = { onAction(MusAction.NoQuiero) }, modifier = Modifier.weight(1f), contentPadding = pad) { Text(stringResource(R.string.act_no_quiero)) }
            Button(onClick = { onAction(MusAction.Quiero) }, colors = ordagoColors, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.act_quiero_ordago)) }
        }
        else -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val hasBet = b.proposed > 0
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (hasBet) {
                    FilledTonalButton(onClick = { onAction(MusAction.NoQuiero) }, modifier = Modifier.weight(1f), contentPadding = pad) { Text(stringResource(R.string.act_no_quiero), maxLines = 1) }
                    Button(onClick = { onAction(MusAction.Quiero) }, colors = primary, modifier = Modifier.weight(1f), contentPadding = pad) { Text(stringResource(R.string.act_quiero), maxLines = 1) }
                } else {
                    FilledTonalButton(onClick = { onAction(MusAction.Paso) }, modifier = Modifier.weight(1f), contentPadding = pad) { Text(stringResource(R.string.act_paso), maxLines = 1) }
                }
                Button(onClick = { onAction(MusAction.Ordago) }, colors = ordagoColors, modifier = Modifier.weight(1f), contentPadding = pad) { Text(stringResource(R.string.act_ordago), maxLines = 1) }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = { onEnvido(ui.envidoAmount - 1) }, enabled = ui.envidoAmount > 2, modifier = Modifier.width(52.dp), colors = outlined, border = outlinedBorder, contentPadding = pad) { Text("−", fontSize = 20.sp) }
                Button(
                    onClick = { onAction(MusAction.Envido(ui.envidoAmount)) }, colors = primary, modifier = Modifier.weight(1f),
                ) {
                    Text(
                        if (hasBet) stringResource(R.string.act_raise_n, ui.envidoAmount) else stringResource(R.string.act_envido_n, ui.envidoAmount),
                        maxLines = 1,
                    )
                }
                OutlinedButton(onClick = { onEnvido(ui.envidoAmount + 1) }, enabled = ui.envidoAmount < 30, modifier = Modifier.width(52.dp), colors = outlined, border = outlinedBorder, contentPadding = pad) { Text("+", fontSize = 20.sp) }
            }
        }
    }
}

// ───────────────────────── fin de mano ─────────────────────────

@Composable
private fun BoxScope.SummaryOverlay(
    view: MusView,
    summary: HandSummary,
    names: List<String>,
    onNextHand: () -> Unit,
    onRematch: () -> Unit,
    onExit: () -> Unit,
) {
    val colors = LocalTableColors.current
    Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.55f)).clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) {})
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.align(Alignment.Center).padding(16.dp).fillMaxWidth(),
    ) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val title = when {
                view.phase == Phase.FIN_PARTIDA && view.winner == 0 -> stringResource(R.string.you_win)
                view.phase == Phase.FIN_PARTIDA -> stringResource(R.string.you_lose)
                else -> stringResource(R.string.hand_end_n, view.handNumber)
            }
            Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
            Text(
                stringResource(R.string.score_line, view.scores[0], view.scores[1]),
                style = MaterialTheme.typography.titleMedium,
            )
            HorizontalDivider()
            for (seat in listOf(0, 1, 2, 3)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        names[seat], modifier = Modifier.width(92.dp), maxLines = 1, overflow = TextOverflow.Ellipsis,
                        color = if (teamOf(seat) == 0) colors.teamUs else colors.teamThem, fontWeight = FontWeight.Bold,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        summary.hands[seat].forEach { CardFace(it, 34.dp, description = cardName(it)) }
                    }
                }
            }
            HorizontalDivider()
            for (rec in view.lances) {
                val w = summary.winners[rec.lance]
                Row {
                    Text(lanceName(rec.lance), fontWeight = FontWeight.SemiBold, modifier = Modifier.width(72.dp))
                    Text(outcomeText(rec), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    if (w != null) Text(names[w], style = MaterialTheme.typography.bodySmall, maxLines = 1)
                }
            }
            if (summary.awards.isNotEmpty()) {
                HorizontalDivider()
                for (a in summary.awards) {
                    Row {
                        Text("${lanceName(a.lance)} · ${awardKindText(a.kind)}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "+${a.points} " + stringResource(if (a.team == 0) R.string.team_us else R.string.team_them),
                            color = if (a.team == 0) colors.teamUs else colors.teamThem, fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            if (view.phase == Phase.FIN_PARTIDA) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onExit, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.exit_confirm)) }
                    Button(onClick = onRematch, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.rematch)) }
                }
            } else {
                Button(onClick = onNextHand, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.next_hand)) }
            }
        }
    }
}

// ───────────────────────── hojas ─────────────────────────

@Composable
private fun HistorySheet(view: MusView, ui: GameUiState) {
    LazyColumn(Modifier.fillMaxWidth().padding(horizontal = 16.dp).heightIn(max = 520.dp)) {
        item {
            Text(stringResource(R.string.history_hand, view.handNumber), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp))
        }
        val entries = view.log.filter { it.action !is MusAction.SiguienteMano }
        items(entries.size) { i ->
            val e = entries[i]
            Column {
                if (i == 0 || entries[i - 1].phase != e.phase) {
                    Text(phaseName(e.phase), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
                }
                Text("${ui.names[e.seat]}: ${actionText(e.action)}", style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (view.lances.isNotEmpty()) {
            item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
            items(view.lances) { r -> Text("${lanceName(r.lance)}: ${outcomeText(r)}", style = MaterialTheme.typography.bodyMedium) }
        }
        if (ui.pastHands.isNotEmpty()) {
            item {
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Text(stringResource(R.string.history_past), style = MaterialTheme.typography.titleMedium)
            }
            items(ui.pastHands.size) { i ->
                val h = ui.pastHands[i]
                val us = h.awards.filter { it.team == 0 }.sumOf { it.points }
                val them = h.awards.filter { it.team == 1 }.sumOf { it.points }
                Text(stringResource(R.string.history_hand_line, i + 1, us, them), style = MaterialTheme.typography.bodyMedium)
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun SenasSheet(view: MusView, names: List<String>, onSena: (Sena) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.senas_title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.senas_hint), style = MaterialTheme.typography.bodySmall)
        val partner = view.senas.filter { it.seat == 2 }
        if (partner.isNotEmpty()) {
            Text(stringResource(R.string.partner_senas, names[2]), style = MaterialTheme.typography.bodyMedium)
            partner.forEach { Text("• ${senaGestureLabel(it.sena)}", style = MaterialTheme.typography.bodyMedium) }
        }
        val remaining = view.config.maxSenasPerHand - view.senas.count { it.seat == HUMAN }
        Text(stringResource(R.string.senas_left, remaining), style = MaterialTheme.typography.labelMedium)
        Sena.entries.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { s ->
                    OutlinedButton(onClick = { onSena(s) }, enabled = remaining > 0, modifier = Modifier.weight(1f)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(senaName(s), fontWeight = FontWeight.Bold, maxLines = 1)
                            Text(senaGesture(s), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun senaGestureLabel(s: Sena) = "${senaName(s)} (${senaGesture(s)})"
