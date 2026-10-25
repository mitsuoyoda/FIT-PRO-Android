package com.fittrainer.pro.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "clientes")
@Serializable
data class ClienteEntity(
    @PrimaryKey val id: String,
    val nome: String,
    val email: String = "",
    val telefone: String = "",
    val dataNascimento: String = "",
    val sexo: String = "masculino",
    val altura: Double? = null,
    val pesoAtual: Double? = null,
    val objetivo: String = "",
    val observacoes: String = "",
    val status: String = "ativo",
    val valorAula: Double? = null,
    val localAulaPadrao: String = "",
    val createdDate: String,
    val updatedDate: String
)

@Entity(tableName = "treinos")
@Serializable
data class TreinoEntity(
    @PrimaryKey val id: String,
    val titulo: String,
    val descricao: String = "",
    val clienteId: String = "",
    val clienteNome: String = "",
    val tipo: String = "forca",
    val duracaoEstimada: Int = 60,
    val exerciciosJson: String = "[]",
    val dataEnvio: String = "",
    val createdDate: String,
    val updatedDate: String
)

@Entity(tableName = "aulas")
@Serializable
data class AulaEntity(
    @PrimaryKey val id: String,
    val data: String,
    val horario: String,
    val tipoAula: String = "presencial",
    val alunosJson: String = "[]",
    val status: String = "agendada",
    val duracaoMinutos: Int = 60,
    val local: String = "",
    val linkOnline: String = "",
    val observacoes: String = "",
    val createdDate: String,
    val updatedDate: String
)

@Entity(tableName = "avaliacoes")
@Serializable
data class AvaliacaoEntity(
    @PrimaryKey val id: String,
    val clienteId: String,
    val clienteNome: String = "",
    val dataAvaliacao: String,
    val sexo: String = "M",
    val idade: Int? = null,
    val peso: Double? = null,
    val altura: Double? = null,
    val percentualGordura: Double? = null,
    val medidasJson: String = "{}",
    val observacoes: String = "",
    val createdDate: String,
    val updatedDate: String
)

@Entity(tableName = "progresso_cliente")
@Serializable
data class ProgressoEntity(
    @PrimaryKey val id: String,
    val clienteId: String,
    val dataRegistro: String,
    val peso: Double? = null,
    val percentualGordura: Double? = null,
    val observacoes: String = "",
    val createdDate: String,
    val updatedDate: String
)

@Entity(tableName = "historico_treinos")
@Serializable
data class HistoricoTreinoEntity(
    @PrimaryKey val id: String,
    val clienteId: String,
    val clienteNome: String = "",
    val treinoId: String? = null,
    val treinoTitulo: String = "",
    val dataExecucao: String,
    val exerciciosJson: String = "[]",
    val observacoes: String = "",
    val createdDate: String,
    val updatedDate: String
)

@Entity(tableName = "transacoes_financeiras")
@Serializable
data class TransacaoEntity(
    @PrimaryKey val id: String,
    val clienteId: String = "",
    val clienteNome: String = "",
    val tipoTransacao: String,
    val dataTransacao: String,
    val valor: Double,
    val referencia: String = "",
    val aulaId: String? = null,
    val metodoPagamento: String = "",
    val createdDate: String,
    val updatedDate: String
)

@Entity(tableName = "exercicios_biblioteca")
@Serializable
data class ExercicioBibliotecaEntity(
    @PrimaryKey val id: String,
    val nome: String,
    val grupoMuscular: String = "",
    val descricao: String = "",
    val createdDate: String,
    val updatedDate: String
)
