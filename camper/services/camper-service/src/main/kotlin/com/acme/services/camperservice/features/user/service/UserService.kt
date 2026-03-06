package com.acme.services.camperservice.features.user.service

import com.acme.clients.userclient.api.UserClient
import com.acme.services.camperservice.features.user.actions.*
import com.acme.services.camperservice.features.user.params.*

class UserService(userClient: UserClient) {
    private val getUserById = GetUserByIdAction(userClient)
    private val createUser = CreateUserAction(userClient)
    private val authenticateUser = AuthenticateUserAction(userClient)
    private val updateUser = UpdateUserAction(userClient)

    fun getById(param: GetUserByIdParam) = getUserById.execute(param)
    fun create(param: CreateUserParam) = createUser.execute(param)
    fun authenticate(param: AuthenticateUserParam) = authenticateUser.execute(param)
    fun update(param: UpdateUserParam) = updateUser.execute(param)
}
