package id.asisten.app.ui.komponen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import id.asisten.app.ui.theme.LocalNeo
import id.asisten.app.ui.theme.tintaKontras

/*
 * Neo-brutalist building blocks: flat colour, thick outline, hard (blur-free) offset shadow.
 * Each block reserves room for its own shadow with padding, so nothing is clipped by a scrolling parent.
 */

private val RadiusKartu = 10.dp
private val RadiusTombol = 8.dp
private val RadiusChip = 8.dp
private val GeserChip = 2.dp

/** Draws a solid hard shadow of [warna], [geser] to the right and below the composable (no blur). */
fun Modifier.bayanganNeo(radius: Dp, warna: Color, geser: Dp): Modifier = drawBehind {
    val g = geser.toPx()
    drawRoundRect(
        color = warna,
        topLeft = Offset(g, g),
        size = size,
        cornerRadius = CornerRadius(radius.toPx()),
    )
}

/**
 * A card: [warna] fill, ink outline, hard shadow. [tinta] is the default text colour inside;
 * unless given it is picked from [warna] for readability. With [onClick] the whole card is tappable.
 */
@Composable
fun KartuNeo(
    modifier: Modifier = Modifier,
    warna: Color = MaterialTheme.colorScheme.surface,
    tinta: Color = warna.tintaKontras(),
    onClick: (() -> Unit)? = null,
    isi: @Composable ColumnScope.() -> Unit,
) {
    val neo = LocalNeo.current
    val bentuk = RoundedCornerShape(RadiusKartu)
    Box(
        modifier = modifier.padding(end = neo.geser, bottom = neo.geser),
        propagateMinConstraints = true,
    ) {
        Column(
            modifier = Modifier
                .bayanganNeo(RadiusKartu, neo.bayangan, neo.geser)
                .background(warna, bentuk)
                .border(neo.tebal, neo.garis, bentuk)
                .clip(bentuk)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        ) {
            CompositionLocalProvider(LocalContentColor provides tinta) { isi() }
        }
    }
}

/**
 * The main button. It sits on a hard shadow and, while pressed, slides onto it (shadow gone), which is
 * the feedback. A disabled button turns flat and grey. The text colour follows [warna] for contrast.
 */
@Composable
fun TombolNeo(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    warna: Color = MaterialTheme.colorScheme.primary,
    isi: @Composable RowScope.() -> Unit,
) {
    val neo = LocalNeo.current
    val sumber = remember { MutableInteractionSource() }
    val ditekan by sumber.collectIsPressedAsState()
    val bentuk = RoundedCornerShape(RadiusTombol)
    val timbul = enabled && !ditekan
    val latar = if (enabled) warna else neo.mati
    val tinta = if (enabled) warna.tintaKontras() else neo.garis.copy(alpha = 0.45f)
    val garis = if (enabled) neo.garis else neo.garis.copy(alpha = 0.35f)
    val geser = neo.geser

    Box(
        modifier = modifier.padding(end = geser, bottom = geser),
        propagateMinConstraints = true,
    ) {
        Row(
            modifier = Modifier
                .offset { if (enabled && ditekan) IntOffset(geser.roundToPx(), geser.roundToPx()) else IntOffset.Zero }
                .then(if (timbul) Modifier.bayanganNeo(RadiusTombol, neo.bayangan, geser) else Modifier)
                .background(latar, bentuk)
                .border(neo.tebal, garis, bentuk)
                .clickable(
                    interactionSource = sumber,
                    indication = null,
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick,
                )
                .defaultMinSize(minHeight = 44.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompositionLocalProvider(LocalContentColor provides tinta) {
                ProvideTextStyle(MaterialTheme.typography.labelLarge) { isi() }
            }
        }
    }
}

/** The secondary button: same shape as [TombolNeo] but on the plain surface colour. */
@Composable
fun TombolTepiNeo(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isi: @Composable RowScope.() -> Unit,
) {
    TombolNeo(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        warna = MaterialTheme.colorScheme.surface,
        isi = isi,
    )
}

/** A quiet text-only action: bold, underlined, no box. For small actions such as "Ke hari ini". */
@Composable
fun TombolTeksNeo(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isi: @Composable RowScope.() -> Unit,
) {
    val garis = LocalNeo.current.garis
    val tinta = if (enabled) MaterialTheme.colorScheme.onSurface else garis.copy(alpha = 0.4f)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .defaultMinSize(minHeight = 40.dp)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalContentColor provides tinta) {
            ProvideTextStyle(MaterialTheme.typography.labelLarge.copy(textDecoration = TextDecoration.Underline)) {
                isi()
            }
        }
    }
}

/**
 * A pill-ish choice chip. The chosen one is filled with the accent colour and lifted on a small shadow.
 * [terpilih] = null makes it a plain action chip (for example a suggestion) instead of a single choice.
 */
@Composable
fun ChipNeo(
    terpilih: Boolean?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isi: @Composable RowScope.() -> Unit,
) {
    val neo = LocalNeo.current
    val bentuk = RoundedCornerShape(RadiusChip)
    val dipilih = terpilih == true
    val latar = if (dipilih) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val tinta = latar.tintaKontras()
    Box(
        modifier = modifier.padding(end = GeserChip, bottom = GeserChip),
        propagateMinConstraints = true,
    ) {
        Row(
            modifier = Modifier
                .then(if (dipilih) Modifier.bayanganNeo(RadiusChip, neo.bayangan, GeserChip) else Modifier)
                .background(latar, bentuk)
                .border(neo.tebal, neo.garis, bentuk)
                .clip(bentuk)
                .then(
                    if (terpilih == null) {
                        Modifier.clickable(role = Role.Button, onClick = onClick)
                    } else {
                        Modifier.selectable(selected = terpilih, role = Role.RadioButton, onClick = onClick)
                    },
                )
                .defaultMinSize(minHeight = 38.dp)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompositionLocalProvider(LocalContentColor provides tinta) {
                ProvideTextStyle(MaterialTheme.typography.labelLarge) { isi() }
            }
        }
    }
}

/** Equal-width tabs made of [ChipNeo]s; replaces the Material tab row. */
@Composable
fun TabNeo(
    daftar: List<String>,
    terpilih: Int,
    onPilih: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        daftar.forEachIndexed { indeks, nama ->
            ChipNeo(
                terpilih = indeks == terpilih,
                onClick = { onPilih(indeks) },
                modifier = Modifier.weight(1f),
            ) {
                Text(nama, maxLines = 1)
            }
        }
    }
}

/** A thick outlined progress bar, [progres] from 0 to 1 (values outside are clamped). */
@Composable
fun BilahProgresNeo(progres: Float, modifier: Modifier = Modifier) {
    val neo = LocalNeo.current
    val bentuk = RoundedCornerShape(6.dp)
    val nilai = progres.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .height(16.dp)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(nilai, 0f..1f) }
            .background(MaterialTheme.colorScheme.surface, bentuk)
            .border(neo.tebal, neo.garis, bentuk)
            .clip(bentuk),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(nilai)
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}
