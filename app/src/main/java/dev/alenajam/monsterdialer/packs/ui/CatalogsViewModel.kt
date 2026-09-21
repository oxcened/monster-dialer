package dev.alenajam.monsterdialer.packs.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.alenajam.monsterdialer.packs.data.CatalogPackInstaller
import dev.alenajam.monsterdialer.packs.data.CatalogSource
import dev.alenajam.monsterdialer.packs.data.CatalogSourceRepository
import dev.alenajam.monsterdialer.packs.data.RemotePackCatalog
import dev.alenajam.monsterdialer.packs.data.RemotePackCatalogClient
import dev.alenajam.monsterdialer.packs.data.RemotePackCatalogPack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CatalogsUiState(
    val sources: List<CatalogSource> = emptyList(),
    val loads: Map<String, CatalogLoad> = emptyMap(),
    val action: CatalogAction? = null,
    val isAddingCatalog: Boolean = false,
    val installingPackId: String? = null,
)

sealed interface CatalogLoad {
    data object Loading : CatalogLoad
    data class Content(val catalog: RemotePackCatalog) : CatalogLoad
    data object Failed : CatalogLoad
}

sealed interface CatalogAction {
    data object Added : CatalogAction
    data object AddFailed : CatalogAction
    data object Removed : CatalogAction
    data object RemoveFailed : CatalogAction
    data object Installed : CatalogAction
    data class InstallFailed(val reason: String?) : CatalogAction
}

@HiltViewModel
class CatalogsViewModel @Inject constructor(
    private val sources: CatalogSourceRepository,
    private val client: RemotePackCatalogClient,
    private val installer: CatalogPackInstaller,
) : ViewModel() {
    private val _state = MutableStateFlow(CatalogsUiState())
    val state: StateFlow<CatalogsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            sources.sources.collectLatest { currentSources ->
                _state.value = _state.value.copy(
                    sources = currentSources,
                    loads = _state.value.loads.filterKeys { key -> currentSources.any { it.url == key } },
                )
                currentSources.forEach { refresh(it.url) }
            }
        }
    }

    fun add(url: String) = viewModelScope.launch(Dispatchers.IO) {
        update { it.copy(isAddingCatalog = true) }
        try {
            runCatching { sources.add(url) }
                .onSuccess { setAction(CatalogAction.Added) }
                .onFailure { setAction(CatalogAction.AddFailed) }
        } finally {
            update { it.copy(isAddingCatalog = false) }
        }
    }

    fun remove(url: String) = viewModelScope.launch(Dispatchers.IO) {
        runCatching { sources.remove(url) }
            .onSuccess { setAction(CatalogAction.Removed) }
            .onFailure { setAction(CatalogAction.RemoveFailed) }
    }

    fun refresh(url: String) = viewModelScope.launch(Dispatchers.IO) {
        updateLoad(url, CatalogLoad.Loading)
        runCatching { client.fetch(url) }
            .onSuccess { updateLoad(url, CatalogLoad.Content(it)) }
            .onFailure { updateLoad(url, CatalogLoad.Failed) }
    }

    fun install(pack: RemotePackCatalogPack) = viewModelScope.launch {
        if (_state.value.installingPackId != null) return@launch
        update { it.copy(installingPackId = pack.id) }
        try {
            installer.install(pack)
                .onSuccess { setAction(CatalogAction.Installed) }
                .onFailure { setAction(CatalogAction.InstallFailed(it.message)) }
        } finally {
            update { it.copy(installingPackId = null) }
        }
    }

    fun dismissAction() = update { it.copy(action = null) }

    private fun updateLoad(url: String, load: CatalogLoad) = update { current ->
        current.copy(loads = current.loads + (url to load))
    }

    private fun setAction(action: CatalogAction) = update { it.copy(action = action) }

    private fun update(transform: (CatalogsUiState) -> CatalogsUiState) {
        _state.value = transform(_state.value)
    }
}
