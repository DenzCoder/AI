package id.asisten.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import id.asisten.app.ui.navigation.AsistenApp
import id.asisten.app.ui.theme.AsistenTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AsistenTheme {
                AsistenApp()
            }
        }
    }
}
