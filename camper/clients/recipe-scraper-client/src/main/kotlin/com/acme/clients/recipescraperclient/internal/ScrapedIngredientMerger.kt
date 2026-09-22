package com.acme.clients.recipescraperclient.internal

import com.acme.clients.recipescraperclient.model.ScrapedIngredient
import com.acme.libs.mealplancalculator.UnitConverter
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Recipe pages list ingredients per section ("For the chicken… For the rice… For the dressing…"), so
 * the same ingredient often appears several times. The app wants one line per ingredient, so lines that
 * resolve to the same ingredient are merged and their quantities summed:
 *
 * - same unit → summed (`2 tbsp + 1 tbsp + 1 tbsp` → `4 tbsp`; `5 clove + 3 clove` → `8 clove`)
 * - compatible units (volume↔volume, weight↔weight) → summed in the smallest unit present, then shown in
 *   the largest unit present if that makes a clean value (`1 tsp + 1 tsp + ¼ tsp` → `2.25 tsp`;
 *   `2 tbsp + ½ cup` → `10 tbsp`)
 * - incompatible units (`2 tbsp` fresh oregano + `1 sprig` fresh oregano) → kept as separate lines
 *
 * Every source line's text is preserved, joined with "; ", so the review screen still shows where each
 * amount came from. Flags are unioned and confidence is HIGH only if every merged line was HIGH.
 * Lines with no resolved ingredient and no suggestion are never merged.
 */
internal object ScrapedIngredientMerger {

    fun merge(lines: List<ScrapedIngredient>): List<ScrapedIngredient> {
        val groups = LinkedHashMap<Any, MutableList<ScrapedIngredient>>()
        lines.forEachIndexed { i, line ->
            val key = line.matchedIngredientId ?: line.suggestedIngredientName?.trim()?.lowercase() ?: "line-$i"
            groups.getOrPut(key) { mutableListOf() }.add(line)
        }
        return groups.values.flatMap { group -> mergeCompatible(group) }
    }

    /** Within one ingredient, merge lines whose units can be added; leave the rest as they are. */
    private fun mergeCompatible(group: List<ScrapedIngredient>): List<ScrapedIngredient> {
        val buckets = mutableListOf<MutableList<ScrapedIngredient>>()
        group.forEach { line ->
            val bucket = buckets.firstOrNull { UnitConverter.areCompatible(it.first().unit, line.unit) }
            if (bucket != null) bucket.add(line) else buckets.add(mutableListOf(line))
        }
        return buckets.map { bucket -> if (bucket.size == 1) bucket.single() else combine(bucket) }
    }

    private fun combine(lines: List<ScrapedIngredient>): ScrapedIngredient {
        val (quantity, unit) = sum(lines)
        val first = lines.first()
        return first.copy(
            originalText = lines.joinToString("; ") { it.originalText },
            quantity = quantity,
            unit = unit,
            confidence = if (lines.all { it.confidence == "HIGH" }) "HIGH" else "LOW",
            reviewFlags = lines.flatMap { it.reviewFlags }.distinct()
        )
    }

    private fun sum(lines: List<ScrapedIngredient>): Pair<BigDecimal, String> {
        val units = lines.map { it.unit }.distinct()
        if (units.size == 1) return lines.sumOf { it.quantity }.stripTrailingZeros() to units.single()

        // Sum in the smallest unit (no rounding), then prefer the largest unit present if it reads cleanly.
        val smallest = units.minBy { UnitConverter.convert(BigDecimal.ONE, it, units.first())!! }
        val total = lines.sumOf { UnitConverter.convert(it.quantity, it.unit, smallest)!! }
        val largest = units.maxBy { UnitConverter.convert(BigDecimal.ONE, it, units.first())!! }
        val inLargest = UnitConverter.convert(total, smallest, largest)!!.setScale(2, RoundingMode.HALF_UP)
        return if (isClean(inLargest)) inLargest.stripTrailingZeros() to largest
        else total.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros() to smallest
    }

    private fun isClean(value: BigDecimal): Boolean {
        val frac = value.remainder(BigDecimal.ONE).abs()
        return listOf("0", "0.25", "0.5", "0.75").any { frac.compareTo(BigDecimal(it)) == 0 }
    }
}
