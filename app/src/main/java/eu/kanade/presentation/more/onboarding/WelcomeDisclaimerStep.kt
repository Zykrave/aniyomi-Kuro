package eu.kanade.presentation.more.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tachiyomi.presentation.core.components.material.padding

internal class WelcomeDisclaimerStep : OnboardingStep {

    private var isChecked by mutableStateOf(false)

    override val isComplete: Boolean
        get() = isChecked

    @Composable
    override fun Content() {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
        ) {
            Text(
                text = "Welcome! Let's set some things up first. You can always change these in the settings later too.\n\n" +
                    "Before you start — Kuro is a media library manager and player. It does not include, host, or provide any content, sources, extensions, or repositories. To use Kuro you must add your own sources that you are legally entitled to access. You are solely responsible for the sources you add and for complying with the laws and the rights of others in your country. Kuro does not endorse or facilitate copyright infringement and is not affiliated with any rights holder, streaming service, publisher, or studio.",
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = isChecked,
                    onCheckedChange = { isChecked = it },
                )
                Text("I understand and accept the Terms of Service and Disclaimer")
            }
        }
    }
}
