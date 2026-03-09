package com.acme.clients.emailclient.api

data class SendEmailParam(
    val to: String,
    val subject: String,
    val html: String
)

data class SendEmailResult(
    val emailId: String
)
