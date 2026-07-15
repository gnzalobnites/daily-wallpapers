package com.gnzalobnites.dailywallpapers

data class RegionOption(
    val localeTag: String,
    val bingMarket: String,
    val countryCode: String,
    val countryNameResId: Int,     // ← Cambio: ahora es Resource ID
    val languageNameResId: Int,    // ← Cambio: ahora es Resource ID
    val flagRes: Int
)

object RegionManager {
    val REGIONS = listOf(
        RegionOption("en", "en-WW", "WW", R.string.region_worldwide, R.string.lang_english, R.drawable.ic_flag_worldwide),
        RegionOption("es-ES", "es-ES", "ES", R.string.region_spain, R.string.lang_spanish, R.drawable.ic_flag_spain),
        RegionOption("es-AR", "es-ES", "AR", R.string.region_argentina, R.string.lang_spanish, R.drawable.ic_flag_argentina),
        RegionOption("es-MX", "es-ES", "MX", R.string.region_mexico, R.string.lang_spanish, R.drawable.ic_flag_mexico),
        RegionOption("en-US", "en-US", "US", R.string.region_usa, R.string.lang_english, R.drawable.ic_flag_usa),
        RegionOption("en-GB", "en-GB", "GB", R.string.region_uk, R.string.lang_english, R.drawable.ic_flag_uk),
        RegionOption("en-AU", "en-AU", "AU", R.string.region_australia, R.string.lang_english, R.drawable.ic_flag_australia),
        RegionOption("de-DE", "de-DE", "DE", R.string.region_germany, R.string.lang_german, R.drawable.ic_flag_germany),
        RegionOption("fr-FR", "fr-FR", "FR", R.string.region_france, R.string.lang_french, R.drawable.ic_flag_france),
        RegionOption("it-IT", "it-IT", "IT", R.string.region_italy, R.string.lang_italian, R.drawable.ic_flag_italy),
        RegionOption("pt-BR", "pt-BR", "BR", R.string.region_brazil, R.string.lang_portuguese, R.drawable.ic_flag_brazil),
        RegionOption("ja-JP", "ja-JP", "JP", R.string.region_japan, R.string.lang_japanese, R.drawable.ic_flag_japan),
        RegionOption("zh-CN", "zh-CN", "CN", R.string.region_china, R.string.lang_chinese, R.drawable.ic_flag_china)
    )

    fun findByTag(tag: String): RegionOption =
        REGIONS.firstOrNull { it.localeTag == tag } ?: REGIONS.first()

    fun findMarketByTag(tag: String): String = findByTag(tag).bingMarket
}