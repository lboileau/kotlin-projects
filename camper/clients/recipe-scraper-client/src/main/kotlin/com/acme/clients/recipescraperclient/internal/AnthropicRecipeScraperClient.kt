package com.acme.clients.recipescraperclient.internal

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.InternalError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.recipescraperclient.api.RecipeScraperClient
import com.acme.clients.recipescraperclient.api.ScrapeRecipeParam
import com.acme.clients.recipescraperclient.model.ScrapedRecipe
import com.anthropic.client.AnthropicClient
import com.anthropic.core.JsonValue
import com.anthropic.models.messages.JsonOutputFormat
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.Model
import com.anthropic.models.messages.OutputConfig
import com.anthropic.models.messages.StopReason
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
        return try {
            val prompt = ScrapePromptBuilder.build(param)
            logger.info(
                "Extracted {} ({} chars) from url={}",
                prompt.content::class.simpleName, prompt.content.text.length, param.sourceUrl
            )
            if (prompt.content.isEmpty) {
                logger.warn("No recipe content found at url={}", param.sourceUrl)
                return failure(InternalError("Could not find a recipe on this page — the site may require a browser or the link may redirect"))
            }

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
                .addUserMessage(prompt.userMessage)
                .apply {
                    // Sonnet/Opus think by default; this is a lookup-and-copy task that doesn't need it,
                    // and the thinking tokens roughly double the output bill. Haiku 4.5 has no thinking by default.
                    if (!model.contains("haiku")) thinking(ThinkingConfigDisabled.builder().build())
                }
                .build()

            val message = client.messages().create(params)
            logger.info(
                "Scrape of url={} used model={} inputTokens={} outputTokens={}",
                param.sourceUrl, model, message.usage().inputTokens(), message.usage().outputTokens()
            )
            val stopReason = message.stopReason().orElse(null)
            if (stopReason == StopReason.MAX_TOKENS) {
                logger.error("AI response truncated for url={}", param.sourceUrl)
                return failure(InternalError("The recipe is too long to import in one go"))
            }
            if (stopReason == StopReason.REFUSAL) {
                logger.error("AI declined to process url={}", param.sourceUrl)
                return failure(InternalError("The AI declined to process this page"))
            }

            val json = message.content().mapNotNull { it.text().orElse(null)?.text() }.joinToString("")
            success(parseScrapedRecipe(json, param))
        } catch (e: com.anthropic.errors.RateLimitException) {
            logger.warn("Rate limited while scraping url={}", param.sourceUrl)
            failure(InternalError("Rate limited by AI service — please wait a moment and try again"))
        } catch (e: com.anthropic.errors.UnauthorizedException) {
            logger.error("Authentication failed for AI service")
            failure(InternalError("AI service authentication failed — check your API key"))
        } catch (e: com.anthropic.errors.PermissionDeniedException) {
            logger.error("Permission denied by AI service")
            failure(InternalError("AI service permission denied — check your API key permissions"))
        } catch (e: com.anthropic.errors.BadRequestException) {
            logger.error("Bad request to AI service: {}", e.message)
            failure(InternalError("AI service rejected the request — the recipe page may be too large"))
        } catch (e: com.fasterxml.jackson.core.JsonProcessingException) {
            logger.error("Failed to parse AI response for url={}: {}", param.sourceUrl, e.message)
            failure(InternalError("Could not read the recipe from this page — the AI returned an unexpected format"))
        } catch (e: Exception) {
            logger.error("Failed to scrape recipe from url={}: {}", param.sourceUrl, e.message)
            failure(InternalError("Something went wrong importing this recipe — please try again"))
        }
    }

    /** Parses the model's JSON and resolves it against the catalogue. Shared with the offline harness. */
    private fun parseScrapedRecipe(json: String, param: ScrapeRecipeParam): ScrapedRecipe =
        mapper.readValue<ScrapedRecipeJson>(json).toDomain(param.existingIngredients)

    companion object {
        const val DEFAULT_MODEL = "claude-sonnet-5"
    }
}
