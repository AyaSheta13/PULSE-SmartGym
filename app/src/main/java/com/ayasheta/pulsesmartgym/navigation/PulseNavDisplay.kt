package com.ayasheta.pulsesmartgym.navigation

import android.app.Application
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
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
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.compose.LocalSavedStateRegistryOwner
import com.ayasheta.pulsesmartgym.core.data.di.PulseDataContainer
import com.ayasheta.pulsesmartgym.core.designsystem.component.generic.PulseTab

class PerStackViewModelStoreHolder : ViewModel() {
    private val stackStores = mutableMapOf<String, ViewModelStore>()

    fun storeFor(stackKey: String): ViewModelStore =
        stackStores.getOrPut(stackKey) { ViewModelStore() }

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

    val authSaveableHolder = key("AUTH") { rememberSaveableStateHolder() }
    val tabSaveableHolders = PulseTab.entries.associateWith { tab ->
        key("TAB_${tab.name}") { rememberSaveableStateHolder() }
    }

    val activeStackKey = when (navigationState.activeFlow) {
        AppEntryFlow.AUTH -> "AUTH"
        AppEntryFlow.MAIN -> "TAB_${navigationState.selectedTab.name}"
    }
    val activeOwner = remember(activeStackKey, stackHolder, savedStateOwner, application) {
        if (activeStackKey == "AUTH") {
            authOwner
        } else {
            StackScopedViewModelStoreOwner(
                viewModelStore = stackHolder.storeFor(activeStackKey),
                savedStateRegistryOwner = savedStateOwner,
                application = application,
            )
        }
    }
    val activeSaveableHolder = when (navigationState.activeFlow) {
        AppEntryFlow.AUTH -> authSaveableHolder
        AppEntryFlow.MAIN -> tabSaveableHolders.getValue(navigationState.selectedTab)
    }

    key(navigationState.activeFlow, navigationState.selectedTab) {
        CompositionLocalProvider(LocalViewModelStoreOwner provides activeOwner) {
            val activeEntries = rememberDecoratedNavEntries(
                backStack = navigationState.activeBackStack,
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(activeSaveableHolder),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                entryProvider = entryProvider,
            )
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
}
