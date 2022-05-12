package com.ojhdtapp.miraipluginforparabox.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ojhdtapp.miraipluginforparabox.domain.model.Secrets

@Entity
class SecretsEntity(
    private val account: Long,
    private val password: String,
    @PrimaryKey val id: Int? = null,
) {
    fun toSecrets(): Secrets = Secrets(
        account, password
    )
}