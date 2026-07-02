package com.josem.monopulcro.data

import java.util.UUID

/**
 * Tarea personalizada del usuario.
 * scheduledDays usa valores ISO: 1=Lunes … 7=Domingo.
 */
data class Task(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val scheduledDays: List<Int>,   // e.g. [1,3,5] = Lun, Mié, Vie
    val notificationEnabled: Boolean = false,
    val notificationHour: Int = 9,
    val notificationMinute: Int = 0,
)
