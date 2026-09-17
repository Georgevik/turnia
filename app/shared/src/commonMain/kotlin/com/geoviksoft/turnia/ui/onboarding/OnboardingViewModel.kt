package com.geoviksoft.turnia.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geoviksoft.turnia.core.domain.repository.AppConfigRepository
import kotlinx.coroutines.launch

class OnboardingViewModel(private val appPreferences: AppConfigRepository) : ViewModel() {

    // Navigating away clears this ViewModel and cancels its scope, so leave only once it is saved.
    fun onFinished(navigate: () -> Unit) {
        viewModelScope.launch {
            appPreferences.setOnboardingSeen(true)
            navigate()
        }
    }
}
