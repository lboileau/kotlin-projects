package com.acme.services.camperservice.features.recipe.dto

data class CreateIngredientRequest(val name: String, val category: String, val defaultUnit: String)

data class UpdateIngredientRequest(val name: String?, val category: String?, val defaultUnit: String?)
