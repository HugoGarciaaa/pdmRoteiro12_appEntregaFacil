package com.example.appentregafacil

import com.example.appentregafacil.domain.model.*
import com.example.appentregafacil.domain.repository.DeliveryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.Instant

internal class FakeDeliveryRepository : DeliveryRepository {
    val rows = MutableStateFlow<List<Delivery>>(emptyList())
    override fun observeActive() = rows.map { rows -> rows.filter { it.deletedAt == null } }
    override fun observeById(id: Long) = rows.map { rows -> rows.find { it.id == id && it.deletedAt == null } }
    override suspend fun getById(id: Long) = rows.value.find { it.id == id && it.deletedAt == null }
    override suspend fun insert(delivery: Delivery): Long {
        val id = (rows.value.maxOfOrNull { it.id } ?: 0L) + 1
        rows.value += delivery.copy(id = id)
        return id
    }
    override suspend fun update(delivery: Delivery) {
        if (getById(delivery.id) == null) throw DeliveryNotFoundException()
        rows.value = rows.value.map { if (it.id == delivery.id) delivery else it }
    }
    override suspend fun changeStatus(id: Long, status: DeliveryStatus, at: Instant) {
        val current = getById(id) ?: throw DeliveryNotFoundException()
        update(current.copy(status = status, updatedAt = at))
    }
    override suspend fun softDelete(id: Long, at: Instant) {
        if (getById(id) == null) throw DeliveryNotFoundException()
        rows.value = rows.value.map { if (it.id == id) it.copy(deletedAt = at, updatedAt = at) else it }
    }
}

internal fun validDraft() = DeliveryDraft(
    recipientName = "Ana Souza", packageDescription = "Livros de Kotlin", postalCode = "01001-000",
    street = "Praça da Sé", number = "100", complement = "Sala 2", neighborhood = "Sé", city = "São Paulo", state = "SP",
)
