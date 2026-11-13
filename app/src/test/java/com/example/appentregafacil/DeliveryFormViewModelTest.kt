package com.example.appentregafacil

import androidx.lifecycle.SavedStateHandle
import com.example.appentregafacil.domain.model.*
import com.example.appentregafacil.domain.repository.AddressRepository
import com.example.appentregafacil.domain.usecase.*
import com.example.appentregafacil.presentation.form.DeliveryFormViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.Clock

@OptIn(ExperimentalCoroutinesApi::class)
class DeliveryFormViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeDeliveryRepository()
    private var calls = 0
    private var shouldFail = false
    private val addressRepository = object : AddressRepository {
        override suspend fun findByPostalCode(postalCode: String): Address {
            calls++
            delay(100)
            if (shouldFail) throw AddressLookupException(AddressFailure.NO_CONNECTION)
            return Address(postalCode, "Praça da Sé", "Sé", "São Paulo", "SP")
        }
    }
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { Dispatchers.resetMain() }
    private fun viewModel(handle: SavedStateHandle = SavedStateHandle()) = DeliveryFormViewModel(
        GetDelivery(repository), LookupPostalCode(addressRepository), SaveDelivery(repository, Clock.systemUTC()), handle)

    @Test fun preventsDuplicateQueriesAndInvalidatesProofAfterPostalCodeChange() = runTest(dispatcher) {
        val vm = viewModel()
        vm.change(DeliveryField.POSTAL_CODE, "01001-000")
        vm.lookup()
        vm.lookup()
        assertTrue(vm.state.value.isLookingUp)
        advanceUntilIdle()
        assertEquals(1, calls)
        assertEquals("01001000", vm.state.value.verifiedPostalCode)
        assertEquals("Praça da Sé", vm.state.value.draft.street)
        vm.change(DeliveryField.POSTAL_CODE, "01310100")
        assertNull(vm.state.value.verifiedPostalCode)
        vm.save()
        advanceUntilIdle()
        assertTrue(vm.state.value.errors.containsKey(DeliveryField.POSTAL_CODE))
        assertTrue(repository.rows.value.isEmpty())
    }

    @Test fun keepsFieldsOnFailureAndAllowsRetry() = runTest(dispatcher) {
        val vm = viewModel()
        vm.change(DeliveryField.RECIPIENT, "Ana Souza")
        vm.change(DeliveryField.NUMBER, "100")
        vm.change(DeliveryField.COMPLEMENT, "Sala 2")
        vm.change(DeliveryField.POSTAL_CODE, "01001000")
        shouldFail = true
        vm.lookup()
        advanceUntilIdle()
        assertEquals("Ana Souza", vm.state.value.draft.recipientName)
        assertEquals("100", vm.state.value.draft.number)
        assertNotNull(vm.state.value.message)
        assertNull(vm.state.value.verifiedPostalCode)
        shouldFail = false
        vm.lookup()
        advanceUntilIdle()
        assertEquals("01001000", vm.state.value.verifiedPostalCode)
        assertEquals("Sala 2", vm.state.value.draft.complement)
    }

    @Test fun restoresDraftAndSuccessfulLookupFromSavedState() = runTest(dispatcher) {
        val handle = SavedStateHandle()
        val vm = viewModel(handle)
        vm.change(DeliveryField.RECIPIENT, "Ana Souza")
        vm.change(DeliveryField.POSTAL_CODE, "01001000")
        vm.lookup()
        advanceUntilIdle()
        val restored = viewModel(handle)
        assertEquals(vm.state.value.draft, restored.state.value.draft)
        assertEquals("01001000", restored.state.value.verifiedPostalCode)
    }

    @Test fun editsExistingDeliveryOfflineAndAvoidsDuplicateSaves() = runTest(dispatcher) {
        val id = SaveDelivery(repository, Clock.systemUTC())(validDraft(), "01001000")
        val vm = viewModel(SavedStateHandle(mapOf("deliveryId" to id)))
        advanceUntilIdle()
        vm.change(DeliveryField.NUMBER, "200")
        shouldFail = true
        vm.save()
        vm.save()
        advanceUntilIdle()
        assertEquals(id, vm.state.value.savedId)
        assertEquals(1, repository.rows.value.size)
        assertEquals("200", repository.getById(id)!!.number)
        assertEquals(0, calls)
    }
}
