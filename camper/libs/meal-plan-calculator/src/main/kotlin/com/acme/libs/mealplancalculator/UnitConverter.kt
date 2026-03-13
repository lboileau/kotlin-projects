package com.acme.libs.mealplancalculator

import com.acme.libs.mealplancalculator.model.UnitCategory
import java.math.BigDecimal

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

    /**
     * Convert a quantity from one unit to another.
     * Returns null if the units are incompatible (different categories or count units).
     */
    fun convert(quantity: BigDecimal, sourceUnit: String, targetUnit: String): BigDecimal? = TODO()

    /**
     * Check if two units can be converted between each other.
     * True for volume-to-volume and weight-to-weight. False for everything else.
     */
    fun areCompatible(unitA: String, unitB: String): Boolean = TODO()

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
    fun bestFit(quantity: BigDecimal, unit: String): Pair<BigDecimal, String> = TODO()

    /**
     * Determine the unit category for a given unit string.
     * Returns null if the unit is not recognized.
     */
    fun categoryOf(unit: String): UnitCategory? = TODO()
}
