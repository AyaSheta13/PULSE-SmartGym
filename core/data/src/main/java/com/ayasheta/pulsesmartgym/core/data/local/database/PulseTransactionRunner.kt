package com.ayasheta.pulsesmartgym.core.data.local.database

import androidx.room.RoomDatabase
import androidx.room.withTransaction

interface PulseTransactionRunner {
    suspend fun <T> runInTransaction(block: suspend () -> T): T
}

class RoomTransactionRunner(
    private val database: RoomDatabase,
) : PulseTransactionRunner {
    override suspend fun <T> runInTransaction(block: suspend () -> T): T =
        database.withTransaction(block)
}
