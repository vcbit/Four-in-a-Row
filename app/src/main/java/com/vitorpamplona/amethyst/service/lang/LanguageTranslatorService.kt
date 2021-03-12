package com.vitorpamplona.amethyst.service.lang

import android.util.LruCache
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.linkedin.urls.detection.UrlDetector
import com.linkedin.urls.detection.UrlDetectorOptions
import java.util.regex.Pattern

class ResultOrError(
    var result: String?,
    var sourceLang: String?,
    var targetLang: String?,
    var error: Exception?
)

object LanguageTranslatorService {
    private val languageIdentification = LanguageIdentification.getClient()
    val lnRegex = Pattern.compile("\\blnbc[a-z0-9]+\\b")

    private val translators =
        object : LruCache<TranslatorOptions, Translator>(10) {
            override fun create(options: TranslatorOptions): Translator {
                return Translation.getClient(options)
            }

            override fun entryRemoved(
                evicted: Boolean,
                key: TranslatorOptions,
                oldValue: Translator,
                newValue: Translator?
            ) {
                oldValue.close()
            }
        }

    fun identifyLanguage(text: String): Task<String> {
  