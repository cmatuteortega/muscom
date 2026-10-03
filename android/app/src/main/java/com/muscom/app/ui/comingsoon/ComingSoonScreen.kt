package com.muscom.app.ui.comingsoon

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.muscom.app.R
import com.muscom.app.ui.SimpleScaffold
import com.muscom.app.ui.home.SoonFeature

@Composable
fun ComingSoonScreen(feature: SoonFeature, onBack: () -> Unit) {
    val (title, body) = when (feature) {
        SoonFeature.QUICK -> R.string.quick_match to R.string.soon_quick
        SoonFeature.PRIVATE -> R.string.private_room to R.string.soon_private
        SoonFeature.LEAGUES -> R.string.leagues to R.string.soon_leagues
        SoonFeature.SHOP -> R.string.shop to R.string.soon_shop
    }
    SimpleScaffold(stringResource(title), onBack) {
        Text(stringResource(body), style = MaterialTheme.typography.bodyLarge)
    }
}
