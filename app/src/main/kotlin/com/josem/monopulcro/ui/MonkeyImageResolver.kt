package com.josem.monopulcro.ui

import com.josem.monopulcro.R
import java.time.LocalDate
import java.util.Random

object MonkeyImageResolver {

    private val PULCRO_STATES = listOf(
        R.drawable.mono_pulcro_1,
        R.drawable.mono_pulcro_2,
        R.drawable.mono_pulcro_3,
    )

    private val COOL_STATES = listOf(
        R.drawable.mono_cool_1,
        R.drawable.mono_cool_2,
        R.drawable.mono_cool_3,
    )

    private val GORRO_STATES = listOf(
        R.drawable.mono_gorro_1,
        R.drawable.mono_gorro_2,
        R.drawable.mono_gorro_3,
    )

    private val CORONA_STATES = listOf(
        R.drawable.mono_corona_1,
        R.drawable.mono_corona_2,
        R.drawable.mono_corona_3,
    )

    private val CHALECO_STATES = listOf(
        R.drawable.mono_chaleco_1,
        R.drawable.mono_chaleco_2,
        R.drawable.mono_chaleco_3,
    )

    /** Añadir mono_astronauta_1/2/3 cuando estén disponibles */
    private val ASTRONAUT_STATES = listOf(
        R.drawable.mono_astronauta,
    )

    /** Añadir mono_de_oro_1/2/3 cuando estén disponibles */
    private val ORO_STATES = listOf(
        R.drawable.mono_de_oro,
    )

    private val ACCESSORY_STATES = mapOf(
        "glasses"   to COOL_STATES,
        "hat"       to GORRO_STATES,
        "crown"     to CORONA_STATES,
        "chaleco"   to CHALECO_STATES,
        "astronaut" to ASTRONAUT_STATES,
        "gold"      to ORO_STATES,
    )

    private val ESTADOS_EXTREMO = listOf(
        R.drawable.mono_sucio_cansado,
        R.drawable.mono_sucio_enfermo,
        R.drawable.mono_sucio_frustrado,
        R.drawable.mono_sucio_llorando,
    )

    /** Imagen fija para iconos, splash y placeholders estáticos */
    val DEFAULT_PULCRO = R.drawable.mono_pulcro_1

    fun resolve(
        isClean: Boolean,
        equippedAccessory: String?,
        streakBroken: Boolean = false,
        missedDays: Int = 0
    ): Int = when {
        isClean && equippedAccessory != null -> resolveCleanAccessory(equippedAccessory)
        isClean                              -> dailyRandom(PULCRO_STATES)
        missedDays >= 4                      -> ESTADOS_EXTREMO[Random(missedDays.toLong()).nextInt(ESTADOS_EXTREMO.size)]
        missedDays == 3                      -> R.drawable.mono_sucio_3
        streakBroken                         -> R.drawable.mono_sucio_2
        else                                 -> R.drawable.mono_sucio_1
    }

    /** Variante del accesorio para previews (tienda); misma lógica diaria que en juego */
    fun previewForAccessory(accessoryId: String): Int = resolveCleanAccessory(accessoryId)

    private fun resolveCleanAccessory(accessoryId: String): Int =
        ACCESSORY_STATES[accessoryId]?.let { dailyRandom(it, accessoryId) }
            ?: dailyRandom(PULCRO_STATES)

    /** Misma variante durante todo el día; cambia al día siguiente */
    private fun dailyRandom(states: List<Int>, seedKey: String = ""): Int {
        val seed = LocalDate.now().toEpochDay() xor seedKey.hashCode().toLong()
        return states[Random(seed).nextInt(states.size)]
    }
}
