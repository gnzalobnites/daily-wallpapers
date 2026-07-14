package com.gnzalobnites.dailywallpapers

data class RegionOption(
    val localeTag: String,      // ej. "es-AR" — se usa para AppCompatDelegate
    val countryCode: String,    // ej. "AR" — para filtrar contenido/feed
    val countryName: String,
    val languageName: String,
    val flagRes: Int
)

object RegionManager {

    val REGIONS = listOf(
        RegionOption("es-ES", "ES", "España", "Español", R.drawable.ic_flag_spain),
        RegionOption("es-AR", "AR", "Argentina", "Español", R.drawable.ic_flag_argentina),
        RegionOption("es-MX", "MX", "México", "Español", R.drawable.ic_flag_mexico),
        RegionOption("en-US", "US", "Estados Unidos", "Inglés", R.drawable.ic_flag_usa),
        RegionOption("en-GB", "GB", "Reino Unido", "Inglés", R.drawable.ic_flag_uk),
        RegionOption("en-AU", "AU", "Australia", "Inglés", R.drawable.ic_flag_australia),
        RegionOption("de-DE", "DE", "Alemania", "Alemán", R.drawable.ic_flag_germany),
        RegionOption("fr-FR", "FR", "Francia", "Francés", R.drawable.ic_flag_france),
        RegionOption("it-IT", "IT", "Italia", "Italiano", R.drawable.ic_flag_italy),
        RegionOption("pt-BR", "BR", "Brasil", "Portugués", R.drawable.ic_flag_brazil),
        RegionOption("ja-JP", "JP", "Japón", "Japonés", R.drawable.ic_flag_japan),
        RegionOption("zh-CN", "CN", "China", "Chino", R.drawable.ic_flag_china)
    )

    fun findByTag(tag: String): RegionOption =
        REGIONS.firstOrNull { it.localeTag == tag } ?: REGIONS.first()
}