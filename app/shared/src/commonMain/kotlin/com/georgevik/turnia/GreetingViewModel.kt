package com.georgevik.turnia

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georgevik.turnia.core.domain.repo.AuthRepository
import com.georgevik.turnia.core.sayHello
import com.georgevik.turnia.interfaces.AuthProvider
import com.georgevik.turnia.interfaces.getPlatform
import kotlinx.coroutines.launch

class GreetingViewModel(
    private val authRepository: AuthRepository,
    private val authProvider: AuthProvider
) : ViewModel() {
    fun signIn() {
        viewModelScope.launch {
            val asdf = authProvider.getGoogleToken() ?: return@launch
            authRepository.signWithGoogle(asdf)
        }
    }

    private val platform = getPlatform()

    val message: String = sayHello(platform.name)
}
