package com.fittrainer.pro.data.export

import com.fittrainer.pro.data.assessoria.DiaPlanoTreino
import com.fittrainer.pro.data.local.AulaEntity
import com.fittrainer.pro.data.local.AvaliacaoEntity
import com.fittrainer.pro.data.local.ClienteEntity
import com.fittrainer.pro.data.local.HistoricoTreinoEntity
import com.fittrainer.pro.data.local.JsonStore
import com.fittrainer.pro.data.local.PlanoAssessoriaEntity
import com.fittrainer.pro.data.local.TransacaoEntity
import com.fittrainer.pro.data.local.TreinoEntity
import com.fittrainer.pro.data.local.WorkoutExercise
import com.fittrainer.pro.util.SafeUrl
import com.fittrainer.pro.util.Time
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.format.DateTimeFormatter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object ClienteExport {
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        explicitNulls = true
        ignoreUnknownKeys = true
    }

    val treinador = TrainerRef(id = "trainer-local", name = "FitTrainer Pro")

    fun perfil(
        cliente: ClienteEntity,
        avaliacoes: List<AvaliacaoEntity>,
        transacoes: List<TransacaoEntity>,
        aulas: List<AulaEntity>,
        historico: List<HistoricoTreinoEntity>
    ): String {
        val exportadoEm = Time.nowIsoOffset()
        val payload = ClientProfileExport(
            exportId = "profile-${cliente.id}-${Time.today()}",
            exportedAt = exportadoEm,
            trainer = treinador,
            clientId = cliente.id,
            client = mapCliente(cliente),
            physicalEvaluations = avaliacoes.filter { it.clienteId == cliente.id }
                .sortedByDescending { it.dataAvaliacao }
                .map(::mapAvaliacao),
            financialSummary = mapFinanceiro(cliente.id, transacoes),
            appointments = aulas.mapNotNull { aula -> mapAula(aula, cliente.id) }
                .sortedByDescending { it.date + it.time },
            workoutHistory = historico.filter { it.clienteId == cliente.id }
                .sortedByDescending { it.dataExecucao }
                .map(::mapHistorico)
        )
        return json.encodeToString(payload)
    }

    fun planoSemanal(
        plano: PlanoAssessoriaEntity,
        clienteId: String,
        semana: Int,
        dias: List<DiaPlanoTreino>,
        treinos: List<TreinoEntity>,
        weekStartIso: String
    ): String? {
        if (clienteId.isBlank()) return null
        val inicio = Time.mondayOf(weekStartIso)
        val fim = inicio.plusDays(6)
        val diasExport = dias.filter { it.semana == semana }.sortedBy { it.diaSemana }.map { slot ->
            val data = inicio.plusDays((slot.diaSemana - 1).toLong()).format(DateTimeFormatter.ISO_LOCAL_DATE)
            val treino = treinos.find { it.id == slot.treinoId }
            val mapeado = treino?.let { mapSets(it, slot.treinoId) }
            WeeklyDayExport(
                date = data,
                title = treino?.titulo ?: "Treino",
                notes = treino?.descricao?.blankToNull(),
                workoutSets = mapeado?.first.orEmpty(),
                exercises = mapeado?.second.orEmpty()
            )
        }
        if (diasExport.isEmpty()) return null
        val principal = dias.mapNotNull { slot -> treinos.find { it.id == slot.treinoId } }.firstOrNull()
        val exportadoEm = Time.nowIsoOffset()
        val payload = WeeklyTrainingExport(
            exportId = "training-${clienteId}-${inicio}-${plano.id}-s$semana",
            exportedAt = exportadoEm,
            trainer = treinador,
            clientId = clienteId,
            trainerWorkoutId = principal?.id ?: plano.id,
            title = plano.nome,
            description = principal?.descricao?.blankToNull(),
            workoutType = mapTipoTreino(principal?.tipo),
            estimatedDurationMinutes = principal?.duracaoEstimada,
            sentAt = exportadoEm,
            createdAt = Time.toIsoOffset(plano.createdDate),
            updatedAt = Time.toIsoOffset(plano.updatedDate),
            weekStart = inicio.format(DateTimeFormatter.ISO_LOCAL_DATE),
            weekEnd = fim.format(DateTimeFormatter.ISO_LOCAL_DATE),
            days = diasExport
        )
        return json.encodeToString(payload)
    }

    fun treinoAvulso(treino: TreinoEntity, clienteId: String): String? {
        if (clienteId.isBlank()) return null
        val hoje = Time.today()
        val inicio = Time.mondayOf(hoje)
        val fim = inicio.plusDays(6)
        val exportadoEm = Time.nowIsoOffset()
        val mapeado = mapSets(treino, treino.id)
        val payload = WeeklyTrainingExport(
            exportId = "training-${clienteId}-$hoje-${treino.id}",
            exportedAt = exportadoEm,
            trainer = treinador,
            clientId = clienteId,
            trainerWorkoutId = treino.id,
            title = treino.titulo,
            description = treino.descricao.blankToNull(),
            workoutType = mapTipoTreino(treino.tipo),
            estimatedDurationMinutes = treino.duracaoEstimada,
            sentAt = exportadoEm,
            createdAt = Time.toIsoOffset(treino.createdDate),
            updatedAt = Time.toIsoOffset(treino.updatedDate),
            weekStart = inicio.format(DateTimeFormatter.ISO_LOCAL_DATE),
            weekEnd = fim.format(DateTimeFormatter.ISO_LOCAL_DATE),
            days = listOf(
                WeeklyDayExport(
                    date = hoje,
                    title = treino.titulo,
                    notes = treino.descricao.blankToNull(),
                    workoutSets = mapeado.first,
                    exercises = mapeado.second
                )
            )
        )
        return json.encodeToString(payload)
    }

    fun nomePerfil(cliente: ClienteEntity): String = "cliente-${slug(cliente.nome)}-perfil.json"

    fun nomePlano(clienteNome: String, semana: Int): String =
        "cliente-${slug(clienteNome)}-semana-$semana.json"

    fun nomeTreino(treino: TreinoEntity): String = "cliente-${slug(treino.titulo)}-treino.json"

    private fun mapCliente(cliente: ClienteEntity) = ClientExport(
        id = cliente.id,
        name = cliente.nome,
        birthDate = Time.toIsoDate(cliente.dataNascimento),
        email = cliente.email.blankToNull(),
        phone = cliente.telefone.blankToNull(),
        sex = mapSexo(cliente.sexo),
        currentWeightKg = cliente.pesoAtual,
        heightCm = cliente.altura,
        goal = cliente.objetivo.blankToNull(),
        notes = cliente.observacoes.blankToNull(),
        status = mapStatus(cliente.status),
        lessonPriceCents = cents(cliente.valorAula),
        defaultLocation = cliente.localAulaPadrao.blankToNull(),
        createdAt = Time.toIsoOffset(cliente.createdDate),
        updatedAt = Time.toIsoOffset(cliente.updatedDate)
    )

    private fun mapAvaliacao(item: AvaliacaoEntity): PhysicalEvaluationExport {
        val dobras = JsonStore.decodeDobras(item.medidasJson)
        val medidas = listOfNotNull(
            dobras.triceps?.let { "triceps" to it },
            dobras.suprailiaca?.let { "suprailiac" to it },
            dobras.coxa?.let { "thigh" to it },
            dobras.peitoral?.let { "chest" to it },
            dobras.abdomen?.let { "abdomen" to it },
            dobras.biceps?.let { "biceps" to it },
            dobras.subescapular?.let { "subscapular" to it },
            dobras.axilarMedia?.let { "midaxillary" to it }
        ).toMap()
        return PhysicalEvaluationExport(
            id = item.id,
            date = Time.toIsoDate(item.dataAvaliacao) ?: item.dataAvaliacao.take(10),
            sex = mapSexo(item.sexo),
            age = item.idade,
            weightKg = item.peso,
            heightCm = item.altura,
            bodyFatPercent = item.percentualGordura,
            measurementsCm = medidas,
            notes = item.observacoes.blankToNull(),
            createdAt = Time.toIsoOffset(item.createdDate),
            updatedAt = Time.toIsoOffset(item.updatedDate)
        )
    }

    private fun mapFinanceiro(clienteId: String, transacoes: List<TransacaoEntity>): FinancialSummaryExport {
        val doAluno = transacoes.filter { it.clienteId == clienteId }
        val lancamentos = doAluno.map { tx ->
            FinancialTransactionExport(
                id = tx.id,
                type = tx.tipoTransacao,
                date = Time.toIsoDate(tx.dataTransacao) ?: tx.dataTransacao.take(10),
                amountCents = cents(kotlin.math.abs(tx.valor)) ?: 0,
                reference = tx.referencia.blankToNull(),
                lessonId = tx.aulaId,
                paymentMethod = tx.metodoPagamento.blankToNull(),
                createdAt = Time.toIsoOffset(tx.createdDate),
                updatedAt = Time.toIsoOffset(tx.updatedDate)
            )
        }
        val pagamentos = doAluno.map { tx ->
            val pago = tx.tipoTransacao == "pagamento"
            FinancialPaymentExport(
                dueDate = Time.toIsoDate(tx.dataTransacao) ?: tx.dataTransacao.take(10),
                amountCents = cents(kotlin.math.abs(tx.valor)) ?: 0,
                status = if (pago) "PAID" else "PENDING",
                paidAt = if (pago) Time.toIsoOffset(tx.createdDate) else null
            )
        }
        val proximo = pagamentos.filter { it.status == "PENDING" }.minByOrNull { it.dueDate }?.dueDate
        return FinancialSummaryExport(
            currency = "BRL",
            balanceCents = null,
            nextDueDate = proximo,
            payments = pagamentos,
            transactions = lancamentos
        )
    }

    private fun mapAula(aula: AulaEntity, clienteId: String): AppointmentExport? {
        val aluno = JsonStore.decodeAlunos(aula.alunosJson).find { it.clienteId == clienteId } ?: return null
        return AppointmentExport(
            id = aula.id,
            date = Time.toIsoDate(aula.data) ?: aula.data.take(10),
            time = aula.horario,
            type = aula.tipoAula,
            status = mapStatusAula(aula.status),
            durationMinutes = aula.duracaoMinutos,
            location = aula.local.blankToNull(),
            onlineUrl = SafeUrl.httpOrHttps(aula.linkOnline)?.toString(),
            notes = aula.observacoes.blankToNull(),
            workoutId = aluno.treinoId,
            workoutTitle = aluno.treinoTitulo,
            checkedIn = aluno.checkInRealizado,
            checkInTime = aluno.checkInHorario,
            recordedPriceCents = cents(aluno.valorAulaRegistrado)
        )
    }

    private fun mapHistorico(item: HistoricoTreinoEntity) = WorkoutHistoryExport(
        id = item.id,
        workoutId = item.treinoId,
        title = item.treinoTitulo.ifBlank { "Treino" },
        date = Time.toIsoDate(item.dataExecucao) ?: item.dataExecucao.take(10),
        notes = item.observacoes.blankToNull(),
        exercises = JsonStore.decodeExecucoes(item.exerciciosJson).map { ex ->
            HistoryExerciseExport(
                name = ex.nome,
                muscleGroup = ex.grupoMuscular.blankToNull(),
                setName = ex.setNome.blankToNull(),
                sets = ex.series,
                repetitions = ex.repeticoes.blankToNull(),
                executionType = mapTipoExecucao(ex.tipoExecucao),
                executionDuration = if (ex.tipoExecucao == "tempo") ex.tempoExecucao?.let { Time.formatMmSs(it) } else null,
                loadKg = ex.pesoKg.replace(",", ".").toDoubleOrNull(),
                completedSets = ex.seriesFeitas
            )
        }
    )

    private fun mapSets(treino: TreinoEntity, prefixo: String): Pair<List<WeeklySetExport>, List<WeeklyExerciseExport>> {
        var ordem = 0
        val workoutSets = JsonStore.decodeSets(treino.exerciciosJson).mapIndexed { setIndex, set ->
            val exercicios = set.exercicios.mapIndexed { exIndex, ex ->
                ordem += 1
                val ultimo = exIndex == set.exercicios.lastIndex
                mapExercicio(
                    ex = ex,
                    id = "$prefixo-$ordem",
                    restSeconds = if (ultimo) set.descansoSegundos.takeIf { it > 0 } else null
                )
            }
            WeeklySetExport(
                id = set.id.ifBlank { "$prefixo-set-${setIndex + 1}" },
                name = set.titulo(setIndex),
                restSeconds = set.descansoSegundos,
                exercises = exercicios
            )
        }
        return workoutSets to workoutSets.flatMap { it.exercises }
    }

    private fun mapExercicio(ex: WorkoutExercise, id: String, restSeconds: Int? = null): WeeklyExerciseExport {
        val video = SafeUrl.httpOrHttps(ex.videoPrincipal)?.toString()
        val videoAlt = SafeUrl.httpOrHttps(ex.videoAlternativo)?.toString()
        val links = listOfNotNull(
            video?.let { VideoLinkExport("Vídeo demonstrativo", it) },
            videoAlt?.let { VideoLinkExport("Vídeo alternativo", it) }
        )
        return WeeklyExerciseExport(
            id = id,
            name = ex.nome,
            sets = ex.series,
            repetitions = if (ex.tipoExecucao == "tempo") null else ex.repeticoes.blankToNull(),
            restSeconds = restSeconds ?: ex.descansoSegundos.takeIf { it > 0 },
            muscleGroup = ex.grupoMuscular.blankToNull(),
            executionType = mapTipoExecucao(ex.tipoExecucao),
            executionDuration = if (ex.tipoExecucao == "tempo") ex.tempoExecucao?.let { Time.formatMmSs(it) } else null,
            instructions = ex.observacoes.blankToNull(),
            catalogId = ex.catalogId.blankToNull(),
            videoUrl = video,
            alternateVideoUrl = videoAlt,
            links = links
        )
    }

    private fun mapSexo(raw: String): String? = when (raw.trim().lowercase()) {
        "", "null" -> null
        "masculino", "m", "male" -> "M"
        "feminino", "f", "female" -> "F"
        else -> raw
    }

    private fun mapStatus(raw: String): String = when (raw.lowercase()) {
        "ativo" -> "active"
        "inativo" -> "inactive"
        else -> raw.ifBlank { "active" }
    }

    private fun mapStatusAula(raw: String): String = when (raw.lowercase()) {
        "agendada" -> "scheduled"
        "cancelada" -> "canceled"
        "realizada", "concluida", "concluída" -> "completed"
        else -> raw
    }

    private fun mapTipoTreino(raw: String?): String? = when (raw?.lowercase()) {
        null, "" -> null
        "forca", "força" -> "strength"
        else -> raw
    }

    private fun mapTipoExecucao(raw: String): String? = when (raw.lowercase()) {
        "repeticao", "repetição" -> "repetition"
        "tempo" -> "time"
        else -> raw.blankToNull()
    }

    private fun cents(valor: Double?): Int? {
        if (valor == null) return null
        return BigDecimal.valueOf(valor).movePointRight(2).setScale(0, RoundingMode.HALF_UP).toInt()
    }

    private fun slug(nome: String): String = nome.lowercase()
        .replace("[^a-z0-9]+".toRegex(), "-")
        .trim('-')
        .ifBlank { "aluno" }
        .take(40)

    private fun String.blankToNull(): String? = trim().takeIf { it.isNotEmpty() }
}
