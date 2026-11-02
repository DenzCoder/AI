package id.asisten.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import id.asisten.app.domain.pengaturan.ModeTema
import id.asisten.app.ui.navigation.AsistenApp
import id.asisten.app.ui.pengaturan.PengaturanViewModel
import id.asisten.app.ui.theme.AsistenTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val pengaturanViewModel: PengaturanViewModel = viewModel()
            val pengaturan by pengaturanViewModel.pengaturan.collectAsStateWithLifecycle()
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
                AsistenApp(pengaturanViewModel = pengaturanViewModel)
            }
        }
    }
}
