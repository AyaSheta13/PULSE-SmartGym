package com.ayasheta.pulsesmartgym.navigation

import android.app.Application
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SAVED_STATE_REGISTRY_OWNER_KEY
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.compose.LocalSavedStateRegistryOwner
import com.ayasheta.pulsesmartgym.core.data.di.PulseDataContainer
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseTab
import com.ayasheta.pulsesmartgym.core.model.profile.AuthState

class PerStackViewModelStoreHolder : ViewModel() {
    private val stackStores = mutableMapOf<String, ViewModelStore>()
    private var activeOwnerId: String? = null
    var sessionEpoch by mutableIntStateOf(0)
        private set

    fun storeFor(stackKey: String): ViewModelStore =
        stackStores.getOrPut(stackKey) { ViewModelStore() }

    /**
     * Clears all Main tab ViewModelStores and advances [sessionEpoch] whenever an
     * authenticated user logs out or switches to a different ownerId, without
     * resetting state during `Initializing -> Authenticated` or normal tab switches.
     */
    fun syncAuthState(authState: AuthState) {
        if (authState is AuthState.Initializing) return
        syncOwner((authState as? AuthState.Authenticated)?.user?.id)
    }

    fun syncOwner(ownerId: String?) {
        if (ownerId == null) {
            if (activeOwnerId != null) {
                clearMainTabStores()
                sessionEpoch += 1
                activeOwnerId = null
            }
        } else {
            if (activeOwnerId != null && activeOwnerId != ownerId) {
                clearMainTabStores()
                sessionEpoch += 1
            }
            activeOwnerId = ownerId
        }
    }

    fun clearMainTabStores() {
        val tabKeys = stackStores.keys.filter { it.startsWith("TAB_") }
        tabKeys.forEach { key ->
            stackStores.remove(key)?.clear()
        }
    }

    override fun onCleared() {
        stackStores.values.forEach { it.clear() }
        stackStores.clear()
        super.onCleared()
    }
}

private class StackScopedViewModelStoreOwner(
    override val viewModelStore: ViewModelStore,
    private val savedStateRegistryOwner: SavedStateRegistryOwner,
    private val application: Application?,
) : ViewModelStoreOwner,
    SavedStateRegistryOwner,
    HasDefaultViewModelProviderFactory {

    override val lifecycle: Lifecycle
        get() = savedStateRegistryOwner.lifecycle

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryOwner.savedStateRegistry

    override val defaultViewModelProviderFactory: ViewModelProvider.Factory
        get() = SavedStateViewModelFactory(
            application = application,
            owner = savedStateRegistryOwner,
            defaultArgs = Bundle.EMPTY,
        )

    override val defaultViewModelCreationExtras: CreationExtras
        get() = MutableCreationExtras().apply {
            set(SAVED_STATE_REGISTRY_OWNER_KEY, savedStateRegistryOwner)
            set(VIEW_MODEL_STORE_OWNER_KEY, this@StackScopedViewModelStoreOwner)
        }
}

@Composable
fun PulseNavDisplay(
    navigationState: PulseNavigationState,
    navigator: PulseNavigator,
    isDebugEntryEnabled: Boolean,
    dataContainer: PulseDataContainer,
    modifier: Modifier = Modifier,
) {
    val stackHolder: PerStackViewModelStoreHolder = viewModel()
    val savedStateOwner = LocalSavedStateRegistryOwner.current
    val application = LocalContext.current.applicationContext as? Application

    val authState by dataContainer.authRepository.observeAuthState()
        .collectAsState(initial = AuthState.Initializing)

    LaunchedEffect(authState) {
        stackHolder.syncAuthState(authState)
    }

    val authOwner = remember(stackHolder, savedStateOwner, application) {
        StackScopedViewModelStoreOwner(
            viewModelStore = stackHolder.storeFor("AUTH"),
            savedStateRegistryOwner = savedStateOwner,
            application = application,
        )
    }

    val entryProvider = rememberPulseEntryProvider(
        navigator = navigator,
        isDebugEntryEnabled = isDebugEntryEnabled,
        dataContainer = dataContainer,
        authFlowViewModelStoreOwner = authOwner,
    )

    val authEntries = rememberStackEntries(
        stackOwner = authOwner,
        backStack = navigationState.authBackStack,
        entryProvider = entryProvider,
    )

    val tabEntriesByTab = key(stackHolder.sessionEpoch) {
        PulseTab.entries.associateWith { tab ->
            rememberTabStackEntries(
                tab = tab,
                stackHolder = stackHolder,
                savedStateOwner = savedStateOwner,
                application = application,
                backStack = navigationState.tabBackStacks.getValue(tab),
                entryProvider = entryProvider,
            )
        }
    }

    val activeEntries = when (navigationState.activeFlow) {
        AppEntryFlow.AUTH -> authEntries
        AppEntryFlow.MAIN -> tabEntriesByTab.getValue(navigationState.selectedTab)
    }

    key(navigationState.activeFlow, navigationState.selectedTab) {
        NavDisplay(
            entries = activeEntries,
            modifier = modifier,
            onBack = { navigator.navigateBack() },
            transitionSpec = PulseNavigationTransitions.forwardTransition(),
            popTransitionSpec = PulseNavigationTransitions.popTransition(),
            predictivePopTransitionSpec = { PulseNavigationTransitions.popTransition().invoke(this) },
        )
    }
}

@Composable
private fun rememberTabStackEntries(
    tab: PulseTab,
    stackHolder: PerStackViewModelStoreHolder,
    savedStateOwner: SavedStateRegistryOwner,
    application: Application?,
    backStack: List<NavKey>,
    entryProvider: (NavKey) -> NavEntry<NavKey>,
): List<NavEntry<NavKey>> {
    val stackOwner = remember(tab, stackHolder, savedStateOwner, application) {
        StackScopedViewModelStoreOwner(
            viewModelStore = stackHolder.storeFor("TAB_${tab.name}"),
            savedStateRegistryOwner = savedStateOwner,
            application = application,
        )
    }
    val saveableStateHolder = rememberSaveableStateHolder()
    return rememberStackEntries(
        stackOwner = stackOwner,
        saveableStateHolder = saveableStateHolder,
        backStack = backStack,
        entryProvider = entryProvider,
    )
}

@Composable
private fun rememberStackEntries(
    stackOwner: ViewModelStoreOwner,
    saveableStateHolder: androidx.compose.runtime.saveable.SaveableStateHolder = rememberSaveableStateHolder(),
    backStack: List<NavKey>,
    entryProvider: (NavKey) -> NavEntry<NavKey>,
): List<NavEntry<NavKey>> = rememberDecoratedNavEntries(
    backStack = backStack,
    entryDecorators = listOf(
        rememberSaveableStateHolderNavEntryDecorator(saveableStateHolder = saveableStateHolder),
        rememberViewModelStoreNavEntryDecorator(viewModelStoreOwner = stackOwner),
    ),
    entryProvider = entryProvider,
)
