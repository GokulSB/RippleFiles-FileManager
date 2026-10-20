package com.ripple.filemanager.data

data class AppLanguage(
    val tag: String,
    val nativeName: String,
    val englishGloss: String? = null
)

object AppLanguages {
    const val DEVICE_DEFAULT_TAG = "device_default"

    // Based on translated resources in the project:
    // English (base), French (fr), Spanish (es), Russian (ru), German (de), Polish (pl), Portuguese (pt).
    // Structure allows adding future languages by appending an entry here,
    // adding the locale to res/xml/locales_config.xml, and adding values-xx/strings.xml.
    val SUPPORTED_LANGUAGES = listOf(
        AppLanguage(
            tag = "en",
            nativeName = "English",
            englishGloss = null
        ),
        AppLanguage(
            tag = "fr",
            nativeName = "Français",
            englishGloss = "French"
        ),
        AppLanguage(
            tag = "es",
            nativeName = "Español",
            englishGloss = "Spanish"
        ),
        AppLanguage(
            tag = "ru",
            nativeName = "Русский",
            englishGloss = "Russian"
        ),
        AppLanguage(
            tag = "de",
            nativeName = "Deutsch",
            englishGloss = "German"
        ),
        AppLanguage(
            tag = "pl",
            nativeName = "Polski",
            englishGloss = "Polish"
        ),
        AppLanguage(
            tag = "pt",
            nativeName = "Português",
            englishGloss = "Portuguese"
        ),
        AppLanguage(
            tag = "ja",
            nativeName = "日本語",
            englishGloss = "Japanese"
        ),
        AppLanguage(
            tag = "zh",
            nativeName = "简体中文",
            englishGloss = "Chinese (Simplified)"
        ),
        AppLanguage(
            tag = "hi",
            nativeName = "हिन्दी",
            englishGloss = "Hindi"
        ),
        AppLanguage(
            tag = "ar",
            nativeName = "العربية",
            englishGloss = "Arabic"
        ),
        AppLanguage(
            tag = "it",
            nativeName = "Italiano",
            englishGloss = "Italian"
        ),
        AppLanguage(
            tag = "ko",
            nativeName = "한국어",
            englishGloss = "Korean"
        ),
        AppLanguage(
            tag = "tr",
            nativeName = "Türkçe",
            englishGloss = "Turkish"
        ),
        AppLanguage(
            tag = "in",
            nativeName = "Bahasa Indonesia",
            englishGloss = "Indonesian"
        )
    )
}
