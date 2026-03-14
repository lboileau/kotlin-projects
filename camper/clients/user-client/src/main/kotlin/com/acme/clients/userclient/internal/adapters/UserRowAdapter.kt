package com.acme.clients.userclient.internal.adapters

import com.acme.clients.userclient.model.User
import java.sql.ResultSet
import java.util.UUID

object UserRowAdapter {

    fun fromResultSet(rs: ResultSet): User = User(
        id = rs.getObject("id", UUID::class.java),
        email = rs.getString("email"),
        username = rs.getString("username"),
        experienceLevel = rs.getString("experience_level"),
        avatarSeed = rs.getString("avatar_seed"),
        profileCompleted = rs.getBoolean("profile_completed"),
        dietaryRestrictions = emptyList(),
        createdAt = rs.getTimestamp("created_at").toInstant(),
        updatedAt = rs.getTimestamp("updated_at").toInstant()
    )
}
