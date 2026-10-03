package com.muscom.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.muscom.app.R
import com.muscom.app.engine.BotLevel
import com.muscom.app.engine.Cards
import com.muscom.app.engine.Suit
import com.muscom.app.ui.cards.CardFace
import com.muscom.app.ui.cards.Felt
import com.muscom.app.ui.theme.LocalTableColors

enum class SoonFeature { QUICK, PRIVATE, LEAGUES, SHOP }

@Composable
fun HomeScreen(
    name: String,
    level: BotLevel,
    onLevel: (BotLevel) -> Unit,
    onPlayBots: () -> Unit,
    onSoon: (SoonFeature) -> Unit,
    onProfile: () -> Unit,
    onSettings: () -> Unit,
) {
    val colors = LocalTableColors.current
    Box(Modifier.fillMaxSize()) {
        Felt(Modifier.fillMaxSize())
        Column(
            Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.app_title), style = MaterialTheme.typography.headlineMedium, color = Color.White)
                    Text(stringResource(R.string.hello_name, name), color = Color.White.copy(alpha = 0.85f))
                }
                IconButton(onClick = onProfile) { Icon(Icons.Filled.Person, stringResource(R.string.profile), tint = Color.White) }
                IconButton(onClick = onSettings) { Icon(Icons.Filled.Settings, stringResource(R.string.settings), tint = Color.White) }
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy((-16).dp, Alignment.CenterHorizontally)) {
                listOf(Cards.make(12, Suit.OROS), Cards.make(11, Suit.COPAS), Cards.make(1, Suit.ESPADAS), Cards.make(7, Suit.BASTOS)).forEachIndexed { k, c ->
                    Box(Modifier.graphicsLayer { rotationZ = (k - 1.5f) * 8f; translationY = kotlin.math.abs(k - 1.5f) * 10f }) { CardFace(c, 70.dp) }
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.play_bots), style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(R.string.play_bots_desc), style = MaterialTheme.typography.bodyMedium)
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        BotLevel.entries.forEachIndexed { i, l ->
                            SegmentedButton(
                                selected = level == l,
                                onClick = { onLevel(l) },
                                shape = SegmentedButtonDefaults.itemShape(i, BotLevel.entries.size),
                            ) { Text(stringResource(levelName(l))) }
                        }
                    }
                    Button(
                        onClick = onPlayBots, modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.highlight, contentColor = Color.Black),
                    ) { Text(stringResource(R.string.play_now)) }
                }
            }
            SoonCard(R.string.quick_match, R.string.quick_match_desc) { onSoon(SoonFeature.QUICK) }
            SoonCard(R.string.private_room, R.string.private_room_desc) { onSoon(SoonFeature.PRIVATE) }
            SoonCard(R.string.leagues, R.string.leagues_desc) { onSoon(SoonFeature.LEAGUES) }
            SoonCard(R.string.shop, R.string.shop_desc) { onSoon(SoonFeature.SHOP) }
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.proto_notice), color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
        }
    }
}

fun levelName(l: BotLevel) = when (l) {
    BotLevel.FACIL -> R.string.level_facil
    BotLevel.NORMAL -> R.string.level_normal
    BotLevel.DIFICIL -> R.string.level_dificil
}

@Composable
private fun SoonCard(title: Int, desc: Int, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.28f), contentColor = Color.White),
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(desc), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
            }
            Text(stringResource(R.string.soon), style = MaterialTheme.typography.labelSmall, color = LocalTableColors.current.highlight)
        }
    }
}
