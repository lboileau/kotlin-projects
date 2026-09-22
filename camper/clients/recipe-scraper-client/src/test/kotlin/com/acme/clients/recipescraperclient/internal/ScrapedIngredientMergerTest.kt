package com.acme.clients.recipescraperclient.internal

import com.acme.clients.recipescraperclient.model.ScrapedIngredient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID

class ScrapedIngredientMergerTest {
    private val oil = UUID.randomUUID()
    private val oregano = UUID.randomUUID()

    private fun line(text: String, qty: String, unit: String, id: UUID? = null, suggested: String? = null,
                     confidence: String = "HIGH", flags: List<String> = emptyList()) =
        ScrapedIngredient(text, BigDecimal(qty), unit, id, suggested, null, null, confidence, flags)

    private fun q(l: ScrapedIngredient) = "${l.quantity.toPlainString()} ${l.unit}"

    @Test
    fun `same ingredient in several sections becomes one line with summed quantity and joined text`() {
        val merged = ScrapedIngredientMerger.merge(listOf(
            line("2 tablespoons olive oil", "2", "tbsp", oil),
            line("8 oz grape tomatoes", "8", "oz", UUID.randomUUID()),
            line("1 tablespoon olive oil", "1", "tbsp", oil),
            line("1 tablespoon extra virgin olive oil", "1", "tbsp", oil)
        ))

        assertEquals(2, merged.size)
        assertEquals("4 tbsp", q(merged[0]))
        assertEquals("2 tablespoons olive oil; 1 tablespoon olive oil; 1 tablespoon extra virgin olive oil", merged[0].originalText)
        assertEquals("8 oz", q(merged[1]))
    }

    @Test
    fun `compatible volume units are summed exactly and shown in the larger unit when clean`() {
        val tsp = ScrapedIngredientMerger.merge(listOf(
            line("1 teaspoon dried oregano", "1", "tsp", oregano),
            line("1 teaspoon dried oregano", "1", "tsp", oregano),
            line("1/4 teaspoon dried oregano", "0.25", "tsp", oregano)
        ))
        assertEquals("2.25 tsp", q(tsp.single()))

        val mixed = ScrapedIngredientMerger.merge(listOf(
            line("2 tablespoons olive oil", "2", "tbsp", oil),
            line("1/2 cup olive oil", "0.5", "cup", oil)
        ))
        assertEquals("10 tbsp", q(mixed.single()), "8 + 2 tbsp; 0.625 cup is not clean so stays in tbsp")

        val clean = ScrapedIngredientMerger.merge(listOf(
            line("4 tablespoons butter", "4", "tbsp", oil),
            line("1/2 cup butter", "0.5", "cup", oil)
        ))
        assertEquals("0.75 cup", q(clean.single()), "12 tbsp reads cleanly as ¾ cup")
    }

    @Test
    fun `count units merge only when identical`() {
        val merged = ScrapedIngredientMerger.merge(listOf(
            line("5 cloves garlic", "5", "clove", oil),
            line("3 cloves garlic", "3", "clove", oil),
            line("2 tbsp chopped fresh oregano", "2", "tbsp", oregano),
            line("fresh oregano (for garnish)", "1", "sprig", oregano, flags = listOf("QUANTITY_ASSUMED"))
        ))

        assertEquals(listOf("8 clove", "2 tbsp", "1 sprig"), merged.map(::q))
    }

    @Test
    fun `new ingredients merge by suggested name and flags and confidence are combined`() {
        val merged = ScrapedIngredientMerger.merge(listOf(
            line("5 oz fresh spinach", "5", "oz", suggested = "spinach", confidence = "LOW", flags = listOf("NEW_INGREDIENT")),
            line("2 oz spinach for garnish", "2", "oz", suggested = "Spinach", confidence = "LOW", flags = listOf("NEW_INGREDIENT", "QUANTITY_ASSUMED")),
            line("1 tsp salt", "1", "tsp", oil, confidence = "HIGH"),
            line("salt to taste", "1", "pinch", oil, confidence = "HIGH", flags = listOf("QUANTITY_ASSUMED"))
        ))

        assertEquals("7 oz", q(merged[0]))
        assertEquals(listOf("NEW_INGREDIENT", "QUANTITY_ASSUMED"), merged[0].reviewFlags)
        assertEquals("LOW", merged[0].confidence)
        assertEquals(listOf("1 tsp", "1 pinch"), merged.drop(1).map(::q), "tsp and pinch are not compatible")
    }

    @Test
    fun `unresolved lines without a suggestion are left alone`() {
        val merged = ScrapedIngredientMerger.merge(listOf(
            line("something", "1", "pieces"), line("something", "1", "pieces")
        ))
        assertEquals(2, merged.size)
    }
}
