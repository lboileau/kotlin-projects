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
    fun `system prompt covers both sources`() {
        assertTrue("recipe web page's content or one or more photos" in ScrapePromptBuilder.SYSTEM_PROMPT)
        assertTrue("do not guess at lines that are cut off or illegible" in ScrapePromptBuilder.SYSTEM_PROMPT)
        // Learned from the first photo run (a meal-kit card): no title in frame, and two quantity columns.
        assertTrue("If no title is visible" in ScrapePromptBuilder.SYSTEM_PROMPT)
        assertTrue("use the largest column for every quantity and set baseServings to that count" in ScrapePromptBuilder.SYSTEM_PROMPT)
    }
}
