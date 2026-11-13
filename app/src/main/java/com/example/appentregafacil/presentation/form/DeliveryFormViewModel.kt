package com.example.appentregafacil.presentation.form

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appentregafacil.domain.model.*
import com.example.appentregafacil.domain.usecase.GetDelivery
import com.example.appentregafacil.domain.usecase.LookupPostalCode
import com.example.appentregafacil.domain.usecase.SaveDelivery
import com.example.appentregafacil.presentation.common.userMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeliveryFormState(
    val draft: DeliveryDraft = DeliveryDraft(),
    val verifiedPostalCode: String? = null,
    val errors: Map<DeliveryField, String> = emptyMap(),
    val isLoading: Boolean = false,
    val loadFailed: Boolean = false,
    val isLookingUp: Boolean = false,
    val isSaving: Boolean = false,
    val message: String? = null,
    val addressMessage: String? = null,
    val savedId: Long? = null,
)

@HiltViewModel
class DeliveryFormViewModel @Inject constructor(
    private val getDelivery: GetDelivery,
    private val lookupPostalCode: LookupPostalCode,
    private val saveDelivery: SaveDelivery,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    val deliveryId: Long? = savedState.get<Long>("deliveryId")
    private val mutableState = MutableStateFlow(DeliveryFormState())
    val state = mutableState.asStateFlow()

    init {
        if (savedState.get<Boolean>("initialized") == true) {
            mutableState.value = DeliveryFormState(
                draft = DeliveryDraft(
                    recipientName = restored(DeliveryField.RECIPIENT), packageDescription = restored(DeliveryField.DESCRIPTION),
                    postalCode = restored(DeliveryField.POSTAL_CODE), street = restored(DeliveryField.STREET),
                    number = restored(DeliveryField.NUMBER), complement = restored(DeliveryField.COMPLEMENT),
                    neighborhood = restored(DeliveryField.NEIGHBORHOOD), city = restored(DeliveryField.CITY), state = restored(DeliveryField.STATE),
                    status = DeliveryStatus.valueOf(savedState["draftStatus"] ?: DeliveryStatus.PENDING.name),
                ), verifiedPostalCode = savedState["verifiedPostalCode"],
            )
        } else if (deliveryId != null) load() else persist()
    }

    fun load() {
        val id = deliveryId ?: return
        if (state.value.isLoading) return
        mutableState.update { it.copy(isLoading = true, loadFailed = false, message = null) }
        viewModelScope.launch {
            try {
                val delivery = getDelivery.once(id)
                mutableState.value = DeliveryFormState(
                    draft = with(delivery) { DeliveryDraft(recipientName, packageDescription, postalCode, street,
                        number, complement, neighborhood, city, state, status) },
                    // O CEP de um registro existente já foi validado no cadastro.
                    verifiedPostalCode = delivery.postalCode,
                )
                persist()
            } catch (error: Exception) {
                mutableState.update { it.copy(loadFailed = true, message = error.userMessage()) }
            } finally { mutableState.update { it.copy(isLoading = false) } }
        }
    }

    fun change(field: DeliveryField, value: String) {
        val current = state.value
        if (current.isSaving || current.isLoading || current.loadFailed || (field == DeliveryField.POSTAL_CODE && current.isLookingUp)) return
        val draft = current.draft
        val updated = when (field) {
            DeliveryField.RECIPIENT -> draft.copy(recipientName = value)
            DeliveryField.DESCRIPTION -> draft.copy(packageDescription = value)
            DeliveryField.POSTAL_CODE -> draft.copy(postalCode = value)
            DeliveryField.STREET -> draft.copy(street = value)
            DeliveryField.NUMBER -> draft.copy(number = value)
            DeliveryField.COMPLEMENT -> draft.copy(complement = value)
            DeliveryField.NEIGHBORHOOD -> draft.copy(neighborhood = value)
            DeliveryField.CITY -> draft.copy(city = value)
            DeliveryField.STATE -> draft.copy(state = value.uppercase().take(2))
        }
        val postalCodeChanged = field == DeliveryField.POSTAL_CODE &&
            PostalCode.normalize(value) != PostalCode.normalize(draft.postalCode)
        mutableState.update { it.copy(draft = updated, errors = it.errors - field, message = null,
            verifiedPostalCode = if (postalCodeChanged) null else it.verifiedPostalCode,
            addressMessage = if (postalCodeChanged) null else it.addressMessage) }
        persist()
    }

    fun changeStatus(status: DeliveryStatus) {
        if (state.value.isSaving || state.value.isLoading || state.value.loadFailed) return
        mutableState.update { it.copy(draft = it.draft.copy(status = status)) }
        persist()
    }

    fun lookup() {
        val current = state.value
        if (current.isLookingUp || current.isSaving || current.isLoading || current.loadFailed) return
        if (!PostalCode.isValid(current.draft.postalCode)) {
            mutableState.update { it.copy(errors = it.errors + (DeliveryField.POSTAL_CODE to "Informe um CEP com 8 dígitos.")) }
            return
        }
        val postalCode = PostalCode.normalize(current.draft.postalCode)
        mutableState.update { it.copy(isLookingUp = true, message = null, addressMessage = null) }
        viewModelScope.launch {
            try {
                val address = lookupPostalCode(postalCode)
                mutableState.update { it.copy(
                    draft = it.draft.copy(postalCode = address.postalCode, street = address.street,
                        neighborhood = address.neighborhood, city = address.city, state = address.state),
                    verifiedPostalCode = postalCode,
                    errors = it.errors - setOf(DeliveryField.POSTAL_CODE, DeliveryField.STREET, DeliveryField.NEIGHBORHOOD, DeliveryField.CITY, DeliveryField.STATE),
                    addressMessage = "CEP consultado com sucesso. Confira o endereço e complete os campos que faltam.",
                ) }
                persist()
            } catch (error: Exception) {
                mutableState.update { it.copy(message = error.userMessage()) }
            } finally { mutableState.update { it.copy(isLookingUp = false) } }
        }
    }

    fun save() {
        val current = state.value
        if (current.isSaving || current.isLookingUp || current.isLoading || current.loadFailed || current.savedId != null) return
        val errors = DeliveryValidation.errors(current.draft, current.verifiedPostalCode)
        if (errors.isNotEmpty()) {
            mutableState.update { it.copy(errors = errors, message = "Revise os campos indicados antes de salvar.") }
            return
        }
        mutableState.update { it.copy(isSaving = true, message = null) }
        viewModelScope.launch {
            try {
                val id = saveDelivery(current.draft, current.verifiedPostalCode, deliveryId)
                mutableState.update { it.copy(savedId = id) }
            } catch (error: FormValidationException) {
                mutableState.update { it.copy(errors = error.errors) }
            } catch (error: Exception) {
                mutableState.update { it.copy(message = error.userMessage()) }
            } finally { mutableState.update { it.copy(isSaving = false) } }
        }
    }

    private fun restored(field: DeliveryField): String = savedState[field.name] ?: ""

    private fun persist() {
        with(state.value.draft) {
            mapOf(DeliveryField.RECIPIENT to recipientName, DeliveryField.DESCRIPTION to packageDescription,
                DeliveryField.POSTAL_CODE to postalCode, DeliveryField.STREET to street, DeliveryField.NUMBER to number,
                DeliveryField.COMPLEMENT to complement, DeliveryField.NEIGHBORHOOD to neighborhood,
                DeliveryField.CITY to city, DeliveryField.STATE to this.state).forEach { (field, value) -> savedState[field.name] = value }
            savedState["draftStatus"] = status.name
        }
        savedState["verifiedPostalCode"] = state.value.verifiedPostalCode
        savedState["initialized"] = true
    }
}
