package com.acme.libs.mealplancalculator.model

import java.math.BigDecimal

enum class PurchaseStatus {
    DONE,
    MORE_NEEDED,
    NOT_PURCHASED,
    NO_LONGER_NEEDED;

    companion object {
        fun derive(quantityRequired: BigDecimal, quantityPurchased: BigDecimal): PurchaseStatus = when {
            quantityRequired.compareTo(BigDecimal.ZERO) == 0 && quantityPurchased.compareTo(BigDecimal.ZERO) > 0 ->
                NO_LONGER_NEEDED
            quantityPurchased.compareTo(BigDecimal.ZERO) == 0 && quantityRequired.compareTo(BigDecimal.ZERO) > 0 ->
                NOT_PURCHASED
            quantityPurchased >= quantityRequired && quantityRequired.compareTo(BigDecimal.ZERO) > 0 ->
                DONE
            quantityPurchased.compareTo(BigDecimal.ZERO) > 0 && quantityPurchased < quantityRequired ->
                MORE_NEEDED
            else -> NOT_PURCHASED
        }
    }
}
