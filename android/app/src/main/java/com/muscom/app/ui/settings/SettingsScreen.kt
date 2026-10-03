package com.muscom.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.selection.toggleable
import com.muscom.app.R
import com.muscom.app.data.Settings
import com.muscom.app.data.ThemeMode
import com.muscom.app.ui.SimpleScaffold

@Composable
fun SettingsScreen(
    settings: Settings,
    onChange: ((Settings) -> Settings) -> Unit,
    onDeleteData: () -> Unit,
    onBack: () -> Unit,
) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    SimpleScaffold(stringResource(R.string.settings), onBack) {
        Section(R.string.set_appearance)
        Choice(
            R.string.set_theme,
            listOf(R.string.theme_system, R.string.theme_light, R.string.theme_dark),
            ThemeMode.entries.indexOf(settings.themeMode),
        ) { i -> onChange { it.copy(themeMode = ThemeMode.entries[i]) } }
        Toggle(R.string.set_contrast, settings.highContrast) { v -> onChange { it.copy(highContrast = v) } }
        val scales = listOf(1f, 1.15f, 1.3f)
        Choice(
            R.string.set_text_size,
            listOf(R.string.size_normal, R.string.size_large, R.string.size_xlarge),
            scales.indexOf(settings.textScale).coerceAtLeast(0),
        ) { i -> onChange { it.copy(textScale = scales[i]) } }

        Section(R.string.set_game)
        Choice(R.string.set_bot_speed, listOf(R.string.speed_slow, R.string.speed_normal, R.string.speed_fast), settings.botSpeed) { i ->
            onChange { it.copy(botSpeed = i) }
        }
        Choice(R.string.set_reyes, listOf(R.string.reyes_8, R.string.reyes_4), if (settings.reyes == 8) 0 else 1) { i ->
            onChange { it.copy(reyes = if (i == 0) 8 else 4) }
        }
        val targets = listOf(40, 30, 25)
        Choice(R.string.set_target, listOf(R.string.target_40, R.string.target_30, R.string.target_25), targets.indexOf(settings.target).coerceAtLeast(0)) { i ->
            onChange { it.copy(target = targets[i]) }
        }
        val times = listOf(30, 60, 15)
        Choice(R.string.set_turn_time, listOf(R.string.time_30, R.string.time_60, R.string.time_15), times.indexOf(settings.turnSeconds).coerceAtLeast(0)) { i ->
            onChange { it.copy(turnSeconds = times[i]) }
        }
        Toggle(R.string.set_sound, settings.sound) { v -> onChange { it.copy(sound = v) } }
        Toggle(R.string.set_haptics, settings.haptics) { v -> onChange { it.copy(haptics = v) } }

        Section(R.string.set_privacy)
        Toggle(R.string.consent_analytics, settings.analyticsConsent == true) { v -> onChange { it.copy(analyticsConsent = v) } }
        Text(stringResource(R.string.privacy_summary), style = MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.delete_data))
        }
        Text(stringResource(R.string.version_info, com.muscom.app.BuildConfig.VERSION_NAME), style = MaterialTheme.typography.labelSmall)
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_data)) },
            text = { Text(stringResource(R.string.delete_data_body)) },
            confirmButton = { TextButton(onClick = { confirmDelete = false; onDeleteData() }) { Text(stringResource(R.string.delete)) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun Section(title: Int) {
    HorizontalDivider()
    Text(stringResource(title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun Toggle(label: Int, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().toggleable(value = value, role = Role.Switch, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(label), modifier = Modifier.weight(1f))
        Switch(checked = value, onCheckedChange = null)
    }
}

@Composable
private fun Choice(label: Int, options: List<Int>, selected: Int, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(label))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            options.forEachIndexed { i, o ->
                SegmentedButton(selected = i == selected, onClick = { onSelect(i) }, shape = SegmentedButtonDefaults.itemShape(i, options.size)) {
                    Text(stringResource(o), maxLines = 1)
                }
            }
        }
    }
}
