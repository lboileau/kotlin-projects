package com.acme.services.camperservice.features.user.service

import com.acme.clients.common.Result
import com.acme.clients.userclient.api.UserClient
import com.acme.services.camperservice.features.user.actions.*
import com.acme.services.camperservice.features.user.dto.AvatarResponse
import com.acme.services.camperservice.features.user.error.UserError
import com.acme.services.camperservice.features.user.model.User
import com.acme.services.camperservice.features.user.params.*

class UserService(userClient: UserClient) {
    private val getUserById = GetUserByIdAction(userClient)
    private val createUser = CreateUserAction(userClient)
    private val authenticateUser = AuthenticateUserAction(userClient)
    private val updateUser = UpdateUserAction(userClient)
    private val randomizeAvatarAction = RandomizeAvatarAction(userClient)
    private val getAvatarAction = GetAvatarAction(userClient)

    fun getById(param: GetUserByIdParam) = getUserById.execute(param)
    fun create(param: CreateUserParam) = createUser.execute(param)
    fun authenticate(param: AuthenticateUserParam) = authenticateUser.execute(param)
    fun update(param: UpdateUserParam) = updateUser.execute(param)

    /** Re-randomize the user's avatar seed. */
    fun randomizeAvatar(param: RandomizeAvatarParam): Result<User, UserError> = randomizeAvatarAction.execute(param)

    /** Get the computed avatar for a user. */
    fun getAvatar(param: GetAvatarParam): Result<AvatarResponse, UserError> = getAvatarAction.execute(param)
}
