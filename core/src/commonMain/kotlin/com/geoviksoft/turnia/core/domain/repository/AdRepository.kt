package com.geoviksoft.turnia.core.domain.repository

import kotlinx.coroutines.flow.Flow

interface AdRepository {

    /**
     * Whether a banner belongs on screen: ads are enabled, the user is on the free tier, and they
     * have done enough in the app that an ad is no longer the first thing they meet.
     */
    val bannerVisible: Flow<Boolean>

    /** Adding an event, or opening a group's or a colleague's calendar. */
    fun actionPerformed()
}
