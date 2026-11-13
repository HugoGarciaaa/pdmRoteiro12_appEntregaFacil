package com.example.appentregafacil

import com.example.appentregafacil.data.remote.BrasilApiService
import com.example.appentregafacil.data.repository.BrasilApiAddressRepository
import com.example.appentregafacil.domain.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class BrasilApiAddressRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: BrasilApiAddressRepository

    @Before fun setup() {
        server = MockWebServer()
        server.start()
        repository = BrasilApiAddressRepository(Retrofit.Builder().baseUrl(server.url("/"))
            .client(OkHttpClient()).addConverterFactory(GsonConverterFactory.create()).build().create(BrasilApiService::class.java))
    }
    @After fun teardown() { server.shutdown() }

    @Test fun callsCorrectEndpointAndMapsRealContract() = runTest {
        server.enqueue(MockResponse().setBody("""{"cep":"01001000","state":"SP","city":"São Paulo","neighborhood":"Sé","street":"Praça da Sé","service":"open-cep","ibge":{"city":"3550308"}}"""))
        val address = repository.findByPostalCode("01001000")
        assertEquals("Praça da Sé", address.street)
        assertEquals("São Paulo", address.city)
        assertEquals("/api/cep/v1/01001000", server.takeRequest().path)
    }

    @Test fun acceptsGeneralPostalCodeWithoutStreet() = runTest {
        server.enqueue(MockResponse().setBody("""{"cep":"01001000","state":"SP","city":"São Paulo"}"""))
        assertEquals("", repository.findByPostalCode("01001000").street)
    }

    @Test fun mapsNotFound() = runTest { assertHttpFailure(404, AddressFailure.NOT_FOUND) }
    @Test fun mapsInvalidPostalCode() = runTest { assertHttpFailure(400, AddressFailure.INVALID_POSTAL_CODE) }
    @Test fun mapsServerError() = runTest { assertHttpFailure(500, AddressFailure.SERVER) }
    @Test fun mapsGatewayTimeout() = runTest { assertHttpFailure(504, AddressFailure.TIMEOUT) }
    @Test fun mapsRateLimit() = runTest { assertHttpFailure(429, AddressFailure.SERVER) }

    @Test fun rejectsMissingAddressFields() = runTest {
        server.enqueue(MockResponse().setBody("{}"))
        assertFailure(AddressFailure.INVALID_RESPONSE) { repository.findByPostalCode("01001000") }
    }

    @Test fun rejectsAddressForDifferentPostalCode() = runTest {
        server.enqueue(MockResponse().setBody("""{"cep":"01310100","state":"SP","city":"São Paulo"}"""))
        assertFailure(AddressFailure.INVALID_RESPONSE) { repository.findByPostalCode("01001000") }
    }

    @Test fun mapsNoConnectionAndTimeout() = runTest {
        listOf(UnknownHostException() to AddressFailure.NO_CONNECTION, SocketTimeoutException() to AddressFailure.TIMEOUT).forEach { (error, reason) ->
            val failing = BrasilApiAddressRepository(object : BrasilApiService {
                override suspend fun findAddress(postalCode: String): com.example.appentregafacil.data.remote.AddressDto = throw error
            })
            assertFailure(reason) { failing.findByPostalCode("01001000") }
        }
    }

    @Test fun propagatesCancellation() = runTest {
        val cancelled = BrasilApiAddressRepository(object : BrasilApiService {
            override suspend fun findAddress(postalCode: String): com.example.appentregafacil.data.remote.AddressDto = throw CancellationException()
        })
        try { cancelled.findByPostalCode("01001000"); fail() } catch (_: CancellationException) { }
    }

    private suspend fun assertHttpFailure(code: Int, reason: AddressFailure) {
        server.enqueue(MockResponse().setResponseCode(code))
        assertFailure(reason) { repository.findByPostalCode("01001000") }
    }
    private suspend fun assertFailure(reason: AddressFailure, action: suspend () -> Unit) {
        try { action(); fail("Deveria falhar: $reason") } catch (error: AddressLookupException) { assertEquals(reason, error.reason) }
    }
}
