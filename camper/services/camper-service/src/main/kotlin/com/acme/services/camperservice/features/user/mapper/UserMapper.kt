package com.acme.services.camperservice.features.user.mapper

import com.acme.clients.userclient.model.User as ClientUser
import com.acme.services.camperservice.features.user.dto.AuthResponse
import com.acme.services.camperservice.features.user.dto.UserResponse
import com.acme.services.camperservice.features.user.model.User

object UserMapper {

    fun fromClient(clientUser: ClientUser): User = User(
        id = clientUser.id,
        email = clientUser.email,
        username = clientUser.username,
        createdAt = clientUser.createdAt,
        updatedAt = clientUser.updatedAt
    )

    fun toResponse(user: User): UserResponse = UserResponse(
        id = user.id,
        email = user.email,
        username = user.username,
        createdAt = user.createdAt,
        updatedAt = user.updatedAt
    )

    fun toAuthResponse(user: User): AuthResponse = AuthResponse(
        id = user.id,
        email = user.email,
        username = user.username
    )
}
