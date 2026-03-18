package com.acmo.clients.worldclient.internal

import com.acmo.clients.common.Result
import com.acmo.clients.common.error.AppError
import com.acmo.clients.worldclient.api.CreateWorldParam
import com.acmo.clients.worldclient.api.DeleteWorldParam
import com.acmo.clients.worldclient.api.GetByIdParam
import com.acmo.clients.worldclient.api.GetListParam
import com.acmo.clients.worldclient.api.UpdateWorldParam
import com.acmo.clients.worldclient.api.WorldClient
import com.acmo.clients.worldclient.internal.operations.CreateWorld
import com.acmo.clients.worldclient.internal.operations.DeleteWorld
import com.acmo.clients.worldclient.internal.operations.GetWorldById
import com.acmo.clients.worldclient.internal.operations.GetWorldList
import com.acmo.clients.worldclient.internal.operations.UpdateWorld
import com.acmo.clients.worldclient.model.World
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
