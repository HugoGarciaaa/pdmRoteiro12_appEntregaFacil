package com.example.appentregafacil.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.example.appentregafacil.domain.model.DeliveryStatus
import java.time.Instant

@Entity(tableName = "deliveries", indices = [Index("deletedAt")])
data class DeliveryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipientName: String,
    val packageDescription: String,
    val postalCode: String,
    val street: String,
    val number: String,
    val complement: String,
    val neighborhood: String,
    val city: String,
    val state: String,
    val status: DeliveryStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant? = null,
)

class DeliveryConverters {
    @TypeConverter fun toInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)
    @TypeConverter fun fromInstant(value: Instant?): Long? = value?.toEpochMilli()
    @TypeConverter fun toStatus(value: String): DeliveryStatus = DeliveryStatus.valueOf(value)
    @TypeConverter fun fromStatus(value: DeliveryStatus): String = value.name
}
