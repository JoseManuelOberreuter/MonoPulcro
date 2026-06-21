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
        isClean && equippedAccessory == "glasses"   -> R.drawable.mono_cool
        isClean && equippedAccessory == "hat"       -> R.drawable.mono_gorro
        isClean && equippedAccessory == "crown"     -> R.drawable.mono_corona
        isClean && equippedAccessory == "astronaut" -> R.drawable.mono_astronauta
        isClean && equippedAccessory == "gold"      -> R.drawable.mono_de_oro
        isClean                                     -> randomPulcro()
        missedDays >= 4                             -> ESTADOS_EXTREMO[Random(missedDays.toLong()).nextInt(ESTADOS_EXTREMO.size)]
        missedDays == 3                             -> R.drawable.mono_sucio_3
        streakBroken                                -> R.drawable.mono_sucio_2
        else                                        -> R.drawable.mono_sucio_1
    }

    /** Misma variante durante todo el día; cambia al día siguiente */
    private fun randomPulcro(): Int =
        PULCRO_STATES[Random(LocalDate.now().toEpochDay()).nextInt(PULCRO_STATES.size)]
}
