package com.acme.clients.mealplanclient.model

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class ShoppingListManualItem(
    val id: UUID,
    val mealPlanId: UUID,
    val ingredientId: UUID?,
    val description: String?,
    val quantity: BigDecimal,
    val unit: String?,
    val quantityPurchased: BigDecimal,
    val createdAt: Instant,
    val updatedAt: Instant,
)
