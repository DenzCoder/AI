package id.asisten.app.domain

import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class SapaanProviderTest {

    @Test
    fun pagiDimulaiJam0400() {
        assertEquals(Sapaan.PAGI, SapaanProvider.untuk(LocalTime.of(4, 0)))
        assertEquals(Sapaan.PAGI, SapaanProvider.untuk(LocalTime.of(10, 59)))
    }

    @Test
    fun siangJam1100Sampai1459() {
        assertEquals(Sapaan.SIANG, SapaanProvider.untuk(LocalTime.of(11, 0)))
        assertEquals(Sapaan.SIANG, SapaanProvider.untuk(LocalTime.of(14, 59)))
    }

    @Test
    fun soreJam1500Sampai1759() {
        assertEquals(Sapaan.SORE, SapaanProvider.untuk(LocalTime.of(15, 0)))
        assertEquals(Sapaan.SORE, SapaanProvider.untuk(LocalTime.of(17, 59)))
    }

    @Test
    fun malamMencakupTengahMalamDanDiniHari() {
        assertEquals(Sapaan.MALAM, SapaanProvider.untuk(LocalTime.of(18, 0)))
        assertEquals(Sapaan.MALAM, SapaanProvider.untuk(LocalTime.of(0, 0)))
        assertEquals(Sapaan.MALAM, SapaanProvider.untuk(LocalTime.of(3, 59)))
    }
}
