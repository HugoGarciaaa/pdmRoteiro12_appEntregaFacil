package com.example.appentregafacil.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appentregafacil.domain.model.Delivery
import com.example.appentregafacil.domain.model.DeliveryStatus
import com.example.appentregafacil.domain.usecase.ChangeDeliveryStatus
import com.example.appentregafacil.domain.usecase.DeleteDelivery
import com.example.appentregafacil.domain.usecase.GetDelivery
import com.example.appentregafacil.presentation.common.userMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeliveryDetailState(
    val delivery: Delivery? = null,
    val isLoading: Boolean = true,
    val isWorking: Boolean = false,
    val message: String? = null,
    val loadFailed: Boolean = false,
    val deleted: Boolean = false,
)

@HiltViewModel
class DeliveryDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val get: GetDelivery,
    private val changeStatus: ChangeDeliveryStatus,
    private val delete: DeleteDelivery,
) : ViewModel() {
    private val id: Long = checkNotNull(savedState["deliveryId"])
    private val mutableState = MutableStateFlow(DeliveryDetailState())
    val state = mutableState.asStateFlow()
    private var observation: Job? = null

    init { load() }

    fun load() {
        observation?.cancel()
        mutableState.update { it.copy(isLoading = true, loadFailed = false, message = null) }
        observation = viewModelScope.launch {
            get(id).catch { error ->
                mutableState.update { it.copy(isLoading = false, loadFailed = true, message = error.userMessage()) }
            }.collect { delivery -> mutableState.update { it.copy(delivery = delivery, isLoading = false) } }
        }
    }

    fun status(status: DeliveryStatus) = work { changeStatus(id, status) }
    fun delete() = work {
        delete(id)
        mutableState.update { it.copy(deleted = true) }
    }
    private fun work(action: suspend () -> Unit) {
        if (state.value.isWorking || state.value.delivery == null) return
        mutableState.update { it.copy(isWorking = true, message = null) }
        viewModelScope.launch {
            try { action() } catch (error: Exception) {
                mutableState.update { it.copy(message = error.userMessage()) }
            } finally { mutableState.update { it.copy(isWorking = false) } }
        }
    }
}
