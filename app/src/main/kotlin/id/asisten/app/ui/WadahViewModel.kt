package id.asisten.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import id.asisten.app.AsistenAplikasi
import id.asisten.app.WadahAplikasi

/** Creates (or reuses) a ViewModel whose constructor needs the app's repositories. */
@Composable
inline fun <reified VM : ViewModel> wadahViewModel(crossinline buat: (WadahAplikasi) -> VM): VM {
    val wadah = (LocalContext.current.applicationContext as AsistenAplikasi).wadah
    return viewModel<VM>(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = buat(wadah) as T
        },
    )
}
