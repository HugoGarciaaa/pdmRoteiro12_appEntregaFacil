package com.example.appentregafacil.domain.model

import java.time.Instant

enum class DeliveryStatus { PENDING, IN_TRANSIT, DELIVERED }

data class Delivery(
    val id: Long,
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

data class Address(
    val postalCode: String,
    val street: String,
    val neighborhood: String,
    val city: String,
    val state: String,
)

data class DeliveryDraft(
    val recipientName: String = "",
    val packageDescription: String = "",
    val postalCode: String = "",
    val street: String = "",
    val number: String = "",
    val complement: String = "",
    val neighborhood: String = "",
    val city: String = "",
    val state: String = "",
    val status: DeliveryStatus = DeliveryStatus.PENDING,
)
