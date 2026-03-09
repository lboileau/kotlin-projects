package com.acme.clients.emailclient.fake

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.emailclient.api.EmailClient
import com.acme.clients.emailclient.api.SendEmailParam
import com.acme.clients.emailclient.api.SendEmailResult
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

class FakeEmailClient : EmailClient {
    private val _sentEmails = CopyOnWriteArrayList<SendEmailParam>()
    val sentEmails: List<SendEmailParam> get() = _sentEmails.toList()

    override fun send(param: SendEmailParam): Result<SendEmailResult, AppError> {
        _sentEmails.add(param)
        return success(SendEmailResult(emailId = UUID.randomUUID().toString()))
    }

    fun reset() = _sentEmails.clear()
}
