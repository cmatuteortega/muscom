package com.muscom.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muscom.app.R
import com.muscom.app.data.Stats
import com.muscom.app.ui.SimpleScaffold
import com.muscom.app.ui.theme.LocalTableColors

@Composable
fun ProfileScreen(name: String, stats: Stats, onBack: () -> Unit) {
    SimpleScaffold(stringResource(R.string.profile), onBack) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.size(72.dp).clip(CircleShape).background(LocalTableColors.current.teamUs), contentAlignment = Alignment.Center) {
                Text(name.take(1).uppercase(), color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black)
            }
            Column {
                Text(name, style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.rating_soon), style = MaterialTheme.typography.bodySmall)
            }
        }
        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StatRow(R.string.stat_played, stats.played.toString())
                StatRow(R.string.stat_won, "${stats.won} (${stats.winRate}%)")
                StatRow(R.string.stat_hands, stats.hands.toString())
                StatRow(R.string.stat_streak, "${stats.streak} / ${stats.bestStreak}")
                StatRow(R.string.stat_ordagos, "${stats.ordagosWon} / ${stats.ordagosLost}")
                StatRow(R.string.stat_points, "${stats.pointsFor} – ${stats.pointsAgainst}")
            }
        }
    }
}

@Composable
private fun StatRow(label: Int, value: String) {
    Row(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {
        Text(stringResource(label), modifier = Modifier.weight(1f))
        Text(value, fontWeight = FontWeight.Bold)
    }
}
