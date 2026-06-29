package com.example.dymnbaby

import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import androidx.core.content.edit
import java.util.Locale

enum class DymnLanguage(val code: String, val title: String) {
    Ukrainian("uk", "Українська"),
    English("en", "English"),
}

val LocalDymnLanguage = compositionLocalOf { DymnLanguage.Ukrainian }

@Volatile
var runtimeDymnLanguage: DymnLanguage = DymnLanguage.Ukrainian

fun dymnText(language: DymnLanguage, uk: String, en: String): String {
    return if (language == DymnLanguage.English) en else uk
}

fun dymnText(context: Context, uk: String, en: String): String {
    return dymnText(loadDymnLanguage(context), uk, en)
}

fun dymnLocale(language: DymnLanguage): Locale {
    return if (language == DymnLanguage.English) Locale.ENGLISH else Locale("uk", "UA")
}

fun loadDymnLanguage(context: Context): DymnLanguage {
    val saved = context.getSharedPreferences(DymnPrefsName, Context.MODE_PRIVATE)
        .getString(DymnLanguageKey, DymnLanguage.Ukrainian.code)
    return DymnLanguage.entries.firstOrNull { it.code == saved } ?: DymnLanguage.Ukrainian
}

fun saveDymnLanguage(context: Context, language: DymnLanguage) {
    context.getSharedPreferences(DymnPrefsName, Context.MODE_PRIVATE).edit {
        putString(DymnLanguageKey, language.code)
    }
}

private const val DymnPrefsName = "dymn_baby_tracker"
private const val DymnLanguageKey = "app_language"
