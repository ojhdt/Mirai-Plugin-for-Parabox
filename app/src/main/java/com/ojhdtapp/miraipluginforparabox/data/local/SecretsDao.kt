package com.ojhdtapp.miraipluginforparabox.data.local

import androidx.room.*
import com.ojhdtapp.miraipluginforparabox.domain.model.Secrets
import kotlinx.coroutines.flow.Flow

@Dao
interface SecretsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSecrets(secrets: Secrets)

    @Delete
    suspend fun deleteSecrets(secrets: Secrets)

    @Query("SELECT * FROM secretsentity")
    suspend fun getAllSecrets() : Flow<List<Secrets>>
}