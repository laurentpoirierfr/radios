package fr.lpoirier.radios.ui

import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import fr.lpoirier.radios.R
import androidx.compose.ui.res.stringResource
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.lpoirier.radios.app
import fr.lpoirier.radios.support.Tip

/** Écran « Soutenir l'app » : dons facultatifs via Google Play. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(onBack: () -> Unit) {
    val support = LocalContext.current.app.support
    val activity = LocalActivity.current
    val state by support.state.collectAsStateWithLifecycle()

    Surface(Modifier.fillMaxSize()) { Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text(stringResource(R.string.support_title)) }, navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.back))
            }
        })
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(if (state.isSupporter) "❤️ " + stringResource(R.string.support_thanks_short) else "❤️", style = MaterialTheme.typography.displaySmall)
            Text(
                stringResource(R.string.support_body),
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.size(4.dp))
            when {
                state.connecting -> CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
                state.prices.isEmpty() -> Text(
                    stringResource(R.string.support_unavailable),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> Tip.entries.forEach { tip ->
                    val price = state.prices[tip] ?: return@forEach
                    OutlinedCard(
                        onClick = { activity?.let { support.launch(it, tip) } },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(tip.emoji, style = MaterialTheme.typography.headlineSmall)
                            Text(
                                stringResource(tip.label),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                            )
                            Text(price, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Text(
                stringResource(R.string.support_footer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } }

    state.message?.let { message ->
        AlertDialog(
            onDismissRequest = support::clearMessage,
            confirmButton = { TextButton(onClick = support::clearMessage) { Text(stringResource(R.string.ok)) } },
            text = { Text(stringResource(message)) },
        )
    }
}
