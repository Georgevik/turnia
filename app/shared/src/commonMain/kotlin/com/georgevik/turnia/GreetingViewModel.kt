package com.georgevik.turnia

import androidx.lifecycle.ViewModel
import com.georgevik.turnia.core.sayHello
import com.georgevik.turnia.interfaces.getPlatform

class GreetingViewModel : ViewModel() {

    private val platform = getPlatform()

    val message: String = sayHello(platform.name)
}
