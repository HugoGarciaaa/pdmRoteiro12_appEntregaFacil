package com.example.appentregafacil.presentation.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appentregafacil.domain.model.Delivery
import com.example.appentregafacil.domain.model.DeliveryStatus
import com.example.appentregafacil.domain.usecase.ObserveDeliveries
import com.example.appentregafacil.presentation.common.userMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class DeliveryListState(
    val deliveries: List<Delivery> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DeliveryListViewModel @Inject constructor(
    private val observe: ObserveDeliveries,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    val query = savedState.getStateFlow("query", "")
    val statusName = savedState.getStateFlow("status", "ALL")
    private val retry = MutableStateFlow(0)

    val state: StateFlow<DeliveryListState> = combine(query, statusName, retry) { query, status, _ ->
        query to DeliveryStatus.entries.find { it.name == status }
    }.flatMapLatest { (query, status) ->
        observe(query, status).map { DeliveryListState(deliveries = it, isLoading = false) }
            .onStart { emit(DeliveryListState()) }
            .catch { emit(DeliveryListState(isLoading = false, error = it.userMessage())) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DeliveryListState())

    fun search(value: String) { savedState["query"] = value }
    fun filter(status: DeliveryStatus?) { savedState["status"] = status?.name ?: "ALL" }
    fun retry() { retry.value++ }
}
