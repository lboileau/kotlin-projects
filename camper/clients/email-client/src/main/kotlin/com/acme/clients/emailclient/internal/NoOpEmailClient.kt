package com.acme.clients.emailclient.internal

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.emailclient.api.EmailClient
import com.acme.clients.emailclient.api.SendEmailParam
import com.acme.clients.emailclient.api.SendEmailResult
import org.slf4j.LoggerFactory
import java.util.UUID

internal class NoOpEmailClient : EmailClient {
    private val logger = LoggerFactory.getLogger(NoOpEmailClient::class.java)

    override fun send(param: SendEmailParam): Result<SendEmailResult, AppError> {
        logger.info("NoOpEmailClient: would send email to={} subject={}", param.to, param.subject)
        return success(SendEmailResult(emailId = "noop-${UUID.randomUUID()}"))
    }
}
