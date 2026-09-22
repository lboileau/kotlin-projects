package com.acme.clients.recipescraperclient.internal

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.InternalError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.recipescraperclient.api.ExistingIngredient
import com.acme.clients.recipescraperclient.api.RecipeScraperClient
import com.acme.clients.recipescraperclient.api.ScrapeRecipeImagesParam
import com.acme.clients.recipescraperclient.api.ScrapeRecipeParam
import com.acme.clients.recipescraperclient.model.ScrapedRecipe
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import java.io.File
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Base64
import java.util.concurrent.atomic.AtomicInteger

/**
 * Local development only: the model call is done by whoever is watching [dir] — a person, or a
 * coding agent that can read images — so the real app flow (picker → upload → draft → review)
 * runs end to end without an API key.
 *
 * Each scrape writes `<dir>/<timestamp>-<n>/` with `system.txt`, `user.txt`, `schema.json`,
 * `meta.json`, and for photos `image-1.jpg` … (for a page, `page.html`). It then waits, up to
 * [timeout], for `response.json` to appear in that folder — the model's answer, matching
 * `schema.json` — and runs it through exactly the post-processing the Anthropic client uses.
 * An empty or missing answer fails the import the same way an unreadable photo would.
 */
internal class RelayRecipeScraperClient(
    private val dir: File,
    private val timeout: Duration = Duration.ofMinutes(4)
) : RecipeScraperClient {
    private val logger = LoggerFactory.getLogger(RelayRecipeScraperClient::class.java)
    private val mapper = jacksonObjectMapper()
    private val counter = AtomicInteger()

    override fun scrape(param: ScrapeRecipeParam): Result<ScrapedRecipe, AppError> {
        val page = ScrapePromptBuilder.buildForPage(param)
        if (page.content.isEmpty) {
            return failure(InternalError("Could not find a recipe on this page — the site may require a browser or the link may redirect"))
        }
        val caseDir = newCaseDir("page")
        File(caseDir, "page.html").writeText(param.html)
        writePrompt(caseDir, page.prompt, mapOf("source" to "url", "url" to param.sourceUrl, "extracted" to page.content::class.simpleName))
        return await(caseDir, param.existingIngredients, allowEmpty = true)
    }

    override fun scrapeImages(param: ScrapeRecipeImagesParam): Result<ScrapedRecipe, AppError> {
        val prompt = ScrapePromptBuilder.buildForImages(param)
        val caseDir = newCaseDir("photos")
        val files = param.images.mapIndexed { i, image ->
            val ext = image.mediaType.substringAfter('/').replace("jpeg", "jpg")
            val name = "image-${i + 1}${image.role?.let { "-$it" } ?: ""}.$ext"
            File(caseDir, name).also { it.writeBytes(Base64.getDecoder().decode(image.base64Data)) }
            mapOf("file" to name, "role" to image.role, "label" to ScrapePromptBuilder.imageLabel(image, i, param.images.size))
        }
        writePrompt(caseDir, prompt, mapOf("source" to "photos", "images" to files))
        return await(caseDir, param.existingIngredients, allowEmpty = false)
    }

    private fun newCaseDir(kind: String): File {
        val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
        return File(dir, "$stamp-${counter.incrementAndGet()}-$kind").apply { mkdirs() }
    }

    private fun writePrompt(caseDir: File, prompt: ScrapePrompt, meta: Map<String, Any?>) {
        File(caseDir, "system.txt").writeText(prompt.system)
        File(caseDir, "user.txt").writeText(prompt.userMessage)
        File(caseDir, "schema.json").writeText(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(prompt.schema))
        File(caseDir, "meta.json").writeText(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(meta + ("answerFile" to "response.json")))
        logger.warn("RELAY: waiting up to {}s for {}/response.json", timeout.seconds, caseDir.absolutePath)
    }

    private fun await(caseDir: File, catalogue: List<ExistingIngredient>, allowEmpty: Boolean): Result<ScrapedRecipe, AppError> {
        val response = File(caseDir, "response.json")
        val deadline = System.currentTimeMillis() + timeout.toMillis()
        while (!response.exists() || response.length() == 0L) {
            if (System.currentTimeMillis() > deadline) {
                logger.error("RELAY: no response.json in {} after {}s", caseDir.name, timeout.seconds)
                return failure(InternalError("No answer arrived in the relay folder (${caseDir.name}) — is anyone watching it?"))
            }
            Thread.sleep(500)
        }
        // A writer may not be done the instant the file appears; settle before reading.
        Thread.sleep(300)
        return try {
            val recipe = mapper.readValue<ScrapedRecipeJson>(response.readText()).toDomain(catalogue)
            if (!allowEmpty && recipe.ingredients.isEmpty()) {
                failure(InternalError("Couldn't read a recipe from the photo — try a clearer shot that shows the ingredient list"))
            } else {
                logger.info("RELAY: {} answered with {} ingredient line(s)", caseDir.name, recipe.ingredients.size)
                success(recipe)
            }
        } catch (e: Exception) {
            logger.error("RELAY: could not parse {}/response.json: {}", caseDir.name, e.message)
            failure(InternalError("The relay answer could not be parsed: ${e.message}"))
        }
    }
}
