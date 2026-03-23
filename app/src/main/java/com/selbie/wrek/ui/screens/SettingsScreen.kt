package com.selbie.wrek.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.selbie.wrek.R
import com.selbie.wrek.data.models.BitratePreference
import com.selbie.wrek.ui.theme.WrekTheme
import com.selbie.wrek.viewmodels.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val settings by viewModel.bitratePreference.collectAsState()
    SettingsContent(
        currentPreference = settings.bitratePreference,
        onPreferenceChanged = { viewModel.setBitratePreference(it) },
        autoStop = settings.autoStop,
        onAutoStopChanged = { viewModel.setAutoStop(it) },
        onNavigateBack = onNavigateBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsContent(
    currentPreference: BitratePreference,
    onPreferenceChanged: (BitratePreference) -> Unit,
    autoStop: Boolean,
    onAutoStopChanged: (Boolean) -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_bitrate_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Column(
                modifier = Modifier.selectableGroup()
            ) {
                BitrateOption(
                    text = stringResource(R.string.settings_bitrate_auto),
                    selected = currentPreference == BitratePreference.AUTO,
                    onClick = { onPreferenceChanged(BitratePreference.AUTO) }
                )

                BitrateOption(
                    text = stringResource(R.string.settings_bitrate_best),
                    selected = currentPreference == BitratePreference.BEST,
                    onClick = { onPreferenceChanged(BitratePreference.BEST) }
                )

                BitrateOption(
                    text = stringResource(R.string.settings_bitrate_modest),
                    selected = currentPreference == BitratePreference.MODEST,
                    onClick = { onPreferenceChanged(BitratePreference.MODEST) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.settings_auto_stop_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Switch(
                    checked = autoStop,
                    onCheckedChange = onAutoStopChanged
                )
            }

            Text(
                text = stringResource(R.string.settings_auto_stop_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Preview(name = "Settings Screen")
@Composable
private fun PreviewSettingsScreen() {
    WrekTheme {
        SettingsContent(
            currentPreference = BitratePreference.AUTO,
            onPreferenceChanged = {},
            autoStop = true,
            onAutoStopChanged = {},
            onNavigateBack = {}
        )
    }
}

@Composable
private fun BitrateOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
