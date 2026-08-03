package com.josem.monopulcro.notifications

/**
 * Slots del recordatorio de hábito (reemplaza el diario único a las 20:00).
 * Familia A: con racha y tareas pendientes.
 * Familia B: sin racha y con días perdidos.
 */
enum class HabitNotificationSlot(
    val id: String,
    val requestCode: Int,
    val hour: Int,
    val minute: Int,
    val family: Family,
) {
    A1("a1", 2101, 9, 0, Family.A),
    A2("a2", 2102, 12, 0, Family.A),
    A3("a3", 2103, 18, 0, Family.A),
    A4("a4", 2104, 21, 0, Family.A),
    A5("a5", 2105, 23, 0, Family.A),
    B("b", 2110, 10, 30, Family.B);

    enum class Family { A, B }

    companion object {
        fun fromId(id: String?): HabitNotificationSlot? =
            entries.find { it.id == id }
    }
}
