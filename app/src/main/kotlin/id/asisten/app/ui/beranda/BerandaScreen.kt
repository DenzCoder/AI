package id.asisten.app.ui.beranda

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import id.asisten.app.R
import id.asisten.app.domain.Sapaan
import id.asisten.app.domain.dasbor.WidgetBeranda

@Composable
fun BerandaScreen(
    modifier: Modifier = Modifier,
    viewModel: BerandaViewModel = viewModel(),
) {
    val sapaan by viewModel.sapaan.collectAsStateWithLifecycle()
    val riwayat by viewModel.riwayat.collectAsStateWithLifecycle()
    val mengatur by viewModel.mengatur.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.segarkan() }

    if (mengatur) {
        PanelAturWidget(
            urutan = riwayat.sekarang.urutan,
            tersembunyi = riwayat.sekarang.tersembunyi,
            dapatDiurungkan = riwayat.dapatDiurungkan,
            onPindah = viewModel::pindah,
            onTampil = viewModel::aturTampil,
            onUrungkan = viewModel::urungkan,
            onStandar = viewModel::kembalikanStandar,
            onSelesai = viewModel::selesaiMengatur,
            modifier = modifier,
        )
    } else {
        Dasbor(
            sapaan = sapaan,
            terlihat = riwayat.sekarang.terlihat,
            onAtur = viewModel::mulaiMengatur,
            modifier = modifier,
        )
    }
}

@Composable
private fun Dasbor(
    sapaan: Sapaan,
    terlihat: List<WidgetBeranda>,
    onAtur: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(sapaan.teks()),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                TextButton(onClick = onAtur) {
                    Text(stringResource(R.string.beranda_atur_widget))
                }
            }
        }
        item(key = "hanako") { KartuHanako() }
        if (terlihat.isEmpty()) {
            item(key = "kosong") {
                Text(
                    text = stringResource(R.string.atur_semua_tersembunyi),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(terlihat, key = { it.name }) { widget -> KartuWidget(widget) }
    }
}

@Composable
private fun KartuHanako() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.hanako_ilustrasi),
                contentDescription = stringResource(R.string.hanako_ilustrasi_desc),
                modifier = Modifier.size(72.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.hanako_kartu_judul),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = stringResource(R.string.hanako_kartu_isi),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun KartuWidget(widget: WidgetBeranda) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(widget.judul()),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(widget.keterangan()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PanelAturWidget(
    urutan: List<WidgetBeranda>,
    tersembunyi: Set<WidgetBeranda>,
    dapatDiurungkan: Boolean,
    onPindah: (WidgetBeranda, Int) -> Unit,
    onTampil: (WidgetBeranda, Boolean) -> Unit,
    onUrungkan: () -> Unit,
    onStandar: () -> Unit,
    onSelesai: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.atur_judul),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                TextButton(onClick = onSelesai) { Text(stringResource(R.string.atur_selesai)) }
            }
        }
        item(key = "keterangan") {
            Text(
                text = stringResource(R.string.atur_keterangan),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item(key = "aksi") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onUrungkan, enabled = dapatDiurungkan) {
                    Text(stringResource(R.string.atur_urungkan))
                }
                OutlinedButton(onClick = onStandar) {
                    Text(stringResource(R.string.atur_standar))
                }
            }
        }
        items(urutan, key = { it.name }) { widget ->
            val nama = stringResource(widget.judul())
            val indeks = urutan.indexOf(widget)
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = nama,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = { onPindah(widget, -1) },
                        enabled = indeks > 0,
                    ) {
                        Icon(
                            Icons.Filled.KeyboardArrowUp,
                            contentDescription = stringResource(R.string.atur_naik, nama),
                        )
                    }
                    IconButton(
                        onClick = { onPindah(widget, +1) },
                        enabled = indeks < urutan.lastIndex,
                    ) {
                        Icon(
                            Icons.Filled.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.atur_turun, nama),
                        )
                    }
                    Switch(
                        checked = widget !in tersembunyi,
                        onCheckedChange = { onTampil(widget, it) },
                    )
                }
            }
        }
    }
}

private fun Sapaan.teks(): Int = when (this) {
    Sapaan.PAGI -> R.string.sapaan_pagi
    Sapaan.SIANG -> R.string.sapaan_siang
    Sapaan.SORE -> R.string.sapaan_sore
    Sapaan.MALAM -> R.string.sapaan_malam
}
