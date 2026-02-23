package com.selbie.wrek.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.selbie.wrek.R
import com.selbie.wrek.ui.theme.WrekTheme
import com.selbie.wrek.utils.openUrl

/**
 * Navigation drawer content for the main activity
 */
@Composable
fun AppDrawer(
    versionName: String,
    onNavigateToSettings: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onCloseDrawer: () -> Unit
) {
    ModalDrawerSheet(
        modifier = Modifier.fillMaxWidth(0.75f) // Max 75% of screen width
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge,
            fontFamily = FontFamily(Font(R.font.metro_df)),
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
        )

        HorizontalDivider()

        val context = LocalContext.current

        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_settings)) },
            selected = false,
            onClick = {
                onCloseDrawer()
                onNavigateToSettings()
            },
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Info, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_about)) },
            selected = false,
            onClick = {
                onCloseDrawer()
                onNavigateToAbout()
            },
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Language, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_wrek_website)) },
            selected = false,
            onClick = {
                onCloseDrawer()
                openUrl(context, "https://www.wrek.org")
            },
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = stringResource(R.string.nav_version, versionName),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
        )
    }
}

@Preview(name = "App Drawer")
@Composable
private fun PreviewAppDrawer() {
    WrekTheme {
        AppDrawer(
            versionName = "2.0.1",
            onNavigateToSettings = {},
            onNavigateToAbout = {},
            onCloseDrawer = {}
        )
    }
}
