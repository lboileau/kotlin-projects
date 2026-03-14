package com.acme.services.camperservice.features.user.mapper

import com.acme.libs.avatargenerator.model.Avatar
import com.acme.services.camperservice.features.user.dto.AvatarResponse

object AvatarMapper {

    fun toResponse(avatar: Avatar): AvatarResponse = AvatarResponse(
        hairStyle = avatar.hairStyle.name.lowercase(),
        hairColor = avatar.hairColor.name.lowercase(),
        skinColor = avatar.skinColor.name.lowercase(),
        clothingStyle = avatar.clothingStyle.name.lowercase(),
        pantsColor = avatar.pantsColor.name.lowercase(),
        shirtColor = avatar.shirtColor.name.lowercase()
    )
}
