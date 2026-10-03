package com.muscom.app.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.muscom.app.R
import com.muscom.app.data.Settings
import com.muscom.app.ui.AppViewModel
import com.muscom.app.ui.comingsoon.ComingSoonScreen
import com.muscom.app.ui.game.GameScreen
import com.muscom.app.ui.game.GameViewModel
import com.muscom.app.ui.home.HomeScreen
import com.muscom.app.ui.home.SoonFeature
import com.muscom.app.ui.login.LoginScreen
import com.muscom.app.ui.onboarding.OnboardingScreen
import com.muscom.app.ui.profile.ProfileScreen
import com.muscom.app.ui.settings.SettingsScreen

object Routes {
    const val ONBOARDING = "onboarding"
    const val LOGIN = "login"
    const val HOME = "home"
    const val GAME = "game"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"
    const val SOON = "soon/{feature}"
}

@Composable
fun AppNav(app: AppViewModel, settings: Settings) {
    val nav = rememberNavController()
    val start = androidx.compose.runtime.remember { when {
        !settings.onboardingDone -> Routes.ONBOARDING
        settings.playerName.isBlank() -> Routes.LOGIN
        else -> Routes.HOME
    } }
    NavHost(navController = nav, startDestination = start) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(onDone = {
                app.update { it.copy(onboardingDone = true) }
                nav.navigate(Routes.LOGIN) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
            })
        }
        composable(Routes.LOGIN) {
            LoginScreen(settings.playerName) { name, analytics ->
                app.update { it.copy(playerName = name, analyticsConsent = analytics, onboardingDone = true) }
                nav.navigate(Routes.HOME) { popUpTo(0) { inclusive = true } }
            }
        }
        composable(Routes.HOME) {
            HomeScreen(
                name = settings.playerName,
                level = settings.botLevel,
                onLevel = { l -> app.update { it.copy(botLevel = l) } },
                onPlayBots = { nav.navigate(Routes.GAME) },
                onSoon = { f -> nav.navigate("soon/${f.name}") },
                onProfile = { nav.navigate(Routes.PROFILE) },
                onSettings = { nav.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.GAME) {
            val vm: GameViewModel = hiltViewModel()
            val ui by vm.ui.collectAsStateWithLifecycle()
            val names = listOf(
                settings.playerName,
                stringResource(R.string.bot_right),
                stringResource(R.string.bot_partner),
                stringResource(R.string.bot_left),
            )
            LaunchedEffect(Unit) { if (vm.ui.value.view == null) vm.start(names, settings.botLevel) }
            GameScreen(
                ui = ui,
                onAction = vm::act,
                onToggleCard = vm::toggleCard,
                onDiscard = vm::discardSelected,
                onEnvido = vm::setEnvido,
                onSena = vm::sena,
                onNextHand = vm::nextHand,
                onRematch = vm::rematch,
                onExit = { nav.popBackStack() },
            )
        }
        composable(Routes.PROFILE) {
            val stats by app.stats.collectAsStateWithLifecycle()
            ProfileScreen(settings.playerName, stats, onBack = { nav.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                settings = settings,
                onChange = app::update,
                onDeleteData = {
                    app.deleteLocalData()
                    nav.navigate(Routes.ONBOARDING) { popUpTo(0) { inclusive = true } }
                },
                onBack = { nav.popBackStack() },
            )
        }
        composable(Routes.SOON) { entry ->
            val f = runCatching { SoonFeature.valueOf(entry.arguments?.getString("feature") ?: "") }.getOrDefault(SoonFeature.QUICK)
            ComingSoonScreen(f, onBack = { nav.popBackStack() })
        }
    }
}
