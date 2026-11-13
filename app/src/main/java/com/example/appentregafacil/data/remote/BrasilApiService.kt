package com.example.appentregafacil.data.remote

import retrofit2.http.GET
import retrofit2.http.Path

// Campos opcionais contemplam CEPs gerais sem logradouro/bairro.
data class AddressDto(
    val cep: String? = null,
    val state: String? = null,
    val city: String? = null,
    val neighborhood: String? = null,
    val street: String? = null,
    val service: String? = null,
)

interface BrasilApiService {
    @GET("api/cep/v1/{cep}")
    suspend fun findAddress(@Path("cep") postalCode: String): AddressDto
}
