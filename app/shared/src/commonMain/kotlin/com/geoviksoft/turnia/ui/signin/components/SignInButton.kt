package com.geoviksoft.turnia.ui.signin.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.painterResource
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.roboto_medium
import turnia.app.shared.generated.resources.signin_apple_logo
import turnia.app.shared.generated.resources.signin_google_logo

/**
 * Both providers are drawn with Google's button spec, so the pair reads as one set. Apple allows a
 * custom button as long as it is no smaller or less prominent than the others; Google does not, so
 * theirs is the one both follow.
 */
enum class SignInProvider(internal val logo: DrawableResource, internal val tintLogo: Boolean) {
    // The G is four colours and must never be recoloured.
    Google(Res.drawable.signin_google_logo, tintLogo = false),

    // Apple's logo is a single colour that follows the text, black on light and white on dark.
    Apple(Res.drawable.signin_apple_logo, tintLogo = true),
}

@Composable
fun SignInButton(
    provider: SignInProvider,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = if (isSystemInDarkTheme()) SignInButtonColors.Dark else SignInButtonColors.Light

    Button(
        onClick = onClick,
        modifier = modifier,
        shape = ButtonDefaults.shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.container,
            contentColor = colors.content,
        ),
        border = BorderStroke(1.dp, colors.border),
        contentPadding = PaddingValues(horizontal = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val logo = painterResource(provider.logo)
            if (provider.tintLogo) {
                Icon(painter = logo, contentDescription = null, modifier = Modifier.size(LogoSize))
            } else {
                Image(painter = logo, contentDescription = null, modifier = Modifier.size(LogoSize))
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = text,
                fontSize = 14.sp,
                fontFamily = FontFamily(Font(Res.font.roboto_medium, FontWeight.Medium)),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private val LogoSize = 20.dp

/** Google's published colours for its light and dark sign-in buttons. */
private enum class SignInButtonColors(val container: Color, val content: Color, val border: Color) {
    Light(container = Color(0xFFFFFFFF), content = Color(0xFF1F1F1F), border = Color(0xFF747775)),
    Dark(container = Color(0xFF131314), content = Color(0xFFE3E3E3), border = Color(0xFF8E918F)),
}

@Preview
@Composable
private fun SignInButtonPreview() {
    SignInButton(SignInProvider.Apple, text = "Continue with Apple", onClick = {})
}
