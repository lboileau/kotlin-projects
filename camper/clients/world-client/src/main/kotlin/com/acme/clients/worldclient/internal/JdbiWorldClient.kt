package com.acme.clients.worldclient.internal

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.worldclient.api.CreateWorldParam
import com.acme.clients.worldclient.api.DeleteWorldParam
import com.acme.clients.worldclient.api.GetByIdParam
import com.acme.clients.worldclient.api.GetListParam
import com.acme.clients.worldclient.api.UpdateWorldParam
import com.acme.clients.worldclient.api.WorldClient
import com.acme.clients.worldclient.internal.operations.CreateWorld
import com.acme.clients.worldclient.internal.operations.DeleteWorld
import com.acme.clients.worldclient.internal.operations.GetWorldById
import com.acme.clients.worldclient.internal.operations.GetWorldList
import com.acme.clients.worldclient.internal.operations.UpdateWorld
import com.acme.clients.worldclient.model.World
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
