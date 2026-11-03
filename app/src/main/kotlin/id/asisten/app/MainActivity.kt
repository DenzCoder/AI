package id.asisten.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.asisten.app.domain.pengaturan.ModeTema
import id.asisten.app.domain.pengaturan.Pengaturan
import id.asisten.app.ui.navigation.AsistenApp
import id.asisten.app.ui.onboarding.OnboardingScreen
import id.asisten.app.ui.pengaturan.PengaturanViewModel
import id.asisten.app.ui.theme.AsistenTheme
import id.asisten.app.ui.wadahViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val pengaturanViewModel = wadahViewModel { PengaturanViewModel(it.pengaturan) }
            val pengaturanTersimpan by pengaturanViewModel.pengaturan.collectAsStateWithLifecycle()
            val profil by pengaturanViewModel.profil.collectAsStateWithLifecycle()

            val pengaturan = pengaturanTersimpan ?: Pengaturan()
            val sistemGelap = isSystemInDarkTheme()
            val gelap = when (pengaturan.modeTema) {
                ModeTema.SISTEM -> sistemGelap
                ModeTema.TERANG -> false
                ModeTema.GELAP -> true
            }

            // Keep status/navigation bar icon contrast correct when the in-app theme
            // differs from the system theme.
            DisposableEffect(gelap) {
                val gaya = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { gelap }
                enableEdgeToEdge(statusBarStyle = gaya, navigationBarStyle = gaya)
                onDispose { }
            }

            AsistenTheme(pengaturan = pengaturan, darkTheme = gelap) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val profilSaatIni = profil
                    when {
                        // Still reading DataStore: show nothing rather than flashing defaults
                        // (wrong theme, or onboarding for a returning user).
                        pengaturanTersimpan == null || profilSaatIni == null -> Unit

                        !profilSaatIni.onboardingSelesai -> OnboardingScreen(
                            pengaturan = pengaturan,
                            onModeTema = pengaturanViewModel::ubahModeTema,
                            onAksen = pengaturanViewModel::ubahAksen,
                            onSelesai = pengaturanViewModel::selesaikanOnboarding,
                        )

                        else -> AsistenApp(pengaturanViewModel = pengaturanViewModel)
                    }
                }
            }
        }
    }
}
