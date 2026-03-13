package com.acme.libs.mealplancalculator

import com.acme.libs.mealplancalculator.model.UnitCategory
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * Converts between compatible measurement units and finds best-fit display units.
 *
 * Units fall into three categories:
 * - **Volume**: tsp, tbsp, cup, ml, l — all convertible to each other
 * - **Weight**: g, kg, oz, lb — all convertible to each other
 * - **Count**: pieces, whole, bunch, can, clove, pinch, slice, sprig — each is its own type
 *
 * Compatible units (same category, excluding count) can be converted between each other.
 * Count units are never compatible with each other or with other categories.
 */
object UnitConverter {

    private val MATH_CONTEXT = MathContext(20)
    private const val INTERMEDIATE_SCALE = 10

    /** Volume units with their conversion factor to the base unit (tsp). */
    private val volumeFactors: Map<String, BigDecimal> = mapOf(
        "tsp" to BigDecimal.ONE,
        "tbsp" to BigDecimal("3"),
        "cup" to BigDecimal("48"),
        "ml" to BigDecimal("0.202884"),
        "l" to BigDecimal("202.884"),
    )

    /** Volume units ordered from smallest to largest for bestFit iteration. */
    private val volumeUnitsOrdered: List<String> = listOf("tsp", "tbsp", "cup", "ml", "l")

    /** Weight units with their conversion factor to the base unit (g). */
    private val weightFactors: Map<String, BigDecimal> = mapOf(
        "g" to BigDecimal.ONE,
        "kg" to BigDecimal("1000"),
        "oz" to BigDecimal("28.3495"),
        "lb" to BigDecimal("453.592"),
    )

    /** Weight units ordered from smallest to largest for bestFit iteration. */
    private val weightUnitsOrdered: List<String> = listOf("g", "oz", "lb", "kg")

    private val countUnits: Set<String> = setOf(
        "pieces", "whole", "bunch", "can", "clove", "pinch", "slice", "sprig",
    )

    /**
     * Convert a quantity from one unit to another.
     * Returns null if the units are incompatible (different categories or count units).
     */
    fun convert(quantity: BigDecimal, sourceUnit: String, targetUnit: String): BigDecimal? {
        if (sourceUnit == targetUnit) return quantity
        if (!areCompatible(sourceUnit, targetUnit)) return null

        val sourceToBase = factorOf(sourceUnit) ?: return null
        val targetToBase = factorOf(targetUnit) ?: return null

        return quantity.multiply(sourceToBase, MATH_CONTEXT)
            .divide(targetToBase, INTERMEDIATE_SCALE, RoundingMode.HALF_UP)
    }

    /**
     * Check if two units can be converted between each other.
     * True for volume-to-volume and weight-to-weight. False for everything else.
     */
    fun areCompatible(unitA: String, unitB: String): Boolean {
        if (unitA == unitB) return true
        val catA = categoryOf(unitA) ?: return false
        val catB = categoryOf(unitB) ?: return false
        if (catA == UnitCategory.COUNT || catB == UnitCategory.COUNT) return false
        return catA == catB
    }

    /**
     * Find the best-fit display unit for a quantity.
     * Scales up through compatible units until reaching the smallest whole number
     * or clean fraction (1/4, 1/2, 3/4).
     *
     * Examples:
     * - 48 tsp -> 1 cup
     * - 6 tbsp -> 6 tbsp (0.375 cups is not a clean fraction)
     * - 1500 ml -> 1.5 l
     */
    fun bestFit(quantity: BigDecimal, unit: String): Pair<BigDecimal, String> {
        val category = categoryOf(unit)
        if (category == null || category == UnitCategory.COUNT) {
            return Pair(quantity.stripTrailingZeros(), unit)
        }

        val orderedUnits = when (category) {
            UnitCategory.VOLUME -> volumeUnitsOrdered
            UnitCategory.WEIGHT -> weightUnitsOrdered
            else -> return Pair(quantity.stripTrailingZeros(), unit)
        }

        // Try each unit that is larger than the current unit
        val currentFactor = factorOf(unit) ?: return Pair(quantity.stripTrailingZeros(), unit)
        val currentBaseQuantity = quantity.multiply(currentFactor, MATH_CONTEXT)

        var bestQuantity = quantity
        var bestUnit = unit

        for (candidateUnit in orderedUnits) {
            val candidateFactor = factorOf(candidateUnit) ?: continue
            // Only consider units that are larger (higher factor) than the current unit
            if (candidateFactor.compareTo(currentFactor) <= 0) continue

            val converted = currentBaseQuantity.divide(candidateFactor, INTERMEDIATE_SCALE, RoundingMode.HALF_UP)
            if (converted.compareTo(BigDecimal.ZERO) <= 0) continue

            if (isCleanValue(converted)) {
                bestQuantity = converted
                bestUnit = candidateUnit
                // Keep looking for even larger clean-fit units
            }
        }

        return Pair(bestQuantity.stripTrailingZeros(), bestUnit)
    }

    /**
     * Determine the unit category for a given unit string.
     * Returns null if the unit is not recognized.
     */
    fun categoryOf(unit: String): UnitCategory? = when {
        volumeFactors.containsKey(unit) -> UnitCategory.VOLUME
        weightFactors.containsKey(unit) -> UnitCategory.WEIGHT
        countUnits.contains(unit) -> UnitCategory.COUNT
        else -> null
    }

    /** Get the base-unit factor for a given unit. */
    private fun factorOf(unit: String): BigDecimal? =
        volumeFactors[unit] ?: weightFactors[unit]

    /**
     * A value is "clean" if it is a whole number or its fractional part is
     * exactly 0.25, 0.5, or 0.75.
     */
    private fun isCleanValue(value: BigDecimal): Boolean {
        val fractionalPart = value.remainder(BigDecimal.ONE).abs()
        if (fractionalPart.compareTo(BigDecimal.ZERO) == 0) return true
        val quarter = BigDecimal("0.25")
        val half = BigDecimal("0.5")
        val threeQuarters = BigDecimal("0.75")
        return fractionalPart.compareTo(quarter) == 0 ||
            fractionalPart.compareTo(half) == 0 ||
            fractionalPart.compareTo(threeQuarters) == 0
    }
}
