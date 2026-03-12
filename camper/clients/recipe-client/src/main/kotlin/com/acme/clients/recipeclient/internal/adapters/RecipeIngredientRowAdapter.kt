package com.acme.clients.recipeclient.internal.adapters

import com.acme.clients.recipeclient.model.RecipeIngredient
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.sql.ResultSet
import java.util.UUID

internal object RecipeIngredientRowAdapter {
    private val mapper = jacksonObjectMapper()
    private val listOfStringType = object : TypeReference<List<String>>() {}

    fun fromResultSet(rs: ResultSet): RecipeIngredient {
        val flagsJson = rs.getString("review_flags") ?: "[]"
        val reviewFlags: List<String> = mapper.readValue(flagsJson, listOfStringType)
        return RecipeIngredient(
            id = rs.getObject("id", UUID::class.java),
            recipeId = rs.getObject("recipe_id", UUID::class.java),
            ingredientId = rs.getObject("ingredient_id", UUID::class.java),
            originalText = rs.getString("original_text"),
            quantity = rs.getBigDecimal("quantity"),
            unit = rs.getString("unit"),
            status = rs.getString("status"),
            matchedIngredientId = rs.getObject("matched_ingredient_id", UUID::class.java),
            suggestedIngredientName = rs.getString("suggested_ingredient_name"),
            suggestedCategory = rs.getString("suggested_category"),
            suggestedUnit = rs.getString("suggested_unit"),
            reviewFlags = reviewFlags,
            createdAt = rs.getTimestamp("created_at").toInstant(),
            updatedAt = rs.getTimestamp("updated_at").toInstant()
        )
    }
}
