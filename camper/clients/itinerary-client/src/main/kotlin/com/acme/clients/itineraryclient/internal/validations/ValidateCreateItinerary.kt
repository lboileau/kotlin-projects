package com.acme.clients.itineraryclient.internal.validations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.itineraryclient.api.CreateItineraryParam
import org.slf4j.LoggerFactory

internal class ValidateCreateItinerary {
    private val logger = LoggerFactory.getLogger(ValidateCreateItinerary::class.java)

    fun execute(param: CreateItineraryParam): Result<Unit, AppError> {
        return success(Unit)
    }
}
