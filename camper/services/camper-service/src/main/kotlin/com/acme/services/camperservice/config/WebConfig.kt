package com.acme.services.camperservice.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.resource.PathResourceResolver
import java.io.File

@Configuration
class WebConfig : WebMvcConfigurer {

    private val staticDir = File("/app/static")

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        if (staticDir.exists()) {
            registry.addResourceHandler("/**")
                .addResourceLocations("file:/app/static/")
                .resourceChain(true)
                .addResolver(object : PathResourceResolver() {
                    override fun getResource(resourcePath: String, location: Resource): Resource? {
                        val requested = FileSystemResource("/app/static/$resourcePath")
                        return if (requested.exists() && requested.isReadable) {
                            requested
                        } else {
                            FileSystemResource("/app/static/index.html")
                        }
                    }
                })
        }
    }
}
