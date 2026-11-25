package com.ayasheta.pulsesmartgym.navigation

import android.animation.ValueAnimator
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.Scene

object PulseNavigationTransitions {

    private fun duration(defaultMs: Int): Int =
        if (ValueAnimator.areAnimatorsEnabled()) defaultMs else 0

    fun forwardTransition(): AnimatedContentTransitionScope<Scene<NavKey>>.() -> ContentTransform = {
        val enterMs = duration(220)
        val exitMs = duration(160)
        (slideInHorizontally(animationSpec = tween(enterMs)) { width -> width / 5 } +
            fadeIn(animationSpec = tween(enterMs)))
            .togetherWith(fadeOut(animationSpec = tween(exitMs)))
    }

    fun popTransition(): AnimatedContentTransitionScope<Scene<NavKey>>.() -> ContentTransform = {
        val enterMs = duration(200)
        val exitMs = duration(180)
        fadeIn(animationSpec = tween(enterMs))
            .togetherWith(
                slideOutHorizontally(animationSpec = tween(exitMs)) { width -> width / 5 } +
                    fadeOut(animationSpec = tween(exitMs)),
            )
    }

    fun workoutEnterTransition(): AnimatedContentTransitionScope<Scene<NavKey>>.() -> ContentTransform = {
        val enterMs = duration(240)
        val exitMs = duration(160)
        (slideInVertically(animationSpec = tween(enterMs)) { height -> height / 6 } +
            fadeIn(animationSpec = tween(enterMs)))
            .togetherWith(fadeOut(animationSpec = tween(exitMs)))
    }

    fun workoutExitTransition(): AnimatedContentTransitionScope<Scene<NavKey>>.() -> ContentTransform = {
        val enterMs = duration(200)
        val exitMs = duration(200)
        fadeIn(animationSpec = tween(enterMs))
            .togetherWith(
                slideOutVertically(animationSpec = tween(exitMs)) { height -> height / 6 } +
                    fadeOut(animationSpec = tween(exitMs)),
            )
    }
}
