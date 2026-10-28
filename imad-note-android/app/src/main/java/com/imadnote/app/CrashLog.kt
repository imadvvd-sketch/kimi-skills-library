package com.imadnote.app

import android.content.Context
import android.os.Build
import java.io.File

/**
 * Keeps the last crash on disk so it can be shown (and copied) on the next launch.
 * The app has no analytics or network access, so this is the only way to learn why it stopped.
 */
object CrashLog {
    private fun file(context: Context) = File(context.filesDir, "last_crash.txt")

    fun install(context: Context) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching {
                val info = "Imad-note ${BuildConfigInfo.versionName(context)} · Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) · ${Build.MANUFACTURER} ${Build.MODEL}\n\n"
                file(context).writeText(info + error.stackTraceToString().take(12_000))
            }
            previous?.uncaughtException(thread, error)
        }
    }

    /** Returns the saved crash report once and deletes it. */
    fun consume(context: Context): String? {
        val f = file(context)
        if (!f.exists()) return null
        return runCatching { f.readText() }.getOrNull().also { f.delete() }
    }
}
