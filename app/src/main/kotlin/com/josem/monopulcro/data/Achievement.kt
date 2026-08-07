package com.josem.monopulcro.data

import com.josem.monopulcro.R

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    /** Emoji de respaldo mientras no haya un ícono propio en [iconRes]. */
    val emoji: String,
    /** Ícono dedicado (ej. `R.drawable.logro_nuevo_look`). Null = usar [emoji]. */
    val iconRes: Int? = null,
)

/** Catálogo fijo de logros v1. No editable por el usuario. */
object Achievements {

    val ALL: List<Achievement> = listOf(
        Achievement(
            "streak_3", "Primeros pasos", "Alcanzá una racha de 3 días", "🔥",
            iconRes = R.drawable.fuego,
        ),
        Achievement(
            "streak_7", "Semana perfecta", "Alcanzá una racha de 7 días", "🌟",
            iconRes = R.drawable.logro_semana_perfecta,
        ),
        Achievement(
            "streak_30", "Mes de oro", "Alcanzá una racha de 30 días", "🏆",
            iconRes = R.drawable.logro_mes_oro,
        ),
        Achievement(
            "streak_100", "Leyenda de la limpieza", "Alcanzá una racha de 100 días", "👑",
            iconRes = R.drawable.logro_leyenda_limpieza,
        ),
        Achievement(
            "bananas_50", "Ahorrista", "Ganá 50 bananas en total", "🍌",
            iconRes = R.drawable.banana,
        ),
        Achievement(
            "bananas_500", "Banana millonario", "Ganá 500 bananas en total", "💰",
            iconRes = R.drawable.logro_banana_millonario,
        ),
        Achievement("motes_10", "Manos limpias", "Limpiá 10 motas de polvo en total", "🧹"),
        Achievement("motes_50", "Cazador de polvo", "Limpiá 50 motas de polvo en total", "✨"),
        Achievement(
            "accessory_first", "Nuevo look", "Comprá tu primer accesorio", "🎩",
            iconRes = R.drawable.logro_nuevo_look,
        ),
        Achievement("accessory_all", "Guardarropa completo", "Comprá los 12 accesorios de la tienda", "👔"),
    )

    fun byId(id: String): Achievement? = ALL.find { it.id == id }
}
