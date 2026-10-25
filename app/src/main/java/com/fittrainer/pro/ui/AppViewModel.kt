package com.fittrainer.pro.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fittrainer.pro.data.local.AulaAluno
import com.fittrainer.pro.data.local.AulaEntity
import com.fittrainer.pro.data.local.AvaliacaoEntity
import com.fittrainer.pro.data.local.ClienteEntity
import com.fittrainer.pro.data.local.HistoricoTreinoEntity
import com.fittrainer.pro.data.local.ProgressoEntity
import com.fittrainer.pro.data.local.TransacaoEntity
import com.fittrainer.pro.data.local.TreinoEntity
import com.fittrainer.pro.data.repository.FitTrainerRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(private val repository: FitTrainerRepository) : ViewModel() {
    val clientes = repository.clientes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val treinos = repository.treinos.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val aulas = repository.aulas.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val avaliacoes = repository.avaliacoes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val progresso = repository.progresso.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val transacoes = repository.transacoes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val historico = repository.historico.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun saveCliente(entity: ClienteEntity) = viewModelScope.launch { repository.saveCliente(entity) }
    fun saveTreino(entity: TreinoEntity) = viewModelScope.launch { repository.saveTreino(entity) }
    fun saveAula(entity: AulaEntity) = viewModelScope.launch { repository.saveAula(entity) }
    fun saveAvaliacao(entity: AvaliacaoEntity) = viewModelScope.launch { repository.saveAvaliacao(entity) }
    fun saveProgresso(entity: ProgressoEntity) = viewModelScope.launch { repository.saveProgresso(entity) }
    fun saveTransacao(entity: TransacaoEntity) = viewModelScope.launch { repository.saveTransacao(entity) }
    fun saveHistorico(entity: HistoricoTreinoEntity) = viewModelScope.launch { repository.saveHistorico(entity) }

    fun deleteCliente(id: String) = viewModelScope.launch { repository.deleteCliente(id) }
    fun deleteTreino(id: String) = viewModelScope.launch { repository.deleteTreino(id) }
    fun deleteAula(id: String) = viewModelScope.launch { repository.deleteAula(id) }
    fun deleteAvaliacao(id: String) = viewModelScope.launch { repository.deleteAvaliacao(id) }
    fun deleteProgresso(id: String) = viewModelScope.launch { repository.deleteProgresso(id) }
    fun deleteTransacao(id: String) = viewModelScope.launch { repository.deleteTransacao(id) }

    fun checkIn(aula: AulaEntity, aluno: AulaAluno, valor: Double?) =
        viewModelScope.launch { repository.checkIn(aula, aluno, valor) }

    companion object {
        fun factory(repository: FitTrainerRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AppViewModel(repository) as T
            }
        }
    }
}
