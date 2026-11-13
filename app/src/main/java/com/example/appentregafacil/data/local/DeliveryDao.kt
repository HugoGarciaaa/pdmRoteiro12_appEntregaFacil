package com.example.appentregafacil.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.appentregafacil.domain.model.DeliveryStatus
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface DeliveryDao {
    @Query("SELECT * FROM deliveries WHERE deletedAt IS NULL ORDER BY createdAt DESC, id DESC")
    fun observeActive(): Flow<List<DeliveryEntity>>

    @Query("SELECT * FROM deliveries WHERE id = :id AND deletedAt IS NULL")
    fun observeById(id: Long): Flow<DeliveryEntity?>

    @Query("SELECT * FROM deliveries WHERE id = :id AND deletedAt IS NULL")
    suspend fun getById(id: Long): DeliveryEntity?

    @Insert suspend fun insert(delivery: DeliveryEntity): Long

    // O WHERE impede que uma edição concorrente restaure uma entrega excluída.
    @Query("""UPDATE deliveries SET recipientName = :recipientName, packageDescription = :packageDescription,
        postalCode = :postalCode, street = :street, number = :number, complement = :complement,
        neighborhood = :neighborhood, city = :city, state = :state, status = :status, updatedAt = :updatedAt
        WHERE id = :id AND deletedAt IS NULL""")
    suspend fun update(id: Long, recipientName: String, packageDescription: String, postalCode: String,
        street: String, number: String, complement: String, neighborhood: String, city: String,
        state: String, status: DeliveryStatus, updatedAt: Instant): Int

    @Query("UPDATE deliveries SET status = :status, updatedAt = :at WHERE id = :id AND deletedAt IS NULL")
    suspend fun changeStatus(id: Long, status: DeliveryStatus, at: Instant): Int

    @Query("UPDATE deliveries SET deletedAt = :at, updatedAt = :at WHERE id = :id AND deletedAt IS NULL")
    suspend fun softDelete(id: Long, at: Instant): Int
}
