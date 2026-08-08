package com.georgevik.turnia

import androidx.lifecycle.ViewModel
import com.georgevik.turnia.core.sayHello

class GreetingViewModel : ViewModel() {

    private val platform = getPlatform()

    val message: String = sayHello(platform.name)
}
