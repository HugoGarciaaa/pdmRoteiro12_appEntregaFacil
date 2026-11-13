package com.example.appentregafacil.presentation.common

import com.example.appentregafacil.domain.model.*
import kotlinx.coroutines.CancellationException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

val DeliveryStatus.label: String
    get() = when (this) {
        DeliveryStatus.PENDING -> "Pendente"
        DeliveryStatus.IN_TRANSIT -> "Em transporte"
        DeliveryStatus.DELIVERED -> "Entregue"
    }

fun Instant.displayDate(): String = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm", Locale.forLanguageTag("pt-BR"))
    .withZone(ZoneId.systemDefault()).format(this)

fun String.displayPostalCode(): String = if (length == 8) "${take(5)}-${takeLast(3)}" else this

fun Throwable.userMessage(): String {
    if (this is CancellationException) throw this
    return when (this) {
        is AddressLookupException -> when (reason) {
            AddressFailure.INVALID_POSTAL_CODE -> "CEP inválido. Informe 8 dígitos e tente novamente."
            AddressFailure.NOT_FOUND -> "CEP não encontrado. Confira o CEP e tente consultar novamente."
            AddressFailure.NO_CONNECTION -> "Não foi possível conectar. Verifique sua internet e tente novamente."
            AddressFailure.TIMEOUT -> "A consulta demorou demais. Tente novamente."
            AddressFailure.SERVER -> "O serviço de CEP está indisponível. Tente novamente em instantes."
            AddressFailure.INVALID_RESPONSE -> "O serviço retornou um endereço inválido. Tente novamente."
        }
        is DeliveryNotFoundException -> "Entrega não encontrada ou excluída. Volte à listagem."
        else -> "Não foi possível acessar os dados. Tente novamente. Seus campos foram mantidos."
    }
}
