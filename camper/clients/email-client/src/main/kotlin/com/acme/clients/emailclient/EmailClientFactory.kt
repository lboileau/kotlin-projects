package com.acme.clients.emailclient

import com.acme.clients.emailclient.api.EmailClient
import com.acme.clients.emailclient.internal.NoOpEmailClient
import com.acme.clients.emailclient.internal.ResendEmailClient
import com.resend.Resend

fun createEmailClient(): EmailClient {
    val apiKey = System.getProperty("RESEND_API_KEY")
        ?: System.getenv("RESEND_API_KEY")
        ?: throw IllegalStateException("RESEND_API_KEY must be set")

    val fromAddress = System.getProperty("EMAIL_FROM")
        ?: System.getenv("EMAIL_FROM")
        ?: "Camper <noreply@example.com>"

    val resend = Resend(apiKey)
    return ResendEmailClient(resend, fromAddress)
}

fun createNoOpEmailClient(): EmailClient = NoOpEmailClient()
