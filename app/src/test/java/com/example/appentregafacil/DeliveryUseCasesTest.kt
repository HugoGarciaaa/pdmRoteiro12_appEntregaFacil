package com.example.appentregafacil

import com.example.appentregafacil.domain.model.*
import com.example.appentregafacil.domain.repository.AddressRepository
import com.example.appentregafacil.domain.usecase.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class DeliveryUseCasesTest {
    private val time = Instant.parse("2026-11-12T15:00:00Z")
    private val clock = Clock.fixed(time, ZoneOffset.UTC)

    @Test fun acceptsPostalCodeWithOrWithoutHyphen() {
        assertTrue(PostalCode.isValid("01001-000"))
        assertTrue(PostalCode.isValid("01001000"))
        assertEquals("01001000", PostalCode.normalize("01001-000"))
        listOf("123", "010010000", "01a01000", "01001--000", "abcdefgh").forEach { assertFalse(PostalCode.isValid(it)) }
    }

    @Test fun rejectsMissingFieldsAndInvalidState() {
        val errors = DeliveryValidation.errors(DeliveryDraft(state = "XX"), null)
        assertTrue(errors.keys.containsAll(setOf(DeliveryField.RECIPIENT, DeliveryField.DESCRIPTION,
            DeliveryField.POSTAL_CODE, DeliveryField.STREET, DeliveryField.NUMBER, DeliveryField.NEIGHBORHOOD, DeliveryField.CITY, DeliveryField.STATE)))
    }

    @Test fun allowsOptionalComplementAndNumberWithoutDigits() {
        assertTrue(DeliveryValidation.errors(validDraft().copy(number = "S/N", complement = ""), "01001000").isEmpty())
    }

    @Test fun rejectsSaveBeforeSuccessfulLookup() = runTest {
        val repository = FakeDeliveryRepository()
        try {
            SaveDelivery(repository, clock)(validDraft(), null)
            fail("Deveria exigir consulta de CEP")
        } catch (error: FormValidationException) { assertTrue(error.errors.containsKey(DeliveryField.POSTAL_CODE)) }
        assertTrue(repository.rows.value.isEmpty())
    }

    @Test fun rejectsProofForAnotherPostalCode() {
        assertTrue(DeliveryValidation.errors(validDraft().copy(postalCode = "01310100"), "01001000").containsKey(DeliveryField.POSTAL_CODE))
    }

    @Test fun createsAndEditsSameIdPreservingCreationDate() = runTest {
        val repository = FakeDeliveryRepository()
        val save = SaveDelivery(repository, clock)
        val id = save(validDraft(), "01001000")
        val later = Clock.fixed(time.plusSeconds(60), ZoneOffset.UTC)
        val updatedId = SaveDelivery(repository, later)(validDraft().copy(number = "200"), "01001000", id)
        assertEquals(id, updatedId)
        assertEquals(1, repository.rows.value.size)
        val updated = repository.getById(id)!!
        assertEquals("200", updated.number)
        assertEquals("01001000", updated.postalCode)
        assertEquals(time, updated.createdAt)
        assertEquals(later.instant(), updated.updatedAt)
    }

    @Test fun statusAndSoftDeletePreserveRowButHideNormalQueries() = runTest {
        val repository = FakeDeliveryRepository()
        val id = SaveDelivery(repository, clock)(validDraft(), "01001000")
        ChangeDeliveryStatus(repository, clock)(id, DeliveryStatus.IN_TRANSIT)
        assertEquals(DeliveryStatus.IN_TRANSIT, repository.getById(id)!!.status)
        DeleteDelivery(repository, clock)(id)
        assertNull(repository.getById(id))
        assertNull(GetDelivery(repository)(id).first())
        assertTrue(ObserveDeliveries(repository)("", null).first().isEmpty())
        assertEquals(1, repository.rows.value.size)
        assertEquals(time, repository.rows.value.single().deletedAt)
    }

    @Test fun combinesRecipientSearchWithStatusFilter() = runTest {
        val repository = FakeDeliveryRepository()
        val save = SaveDelivery(repository, clock)
        save(validDraft(), "01001000")
        save(validDraft().copy(recipientName = "Bruno", status = DeliveryStatus.DELIVERED), "01001000")
        val observe = ObserveDeliveries(repository)
        assertEquals("Ana Souza", observe("ANA", DeliveryStatus.PENDING).first().single().recipientName)
        assertTrue(observe("ana", DeliveryStatus.DELIVERED).first().isEmpty())
        assertEquals(1, observe("", DeliveryStatus.DELIVERED).first().size)
    }

    @Test fun invalidPostalCodeDoesNotCallRepository() = runTest {
        var calls = 0
        val lookup = LookupPostalCode(object : AddressRepository {
            override suspend fun findByPostalCode(postalCode: String): Address { calls++; error("Não deveria chamar") }
        })
        try { lookup("123"); fail() } catch (error: AddressLookupException) {
            assertEquals(AddressFailure.INVALID_POSTAL_CODE, error.reason)
        }
        assertEquals(0, calls)
    }
}
