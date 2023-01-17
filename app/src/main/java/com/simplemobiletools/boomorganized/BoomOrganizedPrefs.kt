package com.simplemobiletools.boomorganized

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.core.content.edit
import androidx.core.net.toUri
import com.simplemobiletools.boomorganized.BoomOrganizedPrefs.Companion.PHOTO_URI_KEY
import com.simplemobiletools.boomorganized.BoomOrganizedPrefs.Companion.SCRIPT_KEY
import com.simplemobiletools.commons.extensions.getSharedPrefs

interface BoomOrganizedPrefs {
    val boomContext: Context

    companion object {
        const val SCRIPT_KEY = "BoomOrganizedPrefs.SCRIPT_KEY"
        const val PHOTO_URI_KEY = "BoomOrganizedPrefs.PHOTO_URI_KEY"
    }
}

val BoomOrganizedPrefs.prefs: SharedPreferences
    get() = boomContext.getSharedPrefs()

var BoomOrganizedPrefs.imageUri: Uri?
    get() {
        val value = prefs.getString(PHOTO_URI_KEY, "")
        return if (value.isNullOrBlank()) null else value.toUri()
    }
    set(value) {
        prefs.edit {
            putString(PHOTO_URI_KEY, value?.toString())
        }
    }

var BoomOrganizedPrefs.script: String
    get() = prefs.getString(SCRIPT_KEY, "") ?: ""
    set(value) {
        prefs.edit {
            putString(SCRIPT_KEY, value)
        }
    }

var BoomOrganizedPrefs.skipEmptyNames: String
    get() = prefs.getString(SCRIPT_KEY, "") ?: ""
    set(value) {
        prefs.edit {
            putString(SCRIPT_KEY, value)
        }
    }
