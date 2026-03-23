package com.selbie.wrek.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.selbie.wrek.R
import com.selbie.wrek.ui.theme.WrekTheme
import com.selbie.wrek.utils.openEmail
import com.selbie.wrek.utils.openUrl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_title)) },
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
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.drawable.wrek_round),
                contentDescription = stringResource(R.string.app_name),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(120.dp)
                    .padding(bottom = 16.dp)
            )

            // App Title
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // Contact Information
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = stringResource(R.string.about_developer),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Clickable email for feedback
                val context = LocalContext.current
                val feedbackText = buildAnnotatedString {
                    append(context.getString(R.string.about_feedback_label))

                    pushStringAnnotation(
                        tag = "EMAIL",
                        annotation = "apps@selbie.com"
                    )
                    withStyle(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        append("apps@selbie.com")
                    }
                    pop()
                }

                ClickableText(
                    text = feedbackText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.padding(bottom = 8.dp),
                    onClick = { offset ->
                        feedbackText.getStringAnnotations(
                            tag = "EMAIL",
                            start = offset,
                            end = offset
                        ).firstOrNull()?.let { annotation ->
                            openEmail(context, annotation.item)
                        }
                    }
                )

                // Clickable URL for www.wrek.org
                val wrekWebText = buildAnnotatedString {
                    append(context.getString(R.string.about_wrek_web_label))

                    // Start clickable URL portion
                    pushStringAnnotation(
                        tag = "URL",
                        annotation = "https://www.wrek.org"
                    )
                    withStyle(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        append("www.wrek.org")
                    }
                    pop() // End URL annotation
                }

                ClickableText(
                    text = wrekWebText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.padding(bottom = 8.dp),
                    onClick = { offset ->
                        // Check if click was on URL annotation
                        wrekWebText.getStringAnnotations(
                            tag = "URL",
                            start = offset,
                            end = offset
                        ).firstOrNull()?.let { annotation ->
                            openUrl(context, annotation.item)
                        }
                    }
                )

                // Clickable email for WREK contact
                val wrekContactText = buildAnnotatedString {
                    append(context.getString(R.string.about_wrek_contact_label))

                    pushStringAnnotation(
                        tag = "EMAIL",
                        annotation = "comments@wrek.org"
                    )
                    withStyle(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        append("comments@wrek.org")
                    }
                    pop()
                }

                ClickableText(
                    text = wrekContactText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    onClick = { offset ->
                        wrekContactText.getStringAnnotations(
                            tag = "EMAIL",
                            start = offset,
                            end = offset
                        ).firstOrNull()?.let { annotation ->
                            openEmail(context, annotation.item)
                        }
                    }
                )
            }

            // About WREK Section
            Text(
                text = stringResource(R.string.about_wrek_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            Text(
                text = stringResource(R.string.about_wrek_content),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth()
            )

            // Licenses Section
            Text(
                text = stringResource(R.string.about_licenses_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 16.dp)
            )

            Text(
                text = stringResource(R.string.about_blurhash_license),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth()
            )

            // Open Source paragraph
            val openSourceContext = LocalContext.current
            val openSourceUrl = "https://github.com/jselbie/wrekonline"
            val openSourceTemplate = stringResource(R.string.about_open_source, openSourceUrl)
            val urlStart = openSourceTemplate.indexOf(openSourceUrl)
            val openSourceText = buildAnnotatedString {
                append(openSourceTemplate.substring(0, urlStart))

                pushStringAnnotation(
                    tag = "URL",
                    annotation = openSourceUrl
                )
                withStyle(
                    style = SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline
                    )
                ) {
                    append(openSourceUrl)
                }
                pop()

                if (urlStart + openSourceUrl.length < openSourceTemplate.length) {
                    append(openSourceTemplate.substring(urlStart + openSourceUrl.length))
                }
            }

            ClickableText(
                text = openSourceText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                onClick = { offset ->
                    openSourceText.getStringAnnotations(
                        tag = "URL",
                        start = offset,
                        end = offset
                    ).firstOrNull()?.let { annotation ->
                        openUrl(openSourceContext, annotation.item)
                    }
                }
            )
        }
    }
}

@Preview(name = "About Screen")
@Composable
private fun PreviewAboutScreen() {
    WrekTheme {
        AboutScreen(onNavigateBack = {})
    }
}
