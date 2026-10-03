package com.muscom.app.ui.game

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.muscom.app.R
import com.muscom.app.engine.AwardKind
import com.muscom.app.engine.Card
import com.muscom.app.engine.Cards
import com.muscom.app.engine.Lance
import com.muscom.app.engine.LanceOutcome
import com.muscom.app.engine.LanceRecord
import com.muscom.app.engine.MusAction
import com.muscom.app.engine.Phase
import com.muscom.app.engine.Sena

@Composable
fun lanceName(l: Lance): String = stringResource(
    when (l) {
        Lance.GRANDE -> R.string.lance_grande
        Lance.CHICA -> R.string.lance_chica
        Lance.PARES -> R.string.lance_pares
        Lance.JUEGO -> R.string.lance_juego
        Lance.PUNTO -> R.string.lance_punto
    },
)

@Composable
fun phaseName(p: Phase): String = p.lance?.let { lanceName(it) } ?: stringResource(
    when (p) {
        Phase.MUS -> R.string.phase_mus
        Phase.DESCARTE -> R.string.phase_descarte
        Phase.FIN_MANO -> R.string.phase_fin_mano
        else -> R.string.phase_fin_partida
    },
)

@Composable
fun senaName(s: Sena): String = stringResource(
    when (s) {
        Sena.DOS_REYES -> R.string.sena_dos_reyes
        Sena.TRES_REYES -> R.string.sena_tres_reyes
        Sena.DOS_ASES -> R.string.sena_dos_ases
        Sena.TRES_ASES -> R.string.sena_tres_ases
        Sena.DUPLES -> R.string.sena_duples
        Sena.MEDIAS -> R.string.sena_medias
        Sena.TREINTA_Y_UNA -> R.string.sena_31
        Sena.CIEGO -> R.string.sena_ciego
    },
)

/** Gesto que acompaña a cada seña (descripción para el compañero). */
@Composable
fun senaGesture(s: Sena): String = stringResource(
    when (s) {
        Sena.DOS_REYES -> R.string.gesto_dos_reyes
        Sena.TRES_REYES -> R.string.gesto_tres_reyes
        Sena.DOS_ASES -> R.string.gesto_dos_ases
        Sena.TRES_ASES -> R.string.gesto_tres_ases
        Sena.DUPLES -> R.string.gesto_duples
        Sena.MEDIAS -> R.string.gesto_medias
        Sena.TREINTA_Y_UNA -> R.string.gesto_31
        Sena.CIEGO -> R.string.gesto_ciego
    },
)

@Composable
fun actionText(a: MusAction): String = when (a) {
    MusAction.Mus -> stringResource(R.string.act_mus)
    MusAction.NoMus -> stringResource(R.string.act_no_mus)
    is MusAction.Descarte -> stringResource(R.string.act_descarto, a.cards.size)
    MusAction.Paso -> stringResource(R.string.act_paso)
    is MusAction.Envido -> stringResource(R.string.act_envido_n, a.amount)
    MusAction.Quiero -> stringResource(R.string.act_quiero)
    MusAction.NoQuiero -> stringResource(R.string.act_no_quiero)
    MusAction.Ordago -> stringResource(R.string.act_ordago)
    is MusAction.SenaAction -> stringResource(R.string.act_sena, senaName(a.sena))
    MusAction.SiguienteMano -> stringResource(R.string.next_hand)
}

@Composable
fun outcomeText(r: LanceRecord): String = when {
    r.ordago && r.outcome == LanceOutcome.QUERIDO -> stringResource(R.string.out_ordago_querido)
    r.outcome == LanceOutcome.PASO -> stringResource(R.string.out_paso)
    r.outcome == LanceOutcome.QUERIDO -> stringResource(R.string.out_querido, r.amount)
    r.outcome == LanceOutcome.NO_QUERIDO -> stringResource(R.string.out_no_querido, r.amount)
    r.outcome == LanceOutcome.SIN_APUESTA -> stringResource(R.string.out_sin_apuesta)
    else -> stringResource(R.string.out_no_hay)
}

@Composable
fun awardKindText(k: AwardKind): String = stringResource(
    when (k) {
        AwardKind.DEJE -> R.string.award_deje
        AwardKind.APUESTA -> R.string.award_apuesta
        AwardKind.PASO -> R.string.award_paso
        AwardKind.JUGADA -> R.string.award_jugada
        AwardKind.ORDAGO -> R.string.award_ordago
    },
)

@Composable
fun cardName(c: Card): String {
    val ranks = stringArrayResource(R.array.rank_names)
    val suits = stringArrayResource(R.array.suit_names)
    val rankIdx = Cards.RANKS.indexOf(Cards.rankOf(c))
    return stringResource(R.string.card_name, ranks[rankIdx], suits[Cards.suitOf(c).ordinal])
}
