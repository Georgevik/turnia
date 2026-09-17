package com.geoviksoft.turnia.ui.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.geoviksoft.turnia.ui.system.components.TurniaLogo
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import turnia.app.shared.generated.resources.Res
import turnia.app.shared.generated.resources.onboarding_get_started
import turnia.app.shared.generated.resources.onboarding_next
import turnia.app.shared.generated.resources.onboarding_page
import turnia.app.shared.generated.resources.onboarding_skip
import turnia.app.shared.generated.resources.welcome_feature_clear_body
import turnia.app.shared.generated.resources.welcome_feature_clear_title
import turnia.app.shared.generated.resources.welcome_feature_notify_body
import turnia.app.shared.generated.resources.welcome_feature_notify_title
import turnia.app.shared.generated.resources.welcome_feature_trace_body
import turnia.app.shared.generated.resources.welcome_feature_trace_title

private enum class OnboardingPage(val title: StringResource, val body: StringResource) {
    Trace(Res.string.welcome_feature_trace_title, Res.string.welcome_feature_trace_body),
    Clear(Res.string.welcome_feature_clear_title, Res.string.welcome_feature_clear_body),
    Notify(Res.string.welcome_feature_notify_title, Res.string.welcome_feature_notify_body),
}

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = koinViewModel(),
) {
    val finish = { viewModel.onFinished(onFinished) }
    val pages = OnboardingPage.entries
    val pagerState = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == pages.lastIndex

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeContentPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TurniaLogo(Modifier.size(36.dp))
            Spacer(Modifier.weight(1f))
            // Kept in the layout on the last page, so the pager does not jump when it goes.
            TextButton(
                onClick = finish,
                enabled = !isLast,
                modifier = Modifier.alpha(if (isLast) 0f else 1f),
            ) {
                Text(stringResource(Res.string.onboarding_skip))
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) { index ->
            PageContent(pages[index])
        }

        PageIndicator(pagerState)

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                if (isLast) finish()
                else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Text(
                stringResource(if (isLast) Res.string.onboarding_get_started else Res.string.onboarding_next),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun PageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when (page) {
            OnboardingPage.Trace -> SwapChainMockup()
            OnboardingPage.Clear -> CalendarMockup()
            OnboardingPage.Notify -> NotificationMockup()
        }

        Spacer(Modifier.height(40.dp))

        Text(
            text = stringResource(page.title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = stringResource(page.body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PageIndicator(pagerState: PagerState) {
    val description = stringResource(
        Res.string.onboarding_page,
        pagerState.currentPage + 1,
        pagerState.pageCount,
    )
    Row(
        modifier = Modifier.semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(pagerState.pageCount) { index ->
            val selected = index == pagerState.currentPage
            val width by animateDpAsState(if (selected) 24.dp else 8.dp)
            val color by animateColorAsState(
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
            )
            Box(
                Modifier
                    .height(8.dp)
                    .width(width)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}
