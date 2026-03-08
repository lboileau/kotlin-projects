package com.acme.clients.emailclient.api

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError

/**
 * Client interface for sending emails.
 *
 * All operations return [Result] to represent success or typed failure
 * without throwing exceptions for expected error conditions.
 */
interface EmailClient {
    /** Send an email and return the provider's email ID. */
    fun send(param: SendEmailParam): Result<SendEmailResult, AppError>
}
