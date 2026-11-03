package id.asisten.app.ui.memori

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.asisten.app.data.repo.MemoriRepository
import id.asisten.app.domain.memori.ItemMemori
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MemoriViewModel(private val repo: MemoriRepository) : ViewModel() {

    val daftar: StateFlow<List<ItemMemori>> =
        repo.semua.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun simpan(item: ItemMemori) {
        viewModelScope.launch { repo.simpan(item) }
    }

    fun hapus(id: Long) {
        viewModelScope.launch { repo.hapus(id) }
    }

    fun hapusSemua() {
        viewModelScope.launch { repo.hapusSemua() }
    }
}
