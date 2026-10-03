package com.muscom.app.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.muscom.app.R
import com.muscom.app.ui.cards.Felt

/**
 * Prototipo: entrada como invitado con nombre público. Google Sign-In y email
 * (con JWT corto + refresh contra el servidor) llegan en la fase 2/3.
 */
@Composable
fun LoginScreen(initialName: String, onContinue: (name: String, analytics: Boolean) -> Unit) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var analytics by rememberSaveable { mutableStateOf(false) }
    val valid = name.trim().length in 2..20
    Box(Modifier.fillMaxSize()) {
        Felt(Modifier.fillMaxSize())
        Column(
            Modifier.fillMaxSize().systemBarsPadding().imePadding().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.app_title), style = MaterialTheme.typography.displaySmall, color = Color.White)
            Text(stringResource(R.string.tagline), color = Color.White.copy(alpha = 0.85f))
            Surface(shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.padding(top = 32.dp).fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.login_google_soon))
                    }
                    OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.login_email_soon))
                    }
                    Text(stringResource(R.string.login_guest_title), style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it.take(20) },
                        label = { Text(stringResource(R.string.public_name)) },
                        supportingText = { Text(stringResource(R.string.public_name_hint)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { if (valid) onContinue(name.trim(), analytics) }),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = analytics, onCheckedChange = { analytics = it })
                        Text(stringResource(R.string.consent_analytics), style = MaterialTheme.typography.bodySmall)
                    }
                    Button(onClick = { onContinue(name.trim(), analytics) }, enabled = valid, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.login_guest))
                    }
                    Text(stringResource(R.string.no_gambling_notice), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
