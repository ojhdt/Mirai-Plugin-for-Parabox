package com.ojhdtapp.miraipluginforparabox.ui.util

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.navigation.NavBackStackEntry
import com.ramcosta.composedestinations.spec.DestinationStyle
import com.ramcosta.composedestinations.spec.DestinationStyle.Animated.None.enterTransition

@OptIn(ExperimentalAnimationApi::class)
object SharedAxisZTransition : DestinationStyle.Animated {
    override fun AnimatedContentScope<NavBackStackEntry>.enterTransition(): EnterTransition? {
        return scaleIn(tween(200), 0.9f) + fadeIn(tween(200))
    }

    override fun AnimatedContentScope<NavBackStackEntry>.exitTransition(): ExitTransition? {
        return scaleOut(tween(200), 1.1f) + fadeOut(tween(200))
    }

    override fun AnimatedContentScope<NavBackStackEntry>.popEnterTransition(): EnterTransition? {
        return scaleIn(tween(200), 1.1f) + fadeIn(tween(200))
    }

    override fun AnimatedContentScope<NavBackStackEntry>.popExitTransition(): ExitTransition? {
        return scaleOut(tween(200), 0.9f) + fadeOut(tween(200))
    }
}