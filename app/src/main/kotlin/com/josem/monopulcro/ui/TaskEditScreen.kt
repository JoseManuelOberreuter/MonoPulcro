package com.josem.monopulcro.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.res.painterResource
import com.josem.monopulcro.R
import com.josem.monopulcro.data.Task
import java.util.UUID

// ─── Días de la semana ────────────────────────────────────────────────────────

private val DAY_LABELS = listOf("L", "M", "X", "J", "V", "S", "D")
private val DAY_FULL   = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditScreen(
    taskId: String,
    onNavigateBack: () -> Unit,
    vm: MonkeyViewModel = viewModel()
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val isNew = taskId == "new"

    // Inicializamos el formulario una sola vez con los datos existentes
    val existingTask = remember(taskId) {
        if (!isNew) vm.uiState.value.allTasks.find { it.id == taskId } else null
    }

    var taskName by rememberSaveable { mutableStateOf(existingTask?.name ?: "") }
    var selectedDays by rememberSaveable {
        mutableStateOf((existingTask?.scheduledDays ?: emptyList()).toSet())
    }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val isValid = taskName.isNotBlank() && selectedDays.isNotEmpty()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            // ── Nombre de la tarea ─────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Nombre de la tarea",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF64748B)
                )
                OutlinedTextField(
                    value = taskName,
                    onValueChange = { taskName = it },
                    placeholder = { Text("ej. Lavar los platos", color = Color(0xFFCBD5E1)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { keyboardController?.hide() }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = Color(0xFF7DD3FC),
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                        focusedContainerColor   = Color.White,
                        unfocusedContainerColor = Color(0xFFF8FAFC)
                    )
                )
            }

            // ── Frecuencia / días ──────────────────────────────────────────────
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

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.mono_pulcro),
                    contentDescription = null,
                    modifier = Modifier.size(200.dp)
                )
            }

            // ── Botón guardar ──────────────────────────────────────────────────
            Button(
                onClick = {
                    keyboardController?.hide()
                    val task = if (isNew) {
                        Task(
                            id = UUID.randomUUID().toString(),
                            name = taskName.trim(),
                            scheduledDays = selectedDays.sorted()
                        )
                    } else {
                        existingTask!!.copy(
                            name = taskName.trim(),
                            scheduledDays = selectedDays.sorted()
                        )
                    }
                    if (isNew) vm.addTask(task) else vm.updateTask(task)
                    onNavigateBack()
                },
                enabled = isValid,
                modifier = Modifier
                    .fillMaxWidth()
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
