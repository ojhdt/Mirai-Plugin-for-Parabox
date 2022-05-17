package com.ojhdtapp.miraipluginforparabox.data.local

import androidx.room.*
import com.ojhdtapp.miraipluginforparabox.data.local.entity.SecretsEntity
import com.ojhdtapp.miraipluginforparabox.domain.model.Secrets
import kotlinx.coroutines.flow.Flow

@Dao
interface SecretsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSecrets(secrets: SecretsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSecrets(secretsList: List<SecretsEntity>)

    @Delete
    suspend fun deleteSecrets(secrets: SecretsEntity)

    @Query("SELECT * FROM secretsentity")
    suspend fun getAllSecrets() : List<SecretsEntity>

    @Query("SELECT * FROM secretsentity")
    fun getAllSecretsFlow() : Flow<List<SecretsEntity>>
}