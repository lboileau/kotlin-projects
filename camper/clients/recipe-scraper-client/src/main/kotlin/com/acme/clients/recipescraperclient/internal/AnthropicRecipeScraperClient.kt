package com.acme.clients.recipescraperclient.internal

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.InternalError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.recipescraperclient.api.ExistingIngredient
import com.acme.clients.recipescraperclient.api.RecipeImage
import com.acme.clients.recipescraperclient.api.RecipeScraperClient
import com.acme.clients.recipescraperclient.api.ScrapeRecipeImagesParam
import com.acme.clients.recipescraperclient.api.ScrapeRecipeParam
import com.acme.clients.recipescraperclient.model.ScrapedRecipe
import com.anthropic.client.AnthropicClient
import com.anthropic.core.JsonValue
import com.anthropic.models.messages.Base64ImageSource
import com.anthropic.models.messages.ContentBlockParam
import com.anthropic.models.messages.ImageBlockParam
import com.anthropic.models.messages.JsonOutputFormat
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.Model
import com.anthropic.models.messages.OutputConfig
import com.anthropic.models.messages.StopReason
import com.anthropic.models.messages.TextBlockParam
import com.anthropic.models.messages.ThinkingConfigDisabled
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory

internal class AnthropicRecipeScraperClient(
    private val client: AnthropicClient,
    private val model: String = DEFAULT_MODEL
) : RecipeScraperClient {
    private val logger = LoggerFactory.getLogger(AnthropicRecipeScraperClient::class.java)
    private val mapper = jacksonObjectMapper()

    override fun scrape(param: ScrapeRecipeParam): Result<ScrapedRecipe, AppError> {
        logger.info("Scraping recipe from url={}", param.sourceUrl)
        return guarded(source = "url=${param.sourceUrl}", sourceNoun = "page") {
            val page = ScrapePromptBuilder.buildForPage(param)
            logger.info(
                "Extracted {} ({} chars) from url={}",
                page.content::class.simpleName, page.content.text.length, param.sourceUrl
            )
            if (page.content.isEmpty) {
                logger.warn("No recipe content found at url={}", param.sourceUrl)
                return@guarded failure(InternalError("Could not find a recipe on this page — the site may require a browser or the link may redirect"))
            }
            complete(
                prompt = page.prompt,
                userContent = listOf(textBlock(page.prompt.userMessage)),
                catalogue = param.existingIngredients,
                source = "url=${param.sourceUrl}"
            )
        }
    }

    override fun scrapeImages(param: ScrapeRecipeImagesParam): Result<ScrapedRecipe, AppError> {
        val source = "images=${param.images.size}"
        logger.info("Scraping recipe from {}", source)
        return guarded(source = source, sourceNoun = "photos") {
            val prompt = ScrapePromptBuilder.buildForImages(param)
            // Images first, then the instructions: the documented placement for image prompts. Each
            // image is preceded by a short label (also per the docs) naming what it shows, which is
            // what the user message refers to.
            val userContent = buildList {
                param.images.forEachIndexed { i, image ->
                    ScrapePromptBuilder.imageLabel(image, i, param.images.size)?.let { add(textBlock(it)) }
                    add(imageBlock(image))
                }
                add(textBlock(prompt.userMessage))
            }
            val result = complete(prompt, userContent, param.existingIngredients, source)
            // The schema has no "nothing here" shape, so the prompt asks for an empty recipe when
            // the photos can't be read; that is a failure, not a draft with nothing in it. Only the
            // ingredient list decides: a blank name alone is a title out of frame, not an unreadable photo.
            if (result is Result.Success && result.value.ingredients.isEmpty()) {
                logger.warn("No readable recipe in {}", source)
                return@guarded failure(InternalError("Couldn't read a recipe from the photo — try a clearer shot that shows the ingredient list"))
            }
            result
        }
    }

    /** One model call: structured output, no thinking, stop-reason checks, then parse against the catalogue. */
    private fun complete(
        prompt: ScrapePrompt,
        userContent: List<ContentBlockParam>,
        catalogue: List<ExistingIngredient>,
        source: String
    ): Result<ScrapedRecipe, AppError> {
        val outputFormat = JsonOutputFormat.builder()
            .schema(
                JsonOutputFormat.Schema.builder()
                    .putAllAdditionalProperties(prompt.schema.mapValues { JsonValue.from(it.value) })
                    .build()
            )
            .build()

        val params = MessageCreateParams.builder()
            .model(Model.of(model))
            .maxTokens(8192L)
            .system(prompt.system)
            .outputConfig(OutputConfig.builder().format(outputFormat).build())
            .addUserMessageOfBlockParams(userContent)
            .apply {
                // Sonnet/Opus think by default; this is a lookup-and-copy task that doesn't need it,
                // and the thinking tokens roughly double the output bill. Haiku 4.5 has no thinking by default.
                if (!model.contains("haiku")) thinking(ThinkingConfigDisabled.builder().build())
            }
            .build()

        val message = client.messages().create(params)
        logger.info(
            "Scrape of {} used model={} inputTokens={} outputTokens={}",
            source, model, message.usage().inputTokens(), message.usage().outputTokens()
        )
        val stopReason = message.stopReason().orElse(null)
        if (stopReason == StopReason.MAX_TOKENS) {
            logger.error("AI response truncated for {}", source)
            return failure(InternalError("The recipe is too long to import in one go"))
        }
        if (stopReason == StopReason.REFUSAL) {
            logger.error("AI declined to process {}", source)
            return failure(InternalError("The AI declined to process this recipe"))
        }

        val json = message.content().mapNotNull { it.text().orElse(null)?.text() }.joinToString("")
        return success(parseScrapedRecipe(json, catalogue))
    }

    /** Maps the SDK's failures to user-facing messages; [sourceNoun] is what the user gave us ("page" / "photos"). */
    private inline fun guarded(
        source: String,
        sourceNoun: String,
        block: () -> Result<ScrapedRecipe, AppError>
    ): Result<ScrapedRecipe, AppError> = try {
        block()
    } catch (e: com.anthropic.errors.RateLimitException) {
        logger.warn("Rate limited while scraping {}", source)
        failure(InternalError("Rate limited by AI service — please wait a moment and try again"))
    } catch (e: com.anthropic.errors.UnauthorizedException) {
        logger.error("Authentication failed for AI service")
        failure(InternalError("AI service authentication failed — check your API key"))
    } catch (e: com.anthropic.errors.PermissionDeniedException) {
        logger.error("Permission denied by AI service")
        failure(InternalError("AI service permission denied — check your API key permissions"))
    } catch (e: com.anthropic.errors.BadRequestException) {
        logger.error("Bad request to AI service: {}", e.message)
        failure(InternalError("AI service rejected the request — the $sourceNoun may be too large"))
    } catch (e: com.fasterxml.jackson.core.JsonProcessingException) {
        logger.error("Failed to parse AI response for {}: {}", source, e.message)
        failure(InternalError("Could not read the recipe from this $sourceNoun — the AI returned an unexpected format"))
    } catch (e: Exception) {
        logger.error("Failed to scrape recipe from {}: {}", source, e.message)
        failure(InternalError("Something went wrong importing this recipe — please try again"))
    }

    private fun textBlock(text: String): ContentBlockParam =
        ContentBlockParam.ofText(TextBlockParam.builder().text(text).build())

    private fun imageBlock(image: RecipeImage): ContentBlockParam =
        ContentBlockParam.ofImage(
            ImageBlockParam.builder()
                .source(
                    Base64ImageSource.builder()
                        .mediaType(Base64ImageSource.MediaType.of(image.mediaType))
                        .data(image.base64Data)
                        .build()
                )
                .build()
        )

    /** Parses the model's JSON and resolves it against the catalogue. Shared with the offline harness. */
    private fun parseScrapedRecipe(json: String, catalogue: List<ExistingIngredient>): ScrapedRecipe =
        mapper.readValue<ScrapedRecipeJson>(json).toDomain(catalogue)

    companion object {
        const val DEFAULT_MODEL = "claude-sonnet-5"
    }
}
