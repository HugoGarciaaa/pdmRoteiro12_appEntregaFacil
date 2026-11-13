package com.example.appentregafacil.data.repository

import com.example.appentregafacil.data.mapper.toDomain
import com.example.appentregafacil.data.remote.BrasilApiService
import com.example.appentregafacil.domain.model.*
import com.example.appentregafacil.domain.repository.AddressRepository
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException
import java.io.InterruptedIOException
import javax.inject.Inject

class BrasilApiAddressRepository @Inject constructor(private val service: BrasilApiService) : AddressRepository {
    override suspend fun findByPostalCode(postalCode: String): Address = try {
        service.findAddress(postalCode).toDomain(postalCode)
    } catch (error: CancellationException) {
        throw error
    } catch (error: AddressLookupException) {
        throw error
    } catch (error: HttpException) {
        val reason = when (error.code()) {
            400, 422 -> AddressFailure.INVALID_POSTAL_CODE
            404 -> AddressFailure.NOT_FOUND
            408, 504 -> AddressFailure.TIMEOUT
            else -> AddressFailure.SERVER
        }
        throw AddressLookupException(reason, error)
    } catch (error: InterruptedIOException) {
        throw AddressLookupException(AddressFailure.TIMEOUT, error)
    } catch (error: IOException) {
        throw AddressLookupException(AddressFailure.NO_CONNECTION, error)
    } catch (error: Exception) {
        throw AddressLookupException(AddressFailure.INVALID_RESPONSE, error)
    }
}
