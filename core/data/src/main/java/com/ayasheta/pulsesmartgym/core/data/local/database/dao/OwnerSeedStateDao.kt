package com.ayasheta.pulsesmartgym.core.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ayasheta.pulsesmartgym.core.data.local.database.entity.OwnerSeedStateEntity

@Dao
interface OwnerSeedStateDao {

    @Query("SELECT * FROM owner_seed_state WHERE ownerProfileId = :ownerId")
    suspend fun get(ownerId: String): OwnerSeedStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: OwnerSeedStateEntity)
}
