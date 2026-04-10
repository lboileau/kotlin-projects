package com.acme.clients.gearpackclient.internal.operations

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.ConflictError
import com.acme.clients.common.failure
import com.acme.clients.gearpackclient.api.GetGearPackByIdParam
import com.acme.clients.gearpackclient.api.UpdateGearPackParam
import com.acme.clients.gearpackclient.internal.validations.ValidateUpdateGearPack
import com.acme.clients.gearpackclient.model.GearPack
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import java.time.Instant

internal class UpdateGearPack(
    private val jdbi: Jdbi,
    private val getGearPackById: GetGearPackById,
) {
    private val logger = LoggerFactory.getLogger(UpdateGearPack::class.java)
    private val validate = ValidateUpdateGearPack()

    fun execute(param: UpdateGearPackParam): Result<GearPack, AppError> {
        val validation = validate.execute(param)
        if (validation is Result.Failure) return validation

        val existing = getGearPackById.execute(GetGearPackByIdParam(param.id))
        if (existing is Result.Failure) return existing

        logger.debug("Updating gear pack id={}", param.id)
        return try {
            val sets = mutableListOf<String>()
            if (param.name != null) sets.add("name = :name")
            if (param.description != null) sets.add("description = :description")
            sets.add("updated_at = :updatedAt")

            jdbi.withHandle<Unit, Exception> { handle ->
                val now = Instant.now()
                handle.createUpdate("UPDATE gear_packs SET ${sets.joinToString(", ")} WHERE id = :id")
                    .bind("id", param.id)
                    .also { q -> if (param.name != null) q.bind("name", param.name) }
                    .also { q -> if (param.description != null) q.bind("description", param.description) }
                    .bind("updatedAt", now)
                    .execute()
            }
            getGearPackById.execute(GetGearPackByIdParam(param.id))
        } catch (e: Exception) {
            if (e.message?.contains("uq_gear_packs_name") == true || e.message?.contains("duplicate key") == true) {
                failure(ConflictError("GearPack", "name '${param.name}' already exists"))
            } else {
                throw e
            }
        }
    }
}
