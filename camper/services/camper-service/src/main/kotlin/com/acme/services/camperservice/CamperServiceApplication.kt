package com.acme.services.camperservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class CamperServiceApplication

fun main(args: Array<String>) {
    runApplication<CamperServiceApplication>(*args)
}
