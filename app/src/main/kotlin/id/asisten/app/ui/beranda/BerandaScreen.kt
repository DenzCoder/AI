package id.asisten.app.ui.beranda

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import id.asisten.app.R
import id.asisten.app.domain.Sapaan

@Composable
fun BerandaScreen(
    modifier: Modifier = Modifier,
    viewModel: BerandaViewModel = viewModel(),
) {
    val sapaan by viewModel.sapaan.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.segarkan() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(sapaan.teks()),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.beranda_keterangan),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = stringResource(R.string.status_pengembangan),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun Sapaan.teks(): Int = when (this) {
    Sapaan.PAGI -> R.string.sapaan_pagi
    Sapaan.SIANG -> R.string.sapaan_siang
    Sapaan.SORE -> R.string.sapaan_sore
    Sapaan.MALAM -> R.string.sapaan_malam
}
