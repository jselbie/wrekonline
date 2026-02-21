package com.selbie.wrek.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.selbie.wrek.R

/**
 * Navigation drawer content for the main activity
 */
@Composable
fun AppDrawer(
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

        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_settings)) },
            selected = false,
            onClick = {
                onCloseDrawer()
                onNavigateToSettings()
            },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )

        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Info, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_about)) },
            selected = false,
            onClick = {
                onCloseDrawer()
                onNavigateToAbout()
            },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}
