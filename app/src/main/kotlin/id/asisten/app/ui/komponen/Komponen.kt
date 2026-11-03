package id.asisten.app.ui.komponen

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp

@Composable
fun JudulHalaman(teks: String, modifier: Modifier = Modifier) {
    Text(
        text = teks,
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier,
    )
}

@Composable
fun JudulBagian(teks: String, modifier: Modifier = Modifier) {
    Text(text = teks, style = MaterialTheme.typography.titleMedium, modifier = modifier)
}

/** Honest empty-state / hint text. */
@Composable
fun TeksPetunjuk(teks: String, modifier: Modifier = Modifier) {
    Text(
        text = teks,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

@Composable
fun TeksGalat(teks: String?, modifier: Modifier = Modifier) {
    if (teks != null) {
        Text(
            text = teks,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = modifier,
        )
    }
}

/** A horizontally scrolling row of single-choice chips. */
@Composable
fun <T> BarisPilihan(
    opsi: List<T>,
    terpilih: T?,
    label: @Composable (T) -> String,
    onPilih: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        opsi.forEach { pilihan ->
            FilterChip(
                selected = pilihan == terpilih,
                onClick = { onPilih(pilihan) },
                label = { Text(label(pilihan)) },
            )
        }
    }
}

@Composable
fun FieldTeks(
    label: String,
    nilai: String,
    onUbah: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    petunjuk: String? = null,
) {
    OutlinedTextField(
        value = nilai,
        onValueChange = onUbah,
        label = { Text(label) },
        singleLine = singleLine,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        supportingText = if (petunjuk != null) ({ Text(petunjuk) }) else null,
        modifier = modifier.fillMaxWidth(),
    )
}

/** Form dialog. The content scrolls so long forms still fit on small screens. */
@Composable
fun DialogForm(
    judul: String,
    onBatal: () -> Unit,
    onSimpan: () -> Unit,
    modifier: Modifier = Modifier,
    labelSimpan: String = "Simpan",
    isi: @Composable ColumnScope.() -> Unit,
) {
    AlertDialog(
        onDismissRequest = onBatal,
        modifier = modifier,
        title = { Text(judul) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                isi()
            }
        },
        confirmButton = { TextButton(onClick = onSimpan) { Text(labelSimpan) } },
        dismissButton = { TextButton(onClick = onBatal) { Text("Batal") } },
    )
}

/** Explicit confirmation before deleting a record. */
@Composable
fun DialogKonfirmasi(
    judul: String,
    pesan: String,
    onYa: () -> Unit,
    onBatal: () -> Unit,
    labelYa: String = "Hapus",
) {
    AlertDialog(
        onDismissRequest = onBatal,
        title = { Text(judul) },
        text = { Text(pesan) },
        confirmButton = { TextButton(onClick = onYa) { Text(labelYa) } },
        dismissButton = { TextButton(onClick = onBatal) { Text("Batal") } },
    )
}

@Composable
fun TombolUbah(onClick: () -> Unit, deskripsi: String = "Ubah") {
    IconButton(onClick = onClick) { Icon(Icons.Filled.Edit, contentDescription = deskripsi) }
}

@Composable
fun TombolHapus(onClick: () -> Unit, deskripsi: String = "Hapus") {
    IconButton(onClick = onClick) { Icon(Icons.Filled.Delete, contentDescription = deskripsi) }
}

@Composable
fun BarisPenuh(modifier: Modifier = Modifier, isi: @Composable () -> Unit) {
    Row(modifier = modifier.fillMaxWidth()) { isi() }
}
