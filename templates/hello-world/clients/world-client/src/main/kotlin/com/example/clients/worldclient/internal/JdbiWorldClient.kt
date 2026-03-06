package com.example.clients.worldclient.internal

import com.example.clients.common.Result
import com.example.clients.common.error.AppError
import com.example.clients.worldclient.api.CreateWorldParam
import com.example.clients.worldclient.api.DeleteWorldParam
import com.example.clients.worldclient.api.GetByIdParam
import com.example.clients.worldclient.api.GetListParam
import com.example.clients.worldclient.api.UpdateWorldParam
import com.example.clients.worldclient.api.WorldClient
import com.example.clients.worldclient.internal.operations.CreateWorld
import com.example.clients.worldclient.internal.operations.DeleteWorld
import com.example.clients.worldclient.internal.operations.GetWorldById
import com.example.clients.worldclient.internal.operations.GetWorldList
import com.example.clients.worldclient.internal.operations.UpdateWorld
import com.example.clients.worldclient.model.World
import org.jdbi.v3.core.Jdbi

/**
 * Facade that delegates to individual operation classes.
 */
internal class JdbiWorldClient(jdbi: Jdbi) : WorldClient {

    private val getWorldById = GetWorldById(jdbi)
    private val getWorldList = GetWorldList(jdbi)
    private val createWorld = CreateWorld(jdbi)
    private val updateWorld = UpdateWorld(jdbi, getWorldById)
    private val deleteWorld = DeleteWorld(jdbi)

    override fun getById(param: GetByIdParam): Result<World, AppError> = getWorldById.execute(param)
    override fun getList(param: GetListParam): Result<List<World>, AppError> = getWorldList.execute(param)
    override fun create(param: CreateWorldParam): Result<World, AppError> = createWorld.execute(param)
    override fun update(param: UpdateWorldParam): Result<World, AppError> = updateWorld.execute(param)
    override fun delete(param: DeleteWorldParam): Result<Unit, AppError> = deleteWorld.execute(param)
}
