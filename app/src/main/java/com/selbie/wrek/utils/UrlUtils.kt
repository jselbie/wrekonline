package com.selbie.wrek.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

/**
 * Opens a URL in the user's default browser
 * Handles edge cases gracefully (no browser installed, invalid URL)
 */
fun openUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Log.w("UrlUtils", "No activity found to handle URL: $url", e)
    } catch (e: Exception) {
        Log.e("UrlUtils", "Error opening URL: $url", e)
    }
}

/**
 * Opens the default email app with a pre-filled "To" address
 * Handles edge cases gracefully (no email app installed)
 */
fun openEmail(context: Context, emailAddress: String) {
    try {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$emailAddress")
        }
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Log.w("UrlUtils", "No activity found to handle email: $emailAddress", e)
    } catch (e: Exception) {
        Log.e("UrlUtils", "Error opening email: $emailAddress", e)
    }
}
