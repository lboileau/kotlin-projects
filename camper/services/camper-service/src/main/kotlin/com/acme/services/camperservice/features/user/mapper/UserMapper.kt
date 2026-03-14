package com.acme.services.camperservice.features.user.mapper

import com.acme.clients.userclient.model.User as ClientUser
import com.acme.libs.avatargenerator.AvatarGenerator
import com.acme.services.camperservice.features.user.dto.AuthResponse
import com.acme.services.camperservice.features.user.dto.UserResponse
import com.acme.services.camperservice.features.user.model.User

object UserMapper {

    fun fromClient(clientUser: ClientUser): User = User(
        id = clientUser.id,
        email = clientUser.email,
        username = clientUser.username,
        experienceLevel = clientUser.experienceLevel,
        avatarSeed = clientUser.avatarSeed,
        profileCompleted = clientUser.profileCompleted,
        dietaryRestrictions = clientUser.dietaryRestrictions,
        createdAt = clientUser.createdAt,
        updatedAt = clientUser.updatedAt
    )

    fun toResponse(user: User): UserResponse = UserResponse(
        id = user.id,
        email = user.email,
        username = user.username,
        experienceLevel = user.experienceLevel,
        avatarSeed = user.avatarSeed,
        profileCompleted = user.profileCompleted,
        dietaryRestrictions = user.dietaryRestrictions,
        avatar = user.avatarSeed?.let { AvatarMapper.toResponse(AvatarGenerator.generate(it)) },
        createdAt = user.createdAt,
        updatedAt = user.updatedAt
    )

    fun toAuthResponse(user: User): AuthResponse = AuthResponse(
        id = user.id,
        email = user.email,
        username = user.username,
        avatarSeed = user.avatarSeed,
        profileCompleted = user.profileCompleted,
        avatar = user.avatarSeed?.let { AvatarMapper.toResponse(AvatarGenerator.generate(it)) }
    )
}
