package com.example.appentregafacil.domain.usecase

import com.example.appentregafacil.domain.model.*
import com.example.appentregafacil.domain.notification.DeliveryNotifier
import com.example.appentregafacil.domain.notification.NotificationResult
import com.example.appentregafacil.domain.repository.AddressRepository
import com.example.appentregafacil.domain.repository.DeliveryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock

class ObserveDeliveries(private val repository: DeliveryRepository) {
    operator fun invoke(query: String, status: DeliveryStatus?): Flow<List<Delivery>> =
        repository.observeActive().map { deliveries ->
            deliveries.filter {
                it.deletedAt == null && it.recipientName.contains(query.trim(), ignoreCase = true) &&
                    (status == null || it.status == status)
            }
        }
}

class GetDelivery(private val repository: DeliveryRepository) {
    operator fun invoke(id: Long): Flow<Delivery?> = repository.observeById(id)
    suspend fun once(id: Long): Delivery = repository.getById(id) ?: throw DeliveryNotFoundException()
}

class LookupPostalCode(private val repository: AddressRepository) {
    suspend operator fun invoke(value: String): Address {
        if (!PostalCode.isValid(value)) throw AddressLookupException(AddressFailure.INVALID_POSTAL_CODE)
        return repository.findByPostalCode(PostalCode.normalize(value))
    }
}

class SaveDelivery(private val repository: DeliveryRepository, private val clock: Clock) {
    suspend operator fun invoke(draft: DeliveryDraft, verifiedPostalCode: String?, id: Long? = null): Long {
        val errors = DeliveryValidation.errors(draft, verifiedPostalCode)
        if (errors.isNotEmpty()) throw FormValidationException(errors)
        val previous = id?.let { repository.getById(it) ?: throw DeliveryNotFoundException() }
        val now = clock.instant()
        val delivery = Delivery(
            id = previous?.id ?: 0L,
            recipientName = draft.recipientName.trim(),
            packageDescription = draft.packageDescription.trim(),
            postalCode = PostalCode.normalize(draft.postalCode),
            street = draft.street.trim(), number = draft.number.trim(), complement = draft.complement.trim(),
            neighborhood = draft.neighborhood.trim(), city = draft.city.trim(), state = draft.state.trim().uppercase(),
            status = draft.status, createdAt = previous?.createdAt ?: now, updatedAt = now,
        )
        return if (previous == null) repository.insert(delivery) else {
            repository.update(delivery)
            delivery.id
        }
    }
}

class ChangeDeliveryStatus(
    private val repository: DeliveryRepository,
    private val clock: Clock,
    private val notifier: DeliveryNotifier,
) {
    suspend operator fun invoke(id: Long, status: DeliveryStatus): NotificationResult? {
        val delivery = repository.getById(id) ?: throw DeliveryNotFoundException()
        if (delivery.status == status) return null
        val now = clock.instant()
        repository.changeStatus(id, status, now)
        // O aviso só é publicado depois da persistência bem-sucedida.
        return notifier.notifyStatusChanged(delivery.copy(status = status, updatedAt = now))
    }
}

class DeleteDelivery(private val repository: DeliveryRepository, private val clock: Clock) {
    suspend operator fun invoke(id: Long) = repository.softDelete(id, clock.instant())
}
