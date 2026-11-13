package com.example.appentregafacil.presentation.form

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.appentregafacil.domain.model.*
import com.example.appentregafacil.presentation.common.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryFormScreen(onBack: () -> Unit, onSaved: (Long) -> Unit, viewModel: DeliveryFormViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val draft = state.draft
    val enabled = !state.isSaving && !state.isLoading && !state.loadFailed
    BackHandler(enabled = state.isSaving) { /* Aguarda a gravação para evitar uma navegação duplicada. */ }
    LaunchedEffect(state.savedId) { state.savedId?.let(onSaved) }
    Scaffold(topBar = {
        TopAppBar(title = { Text(if (viewModel.deliveryId == null) "Nova entrega" else "Editar entrega") },
            navigationIcon = { TextButton(onClick = onBack, enabled = !state.isSaving) { Text("Voltar") } })
    }) { padding ->
        if (state.isLoading) {
            Box(Modifier.padding(padding)) { LoadingContent() }
        } else Column(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.message?.let { MessageCard(it) }
            if (state.loadFailed) {
                Button(onClick = viewModel::load) { Text("Tentar carregar novamente") }
            } else {
                Text("Sobre o pacote", style = MaterialTheme.typography.titleLarge)
                FormField("Destinatário *", draft.recipientName, DeliveryField.RECIPIENT, state, enabled, viewModel::change)
                FormField("Descrição do pacote *", draft.packageDescription, DeliveryField.DESCRIPTION, state, enabled, viewModel::change, singleLine = false)
                Text("Endereço de entrega", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
                Text("Consulte o CEP para buscar o endereço. Complete os dados antes de salvar.", style = MaterialTheme.typography.bodyMedium)
                FormField("CEP *", draft.postalCode, DeliveryField.POSTAL_CODE, state, enabled && !state.isLookingUp, viewModel::change,
                    keyboardType = KeyboardType.Number, helper = "Com ou sem hífen. Ex.: 01001-000")
                OutlinedButton(onClick = viewModel::lookup, enabled = enabled && !state.isLookingUp, modifier = Modifier.fillMaxWidth()) {
                    if (state.isLookingUp) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Consultando CEP…")
                    } else Text("Consultar CEP")
                }
                if (state.verifiedPostalCode == PostalCode.normalize(draft.postalCode)) {
                    MessageCard(state.addressMessage ?: "Endereço validado. Se alterar o CEP, consulte novamente.", isError = false)
                }
                FormField("Rua *", draft.street, DeliveryField.STREET, state, enabled && !state.isLookingUp, viewModel::change)
                FormField("Número *", draft.number, DeliveryField.NUMBER, state, enabled, viewModel::change, helper = "Use S/N se o endereço não tiver número.")
                FormField("Complemento", draft.complement, DeliveryField.COMPLEMENT, state, enabled, viewModel::change)
                FormField("Bairro *", draft.neighborhood, DeliveryField.NEIGHBORHOOD, state, enabled && !state.isLookingUp, viewModel::change)
                FormField("Cidade *", draft.city, DeliveryField.CITY, state, enabled && !state.isLookingUp, viewModel::change)
                FormField("Estado (UF) *", draft.state, DeliveryField.STATE, state, enabled && !state.isLookingUp, viewModel::change)
                Text("Etapa da entrega", style = MaterialTheme.typography.titleMedium)
                StatusSelector(draft.status, { it?.let(viewModel::changeStatus) }, enabled = enabled)
                Text("* Campos obrigatórios", style = MaterialTheme.typography.bodySmall)
                Button(onClick = viewModel::save, enabled = enabled && !state.isLookingUp, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                    if (state.isSaving) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (state.isSaving) "Salvando…" else "Salvar entrega")
                }
            }
        }
    }
}

@Composable
private fun FormField(
    label: String, value: String, field: DeliveryField, state: DeliveryFormState, enabled: Boolean,
    onChange: (DeliveryField, String) -> Unit, singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text, helper: String? = null,
) {
    val error = state.errors[field]
    OutlinedTextField(
        value = value, onValueChange = { onChange(field, it) }, label = { Text(label) },
        enabled = enabled, isError = error != null, singleLine = singleLine,
        minLines = if (singleLine) 1 else 2, modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(capitalization = if (field == DeliveryField.STATE) KeyboardCapitalization.Characters else KeyboardCapitalization.Sentences,
            keyboardType = keyboardType),
        supportingText = if (error != null || helper != null) ({ Text(error ?: helper.orEmpty()) }) else null,
    )
}
