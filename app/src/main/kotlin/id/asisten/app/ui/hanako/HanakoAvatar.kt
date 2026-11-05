package id.asisten.app.ui.hanako

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import id.asisten.app.R
import id.asisten.app.domain.hanako.EkspresiHanako

@DrawableRes
private fun gambar(ekspresi: EkspresiHanako): Int = when (ekspresi) {
    EkspresiHanako.NORMAL -> R.drawable.hanako_normal
    EkspresiHanako.SENYUM -> R.drawable.hanako_senyum
    EkspresiHanako.TERTAWA -> R.drawable.hanako_tertawa
    EkspresiHanako.MALU -> R.drawable.hanako_malu
    EkspresiHanako.SEDIH -> R.drawable.hanako_sedih
    EkspresiHanako.MARAH -> R.drawable.hanako_marah
    EkspresiHanako.BINGUNG -> R.drawable.hanako_bingung
    EkspresiHanako.TERKEJUT -> R.drawable.hanako_terkejut
    EkspresiHanako.CEMBERUT -> R.drawable.hanako_cemberut
    EkspresiHanako.MENGANTUK -> R.drawable.hanako_mengantuk
    EkspresiHanako.BERPIKIR -> R.drawable.hanako_berpikir
    EkspresiHanako.PANIK -> R.drawable.hanako_panik
}

/** Width/height of the cropped expression images (123 x 135 px). */
private const val RASIO = 123f / 135f

/**
 * Hanako's face with the given expression. [lebar] is the width; the height follows the image proportions.
 * The source images are small (about 123 px wide), so keep [lebar] at or below roughly 100 dp.
 */
@Composable
fun HanakoAvatar(
    ekspresi: EkspresiHanako,
    modifier: Modifier = Modifier,
    lebar: Dp = 72.dp,
) {
    Image(
        painter = painterResource(gambar(ekspresi)),
        contentDescription = "Hanako, ekspresi ${ekspresi.label.lowercase()}",
        contentScale = ContentScale.Fit,
        modifier = modifier
            .width(lebar)
            .aspectRatio(RASIO)
            .clip(RoundedCornerShape(16.dp)),
    )
}
