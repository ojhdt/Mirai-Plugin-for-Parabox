package com.ojhdtapp.miraipluginforparabox.data.local

import androidx.room.*
import com.ojhdtapp.miraipluginforparabox.data.local.entity.SecretEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SecretDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSecret(secret: SecretEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSecrets(secretList: List<SecretEntity>)

    @Delete
    suspend fun deleteSecret(secret: SecretEntity)

    @Query("SELECT * FROM secretentity WHERE selected LIMIT 1")
    suspend fun getSelectedSecret() : SecretEntity?

    @Query("SELECT * FROM secretentity")
    suspend fun getAllSecrets() : List<SecretEntity>

    @Query("SELECT * FROM secretentity")
    fun getAllSecretsFlow() : Flow<List<SecretEntity>>
}