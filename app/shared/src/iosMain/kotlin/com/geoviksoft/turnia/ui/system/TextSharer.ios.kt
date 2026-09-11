package com.geoviksoft.turnia.ui.system

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.uikit.LocalUIViewController
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIPasteboard
import platform.UIKit.popoverPresentationController

@Composable
actual fun rememberTextSharer(): TextSharer {
    val viewController = LocalUIViewController.current

    return remember(viewController) {
        object : TextSharer {
            override fun copy(text: String) {
                UIPasteboard.generalPasteboard.string = text
            }

            override fun share(text: String) {
                val sheet = UIActivityViewController(
                    activityItems = listOf(text),
                    applicationActivities = null,
                )
                // An iPad shows the sheet as a popover, and UIKit crashes on one with no anchor.
                sheet.popoverPresentationController?.sourceView = viewController.view
                viewController.presentViewController(sheet, animated = true, completion = null)
            }
        }
    }
}
