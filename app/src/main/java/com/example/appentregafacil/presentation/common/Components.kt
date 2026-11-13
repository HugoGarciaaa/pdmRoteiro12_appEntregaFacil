package com.example.appentregafacil.presentation.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.appentregafacil.domain.model.DeliveryStatus

@Composable
fun StatusBadge(status: DeliveryStatus) {
    val colors = MaterialTheme.colorScheme
    val background = when (status) {
        DeliveryStatus.PENDING -> colors.secondaryContainer
        DeliveryStatus.IN_TRANSIT -> colors.primaryContainer
        DeliveryStatus.DELIVERED -> colors.tertiaryContainer
    }
    Surface(color = background, shape = MaterialTheme.shapes.small) {
        Text(status.label, Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun StatusSelector(selected: DeliveryStatus?, onSelect: (DeliveryStatus?) -> Unit, includeAll: Boolean = false, enabled: Boolean = true) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (includeAll) FilterChip(selected = selected == null, onClick = { onSelect(null) }, label = { Text("Todas") }, enabled = enabled)
        DeliveryStatus.entries.forEach { status ->
            FilterChip(selected = selected == status, onClick = { onSelect(status) }, label = { Text(status.label) }, enabled = enabled)
        }
    }
}

@Composable
fun MessageCard(message: String, isError: Boolean = true) {
    Surface(color = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
        Text(message, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun LoadingContent() {
    Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) { CircularProgressIndicator() }
}
