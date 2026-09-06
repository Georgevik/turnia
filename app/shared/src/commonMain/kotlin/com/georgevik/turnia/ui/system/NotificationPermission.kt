package com.georgevik.turnia.ui.system

import androidx.compose.runtime.Composable

/**
 * Asks the platform, once, for permission to show notifications.
 *
 * Called from the authenticated part of the app and not at launch: the first thing a stranger sees
 * should not be a permission dialog, and there is nothing to notify anyone about until they are
 * signed in and in a group. Both platforms remember the answer, so calling this on every
 * composition asks the user nothing after the first time.
 *
 * On iOS this is also what makes push work at all: the APNs registration it triggers is what
 * finally gives Firebase a token to hand out.
 */
@Composable
expect fun RequestNotificationPermission()
