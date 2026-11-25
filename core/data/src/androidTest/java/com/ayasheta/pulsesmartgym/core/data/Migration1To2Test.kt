package com.ayasheta.pulsesmartgym.core.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ayasheta.pulsesmartgym.core.data.local.database.PulseDatabase
import com.ayasheta.pulsesmartgym.core.data.local.database.RoomTransactionRunner
import com.ayasheta.pulsesmartgym.core.data.local.preferences.PulsePreferencesDataSource
import com.ayasheta.pulsesmartgym.core.data.repository.auth.LocalAuthRepository
import com.ayasheta.pulsesmartgym.core.data.repository.profile.RoomUserProfileRepository
import com.ayasheta.pulsesmartgym.core.model.common.DataError
import com.ayasheta.pulsesmartgym.core.model.common.DomainResult
import com.ayasheta.pulsesmartgym.core.model.common.WeightUnit
import com.ayasheta.pulsesmartgym.core.model.profile.AuthState
import com.ayasheta.pulsesmartgym.core.model.profile.ExperienceLevel
import com.ayasheta.pulsesmartgym.core.model.profile.FitnessGoal
import com.ayasheta.pulsesmartgym.core.model.profile.FitnessGoalsDraft
import com.ayasheta.pulsesmartgym.core.model.profile.GenderIdentity
import com.ayasheta.pulsesmartgym.core.model.profile.PersonalInfoDraft
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class Migration1To2Test {

    private val testDbName = "pulse_migration_test.db"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        PulseDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migrate1To2_preservesWorkoutPlansAndSessions_andSupportsProfiles() {
        helper.createDatabase(testDbName, 1).apply {
            execSQL(
                """
                INSERT INTO workout_plans (id, ownerProfileId, name, description, createdAtEpochMs, updatedAtEpochMs)
                VALUES ('plan_v1', 'user_1', 'Legacy Push', 'Chest & Shoulders', 1000, 1000)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO workout_sessions (id, ownerProfileId, sourcePlanId, planNameSnapshot, status, startedAtEpochMs, endedAtEpochMs)
                VALUES ('sess_v1', 'user_1', 'plan_v1', 'Legacy Push', 'COMPLETED', 2000, 3000)
                """.trimIndent(),
            )
            close()
        }

        val migratedDb = helper.runMigrationsAndValidate(
            testDbName,
            2,
            true,
            PulseDatabase.MIGRATION_1_2,
        )
        migratedDb.query("SELECT id, name FROM workout_plans WHERE id = 'plan_v1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Legacy Push", cursor.getString(1))
        }
        migratedDb.query("SELECT id, status FROM workout_sessions WHERE id = 'sess_v1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("COMPLETED", cursor.getString(1))
        }
        migratedDb.close()
    }

    @Test
    fun localAuthAndProfileRepositories_enforceGuestStabilitySaltedAuthAndOwnerIsolation() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val dbFile = File(context.filesDir, "p4_auth_profile_${System.nanoTime()}.db")
        val prefsFile = File(context.filesDir, "p4_auth_prefs_${System.nanoTime()}.preferences_pb")
        val db = Room.databaseBuilder(context, PulseDatabase::class.java, dbFile.absolutePath)
            .addMigrations(PulseDatabase.MIGRATION_1_2)
            .build()
        try {
            val txRunner = RoomTransactionRunner(db)
            val dataStore = PreferenceDataStoreFactory.create(
                scope = this.backgroundScope,
                produceFile = { prefsFile },
            )
            val prefsSource = PulsePreferencesDataSource(dataStore)
            val authRepo = LocalAuthRepository(
                accountDao = db.localAccountDao(),
                preferencesDataSource = prefsSource,
                transactionRunner = txRunner,
                allowDemoSignIn = false,
            )
            val profileRepo = RoomUserProfileRepository(
                userProfileDao = db.userProfileDao(),
                transactionRunner = txRunner,
            )

            // 1. Guest session persists stable unique ownerId across restore
            val guest1 = (authRepo.continueAsGuest() as DomainResult.Success).data
            assertTrue(guest1.isGuest)
            assertTrue(guest1.id.startsWith("guest_"))
            val restoredGuest = authRepo.restoreSession()
            assertTrue(restoredGuest is AuthState.Authenticated)
            assertEquals(guest1.id, (restoredGuest as AuthState.Authenticated).user.id)

            // 2. Sign out clears active session, then continuing as guest reuses same stable guest ID
            authRepo.signOut()
            assertEquals(AuthState.Unauthenticated, authRepo.observeAuthState().first())
            val guest2 = (authRepo.continueAsGuest() as DomainResult.Success).data
            assertEquals(guest1.id, guest2.id)

            // 3. Unauthenticated password reset is rejected
            authRepo.signOut()
            val resetResult = authRepo.resetPassword("Pulse@2026!")
            assertTrue(resetResult is DomainResult.Failure)
            assertTrue((resetResult as DomainResult.Failure).error is DataError.Unauthorized)

            // 4. Local account registration stores salted hash (never plaintext) and isolates owner
            val reg = authRepo.registerLocalAccount("Alex Miller", "alex@pulse.fit", "Pulse@2026!")
            assertTrue(reg is DomainResult.Success)
            val localUser = (reg as DomainResult.Success).data
            assertNotEquals(guest1.id, localUser.id)
            val storedAccount = db.localAccountDao().getByEmail("alex@pulse.fit")!!
            assertNotEquals("Pulse@2026!", storedAccount.passwordHash)

            // 4b. Duplicate registration is rejected
            val dupReg = authRepo.registerLocalAccount("Alex Two", "ALEX@pulse.fit", "Pulse@2026!")
            assertTrue(dupReg is DomainResult.Failure)

            // 4c. Logout -> wrong password rejected -> valid login with rememberMe = false
            authRepo.signOut()
            val badLogin = authRepo.signInWithEmail("alex@pulse.fit", "Wrong@123!", rememberMe = true)
            assertTrue(badLogin is DomainResult.Failure)
            assertTrue((badLogin as DomainResult.Failure).error is DataError.Unauthorized)

            val loginNoRemember = authRepo.signInWithEmail("alex@pulse.fit", "Pulse@2026!", rememberMe = false)
            assertTrue(loginNoRemember is DomainResult.Success)

            // Simulate process death with fresh LocalAuthRepository: rememberMe = false -> Unauthenticated
            val processDeathRepo1 = LocalAuthRepository(
                accountDao = db.localAccountDao(),
                preferencesDataSource = prefsSource,
                transactionRunner = txRunner,
                allowDemoSignIn = false,
            )
            assertEquals(AuthState.Initializing, processDeathRepo1.observeAuthState().first())
            assertEquals(AuthState.Unauthenticated, processDeathRepo1.restoreSession())

            // Login with rememberMe = true -> fresh LocalAuthRepository restores Authenticated
            assertTrue(
                processDeathRepo1.signInWithEmail("alex@pulse.fit", "Pulse@2026!", rememberMe = true) is DomainResult.Success,
            )
            val processDeathRepo2 = LocalAuthRepository(
                accountDao = db.localAccountDao(),
                preferencesDataSource = prefsSource,
                transactionRunner = txRunner,
                allowDemoSignIn = false,
            )
            val restoredLocal = processDeathRepo2.restoreSession()
            assertTrue(restoredLocal is AuthState.Authenticated)
            assertEquals(localUser.id, (restoredLocal as AuthState.Authenticated).user.id)

            // 5. Save completed profile under localUser.id and verify guest has no profile
            val saveProfile = profileRepo.saveCompletedProfile(
                ownerId = localUser.id,
                personalInfo = PersonalInfoDraft(
                    fullName = "Alex Miller",
                    gender = GenderIdentity.MALE,
                    age = 28,
                    heightCm = 182,
                    weightKg = 78.0,
                    measurementUnit = WeightUnit.KG,
                ),
                goals = FitnessGoalsDraft(
                    fitnessGoal = FitnessGoal.HYPERTROPHY,
                    experienceLevel = ExperienceLevel.INTERMEDIATE,
                    weeklyWorkouts = 4,
                ),
            )
            assertTrue(saveProfile is DomainResult.Success)
            assertTrue(profileRepo.isProfileSetupCompleted(localUser.id))
            assertFalse(profileRepo.isProfileSetupCompleted(guest1.id))
            assertNull(profileRepo.getProfile(guest1.id))
        } finally {
            db.close()
            if (dbFile.exists()) dbFile.delete()
            if (prefsFile.exists()) prefsFile.delete()
        }
    }
}
