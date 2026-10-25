package com.fittrainer.pro.data.repository

import androidx.room.withTransaction
import com.fittrainer.pro.data.local.AppDatabase
import com.fittrainer.pro.data.local.AulaAluno
import com.fittrainer.pro.data.local.AulaEntity
import com.fittrainer.pro.data.local.AvaliacaoEntity
import com.fittrainer.pro.data.local.ClienteEntity
import com.fittrainer.pro.data.local.ExercicioBibliotecaEntity
import com.fittrainer.pro.data.local.HistoricoTreinoEntity
import com.fittrainer.pro.data.local.JsonStore
import com.fittrainer.pro.data.local.ProgressoEntity
import com.fittrainer.pro.data.local.TransacaoEntity
import com.fittrainer.pro.data.local.TreinoEntity
import com.fittrainer.pro.util.Time
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
data class BackupPayload(
    val format: String = "fittrainer-pro-backup",
    val version: Int = 1,
    val exportedAt: String = Time.nowIso(),
    val clientes: List<ClienteEntity> = emptyList(),
    val treinos: List<TreinoEntity> = emptyList(),
    val aulas: List<AulaEntity> = emptyList(),
    val avaliacoes: List<AvaliacaoEntity> = emptyList(),
    val progresso: List<ProgressoEntity> = emptyList(),
    val historicoTreinos: List<HistoricoTreinoEntity> = emptyList(),
    val transacoes: List<TransacaoEntity> = emptyList(),
    val exerciciosBiblioteca: List<ExercicioBibliotecaEntity> = emptyList()
)

class FitTrainerRepository(private val db: AppDatabase) {
    val clientes: Flow<List<ClienteEntity>> = db.clienteDao().observeAll()
    val treinos: Flow<List<TreinoEntity>> = db.treinoDao().observeAll()
    val aulas: Flow<List<AulaEntity>> = db.aulaDao().observeAll()
    val avaliacoes: Flow<List<AvaliacaoEntity>> = db.avaliacaoDao().observeAll()
    val progresso: Flow<List<ProgressoEntity>> = db.progressoDao().observeAll()
    val transacoes: Flow<List<TransacaoEntity>> = db.transacaoDao().observeAll()
    val historico: Flow<List<HistoricoTreinoEntity>> = db.historicoDao().observeAll()

    suspend fun cliente(id: String) = db.clienteDao().getById(id)
    suspend fun treino(id: String) = db.treinoDao().getById(id)
    suspend fun aula(id: String) = db.aulaDao().getById(id)

    suspend fun saveCliente(entity: ClienteEntity) = db.clienteDao().upsert(entity)
    suspend fun saveTreino(entity: TreinoEntity) = db.treinoDao().upsert(entity)
    suspend fun saveAula(entity: AulaEntity) = db.aulaDao().upsert(entity)
    suspend fun saveAvaliacao(entity: AvaliacaoEntity) = db.avaliacaoDao().upsert(entity)
    suspend fun saveProgresso(entity: ProgressoEntity) = db.progressoDao().upsert(entity)
    suspend fun saveTransacao(entity: TransacaoEntity) = db.transacaoDao().upsert(entity)
    suspend fun saveHistorico(entity: HistoricoTreinoEntity) = db.historicoDao().upsert(entity)

    suspend fun deleteCliente(id: String) {
        db.treinoDao().deleteByCliente(id)
        db.avaliacaoDao().deleteByCliente(id)
        db.progressoDao().deleteByCliente(id)
        db.historicoDao().deleteByCliente(id)
        db.transacaoDao().deleteByCliente(id)
        db.clienteDao().deleteById(id)
    }

    suspend fun deleteTreino(id: String) = db.treinoDao().deleteById(id)
    suspend fun deleteAula(id: String) = db.aulaDao().deleteById(id)
    suspend fun deleteAvaliacao(id: String) = db.avaliacaoDao().deleteById(id)
    suspend fun deleteProgresso(id: String) = db.progressoDao().deleteById(id)
    suspend fun deleteTransacao(id: String) = db.transacaoDao().deleteById(id)

    suspend fun checkIn(aula: AulaEntity, aluno: AulaAluno, valorAula: Double?) {
        val horario = java.time.LocalTime.now().toString().take(5)
        val alunos = JsonStore.decodeAlunos(aula.alunosJson).map {
            if (it.clienteId == aluno.clienteId) {
                it.copy(
                    checkInRealizado = true,
                    checkInHorario = horario,
                    valorAulaRegistrado = valorAula
                )
            } else it
        }
        saveAula(aula.copy(alunosJson = JsonStore.encodeAlunos(alunos), updatedDate = Time.nowIso()))
        saveTransacao(
            TransacaoEntity(
                id = Time.newId(),
                clienteId = aluno.clienteId,
                clienteNome = aluno.clienteNome,
                tipoTransacao = "aula_realizada",
                dataTransacao = "${aula.data}T${aula.horario}",
                valor = valorAula ?: 0.0,
                referencia = "Aula realizada - ${Time.formatDate(aula.data)} às ${aula.horario}",
                aulaId = aula.id,
                createdDate = Time.nowIso(),
                updatedDate = Time.nowIso()
            )
        )
    }

    suspend fun exportBackup(): BackupPayload = BackupPayload(
        exportedAt = Time.nowIso(),
        clientes = db.clienteDao().getAll(),
        treinos = db.treinoDao().getAll(),
        aulas = db.aulaDao().getAll(),
        avaliacoes = db.avaliacaoDao().getAll(),
        progresso = db.progressoDao().getAll(),
        historicoTreinos = db.historicoDao().getAll(),
        transacoes = db.transacaoDao().getAll(),
        exerciciosBiblioteca = db.exercicioBibliotecaDao().getAll()
    )

    suspend fun importBackup(payload: BackupPayload, replace: Boolean) {
        if (payload.format != "fittrainer-pro-backup") {
            error("Arquivo de backup inválido")
        }
        if (payload.version > 1) {
            error("Este backup é de uma versão mais nova do app")
        }
        db.withTransaction {
            if (replace) {
                db.historicoDao().deleteAll()
                db.transacaoDao().deleteAll()
                db.progressoDao().deleteAll()
                db.avaliacaoDao().deleteAll()
                db.aulaDao().deleteAll()
                db.treinoDao().deleteAll()
                db.exercicioBibliotecaDao().deleteAll()
                db.clienteDao().deleteAll()
            }
            db.clienteDao().upsertAll(payload.clientes)
            db.treinoDao().upsertAll(payload.treinos)
            db.aulaDao().upsertAll(payload.aulas)
            db.avaliacaoDao().upsertAll(payload.avaliacoes)
            db.progressoDao().upsertAll(payload.progresso)
            db.historicoDao().upsertAll(payload.historicoTreinos)
            db.transacaoDao().upsertAll(payload.transacoes)
            db.exercicioBibliotecaDao().upsertAll(payload.exerciciosBiblioteca)
        }
    }
}
