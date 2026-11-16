package com.fittrainer.pro.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fittrainer.pro.data.local.AulaAluno
import com.fittrainer.pro.data.local.AulaEntity
import com.fittrainer.pro.data.local.AvaliacaoEntity
import com.fittrainer.pro.data.local.ClienteEntity
import com.fittrainer.pro.data.local.ExercicioBibliotecaEntity
import com.fittrainer.pro.data.local.HistoricoTreinoEntity
import com.fittrainer.pro.data.local.PlanoAssessoriaEntity
import com.fittrainer.pro.data.local.ProgressoEntity
import com.fittrainer.pro.data.local.TransacaoEntity
import com.fittrainer.pro.data.local.TreinoEntity
import com.fittrainer.pro.data.repository.FitTrainerRepository
import com.fittrainer.pro.data.firebase.TrainerCloudBackup
import com.fittrainer.pro.data.firebase.TrainerFirebaseSync
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(private val repository: FitTrainerRepository) : ViewModel() {
    private val _cloudBackupStatus = MutableStateFlow("Backup na nuvem: entre na conta do treinador para ativar.")
    val cloudBackupStatus: StateFlow<String> = _cloudBackupStatus
    private var cloudBackupRunning = false
    private var cloudBackupRequestedAgain = false
    private var cloudContext: Context? = null
    val clientes = repository.clientes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val treinos = repository.treinos.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val aulas = repository.aulas.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val avaliacoes = repository.avaliacoes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val progresso = repository.progresso.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val transacoes = repository.transacoes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val historico = repository.historico.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val exerciciosBiblioteca = repository.exerciciosBiblioteca.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val planosAssessoria = repository.planosAssessoria.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun syncCloudBackup(context: Context) {
        cloudContext = context.applicationContext
        if (!TrainerFirebaseSync.isSignedIn()) return
        if (cloudBackupRunning) {
            cloudBackupRequestedAgain = true
            return
        }
        cloudBackupRunning = true
        _cloudBackupStatus.value = "Sincronizando backup seguro com o Firebase…"
        viewModelScope.launch {
            try {
                val result = TrainerCloudBackup.sync(context.applicationContext, repository)
                _cloudBackupStatus.value = when {
                    result.waitingForNetwork && result.uploaded > 0 -> "Backup aguardando conexão. ${result.uploaded} registro(s) ficaram na fila do Firebase."
                    result.waitingForNetwork -> "Sem conexão com o Firebase. Os dados permanecem no aparelho e serão mesclados quando a rede voltar."
                    result.uploaded > 0 || result.restored > 0 -> "Backup atualizado: ${result.uploaded} registro(s) enviados e ${result.restored} recuperado(s)."
                    else -> "Backup em dia. Nenhuma alteração nova."
                }
            } catch (error: Exception) {
                _cloudBackupStatus.value = "Backup não sincronizado: ${error.message ?: "verifique a conexão"}"
            } finally {
                cloudBackupRunning = false
                if (cloudBackupRequestedAgain) {
                    cloudBackupRequestedAgain = false
                    cloudContext?.let(::syncCloudBackup)
                }
            }
        }
    }

    private fun syncAfterLocalChange() {
        cloudContext?.let(::syncCloudBackup)
    }

    var assessoriaPendente: AssessoriaSlot? = null
        private set

    fun marcarSlotAssessoria(planoId: String, semana: Int, dia: Int) {
        assessoriaPendente = AssessoriaSlot(planoId, semana, dia)
    }

    fun prepararTreinoNaAssessoria(plano: PlanoAssessoriaEntity, semana: Int, dia: Int, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.savePlanoAssessoria(plano)
            assessoriaPendente = AssessoriaSlot(plano.id, semana, dia)
            syncAfterLocalChange()
            onDone()
        }
    }

    fun limparSlotAssessoria() {
        assessoriaPendente = null
    }

    fun saveCliente(entity: ClienteEntity) = viewModelScope.launch { repository.saveCliente(entity); syncAfterLocalChange() }
    fun saveTreino(entity: TreinoEntity, onDone: () -> Unit = {}) = viewModelScope.launch {
        repository.saveTreino(entity)
        assessoriaPendente?.let { slot ->
            repository.vincularTreinoAoPlano(slot.planoId, slot.semana, slot.dia, entity.id)
            assessoriaPendente = null
        }
        syncAfterLocalChange()
        onDone()
    }

    fun savePlanoAssessoria(entity: PlanoAssessoriaEntity) = viewModelScope.launch { repository.savePlanoAssessoria(entity); syncAfterLocalChange() }
    fun saveAula(entity: AulaEntity) = viewModelScope.launch { repository.saveAula(entity); syncAfterLocalChange() }
    fun saveAvaliacao(entity: AvaliacaoEntity) = viewModelScope.launch { repository.saveAvaliacao(entity); syncAfterLocalChange() }
    fun saveProgresso(entity: ProgressoEntity) = viewModelScope.launch { repository.saveProgresso(entity); syncAfterLocalChange() }
    fun saveTransacao(entity: TransacaoEntity) = viewModelScope.launch { repository.saveTransacao(entity); syncAfterLocalChange() }
    fun saveHistorico(entity: HistoricoTreinoEntity) = viewModelScope.launch { repository.saveHistorico(entity); syncAfterLocalChange() }
    fun saveExercicioBiblioteca(entity: ExercicioBibliotecaEntity) = viewModelScope.launch { repository.saveExercicioBiblioteca(entity); syncAfterLocalChange() }

    fun deleteCliente(id: String) = viewModelScope.launch { repository.deleteCliente(id); syncAfterLocalChange() }
    fun deleteTreino(id: String) = viewModelScope.launch { repository.deleteTreino(id); syncAfterLocalChange() }
    fun deleteAula(id: String) = viewModelScope.launch { repository.deleteAula(id); syncAfterLocalChange() }
    fun deleteAvaliacao(id: String) = viewModelScope.launch { repository.deleteAvaliacao(id); syncAfterLocalChange() }
    fun deleteProgresso(id: String) = viewModelScope.launch { repository.deleteProgresso(id); syncAfterLocalChange() }
    fun deleteTransacao(id: String) = viewModelScope.launch { repository.deleteTransacao(id); syncAfterLocalChange() }
    fun deleteExercicioBiblioteca(id: String) = viewModelScope.launch { repository.deleteExercicioBiblioteca(id); syncAfterLocalChange() }
    fun deletePlanoAssessoria(id: String) = viewModelScope.launch { repository.deletePlanoAssessoria(id); syncAfterLocalChange() }

    fun checkIn(aula: AulaEntity, aluno: AulaAluno, valor: Double?) =
        viewModelScope.launch { repository.checkIn(aula, aluno, valor); syncAfterLocalChange() }

    fun setCheckIn(aulaId: String, aluno: AulaAluno, realizado: Boolean, valor: Double?) =
        viewModelScope.launch { repository.setCheckIn(aulaId, aluno, realizado, valor); syncAfterLocalChange() }

    data class AssessoriaSlot(val planoId: String, val semana: Int, val dia: Int)

    companion object {
        fun factory(repository: FitTrainerRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AppViewModel(repository) as T
            }
        }
    }
}
