package com.example.appentregafacil.presentation.notification

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle

internal enum class NotificationPermissionAction { REQUEST, OPEN_SETTINGS }

internal fun notificationPermissionAction(sdkInt: Int, granted: Boolean, denied: Boolean): NotificationPermissionAction =
    if (sdkInt >= 33 && !granted && !denied) NotificationPermissionAction.REQUEST else NotificationPermissionAction.OPEN_SETTINGS

@Composable
fun NotificationPermissionCard(enabled: Boolean = true, viewModel: NotificationPermissionViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val notificationsEnabled by viewModel.enabled.collectAsStateWithLifecycle()
    var denied by rememberSaveable { mutableStateOf(false) }
    var requesting by rememberSaveable { mutableStateOf(false) }
    var rationale by rememberSaveable { mutableStateOf(false) }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        requesting = false
        denied = !granted
        message = if (granted) null else "Permissão não concedida. Você pode continuar gerenciando as entregas e ativar os avisos nas configurações."
        viewModel.refresh()
    }
    // Revalida a permissão e o canal depois de voltar das configurações do Android.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requesting = true
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else viewModel.refresh()
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Notificações de entrega", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text(if (notificationsEnabled) "Avisos ativados. Uma notificação será enviada ao alterar o status nesta tela."
                else "Ative os avisos para acompanhar as alterações de status. O cadastro e a edição funcionam mesmo sem essa permissão.")
            if (!notificationsEnabled) {
                message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                OutlinedButton(enabled = enabled && !requesting, onClick = {
                    viewModel.refresh()
                    if (!viewModel.enabled.value) {
                        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                        when (notificationPermissionAction(Build.VERSION.SDK_INT, granted, denied)) {
                            NotificationPermissionAction.REQUEST -> {
                                if (activity != null && ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS)) rationale = true
                                else requestPermission()
                            }
                            NotificationPermissionAction.OPEN_SETTINGS -> {
                                val settings = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                } else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri())
                                try { context.startActivity(settings) } catch (_: ActivityNotFoundException) {
                                    message = "Abra as configurações do Android e permita as notificações do EntregaFácil."
                                }
                            }
                        }
                    }
                }) { Text(if (requesting) "Aguardando permissão…" else if (denied) "Abrir configurações" else "Ativar notificações") }
            }
        }
    }
    if (rationale) AlertDialog(
        onDismissRequest = { rationale = false },
        title = { Text("Receber avisos de entrega?") },
        text = { Text("A permissão permite mostrar as alterações de status na área de notificações. Ela é opcional e não interfere nas suas entregas.") },
        confirmButton = { TextButton(onClick = { rationale = false; requestPermission() }) { Text("Continuar") } },
        dismissButton = { TextButton(onClick = { rationale = false }) { Text("Agora não") } },
    )
}
