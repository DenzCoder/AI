package id.asisten.app.ui.komponen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import id.asisten.app.ui.theme.LocalNeo
import id.asisten.app.ui.theme.tintaKontras

/** The page title as a yellow sticker: ink outline, hard shadow, heavy type. */
@Composable
fun JudulHalaman(teks: String, modifier: Modifier = Modifier) {
    val neo = LocalNeo.current
    val warna = MaterialTheme.colorScheme.secondary
    val bentuk = RoundedCornerShape(8.dp)
    Box(modifier = modifier.padding(end = 4.dp, bottom = 4.dp)) {
        Text(
            text = teks,
            style = MaterialTheme.typography.headlineSmall,
            color = warna.tintaKontras(),
            modifier = Modifier
                .bayanganNeo(8.dp, neo.bayangan, 4.dp)
                .background(warna, bentuk)
                .border(3.dp, neo.garis, bentuk)
                .padding(horizontal = 14.dp, vertical = 6.dp),
        )
    }
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
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        opsi.forEach { pilihan ->
            ChipNeo(terpilih = pilihan == terpilih, onClick = { onPilih(pilihan) }) {
                Text(label(pilihan))
            }
        }
    }
}

/** Text field colours for the neo look: white box, ink outline (also while focused). */
@Composable
fun warnaFieldNeo(): TextFieldColors {
    val skema = MaterialTheme.colorScheme
    return OutlinedTextFieldDefaults.colors(
        focusedContainerColor = skema.surface,
        unfocusedContainerColor = skema.surface,
        focusedBorderColor = skema.onSurface,
        unfocusedBorderColor = skema.onSurface,
        focusedLabelColor = skema.onSurface,
        unfocusedLabelColor = skema.onSurfaceVariant,
        cursorColor = skema.onSurface,
    )
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
        colors = warnaFieldNeo(),
        modifier = modifier.fillMaxWidth(),
    )
}

/**
 * The shared dialog frame: a neo card with the title, the scrolling content and a row of buttons.
 * The content scrolls so long forms still fit on small screens (the buttons stay in view).
 */
@Composable
private fun KerangkaDialog(
    judul: String,
    onTutup: () -> Unit,
    tombol: @Composable () -> Unit,
    isi: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = onTutup) {
        KartuNeo(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(judul, style = MaterialTheme.typography.titleLarge)
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    isi()
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                ) {
                    tombol()
                }
            }
        }
    }
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
    KerangkaDialog(
        judul = judul,
        onTutup = onBatal,
        tombol = {
            TombolTepiNeo(onClick = onBatal) { Text("Batal") }
            TombolNeo(onClick = onSimpan) { Text(labelSimpan) }
        },
        isi = isi,
    )
}

/** Explicit confirmation before deleting a record. The confirm button is red because the default action is a delete. */
@Composable
fun DialogKonfirmasi(
    judul: String,
    pesan: String,
    onYa: () -> Unit,
    onBatal: () -> Unit,
    labelYa: String = "Hapus",
) {
    KerangkaDialog(
        judul = judul,
        onTutup = onBatal,
        tombol = {
            TombolTepiNeo(onClick = onBatal) { Text("Batal") }
            TombolNeo(onClick = onYa, warna = MaterialTheme.colorScheme.error) { Text(labelYa) }
        },
        isi = { Text(pesan) },
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
