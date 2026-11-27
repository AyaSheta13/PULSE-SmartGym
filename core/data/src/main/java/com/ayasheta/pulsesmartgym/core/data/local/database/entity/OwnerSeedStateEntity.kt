package com.ayasheta.pulsesmartgym.core.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "owner_seed_state")
data class OwnerSeedStateEntity(
    @PrimaryKey
    val ownerProfileId: String,
    val starterPlansSeeded: Boolean,
    val seededAtEpochMs: Long,
)
