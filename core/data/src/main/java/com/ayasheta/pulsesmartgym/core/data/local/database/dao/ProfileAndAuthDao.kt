package com.ayasheta.pulsesmartgym.core.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ayasheta.pulsesmartgym.core.data.local.database.entity.LocalAccountEntity
import com.ayasheta.pulsesmartgym.core.data.local.database.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalAccountDao {
    @Query("SELECT * FROM local_accounts WHERE email = :normalizedEmail LIMIT 1")
    suspend fun getByEmail(normalizedEmail: String): LocalAccountEntity?

    @Query("SELECT * FROM local_accounts WHERE id = :accountId LIMIT 1")
    suspend fun getById(accountId: String): LocalAccountEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAccount(entity: LocalAccountEntity)

    @Query("UPDATE local_accounts SET passwordSalt = :salt, passwordHash = :hash WHERE id = :accountId")
    suspend fun updatePasswordHash(accountId: String, salt: String, hash: String): Int
}

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profiles WHERE ownerId = :ownerId LIMIT 1")
    fun observeByOwner(ownerId: String): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profiles WHERE ownerId = :ownerId LIMIT 1")
    suspend fun getByOwner(ownerId: String): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(entity: UserProfileEntity)
}
