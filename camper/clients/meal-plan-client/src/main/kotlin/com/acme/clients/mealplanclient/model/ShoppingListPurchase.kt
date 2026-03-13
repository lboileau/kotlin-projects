package com.acme.clients.mealplanclient.model

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class ShoppingListPurchase(
    val id: UUID,
    val mealPlanId: UUID,
    val ingredientId: UUID,
    val unit: String,
    val quantityPurchased: BigDecimal,
    val createdAt: Instant,
    val updatedAt: Instant,
)
