package com.example.appentregafacil.domain.model

object PostalCode {
    fun normalize(value: String): String = value.filter { it in '0'..'9' }
    fun isValid(value: String): Boolean = Regex("[0-9]{5}-?[0-9]{3}").matches(value.trim())
}

enum class DeliveryField { RECIPIENT, DESCRIPTION, POSTAL_CODE, STREET, NUMBER, COMPLEMENT, NEIGHBORHOOD, CITY, STATE }

class FormValidationException(val errors: Map<DeliveryField, String>) : IllegalArgumentException()
class DeliveryNotFoundException : IllegalStateException("Entrega não encontrada ou excluída.")

enum class AddressFailure { INVALID_POSTAL_CODE, NOT_FOUND, NO_CONNECTION, TIMEOUT, SERVER, INVALID_RESPONSE }
class AddressLookupException(val reason: AddressFailure, cause: Throwable? = null) : Exception(cause)

object DeliveryValidation {
    private val states = setOf("AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS", "MG", "PA", "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO")

    fun errors(draft: DeliveryDraft, verifiedPostalCode: String?): Map<DeliveryField, String> = buildMap {
        if (draft.recipientName.trim().length < 2) put(DeliveryField.RECIPIENT, "Informe o destinatário (mínimo de 2 caracteres).")
        if (draft.packageDescription.trim().length < 3) put(DeliveryField.DESCRIPTION, "Descreva o pacote (mínimo de 3 caracteres).")
        if (!PostalCode.isValid(draft.postalCode)) {
            put(DeliveryField.POSTAL_CODE, "Informe um CEP com 8 dígitos.")
        } else if (verifiedPostalCode != PostalCode.normalize(draft.postalCode)) {
            put(DeliveryField.POSTAL_CODE, "Consulte este CEP com sucesso antes de salvar.")
        }
        if (draft.street.isBlank()) put(DeliveryField.STREET, "Informe a rua.")
        if (draft.number.isBlank()) put(DeliveryField.NUMBER, "Informe o número ou S/N.")
        if (draft.neighborhood.isBlank()) put(DeliveryField.NEIGHBORHOOD, "Informe o bairro.")
        if (draft.city.isBlank()) put(DeliveryField.CITY, "Informe a cidade.")
        if (draft.state.trim().uppercase() !in states) put(DeliveryField.STATE, "Informe uma UF brasileira válida.")
    }
}
