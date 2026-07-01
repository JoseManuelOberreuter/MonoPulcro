package com.josem.monopulcro.data

data class DustMote(
    /** 0..1 posición horizontal del centro de la mota */
    val xFrac: Float,
    /** 0..1 posición vertical del centro de la mota */
    val yFrac: Float,
    val sizeDp: Float
) {
    companion object {
        /**
         * Pelusas fijas alrededor del mono (+1 por hora, en este orden).
         * xFrac/yFrac = centro de la mota (0..1) sobre el cuadro del mono (~240 dp).
         */
        val SLOTS: List<DustMote> = listOf(
            DustMote(xFrac = 0.72f, yFrac = 0.32f, sizeDp = 46f), // hombro derecho
            DustMote(xFrac = 0.30f, yFrac = 0.54f, sizeDp = 44f), // flanco izquierdo
            DustMote(xFrac = 0.66f, yFrac = 0.52f, sizeDp = 40f), // flanco derecho
            DustMote(xFrac = 0.30f, yFrac = 0.72f, sizeDp = 32f), // pierna izquierda
            DustMote(xFrac = 0.72f, yFrac = 0.74f, sizeDp = 60f), // pierna derecha
        )
    }
}

fun dustMotesForCount(count: Int, max: Int = DustMote.SLOTS.size): List<DustMote> =
    DustMote.SLOTS.take(count.coerceIn(0, max.coerceAtMost(DustMote.SLOTS.size)))
