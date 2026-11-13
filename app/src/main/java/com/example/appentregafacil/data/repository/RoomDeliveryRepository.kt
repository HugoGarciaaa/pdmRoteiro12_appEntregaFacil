package com.example.appentregafacil.data.repository

import com.example.appentregafacil.data.local.DeliveryDao
import com.example.appentregafacil.data.mapper.toDomain
import com.example.appentregafacil.data.mapper.toEntity
import com.example.appentregafacil.domain.model.*
import com.example.appentregafacil.domain.repository.DeliveryRepository
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

class RoomDeliveryRepository @Inject constructor(private val dao: DeliveryDao) : DeliveryRepository {
    override fun observeActive() = dao.observeActive().map { rows -> rows.map { it.toDomain() } }
    override fun observeById(id: Long) = dao.observeById(id).map { it?.toDomain() }
    override suspend fun getById(id: Long) = dao.getById(id)?.toDomain()
    override suspend fun insert(delivery: Delivery) = dao.insert(delivery.toEntity())
    override suspend fun update(delivery: Delivery) {
        with(delivery) {
            requireUpdated(dao.update(id, recipientName, packageDescription, postalCode, street, number,
                complement, neighborhood, city, state, status, updatedAt))
        }
    }
    override suspend fun changeStatus(id: Long, status: DeliveryStatus, at: Instant) {
        requireUpdated(dao.changeStatus(id, status, at))
    }
    override suspend fun softDelete(id: Long, at: Instant) { requireUpdated(dao.softDelete(id, at)) }

    private fun requireUpdated(rows: Int) { if (rows != 1) throw DeliveryNotFoundException() }
}
