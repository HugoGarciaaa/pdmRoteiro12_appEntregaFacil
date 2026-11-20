package com.example.appentregafacil.presentation.detail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.appentregafacil.presentation.common.*
import com.example.appentregafacil.presentation.notification.NotificationPermissionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryDetailScreen(onBack: () -> Unit, onEdit: (Long) -> Unit, onDeleted: () -> Unit,
    viewModel: DeliveryDetailViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = state.isWorking) { }
    LaunchedEffect(state.deleted) { if (state.deleted) onDeleted() }
    Scaffold(topBar = {
        TopAppBar(title = { Text("Detalhes da entrega") },
            navigationIcon = { TextButton(onClick = onBack, enabled = !state.isWorking) { Text("Voltar") } })
    }) { padding ->
        if (state.isLoading) Box(Modifier.padding(padding)) { LoadingContent() }
        else Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            state.message?.let { MessageCard(it) }
            state.notice?.let { MessageCard(it, isError = false) }
            if (state.loadFailed) Button(onClick = viewModel::load) { Text("Tentar novamente") }
            val delivery = state.delivery
            if (delivery == null && !state.loadFailed && !state.deleted && !state.isWorking) Text("Entrega não encontrada ou excluída.")
            delivery?.let {
                Text("Entrega #${it.id}", style = MaterialTheme.typography.labelLarge)
                Text(it.recipientName, style = MaterialTheme.typography.headlineMedium)
                StatusBadge(it.status)
                DetailCard("Pacote") { Text(it.packageDescription) }
                DetailCard("Destino") {
                    Text("${it.street}, ${it.number}", style = MaterialTheme.typography.titleMedium)
                    Text("Complemento: ${it.complement.ifBlank { "Não informado" }}")
                    Text("Bairro: ${it.neighborhood}")
                    Text("${it.city} / ${it.state}")
                    Text("CEP ${it.postalCode.displayPostalCode()}")
                }
                DetailCard("Acompanhamento") {
                    Text("Criada em ${it.createdAt.displayDate()}")
                    Text("Atualizada em ${it.updatedAt.displayDate()}")
                    Text("Alterar status", style = MaterialTheme.typography.titleSmall)
                    StatusSelector(it.status, { selected -> selected?.let(viewModel::status) }, enabled = !state.isWorking)
                }
                if (state.isWorking) LinearProgressIndicator(Modifier.fillMaxWidth())
                NotificationPermissionCard(enabled = !state.isWorking)
                Button(onClick = { onEdit(it.id) }, enabled = !state.isWorking, modifier = Modifier.fillMaxWidth()) { Text("Editar entrega") }
                OutlinedButton(onClick = { confirmDelete = true }, enabled = !state.isWorking,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error), modifier = Modifier.fillMaxWidth()) { Text("Excluir entrega") }
            }
        }
    }
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false }, title = { Text("Excluir entrega?") },
        text = { Text("A entrega deixará de aparecer nas consultas. Seus dados serão preservados com a data de exclusão.") },
        confirmButton = { TextButton(onClick = { confirmDelete = false; viewModel.delete() }) { Text("Excluir") } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") } },
    )
}

@Composable
private fun DetailCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}
