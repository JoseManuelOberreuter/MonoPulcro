package com.josem.monopulcro.ui

import com.josem.monopulcro.R
import java.util.Random

object MonkeyImageResolver {

    private const val MS_PER_VARIANT_SLOT = 3 * 60 * 60 * 1000L

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

    private val PAYASO_STATES = listOf(
        R.drawable.mono_payaso_1,
        R.drawable.mono_payaso_2,
        R.drawable.mono_payaso_3,
    )

    private val ASTRONAUT_STATES = listOf(
        R.drawable.mono_astronauta_1,
        R.drawable.mono_astronauta_2,
        R.drawable.mono_astronauta_3,
    )

    private val ACCESSORY_STATES = mapOf(
        "glasses"   to COOL_STATES,
        "hat"       to GORRO_STATES,
        "crown"     to CORONA_STATES,
        "chaleco"   to CHALECO_STATES,
        "payaso"    to PAYASO_STATES,
        "astronaut" to ASTRONAUT_STATES,
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
        isClean                              -> variantRandom(PULCRO_STATES)
        missedDays >= 4                      -> ESTADOS_EXTREMO[Random(missedDays.toLong()).nextInt(ESTADOS_EXTREMO.size)]
        missedDays == 3                      -> R.drawable.mono_sucio_3
        streakBroken                         -> R.drawable.mono_sucio_2
        else                                 -> R.drawable.mono_sucio_1
    }

    /** Variante _1 fija de cada accesorio para la tienda */
    fun previewForAccessory(accessoryId: String): Int =
        ACCESSORY_STATES[accessoryId]?.firstOrNull() ?: DEFAULT_PULCRO

    private fun resolveCleanAccessory(accessoryId: String): Int =
        ACCESSORY_STATES[accessoryId]?.let { variantRandom(it, accessoryId) }
            ?: variantRandom(PULCRO_STATES)

    /** Misma variante durante 3 h; cambia al iniciar el siguiente bloque */
    private fun variantRandom(states: List<Int>, seedKey: String = ""): Int {
        val slot = System.currentTimeMillis() / MS_PER_VARIANT_SLOT
        val seed = slot xor seedKey.hashCode().toLong()
        return states[Random(seed).nextInt(states.size)]
    }
}
