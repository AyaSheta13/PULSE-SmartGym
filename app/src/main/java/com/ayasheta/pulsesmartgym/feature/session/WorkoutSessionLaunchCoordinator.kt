package com.ayasheta.pulsesmartgym.feature.session

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ayasheta.pulsesmartgym.core.designsystem.theme.PulseTheme
import com.ayasheta.pulsesmartgym.core.model.common.DataError
import com.ayasheta.pulsesmartgym.core.model.common.DomainResult
import com.ayasheta.pulsesmartgym.core.model.profile.AuthState
import com.ayasheta.pulsesmartgym.core.model.repository.AuthRepository
import com.ayasheta.pulsesmartgym.core.model.repository.WorkoutSessionRepository
import com.ayasheta.pulsesmartgym.feature.workout.components.WorkoutPalette
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Stable
class WorkoutSessionLaunchCoordinator(
    private val scope: CoroutineScope,
    private val authRepository: AuthRepository,
    private val workoutSessionRepository: WorkoutSessionRepository,
    private val onNavigateToSession: (planId: String, sessionId: String) -> Unit,
) {
    var isStarting by mutableStateOf(false)
        private set

    var conflict by mutableStateOf<DataError.ActiveSessionConflict?>(null)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun startOrResumePlan(planId: String) {
        if (isStarting) return
        scope.launch {
            isStarting = true
            errorMessage = null
            conflict = null
            try {
                val auth = authRepository.restoreSession()
                if (auth !is AuthState.Authenticated) {
                    errorMessage = "Please sign in to start a workout session."
                    return@launch
                }
                val expectedOwnerId = auth.user.id
                val result = workoutSessionRepository.startSessionFromPlan(expectedOwnerId, planId)
                val latestOwnerId = (authRepository.restoreSession() as? AuthState.Authenticated)?.user?.id
                if (latestOwnerId != expectedOwnerId) {
                    return@launch
                }
                when (result) {
                    is DomainResult.Success -> {
                        val session = result.data
                        onNavigateToSession(session.sourcePlanId ?: planId, session.id)
                    }
                    is DomainResult.Failure -> {
                        when (val err = result.error) {
                            is DataError.ActiveSessionConflict -> conflict = err
                            else -> errorMessage = err.userMessage()
                        }
                    }
                }
            } finally {
                isStarting = false
            }
        }
    }

    fun repeatHistoricalSession(historicalSessionId: String) {
        if (isStarting) return
        scope.launch {
            isStarting = true
            errorMessage = null
            conflict = null
            try {
                val auth = authRepository.restoreSession()
                if (auth !is AuthState.Authenticated) {
                    errorMessage = "Please sign in to repeat this workout."
                    return@launch
                }
                val expectedOwnerId = auth.user.id
                val result = workoutSessionRepository.startSessionFromHistory(expectedOwnerId, historicalSessionId)
                val latestOwnerId = (authRepository.restoreSession() as? AuthState.Authenticated)?.user?.id
                if (latestOwnerId != expectedOwnerId) {
                    return@launch
                }
                when (result) {
                    is DomainResult.Success -> {
                        val session = result.data
                        onNavigateToSession(session.sourcePlanId ?: "", session.id)
                    }
                    is DomainResult.Failure -> {
                        when (val err = result.error) {
                            is DataError.ActiveSessionConflict -> conflict = err
                            else -> errorMessage = err.userMessage()
                        }
                    }
                }
            } finally {
                isStarting = false
            }
        }
    }

    fun resumeConflictingSession() {
        val active = conflict ?: return
        conflict = null
        onNavigateToSession(active.activePlanId ?: "", active.activeSessionId)
    }

    fun dismissConflict() {
        conflict = null
    }

    fun dismissError() {
        errorMessage = null
    }
}

@Composable
fun WorkoutSessionLaunchOverlays(
    coordinator: WorkoutSessionLaunchCoordinator,
) {
    coordinator.conflict?.let { active ->
        Dialog(
            onDismissRequest = coordinator::dismissConflict,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable(onClick = coordinator::dismissConflict),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .background(Color.White)
                        .clickable(enabled = false, onClick = {})
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 24.dp)
                        .testTag("dialog_active_session_conflict"),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    BasicText(
                        text = "Active Workout In Progress",
                        style = PulseTheme.typography.cardTitle.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = WorkoutPalette.TextPrimary,
                        ),
                    )
                    BasicText(
                        text = "\"${active.activePlanName}\" is currently active. Resume your active workout or cancel without starting a second session.",
                        style = PulseTheme.typography.contentPrimary.copy(
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = WorkoutPalette.TextMuted,
                        ),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(WorkoutPalette.PrimaryCoral)
                            .clickable(onClick = coordinator::resumeConflictingSession)
                            .testTag("button_resume_active_session"),
                        contentAlignment = Alignment.Center,
                    ) {
                        BasicText(
                            text = "Resume ${active.activePlanName}",
                            style = PulseTheme.typography.ctaPrimary.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            ),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEEEAE6))
                            .clickable(onClick = coordinator::dismissConflict)
                            .testTag("button_cancel_active_conflict"),
                        contentAlignment = Alignment.Center,
                    ) {
                        BasicText(
                            text = "Cancel",
                            style = PulseTheme.typography.ctaPrimary.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = WorkoutPalette.TextPrimary,
                            ),
                        )
                    }
                }
            }
        }
    }

    coordinator.errorMessage?.let { message ->
        Dialog(
            onDismissRequest = coordinator::dismissError,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable(onClick = coordinator::dismissError),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .background(Color.White)
                        .clickable(enabled = false, onClick = {})
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 24.dp)
                        .testTag("dialog_start_session_error"),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    BasicText(
                        text = "Unable to Start Workout",
                        style = PulseTheme.typography.cardTitle.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = WorkoutPalette.TextPrimary,
                        ),
                    )
                    BasicText(
                        text = message,
                        style = PulseTheme.typography.contentPrimary.copy(
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = WorkoutPalette.ErrorText,
                        ),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(WorkoutPalette.PrimaryCoral)
                            .clickable(onClick = coordinator::dismissError)
                            .testTag("button_dismiss_start_error"),
                        contentAlignment = Alignment.Center,
                    ) {
                        BasicText(
                            text = "OK",
                            style = PulseTheme.typography.ctaPrimary.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            ),
                        )
                    }
                }
            }
        }
    }
}
