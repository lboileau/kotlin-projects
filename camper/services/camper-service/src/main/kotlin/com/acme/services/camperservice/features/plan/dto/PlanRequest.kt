package com.acme.services.camperservice.features.plan.dto

data class CreatePlanRequest(val name: String)

data class UpdatePlanRequest(val name: String)

data class AddMemberRequest(val email: String)
