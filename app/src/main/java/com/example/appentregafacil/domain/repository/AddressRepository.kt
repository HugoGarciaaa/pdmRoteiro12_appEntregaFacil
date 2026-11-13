package com.example.appentregafacil.domain.repository

import com.example.appentregafacil.domain.model.Address

interface AddressRepository {
    suspend fun findByPostalCode(postalCode: String): Address
}
