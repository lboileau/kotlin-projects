package com.acme.clients.recipescraperclient.internal

import com.acme.clients.common.Result
import com.acme.clients.recipescraperclient.api.ExistingIngredient
import com.acme.clients.recipescraperclient.api.ScrapeRecipeParam
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.util.UUID

/**
 * Manual harness: runs the real Anthropic client against a saved page.
 * Skipped unless -Pscrape.dir and -Pscrape.apiKey are passed (see build.gradle.kts). Reads:
 *   <dir>/page.html          — raw HTML (scrape.input=raw, default)
 *   <dir>/recipe-jsonld.json — pre-extracted JSON-LD (scrape.input=jsonld)
 *   <dir>/ingredients.json   — [{id,name,category,defaultUnit}]
 * Writes <dir>/result-<input>.txt
 */
class ScrapeHarnessTest {
    private data class IngredientRow(val id: UUID, val name: String, val category: String, val defaultUnit: String)

    @Test
    fun scrape() {
        val dir = System.getProperty("scrape.dir")
        val apiKey = System.getProperty("scrape.apiKey")
        assumeTrue(dir != null && apiKey != null, "harness properties not set")
        val input = System.getProperty("scrape.input") ?: "raw"
        val mapper = jacksonObjectMapper()

        val existing = mapper.readValue<List<IngredientRow>>(File(dir, "ingredients.json"))
            .map { ExistingIngredient(it.id, it.name, it.category, it.defaultUnit) }
        val html = when (input) {
            "jsonld" -> File(dir, "recipe-jsonld.json").readText()
            else -> File(dir, "page.html").readText()
        }

        val model = System.getProperty("scrape.model") ?: AnthropicRecipeScraperClient.DEFAULT_MODEL
        val client = AnthropicRecipeScraperClient(AnthropicOkHttpClient.builder().apiKey(apiKey).build(), model)
        val result = client.scrape(ScrapeRecipeParam(html, "https://juliasalbum.com/chicken-rice-feta-tomatoes/", existing))

        val byId = existing.associateBy { it.id }
        val out = buildString {
            when (result) {
                is Result.Failure -> appendLine("FAILURE: ${result.error.message}")
                is Result.Success -> {
                    val r = result.value
                    appendLine("name: ${r.name}")
                    appendLine("servings: ${r.baseServings}  meal: ${r.meal}  theme: ${r.theme}")
                    appendLine("description: ${r.description}")
                    appendLine("ingredients (${r.ingredients.size}):")
                    r.ingredients.forEach { i ->
                        val match = i.matchedIngredientId?.let { byId[it]?.name ?: "!!UNKNOWN-ID $it" }
                        appendLine("  ${i.quantity} ${i.unit.padEnd(6)} | ${i.originalText.padEnd(45)} | match=${match ?: "-"} suggest=${i.suggestedIngredientName ?: "-"} ${i.confidence} ${i.reviewFlags}")
                    }
                }
            }
        }
        File(dir, "result-$input-${model.replace('.', '-')}.txt").writeText(out)
        println(out)
    }
}
