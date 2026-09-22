package com.acme.clients.recipescraperclient.internal

import com.fasterxml.jackson.core.json.JsonReadFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.jsoup.Jsoup
import org.jsoup.parser.Parser

/** What we hand the model instead of raw page HTML. */
internal sealed class RecipePageContent {
    abstract val text: String

    /** A schema.org/Recipe node found in the page's JSON-LD — compact and authoritative. */
    data class JsonLd(override val text: String) : RecipePageContent()

    /** Visible page text with scripts, styles, and chrome stripped. Used only when no JSON-LD recipe exists. */
    data class VisibleText(override val text: String) : RecipePageContent()

    /** Nothing usable — e.g. a JS-redirect stub or consent wall. Sending this to the model would only invite guessing. */
    val isEmpty: Boolean get() = text.length < MIN_CONTENT_CHARS

    companion object {
        const val MIN_CONTENT_CHARS = 200
    }
}

/**
 * Reduces a recipe page to the part that actually describes the recipe.
 *
 * Recipe sites are routinely 300–600KB of ads, styles, and comments with the ingredient list
 * buried past the first 100KB, so sending a raw prefix of the HTML starves the model. Nearly all
 * of them embed a schema.org Recipe in JSON-LD, which carries the name, yield, and a clean
 * `recipeIngredient` list — that is what we prefer.
 */
internal object RecipePageExtractor {
    // WordPress recipe plugins routinely emit raw tabs/newlines inside JSON strings; strict parsing
    // would throw away an otherwise perfect Recipe node.
    private val mapper = jacksonObjectMapper()
        .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature())

    /** Some sites wrap the JSON in `// <![CDATA[ ... // ]]>` comment lines. */
    private val cdataLine = Regex("""^\s*//\s*(<!\[CDATA\[|]]>)\s*$""", RegexOption.MULTILINE)

    /** Fields of the Recipe node worth keeping as-is; everything else (images, ratings, video) is noise. */
    private val recipeFields = listOf(
        "name", "description", "recipeYield", "recipeIngredient",
        "recipeCategory", "recipeCuisine", "keywords"
    )

    /** The instructions field, kept too — but flattened, see [flattenInstructions]. */
    private const val INSTRUCTIONS_FIELD = "recipeInstructions"

    private const val MAX_VISIBLE_TEXT_CHARS = 60_000

    fun extract(html: String): RecipePageContent {
        val doc = Jsoup.parse(html)

        doc.select("script[type=application/ld+json]").forEach { script ->
            val recipe = runCatching { mapper.readTree(script.data().replace(cdataLine, "")) }.getOrNull()
                ?.let { findRecipeNode(it) }
            if (recipe != null && recipe.has("recipeIngredient")) {
                return RecipePageContent.JsonLd(mapper.writeValueAsString(compact(recipe)))
            }
        }

        doc.select("script, style, noscript, svg, iframe, nav, header, footer, form, aside").remove()
        val text = doc.body()?.text().orEmpty().take(MAX_VISIBLE_TEXT_CHARS)
        return RecipePageContent.VisibleText(text)
    }

    /** JSON-LD may be a single node, an array of nodes, or a `@graph` wrapper; @type may be a string or array. */
    private fun findRecipeNode(node: JsonNode): JsonNode? {
        if (node.isArray) return node.firstNotNullOfOrNull { findRecipeNode(it) }
        if (!node.isObject) return null
        if (isRecipeType(node.get("@type"))) return node
        return node.get("@graph")?.let { findRecipeNode(it) }
    }

    private fun isRecipeType(type: JsonNode?): Boolean = when {
        type == null -> false
        type.isTextual -> type.asText() == "Recipe"
        type.isArray -> type.any { it.isTextual && it.asText() == "Recipe" }
        else -> false
    }

    private fun compact(recipe: JsonNode): ObjectNode {
        val out = mapper.createObjectNode()
        recipeFields.forEach { field -> recipe.get(field)?.let { out.set<JsonNode>(field, unescaped(it)) } }
        recipe.get(INSTRUCTIONS_FIELD)?.let { node ->
            val steps = flattenInstructions(node)
            if (steps.isNotEmpty()) {
                out.set<JsonNode>(INSTRUCTIONS_FIELD, mapper.createArrayNode().also { arr -> steps.forEach { arr.add(it) } })
            }
        }
        return out
    }

    /**
     * schema.org allows `recipeInstructions` to be a string, a list of strings, a list of
     * `HowToStep` objects, or `HowToSection`s each holding steps in `itemListElement`. Only the
     * step text is worth the tokens — names, urls and images per step are dropped, section
     * headings too — and sites embed HTML in the text, so it goes through Jsoup.
     */
    fun flattenInstructions(node: JsonNode): List<String> = when {
        node.isTextual -> listOf(cleanStep(node.asText()))
        node.isArray -> node.flatMap { flattenInstructions(it) }
        node.isObject -> {
            val items = node.get("itemListElement")
            if (items != null) flattenInstructions(items)
            else (node.get("text") ?: node.get("name"))?.takeIf { it.isTextual }?.let { listOf(cleanStep(it.asText())) }.orEmpty()
        }
        else -> emptyList()
    }.filter { it.isNotBlank() }

    private fun cleanStep(raw: String): String =
        Jsoup.parse(Parser.unescapeEntities(raw, false)).text().trim()

    /** JSON-LD strings often carry HTML entities (`&amp;`, `&frac12;`); decode them so originalText reads cleanly. */
    private fun unescaped(node: JsonNode): JsonNode = when {
        node.isTextual -> TextNode(Parser.unescapeEntities(node.asText(), false))
        node.isArray -> mapper.createArrayNode().also { arr -> node.forEach { arr.add(unescaped(it)) } }
        node.isObject -> mapper.createObjectNode().also { obj -> node.fields().forEach { (k, v) -> obj.set<JsonNode>(k, unescaped(v)) } }
        else -> node
    }
}
