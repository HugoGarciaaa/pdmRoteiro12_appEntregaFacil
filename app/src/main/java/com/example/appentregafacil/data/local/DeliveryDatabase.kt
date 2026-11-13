package com.example.appentregafacil.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [DeliveryEntity::class], version = 1, exportSchema = true)
@TypeConverters(DeliveryConverters::class)
abstract class DeliveryDatabase : RoomDatabase() {
    abstract fun deliveryDao(): DeliveryDao
}
