package com.acme.clients.recipescraperclient.internal

import com.acme.clients.recipescraperclient.api.ExistingIngredient
import com.acme.clients.recipescraperclient.api.RecipeImage
import com.acme.clients.recipescraperclient.api.ScrapeRecipeImagesParam
import com.acme.clients.recipescraperclient.api.ScrapeRecipeParam
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class ScrapePromptBuilderTest {
    private val catalogue = listOf(
        ExistingIngredient(UUID.randomUUID(), "chicken thighs", "meat", "lb"),
        ExistingIngredient(UUID.randomUUID(), "rice", "pantry", "cup")
    )
    private val image = RecipeImage("image/jpeg", "AAAA")
    private val ingredientsPhoto = RecipeImage("image/jpeg", "AAAA", RecipeImage.ROLE_INGREDIENTS)
    private val instructionsPhoto = RecipeImage("image/jpeg", "BBBB", RecipeImage.ROLE_INSTRUCTIONS)

    @Test
    fun `page prompt carries source url, catalogue refs and the extracted content`() {
        val html = """<html><body><p>${"x".repeat(300)}</p></body></html>"""
        val page = ScrapePromptBuilder.buildForPage(ScrapeRecipeParam(html, "https://example.com/r", catalogue))

        assertTrue(page.content is RecipePageContent.VisibleText)
        assertEquals(ScrapePromptBuilder.SYSTEM_PROMPT, page.prompt.system)
        val user = page.prompt.userMessage
        assertTrue(user.startsWith("Source URL: https://example.com/r\n"), user)
        assertTrue("Ingredient catalogue (ref: name):\n0: chicken thighs\n1: rice\n" in user, user)
        assertTrue("Recipe page text (visible text only; scripts and styles removed):\n" in user, user)
        // Nothing left indented: the template must not rely on trimIndent over multi-line interpolations.
        assertFalse(user.lines().any { it.startsWith(" ") }, user)
    }

    @Test
    fun `image prompt keeps the catalogue block and drops the url and content label`() {
        val prompt = ScrapePromptBuilder.buildForImages(ScrapeRecipeImagesParam(listOf(image), catalogue))

        assertEquals(ScrapePromptBuilder.SYSTEM_PROMPT, prompt.system)
        assertEquals(scrapedRecipeSchema(), prompt.schema)
        val user = prompt.userMessage
        assertTrue(user.startsWith("Ingredient catalogue (ref: name):\n0: chicken thighs\n1: rice\n"), user)
        assertFalse("Source URL" in user, user)
        assertFalse("Recipe page" in user, user)
        assertTrue("The photo above is of one recipe." in user, user)
        assertTrue(ScrapePromptBuilder.UNREADABLE_INSTRUCTION in user, user)
    }

    @Test
    fun `image prompt words several photos as pages of the same recipe in order`() {
        val prompt = ScrapePromptBuilder.buildForImages(ScrapeRecipeImagesParam(listOf(image, image, image), catalogue))

        assertTrue("The 3 photos above (Photo 1 to Photo 3) are of one recipe, in reading order" in prompt.userMessage, prompt.userMessage)
        assertTrue("extract only the first" in prompt.userMessage, prompt.userMessage)
    }

    @Test
    fun `an ingredients photo alone asks for the lines and says steps may be empty`() {
        val prompt = ScrapePromptBuilder.buildForImages(ScrapeRecipeImagesParam(listOf(ingredientsPhoto), catalogue))

        assertTrue("The ingredients photo above shows one recipe's ingredient list" in prompt.userMessage, prompt.userMessage)
        assertTrue("There is no photo of the method, so steps is an empty list" in prompt.userMessage, prompt.userMessage)
        assertEquals("Ingredients photo:", ScrapePromptBuilder.imageLabel(ingredientsPhoto, 0, 1))
    }

    @Test
    fun `ingredients plus instructions photos are each told what to be read for`() {
        val prompt = ScrapePromptBuilder.buildForImages(ScrapeRecipeImagesParam(listOf(ingredientsPhoto, instructionsPhoto), catalogue))

        assertTrue("The ingredients photo shows its ingredient list: read every ingredient line from it." in prompt.userMessage, prompt.userMessage)
        assertTrue("The instructions photo shows its method: read the steps from it." in prompt.userMessage, prompt.userMessage)
        assertEquals("Ingredients photo:", ScrapePromptBuilder.imageLabel(ingredientsPhoto, 0, 2))
        assertEquals("Instructions photo:", ScrapePromptBuilder.imageLabel(instructionsPhoto, 1, 2))
    }

    @Test
    fun `unlabelled photos get a number only when there are several`() {
        assertEquals(null, ScrapePromptBuilder.imageLabel(image, 0, 1))
        assertEquals("Photo 2:", ScrapePromptBuilder.imageLabel(image, 1, 3))
    }

    @Test
    fun `system prompt covers both sources`() {
        assertTrue("recipe web page's content or one or more photos" in ScrapePromptBuilder.SYSTEM_PROMPT)
        assertTrue("do not guess at lines that are cut off or illegible" in ScrapePromptBuilder.SYSTEM_PROMPT)
        // Learned from the first photo run (a meal-kit card): no title in frame, and two quantity columns.
        assertTrue("If no title is visible" in ScrapePromptBuilder.SYSTEM_PROMPT)
        assertTrue("use the largest column for every quantity and set baseServings to that count" in ScrapePromptBuilder.SYSTEM_PROMPT)
        assertTrue("Steps — the method, as an ordered list with one entry per step" in ScrapePromptBuilder.SYSTEM_PROMPT)
        @Suppress("UNCHECKED_CAST")
        val properties = ScrapePromptBuilder.buildForImages(ScrapeRecipeImagesParam(listOf(image), catalogue)).schema["properties"] as Map<String, Any>
        assertTrue("steps" in properties)
    }
}
