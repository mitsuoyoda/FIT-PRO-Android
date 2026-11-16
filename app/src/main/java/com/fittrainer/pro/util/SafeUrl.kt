package com.fittrainer.pro.util

import android.net.Uri

object SafeUrl {
    fun httpOrHttps(raw: String): Uri? {
        val uri = runCatching { Uri.parse(raw.trim()) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase() ?: return null
        if (scheme != "http" && scheme != "https") return null
        if (uri.host.isNullOrBlank()) return null
        return uri
    }
}
