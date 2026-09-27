package dev.alenajam.monsterdialer.app

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.alenajam.monsterdialer.app.data.OnboardingStore
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val onboardingStore: OnboardingStore,
) : ViewModel() {
    private val _showWelcome = MutableStateFlow(onboardingStore.shouldShowWelcome())
    val showWelcome: StateFlow<Boolean> = _showWelcome.asStateFlow()

    private val _showSetupCompletion = MutableStateFlow(false)
    val showSetupCompletion: StateFlow<Boolean> = _showSetupCompletion.asStateFlow()

    fun refreshSetupCompletion(isSetupComplete: Boolean) {
        _showSetupCompletion.value = isSetupComplete && onboardingStore.shouldShowSetupCompletion()
    }

    fun completeWelcome() {
        onboardingStore.markWelcomeCompleted()
        _showWelcome.value = false
    }

    fun completeSetupCompletion() {
        onboardingStore.markSetupCompletionShown()
        _showSetupCompletion.value = false
    }
}
