package com.acme.clients.recipescraperclient.internal

import com.acme.clients.recipescraperclient.api.ExistingIngredient
import com.acme.clients.recipescraperclient.api.RecipeImage
import com.acme.clients.recipescraperclient.api.ScrapeRecipeImagesParam
import com.acme.clients.recipescraperclient.api.ScrapeRecipeParam
import com.acme.clients.recipescraperclient.model.ScrapedRecipe
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.Base64
import java.util.UUID

/**
 * Offline harness: exercises the whole scrape pipeline *except* the model call, so the prompt can be
 * answered by something other than the paid API (a local agent, a person) and the answer fed back in.
 *
 *   -Pscrape.dir=<dir> -Pscrape.offline=prompt
 *     For each URL in <dir>/urls.txt: fetch the page as the service does, run the extractor, and write
 *     <dir>/offline/<n>/{system.txt,user.txt,schema.json,meta.json,page.html}. No API call.
 *   -Pscrape.dir=<dir> -Pscrape.offline=prompt-images
 *     Treat the image files in <dir>/images/ (sorted by name, at most RecipeImage.MAX_IMAGES) as one recipe's
 *     photos and write <dir>/offline/images/{system.txt,user.txt,schema.json,meta.json}. The photos are not
 *     copied; open them yourself alongside user.txt. No API call.
 *   -Pscrape.dir=<dir> -Pscrape.offline=parse
 *     For each <dir>/offline/<n>/response.json present: parse it through the real mapping against
 *     <dir>/ingredients.json and write <dir>/offline/<n>/result.txt.
 */
class ScrapeOfflineHarnessTest {
    private data class IngredientRow(val id: UUID, val name: String, val category: String, val defaultUnit: String)

    private val mapper = jacksonObjectMapper()

    @Test
    fun offline() {
        val dir = System.getProperty("scrape.dir")?.let(::File)
        val mode = System.getProperty("scrape.offline")
        assumeTrue(dir != null && mode != null, "harness properties not set")

        val catalogue = mapper.readValue<List<IngredientRow>>(File(dir, "ingredients.json"))
            .map { ExistingIngredient(it.id, it.name, it.category, it.defaultUnit) }
        val out = File(dir, "offline").apply { mkdirs() }

        when (mode) {
            "prompt" -> writePrompts(File(dir, "urls.txt").readLines().map { it.trim() }.filter { it.isNotEmpty() }, catalogue, out)
            "prompt-images" -> writeImagesPrompt(File(dir, "images"), catalogue, out)
            "parse" -> parseResponses(catalogue, out)
            else -> error("unknown scrape.offline mode: $mode")
        }
    }

    private fun writePrompts(urls: List<String>, catalogue: List<ExistingIngredient>, out: File) {
        val http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()
        urls.forEachIndexed { n, url ->
            val caseDir = File(out, "%02d".format(n)).apply { mkdirs() }
            val meta = linkedMapOf<String, Any?>("url" to url)
            try {
                // Same request the service makes (ImportRecipeAction.defaultHtmlFetcher)
                val request = HttpRequest.newBuilder().uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0 (compatible; CamperBot/1.0)").GET().build()
                val response = http.send(request, HttpResponse.BodyHandlers.ofString())
                meta["status"] = response.statusCode()
                if (response.statusCode() !in 200..299) error("HTTP ${response.statusCode()}")
                val html = response.body()
                File(caseDir, "page.html").writeText(html)
                meta["htmlChars"] = html.length

                val page = ScrapePromptBuilder.buildForPage(ScrapeRecipeParam(html, url, catalogue))
                meta["extracted"] = page.content::class.simpleName
                meta["extractedChars"] = page.content.text.length
                File(caseDir, "system.txt").writeText(page.prompt.system)
                File(caseDir, "user.txt").writeText(page.prompt.userMessage)
                File(caseDir, "schema.json").writeText(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(page.prompt.schema))
            } catch (e: Exception) {
                meta["error"] = e.message ?: e::class.simpleName
            }
            File(caseDir, "meta.json").writeText(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(meta))
            println("${caseDir.name} ${meta["extracted"] ?: "ERROR"} ${meta["extractedChars"] ?: meta["error"]} $url")
        }
    }

    private fun writeImagesPrompt(imagesDir: File, catalogue: List<ExistingIngredient>, out: File) {
        val mediaTypes = mapOf("jpg" to "image/jpeg", "jpeg" to "image/jpeg", "png" to "image/png", "gif" to "image/gif", "webp" to "image/webp")
        val files = imagesDir.listFiles { f -> f.isFile && f.extension.lowercase() in mediaTypes }
            ?.sortedBy { it.name }?.take(RecipeImage.MAX_IMAGES).orEmpty()
        check(files.isNotEmpty()) { "no image files in $imagesDir" }

        val images = files.map { RecipeImage(mediaTypes.getValue(it.extension.lowercase()), Base64.getEncoder().encodeToString(it.readBytes())) }
        val prompt = ScrapePromptBuilder.buildForImages(ScrapeRecipeImagesParam(images, catalogue))

        val caseDir = File(out, "images").apply { mkdirs() }
        File(caseDir, "system.txt").writeText(prompt.system)
        File(caseDir, "user.txt").writeText(prompt.userMessage)
        File(caseDir, "schema.json").writeText(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(prompt.schema))
        val meta = linkedMapOf<String, Any?>(
            "images" to files.map { mapOf("file" to it.name, "mediaType" to mediaTypes.getValue(it.extension.lowercase()), "bytes" to it.length()) }
        )
        File(caseDir, "meta.json").writeText(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(meta))
        println("${caseDir.name} ${files.size} photo(s): ${files.joinToString { it.name }}")
    }

    private fun parseResponses(catalogue: List<ExistingIngredient>, out: File) {
        val byId = catalogue.associateBy { it.id }
        out.listFiles { f -> f.isDirectory }!!.sortedBy { it.name }.forEach { caseDir ->
            val response = File(caseDir, "response.json").takeIf { it.exists() } ?: return@forEach
            val text = try {
                val recipe: ScrapedRecipe = mapper.readValue<ScrapedRecipeJson>(response.readText()).toDomain(catalogue)
                File(caseDir, "result.json").writeText(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(recipe))
                render(recipe, byId)
            } catch (e: Exception) {
                "PARSE FAILURE: ${e.message}"
            }
            File(caseDir, "result.txt").writeText(text)
            println("${caseDir.name}: ${text.lineSequence().first()}")
        }
    }

    private fun render(r: ScrapedRecipe, byId: Map<UUID, ExistingIngredient>) = buildString {
        appendLine("name: ${r.name}")
        appendLine("servings: ${r.baseServings}  meal: ${r.meal}  theme: ${r.theme}")
        appendLine("ingredients (${r.ingredients.size}):")
        r.ingredients.forEach { i ->
            val match = i.matchedIngredientId?.let { byId[it]?.name }
            appendLine("  ${i.quantity.toPlainString().padStart(6)} ${i.unit.padEnd(6)} | ${i.originalText.padEnd(50)} | match=${match ?: "-"} suggest=${i.suggestedIngredientName ?: "-"} ${i.confidence} ${i.reviewFlags}")
        }
    }
}
