package com.acme.services.camperservice.config

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.itemclient.api.*
import com.acme.clients.itemclient.model.Item
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ItemClientConfig {
    @Bean
    fun itemClient(): ItemClient = object : ItemClient {
        override fun create(param: CreateItemParam): Result<Item, AppError> = TODO()
        override fun getById(param: GetByIdParam): Result<Item, AppError> = TODO()
        override fun getByPlanId(param: GetByPlanIdParam): Result<List<Item>, AppError> = TODO()
        override fun getByUserId(param: GetByUserIdParam): Result<List<Item>, AppError> = TODO()
        override fun update(param: UpdateItemParam): Result<Item, AppError> = TODO()
        override fun delete(param: DeleteItemParam): Result<Unit, AppError> = TODO()
    }
}
