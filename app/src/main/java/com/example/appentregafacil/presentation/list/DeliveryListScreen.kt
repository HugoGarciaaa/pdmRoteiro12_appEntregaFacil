package com.example.appentregafacil.presentation.list

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.appentregafacil.domain.model.DeliveryStatus
import com.example.appentregafacil.presentation.common.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryListScreen(onNew: () -> Unit, onDetails: (Long) -> Unit, viewModel: DeliveryListViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val statusName by viewModel.statusName.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { TopAppBar(title = { Text("EntregaFácil", fontWeight = FontWeight.Bold) }) },
        floatingActionButton = { ExtendedFloatingActionButton(onClick = onNew) { Text("+  Nova entrega") } },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Cada pacote, um destino.", style = MaterialTheme.typography.titleLarge)
            Text("Acompanhe suas entregas do cadastro à chegada.", style = MaterialTheme.typography.bodyMedium)
            OutlinedTextField(value = query, onValueChange = viewModel::search, label = { Text("Pesquisar destinatário") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            StatusSelector(selected = DeliveryStatus.entries.find { it.name == statusName }, onSelect = viewModel::filter, includeAll = true)
            when {
                state.isLoading -> LoadingContent()
                state.error != null -> {
                    MessageCard(state.error!!)
                    Button(onClick = viewModel::retry) { Text("Tentar novamente") }
                }
                state.deliveries.isEmpty() -> {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Nenhuma entrega por aqui", style = MaterialTheme.typography.titleMedium)
                            Text(if (query.isNotBlank() || statusName != "ALL") "Ajuste a pesquisa ou o filtro para encontrar entregas."
                                else "Cadastre seu primeiro pacote e acompanhe o caminho até o destinatário.")
                        }
                    }
                }
                else -> LazyColumn(contentPadding = PaddingValues(bottom = 100.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item { Text("${state.deliveries.size} entrega(s)", style = MaterialTheme.typography.labelLarge) }
                    items(state.deliveries, key = { it.id }) { delivery ->
                        Card(onClick = { onDetails(delivery.id) }, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(delivery.recipientName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(delivery.packageDescription, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text("${delivery.city} / ${delivery.state}  •  #${delivery.id}", style = MaterialTheme.typography.bodySmall)
                                StatusBadge(delivery.status)
                            }
                        }
                    }
                }
            }
        }
    }
}
