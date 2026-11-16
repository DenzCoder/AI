package id.asisten.app.ui.hanako

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.asisten.app.R
import id.asisten.app.domain.hanako.EkspresiHanako
import id.asisten.app.domain.hanako.JenisGalat
import id.asisten.app.domain.hanako.LabelHanako
import id.asisten.app.domain.hanako.PeranChat
import id.asisten.app.domain.hanako.PesanChat
import id.asisten.app.domain.hanako.PilihEkspresi
import id.asisten.app.domain.hanako.UsulanTampil
import id.asisten.app.domain.suara.LabelSuara
import id.asisten.app.domain.suara.PenyisipDiktean
import id.asisten.app.domain.suara.StatusSuara
import id.asisten.app.ui.wadahViewModel
import java.time.LocalTime

/**
 * The "AI Hanako" tab: a chat with Hanako, plus her settings (Gemini key, model, what she may see).
 * Without a key she answers from the data on the phone only (PenjawabLokal); with a key she uses Gemini.
 */
@Composable
fun HanakoScreen(modifier: Modifier = Modifier) {
    val vm = wadahViewModel {
        HanakoViewModel(
            it.hanako, it.pengaturan, it.jadwal, it.aktivitas, it.memori, it.klienGemini, it.suara, it.pembaca, it.pelaksanaAksi,
        )
    }
    var tampilPengaturan by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = tampilPengaturan) { tampilPengaturan = false }

    if (tampilPengaturan) {
        PengaturanHanakoScreen(vm = vm, onKembali = { tampilPengaturan = false }, modifier = modifier)
    } else {
        ObrolanHanako(vm = vm, onBukaPengaturan = { tampilPengaturan = true }, modifier = modifier)
    }
}

@Composable
private fun ObrolanHanako(vm: HanakoViewModel, onBukaPengaturan: () -> Unit, modifier: Modifier = Modifier) {
    val riwayat by vm.riwayat.collectAsStateWithLifecycle()
    val pengaturan by vm.pengaturanHanako.collectAsStateWithLifecycle()
    val sedangMenjawab by vm.sedangMenjawab.collectAsStateWithLifecycle()
    val galat by vm.galat.collectAsStateWithLifecycle()
    val sedangBicara by vm.sedangBicara.collectAsStateWithLifecycle()
    val usulan by vm.usulan.collectAsStateWithLifecycle()
    val sedangMelaksanakan by vm.sedangMelaksanakan.collectAsStateWithLifecycle()
    var teks by rememberSaveable { mutableStateOf("") }
    var pesanSuara by rememberSaveable { mutableStateOf<String?>(null) }
    val petunjukDiktean = stringResource(R.string.hanako_diktean_prompt)

    // New replies are read aloud when the user switched that on; speech stops when the screen is left.
    LaunchedEffect(Unit) {
        vm.balasanBaru.collect { pesan -> if (vm.pengaturanSuara.value.bacaOtomatis) vm.bacaKeras(pesan.isi) }
    }
    DisposableEffect(Unit) { onDispose { vm.berhenti() } }
    // The engine reports once whether Indonesian speech works; if it does not, say so under the chat.
    val statusSuara by vm.statusSuara.collectAsStateWithLifecycle()
    LaunchedEffect(statusSuara) {
        val s = statusSuara
        if (s != null && s != StatusSuara.SIAP) pesanSuara = LabelSuara.status(s)
    }

    // The system's own speech dialog does the listening (no microphone permission needed in this app).
    val peluncurDiktean = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { hasil ->
        if (hasil.resultCode == Activity.RESULT_OK) {
            val dengar = PenyisipDiktean.pilihHasil(hasil.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS))
            if (dengar == null) pesanSuara = LabelSuara.DIKTEAN_KOSONG else teks = PenyisipDiktean.gabung(teks, dengar)
        }
    }
    val mulaiDiktean: () -> Unit = {
        vm.berhenti()
        pesanSuara = null
        val maksud = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
            putExtra(RecognizerIntent.EXTRA_PROMPT, petunjukDiktean)
        }
        try {
            peluncurDiktean.launch(maksud)
        } catch (e: ActivityNotFoundException) {
            pesanSuara = LabelSuara.DIKTEAN_TIDAK_ADA
        }
    }

    val online = pengaturan?.online == true
    val ekspresiTerakhir = riwayat.lastOrNull { it.peran == PeranChat.HANAKO && !it.aksi }?.ekspresi
    val ekspresi = PilihEkspresi.untukChat(sedangMenjawab, galat?.jenis, ekspresiTerakhir, LocalTime.now())

    val daftar = rememberLazyListState()
    val jumlahItem = riwayat.size + usulan.size + if (sedangMenjawab) 1 else 0
    LaunchedEffect(jumlahItem) {
        if (jumlahItem > 0) daftar.animateScrollToItem(jumlahItem - 1)
    }

    Column(modifier = modifier.fillMaxSize().imePadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HanakoAvatar(ekspresi = ekspresi, lebar = 56.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.hanako_nama),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = LabelHanako.status(online),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (sedangBicara) {
                IconButton(onClick = { vm.berhenti() }) {
                    Icon(painterResource(R.drawable.ic_berhenti), contentDescription = stringResource(R.string.hanako_berhenti_bicara))
                }
            }
            IconButton(onClick = onBukaPengaturan) {
                Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.hanako_pengaturan_desc))
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (riwayat.isEmpty() && usulan.isEmpty() && !sedangMenjawab) {
                SapaanKosong(onSaran = { vm.kirim(it) })
            } else {
                LazyColumn(
                    state = daftar,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    itemsIndexed(riwayat) { _, pesan -> BalonPesan(pesan, onDengarkan = { vm.bacaKeras(pesan.isi) }) }
                    items(usulan, key = { it.id }) { u ->
                        KartuUsulan(
                            usulan = u,
                            sibuk = sedangMelaksanakan,
                            onSetujui = { vm.setujui(u.id) },
                            onTolak = { vm.tolak(u.id) },
                        )
                    }
                    if (sedangMenjawab) item { BalonMenunggu() }
                }
            }
        }

        galat?.let { g ->
            KartuGalat(
                jenis = g.jenis,
                rinci = g.rinci,
                onCobaLagi = { vm.cobaLagi() },
                onPengaturan = {
                    vm.tutupGalat()
                    onBukaPengaturan()
                },
                onTutup = { vm.tutupGalat() },
            )
        }

        pesanSuara?.let { pesan ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = pesan,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { pesanSuara = null }) { Text(stringResource(R.string.hanako_tutup)) }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = teks,
                onValueChange = { teks = it.take(2000) },
                placeholder = { Text(stringResource(R.string.hanako_input_petunjuk)) },
                maxLines = 4,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = mulaiDiktean, enabled = !sedangMenjawab) {
                Icon(painterResource(R.drawable.ic_mikrofon), contentDescription = stringResource(R.string.hanako_mikrofon))
            }
            IconButton(
                onClick = {
                    vm.kirim(teks)
                    teks = ""
                },
                enabled = teks.isNotBlank() && !sedangMenjawab,
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.hanako_kirim))
            }
        }
    }
}

@Composable
private fun SapaanKosong(onSaran: (String) -> Unit) {
    val saran = listOf(
        stringResource(R.string.hanako_saran_jadwal),
        stringResource(R.string.hanako_saran_tugas),
        stringResource(R.string.hanako_saran_semangat),
    )
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.hanako_kosong_judul),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.hanako_kosong_isi),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            saran.forEach { s ->
                SuggestionChip(onClick = { onSaran(s) }, label = { Text(s) })
            }
        }
    }
}

@Composable
private fun BalonPesan(pesan: PesanChat, onDengarkan: () -> Unit) {
    val dariPengguna = pesan.peran == PeranChat.PENGGUNA
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (dariPengguna) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top,
    ) {
        if (!dariPengguna) {
            HanakoAvatar(ekspresi = pesan.ekspresi ?: EkspresiHanako.NORMAL, lebar = 40.dp)
            Box(modifier = Modifier.size(8.dp))
        }
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = when {
                dariPengguna -> MaterialTheme.colorScheme.primaryContainer
                pesan.aksi -> MaterialTheme.colorScheme.surfaceVariant
                else -> MaterialTheme.colorScheme.secondaryContainer
            },
            modifier = Modifier.widthIn(max = 300.dp),
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                SelectionContainer {
                    Text(text = pesan.isi, style = MaterialTheme.typography.bodyLarge)
                }
                if (pesan.aksi) {
                    Text(
                        text = stringResource(R.string.hanako_label_aksi),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                if (!dariPengguna && !pesan.aksi) {
                    IconButton(onClick = onDengarkan, modifier = Modifier.size(32.dp)) {
                        Icon(
                            painterResource(R.drawable.ic_suara),
                            contentDescription = stringResource(R.string.hanako_dengarkan),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                if (pesan.lokal) {
                    Text(
                        text = stringResource(R.string.hanako_label_lokal),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

/**
 * A change Hanako proposes. Nothing has happened yet: it only runs when the user taps Setujui. Deleting is shown in
 * the error colours and the button says what it does.
 */
@Composable
private fun KartuUsulan(usulan: UsulanTampil, sibuk: Boolean, onSetujui: () -> Unit, onTolak: () -> Unit) {
    val hapus = usulan.aksi.destruktif
    val wadah = if (hapus) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer
    val isi = if (hapus) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = wadah, contentColor = isi)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.hanako_usulan_judul),
                style = MaterialTheme.typography.labelMedium,
                color = isi,
            )
            SelectionContainer {
                Text(text = usulan.ringkas, style = MaterialTheme.typography.bodyLarge, color = isi)
            }
            usulan.peringatan.forEach { w ->
                Text(text = w, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = isi)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onSetujui,
                    enabled = !sibuk,
                    colors = if (hapus) {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        )
                    } else {
                        ButtonDefaults.buttonColors()
                    },
                ) {
                    Text(stringResource(if (hapus) R.string.hanako_usulan_hapus_setujui else R.string.hanako_setujui))
                }
                OutlinedButton(onClick = onTolak, enabled = !sibuk) { Text(stringResource(R.string.hanako_tolak)) }
            }
        }
    }
}

@Composable
private fun BalonMenunggu() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        HanakoAvatar(ekspresi = EkspresiHanako.BERPIKIR, lebar = 40.dp)
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Text(text = stringResource(R.string.hanako_mengetik), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun KartuGalat(
    jenis: JenisGalat,
    rinci: String,
    onCobaLagi: () -> Unit,
    onPengaturan: () -> Unit,
    onTutup: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = LabelHanako.galat(jenis),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            if (rinci.isNotBlank()) {
                Text(
                    text = stringResource(R.string.hanako_rincian, rinci),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (LabelHanako.bisaDiulang(jenis)) {
                    Button(onClick = onCobaLagi) { Text(stringResource(R.string.hanako_coba_lagi)) }
                }
                if (LabelHanako.perluPengaturan(jenis)) {
                    OutlinedButton(onClick = onPengaturan) { Text(stringResource(R.string.hanako_buka_pengaturan)) }
                }
                TextButton(onClick = onTutup) { Text(stringResource(R.string.hanako_tutup)) }
            }
        }
    }
}
