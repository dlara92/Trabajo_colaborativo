package com.example.trabajo_colaborativo

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.DatePicker
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import java.util.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

// Configuración para que inicie automaticamente la aplicación al encerderse el dispositivo
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Verificamos que la acción sea efectivamente el inicio del dispositivo
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val i = Intent(context, MainActivity::class.java)
            // FLAG_ACTIVITY_NEW_TASK es obligatorio al iniciar una actividad desde fuera de otra actividad
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(i)
        }
    }
}

// 1. Modelo de Datos
data class Task(
    val id: Int,
    val name: String,
    val description: String,
    val dueDate: String,
    var isDone: Boolean = false
)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                TaskApp()
            }
        }
    }
}

// 2. Controlador de Navegación y Estado Global
@Composable
fun TaskApp() {
    val navController = rememberNavController()
    // Lista reactiva de tareas
    val tasks = remember { mutableStateListOf<Task>() }
    var taskIdCounter by remember { mutableStateOf(0) }

    NavHost(navController = navController, startDestination = "list_screen") {
        composable("list_screen") {
            TaskListScreen(navController, tasks,
                onToggleDone = { task ->
                    val index = tasks.indexOf(task)
                    if (index != -1) {
                        tasks[index] = task.copy(isDone = true)
                    }
                },
                onDelete = { task -> tasks.remove(task) }
            )
        }
        composable("add_screen") {
            AddTaskScreen(navController, onSave = { name, desc, date ->
                tasks.add(Task(id = taskIdCounter++, name = name, description = desc, dueDate = date))
            })
        }
        composable("detail_screen/{taskId}") { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString("taskId")?.toIntOrNull()
            val task = tasks.find { it.id == taskId }
            if (task != null) {
                TaskDetailScreen(navController, task)
            }
        }
    }
}

// 3. Pantalla Inicial (Lista de Tareas)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    navController: NavController,
    tasks: List<Task>,
    onToggleDone: (Task) -> Unit,
    onDelete: (Task) -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Mis Tareas") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("add_screen") }) {
                Icon(Icons.Filled.Add, contentDescription = "Añadir Tarea")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Encabezado de la "Tabla"
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Tarea", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("Fecha", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("Acciones", fontWeight = FontWeight.Bold)
                }
                Divider()
            }

            // Filas de tareas
            items(tasks) { task ->
                val backgroundColor = if (task.isDone) Color(0xFFE8F5E9) else Color.Transparent // Verde claro si está realizada

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(backgroundColor)
                        .clickable { navController.navigate("detail_screen/${task.id}") }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        if (task.isDone) {
                            Icon(Icons.Filled.Check, contentDescription = "Realizada", tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp).padding(end = 4.dp))
                        }
                        Text(task.name)
                    }

                    Text(task.dueDate, modifier = Modifier.weight(1f))

                    Row {
                        if (!task.isDone) {
                            TextButton(onClick = { onToggleDone(task) }) {
                                Text("Realizado")
                            }
                        }
                        IconButton(onClick = { onDelete(task) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Borrar", tint = Color.Red)
                        }
                    }
                }
                Divider()
            }
        }
    }
}
// 4. Pantalla para Añadir Tareas
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskScreen(navController: NavController, onSave: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
            date = "$dayOfMonth/${month + 1}/$year"
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Scaffold(
        topBar = { TopAppBar(title = { Text("Nueva Tarea") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre de la tarea") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descripción de la tarea") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            OutlinedButton(
                onClick = { datePickerDialog.show() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (date.isEmpty()) "Seleccionar Fecha" else "Fecha: $date")
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(onClick = { navController.popBackStack() }) {
                    Text("Descartar", color = Color.Red)
                }
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onSave(name, description, date)
                            navController.popBackStack() // Vuelve a la pantalla inicial
                        }
                    }
                ) {
                    Text("Guardar")
                }
            }
        }
    }
}

// 5. Pantalla de Detalles de la Tarea
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(navController: NavController, task: Task) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Detalles de la Tarea") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(text = task.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Fecha límite: ${task.dueDate}", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = task.description, style = MaterialTheme.typography.bodyLarge)

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Volver atrás")
            }
        }
    }
}