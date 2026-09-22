package com.acme.services.camperservice.config

import com.acme.clients.photostorageclient.api.PhotoStorageClient
import com.acme.clients.photostorageclient.createLocalPhotoStorageClient
import com.acme.clients.photostorageclient.createS3PhotoStorageClient
import com.acme.clients.photostorageclient.isS3PhotoStorageConfigured
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.File

@Configuration
class PhotoStorageClientConfig {
    private val logger = LoggerFactory.getLogger(PhotoStorageClientConfig::class.java)

    @Bean
    @ConditionalOnMissingBean
    fun photoStorageClient(): PhotoStorageClient {
        if (isS3PhotoStorageConfigured()) {
            logger.info("AWS_S3_BUCKET_NAME set, storing recipe photos in the bucket")
            return createS3PhotoStorageClient()
        }
        val dir = File(System.getProperty("PHOTO_STORE_DIR") ?: System.getenv("PHOTO_STORE_DIR") ?: ".photos")
        logger.warn("AWS_S3_BUCKET_NAME not set, storing recipe photos as files under {} (served at /api/photo-store)", dir.absolutePath)
        return createLocalPhotoStorageClient(dir, "/api/photo-store")
    }
}
