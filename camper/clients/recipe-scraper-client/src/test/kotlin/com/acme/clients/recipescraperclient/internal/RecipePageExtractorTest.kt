package com.acme.clients.recipescraperclient.internal

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RecipePageExtractorTest {

    @Test
    fun `prefers the schema-org Recipe node inside a JSON-LD graph`() {
        val html = """
            <html><head>
            <script type="application/ld+json">
            {"@context":"https://schema.org","@graph":[
              {"@type":"WebPage","name":"Page"},
              {"@type":["Recipe"],"name":"Lemon Rice","recipeYield":["4"],
               "recipeIngredient":["2 cups rice","1 lemon"],"image":"big.jpg","aggregateRating":{"ratingValue":5}}
            ]}
            </script>
            </head><body><style>.ad{}</style><p>lots of blog text</p></body></html>
        """.trimIndent()

        val content = RecipePageExtractor.extract(html)

        assertTrue(content is RecipePageContent.JsonLd)
        assertTrue(content.text.contains("\"recipeIngredient\":[\"2 cups rice\",\"1 lemon\"]"))
        assertTrue(content.text.contains("\"name\":\"Lemon Rice\""))
        assertFalse(content.text.contains("aggregateRating"), "noise fields are dropped")
        assertFalse(content.text.contains("blog text"))
    }

    @Test
    fun `finds a top-level Recipe and skips JSON-LD blocks without one`() {
        val html = """
            <script type="application/ld+json">{"@type":"Organization","name":"Site"}</script>
            <script type="application/ld+json">{"@type":"Recipe","name":"Soup","recipeIngredient":["1 onion"]}</script>
        """.trimIndent()

        val content = RecipePageExtractor.extract(html)

        assertTrue(content is RecipePageContent.JsonLd)
        assertTrue(content.text.contains("\"name\":\"Soup\""))
    }

    @Test
    fun `tolerates raw control characters and CDATA comment wrappers in JSON-LD`() {
        val html = """
            <script type="application/ld+json">
            // <![CDATA[
            {"@type":"Recipe","name":"Pork","description":"line one
            	line two with a tab","recipeIngredient":["1 lb pork"]}
            // ]]>
            </script>
        """.trimIndent()

        val content = RecipePageExtractor.extract(html)

        assertTrue(content is RecipePageContent.JsonLd, "got $content")
        assertTrue(content.text.contains("\"recipeIngredient\":[\"1 lb pork\"]"))
    }

    @Test
    fun `decodes HTML entities inside JSON-LD strings`() {
        val html = """<script type="application/ld+json">{"@type":"Recipe","name":"Mac &amp; Cheese","recipeIngredient":["&frac12; cup milk"]}</script>"""

        val content = RecipePageExtractor.extract(html)

        assertTrue(content.text.contains("\"name\":\"Mac & Cheese\""), content.text)
        assertTrue(content.text.contains("\"½ cup milk\""), content.text)
    }

    @Test
    fun `a redirect stub with no body text is reported as empty`() {
        val html = """<html><head><script>window.location.href="/lander"</script></head><body></body></html>"""

        val content = RecipePageExtractor.extract(html)

        assertTrue(content is RecipePageContent.VisibleText)
        assertTrue(content.isEmpty)
    }

    @Test
    fun `falls back to visible text with scripts, styles and chrome stripped`() {
        val html = """
            <html><head><style>.x{color:red}</style><script>var a=1;</script></head>
            <body><nav>Home Recipes</nav><header>Site header</header>
            <h1>Soup</h1><ul><li>1 onion</li><li>2 cups stock</li></ul>
            <footer>© site</footer><script type="application/ld+json">not json</script></body></html>
        """.trimIndent()

        val content = RecipePageExtractor.extract(html)

        assertTrue(content is RecipePageContent.VisibleText)
        assertEquals("Soup 1 onion 2 cups stock", content.text)
    }

    @Test
    fun `caps fallback text length`() {
        val html = "<body>" + "ingredient ".repeat(20_000) + "</body>"

        val content = RecipePageExtractor.extract(html)

        assertEquals(60_000, content.text.length)
    }

    @Test
    fun `keeps recipeInstructions flattened to step texts - HowToSection, HowToStep, strings and HTML`() {
        val html = """
            <html><head><script type="application/ld+json">
            {"@type":"Recipe","name":"Curry","recipeIngredient":["1 onion"],
             "recipeInstructions":[
               {"@type":"HowToSection","name":"Prep","itemListElement":[
                 {"@type":"HowToStep","name":"Chop","text":"Chop the <b>onion</b> finely.","url":"#s1","image":"s1.jpg"},
                 {"@type":"HowToStep","text":"Heat the oil &amp; fry it."}
               ]},
               "Simmer for 20 minutes.",
               {"@type":"HowToStep","name":"Serve with rice."}
             ]}
            </script></head><body></body></html>
        """.trimIndent()

        val content = RecipePageExtractor.extract(html)

        assertTrue(content is RecipePageContent.JsonLd)
        assertTrue(
            content.text.contains("\"recipeInstructions\":[\"Chop the onion finely.\",\"Heat the oil & fry it.\",\"Simmer for 20 minutes.\",\"Serve with rice.\"]"),
            content.text
        )
        assertFalse(content.text.contains("HowToSection"), "section objects are flattened away")
        assertFalse(content.text.contains("s1.jpg"), "per-step images and urls are dropped")
    }

    @Test
    fun `a single-string recipeInstructions is kept as one entry and an empty one is omitted`() {
        val one = RecipePageExtractor.extract(
            """<html><head><script type="application/ld+json">{"@type":"Recipe","recipeIngredient":["x"],"recipeInstructions":"Mix it all."}</script></head><body></body></html>"""
        )
        assertTrue(one.text.contains("\"recipeInstructions\":[\"Mix it all.\"]"), one.text)

        val none = RecipePageExtractor.extract(
            """<html><head><script type="application/ld+json">{"@type":"Recipe","recipeIngredient":["x"],"recipeInstructions":[]}</script></head><body></body></html>"""
        )
        assertFalse(none.text.contains("recipeInstructions"), none.text)
    }
}
