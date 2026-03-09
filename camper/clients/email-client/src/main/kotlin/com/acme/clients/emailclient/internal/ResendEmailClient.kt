package com.acme.clients.emailclient.internal

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.InternalError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.emailclient.api.EmailClient
import com.acme.clients.emailclient.api.SendEmailParam
import com.acme.clients.emailclient.api.SendEmailResult
import com.resend.Resend
import com.resend.core.exception.ResendException
import com.resend.services.emails.model.CreateEmailOptions
import org.slf4j.LoggerFactory

internal class ResendEmailClient(
    private val resend: Resend,
    private val fromAddress: String
) : EmailClient {
    private val logger = LoggerFactory.getLogger(ResendEmailClient::class.java)

    override fun send(param: SendEmailParam): Result<SendEmailResult, AppError> {
        logger.info("Sending email to={} subject={}", param.to, param.subject)
        return try {
            val options = CreateEmailOptions.builder()
                .from(fromAddress)
                .to(param.to)
                .subject(param.subject)
                .html(param.html)
                .build()

            val response = resend.emails().send(options)
            logger.info("Email sent successfully emailId={}", response.id)
            success(SendEmailResult(emailId = response.id))
        } catch (e: ResendException) {
            logger.error("Failed to send email to={}: {}", param.to, e.message)
            failure(InternalError("Failed to send email: ${e.message}"))
        } catch (e: Exception) {
            logger.error("Failed to send email to={}: {}", param.to, e.message)
            failure(InternalError("Failed to send email: ${e.message}"))
        }
    }
}
