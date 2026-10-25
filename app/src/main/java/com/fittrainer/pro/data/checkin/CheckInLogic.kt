package com.fittrainer.pro.data.checkin

import com.fittrainer.pro.data.local.AulaAluno
import com.fittrainer.pro.data.local.AulaEntity
import com.fittrainer.pro.data.local.JsonStore
import com.fittrainer.pro.data.local.TransacaoEntity

data class CheckInPedido(
    val aulaId: String,
    val aluno: AulaAluno,
    val realizar: Boolean,
    val valor: Double?
)

data class CheckInAplicacao(
    val aula: AulaEntity,
    val transacaoUpsert: TransacaoEntity?,
    val transacaoDeleteId: String?
)

object CheckInLogic {
    fun abrirPedido(
        atual: CheckInPedido?,
        aulaId: String,
        aluno: AulaAluno,
        realizar: Boolean,
        valor: Double?
    ): CheckInPedido = atual ?: CheckInPedido(aulaId, aluno, realizar, valor)

    fun aplicar(
        aula: AulaEntity,
        aluno: AulaAluno,
        realizado: Boolean,
        valorAula: Double?,
        horario: String?,
        agoraIso: String,
        transacaoExistente: TransacaoEntity?,
        novoId: String,
        dataFormatada: String
    ): CheckInAplicacao? {
        val alunos = JsonStore.decodeAlunos(aula.alunosJson)
        if (alunos.none { it.clienteId == aluno.clienteId }) return null
        val atualizados = alunos.map { atual ->
            if (atual.clienteId != aluno.clienteId) atual
            else atual.copy(
                checkInRealizado = realizado,
                checkInHorario = horario,
                valorAulaRegistrado = if (realizado) valorAula else null
            )
        }
        val aulaAtualizada = aula.copy(
            alunosJson = JsonStore.encodeAlunos(atualizados),
            updatedDate = agoraIso
        )
        return if (realizado) {
            CheckInAplicacao(
                aula = aulaAtualizada,
                transacaoUpsert = TransacaoEntity(
                    id = transacaoExistente?.id ?: novoId,
                    clienteId = aluno.clienteId,
                    clienteNome = aluno.clienteNome,
                    tipoTransacao = "aula_realizada",
                    dataTransacao = transacaoExistente?.dataTransacao ?: "${aula.data}T${aula.horario}",
                    valor = valorAula ?: 0.0,
                    referencia = "Aula realizada - $dataFormatada às ${aula.horario}",
                    aulaId = aula.id,
                    createdDate = transacaoExistente?.createdDate ?: agoraIso,
                    updatedDate = agoraIso
                ),
                transacaoDeleteId = null
            )
        } else {
            CheckInAplicacao(
                aula = aulaAtualizada,
                transacaoUpsert = null,
                transacaoDeleteId = transacaoExistente?.id
            )
        }
    }
}
