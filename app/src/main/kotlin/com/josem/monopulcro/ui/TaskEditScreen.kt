package com.josem.monopulcro.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.res.painterResource
import com.josem.monopulcro.R
import com.josem.monopulcro.data.PredefinedTasks
import com.josem.monopulcro.data.Task
import java.util.UUID

// ─── Días de la semana ────────────────────────────────────────────────────────

private val DAY_LABELS = listOf("L", "M", "X", "J", "V", "S", "D")
private val DAY_FULL   = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
private const val CUSTOM_TASK_LABEL = "Tarea personalizada"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditScreen(
    taskId: String,
    onNavigateBack: () -> Unit,
    vm: MonkeyViewModel = viewModel()
) {
    val isNew = taskId == "new"

    // Inicializamos el formulario una sola vez con los datos existentes
    val existingTask = remember(taskId) {
        if (!isNew) vm.uiState.value.allTasks.find { it.id == taskId } else null
    }

    var taskName by rememberSaveable { mutableStateOf(existingTask?.name ?: "") }
    var isCustomTask by rememberSaveable {
        mutableStateOf(existingTask?.name?.let { it !in PredefinedTasks.names } ?: false)
    }
    var selectedDays by rememberSaveable {
        mutableStateOf((existingTask?.scheduledDays ?: emptyList()).toSet())
    }
    var notificationEnabled by rememberSaveable {
        mutableStateOf(existingTask?.notificationEnabled ?: false)
    }
    var notificationHour by rememberSaveable {
        mutableStateOf(existingTask?.notificationHour?.coerceIn(0, 23) ?: 9)
    }
    var notificationMinute by rememberSaveable {
        mutableStateOf(existingTask?.notificationMinute?.coerceIn(0, 59) ?: 0)
    }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val trimmedName = taskName.trim()
    val isValid = trimmedName.isNotBlank() && selectedDays.isNotEmpty()

    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isNew) "Nueva tarea" else "Editar tarea",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                actions = {
                    if (!isNew) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Eliminar",
                                tint = Color(0xFFDC2626)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF1E293B)
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
        ) {
            Column(
                modifier = Modifier.padding(top = 20.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                // ── Nombre de la tarea ─────────────────────────────────────────
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Elige una tarea",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64748B)
                    )
                    TaskNameSelector(
                        taskName = taskName,
                        isCustomTask = isCustomTask,
                        onTaskNameChange = { taskName = it },
                        onCustomModeChange = { isCustom ->
                            isCustomTask = isCustom
                            if (isCustom && taskName in PredefinedTasks.names) {
                                taskName = ""
                            }
                        },
                    )
                }

                // ── Frecuencia / días ──────────────────────────────────────────
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "¿Qué días la harás?",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64748B)
                    )
                    Text(
                        text = if (selectedDays.isEmpty())
                            "Selecciona al menos un día"
                        else
                            formatSelectedDays(selectedDays),
                        fontSize = 13.sp,
                        color = if (selectedDays.isEmpty()) Color(0xFFDC2626) else Color(0xFF16A34A),
                        fontWeight = FontWeight.Medium
                    )
                    DayPicker(
                        selectedDays = selectedDays,
                        onDayToggle = { day ->
                            selectedDays = if (day in selectedDays)
                                selectedDays - day
                            else
                                selectedDays + day
                        }
                    )
                }

                // ── Notificación ─────────────────────────────────────────────
                TaskNotificationSection(
                    enabled = notificationEnabled,
                    onEnabledChange = { notificationEnabled = it },
                    hour = notificationHour,
                    minute = notificationMinute,
                    onTimeChange = { hour, minute ->
                        notificationHour = hour
                        notificationMinute = minute
                    },
                )
            }

            // El mono ocupa solo el espacio libre entre formulario y CTA.
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                val monkeySize = maxHeight.coerceIn(0.dp, 160.dp)
                if (monkeySize >= 64.dp) {
                    Image(
                        painter = painterResource(MonkeyImageResolver.DEFAULT_PULCRO),
                        contentDescription = null,
                        modifier = Modifier.size(monkeySize),
                    )
                }
            }

            Button(
                onClick = {
                    val task = if (isNew) {
                        Task(
                            id = UUID.randomUUID().toString(),
                            name = trimmedName,
                            scheduledDays = selectedDays.sorted(),
                            notificationEnabled = notificationEnabled,
                            notificationHour = notificationHour,
                            notificationMinute = notificationMinute,
                        )
                    } else {
                        existingTask!!.copy(
                            name = trimmedName,
                            scheduledDays = selectedDays.sorted(),
                            notificationEnabled = notificationEnabled,
                            notificationHour = notificationHour,
                            notificationMinute = notificationMinute,
                        )
                    }
                    if (isNew) vm.addTask(task) else vm.updateTask(task)
                    onNavigateBack()
                },
                enabled = isValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 20.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0EA5E9),
                    disabledContainerColor = Color(0xFFE2E8F0)
                )
            ) {
                Text(
                    text = if (isNew) "Agregar tarea" else "Guardar cambios",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isValid) Color.White else Color(0xFF94A3B8)
                )
            }
        }
    }

    // ── Diálogo de confirmación para eliminar ──────────────────────────────────
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar tarea", fontWeight = FontWeight.Bold) },
            text  = { Text("¿Seguro que quieres eliminar \"${existingTask?.name}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.deleteTask(taskId)
                        showDeleteDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text("Eliminar", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

// ─── Notificación por tarea ───────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskNotificationSection(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    hour: Int,
    minute: Int,
    onTimeChange: (Int, Int) -> Unit,
) {
    var showTimePicker by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Recordatorio",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF64748B),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "Recibir notificación",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1E293B),
            )
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = Color(0xFF0EA5E9),
                    checkedThumbColor = Color.White,
                ),
            )
        }
        if (enabled) {
            Text(
                text = "¿A qué hora quieres que te notifiquemos?",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF64748B),
            )
            OutlinedButton(
                onClick = { showTimePicker = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF0EA5E9),
                ),
            ) {
                Text(
                    text = formatTime(hour, minute),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }

    if (showTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = hour,
            initialMinute = minute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Hora del recordatorio", fontWeight = FontWeight.Bold) },
            text = {
                TimePicker(
                    state = timeState,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onTimeChange(timeState.hour, timeState.minute)
                        showTimePicker = false
                    },
                ) {
                    Text("Listo", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancelar")
                }
            },
        )
    }
}

private fun formatTime(hour: Int, minute: Int): String =
    "%02d:%02d".format(hour, minute)

// ─── Selector de tarea ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskNameSelector(
    taskName: String,
    isCustomTask: Boolean,
    onTaskNameChange: (String) -> Unit,
    onCustomModeChange: (Boolean) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    val predefinedOptions = PredefinedTasks.names
    val dropdownValue = if (isCustomTask) CUSTOM_TASK_LABEL else taskName

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedTextField(
                value = dropdownValue,
                onValueChange = {},
                readOnly = true,
                placeholder = { Text("Selecciona una tarea", color = Color(0xFFCBD5E1)) },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF7DD3FC),
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color(0xFFF8FAFC),
                ),
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.heightIn(max = 320.dp),
            ) {
                predefinedOptions.forEach { name ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = name,
                                fontWeight = if (!isCustomTask && name == taskName) {
                                    FontWeight.SemiBold
                                } else {
                                    FontWeight.Normal
                                },
                            )
                        },
                        onClick = {
                            onCustomModeChange(false)
                            onTaskNameChange(name)
                            expanded = false
                        },
                    )
                }
                DropdownMenuItem(
                    text = {
                        Text(
                            text = CUSTOM_TASK_LABEL,
                            fontWeight = if (isCustomTask) FontWeight.SemiBold else FontWeight.Normal,
                            color = Color(0xFF0EA5E9),
                        )
                    },
                    onClick = {
                        onCustomModeChange(true)
                        expanded = false
                    },
                )
            }
        }

        if (isCustomTask) {
            OutlinedTextField(
                value = taskName,
                onValueChange = onTaskNameChange,
                placeholder = { Text("Escribe tu tarea", color = Color(0xFFCBD5E1)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = { keyboardController?.hide() },
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF7DD3FC),
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color(0xFFF8FAFC),
                ),
            )
        }
    }
}

// ─── Selector de días ─────────────────────────────────────────────────────────

@Composable
private fun DayPicker(
    selectedDays: Set<Int>,
    onDayToggle: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        DAY_LABELS.forEachIndexed { idx, label ->
            val dayNum = idx + 1  // 1=Lun … 7=Dom
            val selected = dayNum in selectedDays

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        color  = if (selected) Color(0xFF0EA5E9) else Color(0xFFF1F5F9),
                        shape  = CircleShape
                    )
                    .border(
                        width = if (selected) 0.dp else 1.dp,
                        color = Color(0xFFE2E8F0),
                        shape = CircleShape
                    )
            ) {
                TextButton(
                    onClick = { onDayToggle(dayNum) },
                    modifier = Modifier.fillMaxSize(),
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 14.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) Color.White else Color(0xFF64748B)
                    )
                }
            }
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

private fun formatSelectedDays(days: Set<Int>): String {
    if (days.size == 7) return "Todos los días"
    val sorted = days.sorted()
    return sorted.joinToString(", ") { DAY_FULL[it - 1] }
}
