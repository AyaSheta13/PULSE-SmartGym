package com.ayasheta.pulsesmartgym.navigation

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SAVED_STATE_REGISTRY_OWNER_KEY
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.enableSavedStateHandles
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.compose.LocalSavedStateRegistryOwner

val LocalPulseViewModelStoreOwner = staticCompositionLocalOf<ViewModelStoreOwner?> { null }

/**
 * Parent ViewModel that retains per-NavEntry [ViewModelStore] instances across
 * configuration changes / Activity recreation and clears them when entries are popped
 * or when the parent store is cleared.
 */
class NavEntryViewModelStoreRegistry : ViewModel() {
    private val stores = mutableMapOf<Any, ViewModelStore>()

    fun getOrCreateStore(contentKey: Any): ViewModelStore =
        stores.getOrPut(contentKey) { ViewModelStore() }

    fun clearStore(contentKey: Any) {
        stores.remove(contentKey)?.clear()
    }

    override fun onCleared() {
        stores.values.forEach { it.clear() }
        stores.clear()
        super.onCleared()
    }
}

@Composable
fun <T : Any> rememberPulseViewModelStoreNavEntryDecorator(): NavEntryDecorator<T> {
    val context = LocalContext.current
    val parentOwner = LocalPulseViewModelStoreOwner.current
        ?: remember(context) {
            context.findViewModelStoreOwner() ?: object : ViewModelStoreOwner {
                override val viewModelStore: ViewModelStore = ViewModelStore()
            }
        }
    val registry = remember(parentOwner) {
        ViewModelProvider(parentOwner).get(NavEntryViewModelStoreRegistry::class.java)
    }
    return remember(registry) {
        NavEntryDecorator<T>(
            onPop = { contentKey -> registry.clearStore(contentKey) },
            decorate = { entry: NavEntry<T> ->
                val savedStateOwner = LocalSavedStateRegistryOwner.current
                val entryStore = remember(entry.contentKey, registry) {
                    registry.getOrCreateStore(entry.contentKey)
                }
                val entryViewModelStoreOwner = remember(entryStore, savedStateOwner, context) {
                    PulseNavEntryViewModelStoreOwner(
                        viewModelStore = entryStore,
                        savedStateRegistryOwner = savedStateOwner,
                        application = context.applicationContext as? Application,
                    )
                }
                CompositionLocalProvider(LocalPulseViewModelStoreOwner provides entryViewModelStoreOwner) {
                    entry.Content()
                }
            },
        )
    }
}

@PublishedApi
internal tailrec fun Context.findViewModelStoreOwner(): ViewModelStoreOwner? = when (this) {
    is ViewModelStoreOwner -> this
    is ContextWrapper -> baseContext.findViewModelStoreOwner()
    else -> null
}

private class PulseNavEntryViewModelStoreOwner(
    override val viewModelStore: ViewModelStore,
    private val savedStateRegistryOwner: SavedStateRegistryOwner,
    private val application: Application?,
) : ViewModelStoreOwner,
    SavedStateRegistryOwner,
    HasDefaultViewModelProviderFactory {

    init {
        runCatching { enableSavedStateHandles() }
    }

    override val lifecycle: Lifecycle
        get() = savedStateRegistryOwner.lifecycle

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryOwner.savedStateRegistry

    override val defaultViewModelProviderFactory: ViewModelProvider.Factory
        get() = SavedStateViewModelFactory(
            application = application,
            owner = this,
            defaultArgs = Bundle.EMPTY,
        )

    override val defaultViewModelCreationExtras: CreationExtras
        get() = MutableCreationExtras().apply {
            set(SAVED_STATE_REGISTRY_OWNER_KEY, this@PulseNavEntryViewModelStoreOwner)
            set(VIEW_MODEL_STORE_OWNER_KEY, this@PulseNavEntryViewModelStoreOwner)
        }
}

@Composable
inline fun <reified VM : ViewModel> pulseNavViewModel(
    key: String? = null,
    crossinline creator: (SavedStateHandle) -> VM,
): VM {
    val context = LocalContext.current
    val owner = LocalPulseViewModelStoreOwner.current
        ?: remember(context) {
            context.findViewModelStoreOwner() ?: object : ViewModelStoreOwner {
                override val viewModelStore: ViewModelStore = ViewModelStore()
            }
        }
    val factory = remember(owner) {
        if (owner is SavedStateRegistryOwner) {
            runCatching { owner.enableSavedStateHandles() }
        }
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val handle = runCatching { extras.createSavedStateHandle() }
                    .getOrElse { SavedStateHandle() }
                return creator(handle) as T
            }
        }
    }
    val provider = ViewModelProvider(owner, factory)
    return if (key != null) {
        provider.get(key, VM::class.java)
    } else {
        provider.get(VM::class.java)
    }
}
