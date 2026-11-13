package com.example.appentregafacil.domain.repository

import com.example.appentregafacil.domain.model.Delivery
import com.example.appentregafacil.domain.model.DeliveryStatus
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface DeliveryRepository {
    fun observeActive(): Flow<List<Delivery>>
    fun observeById(id: Long): Flow<Delivery?>
    suspend fun getById(id: Long): Delivery?
    suspend fun insert(delivery: Delivery): Long
    suspend fun update(delivery: Delivery)
    suspend fun changeStatus(id: Long, status: DeliveryStatus, at: Instant)
    suspend fun softDelete(id: Long, at: Instant)
}
