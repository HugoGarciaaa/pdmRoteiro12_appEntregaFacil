package com.example.appentregafacil.data.mapper

import com.example.appentregafacil.data.local.DeliveryEntity
import com.example.appentregafacil.data.remote.AddressDto
import com.example.appentregafacil.domain.model.*

fun DeliveryEntity.toDomain() = Delivery(id, recipientName, packageDescription, postalCode, street,
    number, complement, neighborhood, city, state, status, createdAt, updatedAt, deletedAt)

fun Delivery.toEntity() = DeliveryEntity(id, recipientName, packageDescription, postalCode, street,
    number, complement, neighborhood, city, state, status, createdAt, updatedAt, deletedAt)

fun AddressDto.toDomain(expectedPostalCode: String): Address {
    if (cep == null || !PostalCode.isValid(cep) || PostalCode.normalize(cep) != expectedPostalCode ||
        city.isNullOrBlank() || state.isNullOrBlank()) {
        throw AddressLookupException(AddressFailure.INVALID_RESPONSE)
    }
    return Address(PostalCode.normalize(cep), street.orEmpty(), neighborhood.orEmpty(), city, state)
}
