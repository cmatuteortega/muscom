package com.muscom.app.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.muscom.app.R
import com.muscom.app.engine.Cards
import com.muscom.app.engine.Suit
import com.muscom.app.ui.cards.CardBack
import com.muscom.app.ui.cards.CardFace
import com.muscom.app.ui.cards.Felt
import com.muscom.app.ui.theme.LocalTableColors
import kotlinx.coroutines.launch

private data class Page(val title: Int, val body: Int, val cards: List<Int>)

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val pages = listOf(
        Page(R.string.onb1_title, R.string.onb1_body, listOf(Cards.make(12, Suit.OROS), Cards.make(3, Suit.COPAS), Cards.make(1, Suit.ESPADAS), Cards.make(2, Suit.BASTOS))),
        Page(R.string.onb2_title, R.string.onb2_body, listOf(Cards.make(11, Suit.COPAS), Cards.make(10, Suit.ESPADAS), Cards.make(12, Suit.BASTOS), Cards.make(1, Suit.OROS))),
        Page(R.string.onb3_title, R.string.onb3_body, listOf(Cards.make(7, Suit.OROS), Cards.make(7, Suit.COPAS), Cards.make(5, Suit.ESPADAS), Cards.make(5, Suit.BASTOS))),
    )
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val colors = LocalTableColors.current
    Box(Modifier.fillMaxSize()) {
        Felt(Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(24.dp)) {
            HorizontalPager(pager, Modifier.weight(1f)) { i ->
                val p = pages[i]
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Row(horizontalArrangement = Arrangement.spacedBy((-18).dp)) {
                        p.cards.forEachIndexed { k, c ->
                            Box(Modifier.graphicsLayer { rotationZ = (k - 1.5f) * 9f; translationY = kotlin.math.abs(k - 1.5f) * 12f }) {
                                if (i == 0 && k == 3) CardBack(78.dp) else CardFace(c, 78.dp)
                            }
                        }
                    }
                    Spacer(Modifier.height(36.dp))
                    Text(stringResource(p.title), style = MaterialTheme.typography.headlineMedium, color = Color.White, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(p.body), style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.9f), textAlign = TextAlign.Center)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                repeat(pages.size) { i ->
                    Box(
                        Modifier.padding(4.dp).size(if (pager.currentPage == i) 10.dp else 8.dp).clip(CircleShape)
                            .background(if (pager.currentPage == i) colors.highlight else Color.White.copy(alpha = 0.4f)),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDone) { Text(stringResource(R.string.skip), color = Color.White) }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = {
                        if (pager.currentPage == pages.lastIndex) onDone() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.highlight, contentColor = Color.Black),
                ) { Text(stringResource(if (pager.currentPage == pages.lastIndex) R.string.start else R.string.next)) }
            }
        }
    }
}
