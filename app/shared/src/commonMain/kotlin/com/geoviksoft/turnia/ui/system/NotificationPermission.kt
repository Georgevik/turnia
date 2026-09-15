package com.geoviksoft.turnia.ui.system

import androidx.compose.runtime.Composable

/**
 * Asks the platform, once per install, for permission to show notifications.
 *
 * Called from the user's own calendar and not at launch: the first thing a stranger sees should not
 * be a permission dialog, and there is nothing to notify anyone about until they are signed in.
 * iOS never shows its dialog twice; Android would ask again after one refusal, so it keeps a flag.
 *
 * On iOS this is also what makes push work at all: the APNs registration it triggers is what
 * finally gives Firebase a token to hand out.
 */
@Composable
expect fun RequestNotificationPermission()
