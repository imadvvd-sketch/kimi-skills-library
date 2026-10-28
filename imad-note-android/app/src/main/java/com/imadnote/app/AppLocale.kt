package com.imadnote.app

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import java.util.Locale

/**
 * In-app language. Stored in SharedPreferences because it must be read synchronously
 * before the first frame (attachBaseContext). The app ships Arabic and English; when the
 * system language is neither, Arabic is used.
 */
object AppLocale {
    enum class Language { SYSTEM, ARABIC, ENGLISH }

    private const val PREFS = "app_locale"
    private const val KEY = "language"

    fun get(context: Context): Language =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
            ?.let { name -> Language.entries.firstOrNull { it.name == name } } ?: Language.SYSTEM

    fun set(context: Context, language: Language) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, language.name).apply()
    }

    fun resolve(context: Context): Locale = when (get(context)) {
        Language.ARABIC -> Locale("ar")
        Language.ENGLISH -> Locale.ENGLISH
        Language.SYSTEM -> {
            val system = Resources.getSystem().configuration.locales[0]
            if (system.language == "ar" || system.language == "en") system else Locale("ar")
        }
    }

    fun wrap(base: Context): Context {
        val locale = resolve(base)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return base.createConfigurationContext(config)
    }
}
