package com.example.util

import android.util.Base64
import com.example.BuildConfig

object SecurityUtils {
    fun getDecryptedUrl(): String {
        return try {
            val base64Url = BuildConfig.BOOKS_API_URL
            if (base64Url.isNullOrEmpty() || base64Url == "MY_NEW_API_KEY_DEFAULT_VALUE") {
                // Return default direct URL as safety fallback if key injection was skipped
                "https://opensheet.elk.sh/1_xjsWSSfJAt2iP0XPE5pBaIiC0dexdq52N-G82PY-qM/Books"
            } else {
                val decodedBytes = Base64.decode(base64Url, Base64.DEFAULT)
                String(decodedBytes, Charsets.UTF_8).trim()
            }
        } catch (e: Exception) {
            "https://opensheet.elk.sh/1_xjsWSSfJAt2iP0XPE5pBaIiC0dexdq52N-G82PY-qM/Books"
        }
    }
}
