package com.example.services.hello.features.world.dto

data class CreateWorldRequest(val name: String, val greeting: String)

data class UpdateWorldRequest(val name: String? = null, val greeting: String? = null)
