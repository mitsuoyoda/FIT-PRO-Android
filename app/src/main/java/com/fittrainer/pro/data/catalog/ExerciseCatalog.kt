package com.fittrainer.pro.data.catalog

import android.content.Context
import com.fittrainer.pro.data.local.ExercicioBibliotecaEntity
import com.fittrainer.pro.data.local.JsonStore
import kotlinx.serialization.Serializable
import java.text.Normalizer

@Serializable
data class ExerciseCatalogItem(
    val id: String = "",
    val nomeEn: String = "",
    val nomePt: String = "",
    val grupo: String = "",
    val secundarios: String = "",
    val video: String = "",
    val videoAlt: String = "",
    val fonte: String = "",
    val pesoRefKg: String = "",
    val calorias: String = "",
    val premissa: String = "",
    val ordem: Int = 9999
) {
    val displayName: String get() = nomePt.ifBlank { nomeEn }

    val videos: List<Pair<String, String>>
        get() = buildList {
            if (video.isNotBlank()) add("Vídeo (masculino)" to video)
            if (videoAlt.isNotBlank()) add("Vídeo (feminino)" to videoAlt)
        }
}

object ExerciseCatalog {
    @Volatile
    private var cache: List<ExerciseCatalogItem>? = null

    fun items(context: Context): List<ExerciseCatalogItem> {
        cache?.let { return it }
        val raw = context.assets.open("exercicios.json").bufferedReader().use { it.readText() }
        return JsonStore.json.decodeFromString<List<ExerciseCatalogItem>>(raw)
            .sortedBy { it.ordem }
            .also { cache = it }
    }

    fun search(context: Context, query: String, limit: Int = 8): List<ExerciseCatalogItem> {
        val needle = query.normalized()
        if (needle.length < 2) return emptyList()
        return items(context).asSequence()
            .filter { item ->
                item.nomePt.normalized().contains(needle) ||
                    item.nomeEn.normalized().contains(needle) ||
                    item.grupo.normalized().contains(needle)
            }
            .sortedBy { it.ordem }
            .take(limit)
            .toList()
    }

    fun findByName(context: Context, name: String): ExerciseCatalogItem? {
        val needle = name.normalized()
        if (needle.isBlank()) return null
        val all = items(context)
        return all.firstOrNull { it.displayName.normalized() == needle }
            ?: all.firstOrNull { it.nomeEn.normalized() == needle }
    }

    fun fromBiblioteca(entity: ExercicioBibliotecaEntity): ExerciseCatalogItem = ExerciseCatalogItem(
        id = entity.id,
        nomePt = entity.nome,
        grupo = entity.grupoMuscular,
        secundarios = entity.descricao,
        video = entity.video,
        videoAlt = entity.videoAlt,
        calorias = entity.calorias,
        fonte = "biblioteca",
        ordem = 0
    )

    fun merged(context: Context, biblioteca: List<ExercicioBibliotecaEntity>): List<ExerciseCatalogItem> {
        val extras = biblioteca.associateBy { it.id }
        val catalogo = items(context).map { item ->
            extras[item.id]?.let { fromBiblioteca(it) } ?: item
        }
        val idsCatalogo = catalogo.map { it.id }.toSet()
        val soBiblioteca = biblioteca
            .filter { it.id !in idsCatalogo }
            .map { fromBiblioteca(it) }
        return (soBiblioteca + catalogo).sortedWith(compareBy({ it.fonte != "biblioteca" }, { it.displayName.lowercase() }))
    }

    fun searchMerged(
        context: Context,
        biblioteca: List<ExercicioBibliotecaEntity>,
        query: String,
        limit: Int = 8
    ): List<ExerciseCatalogItem> {
        val needle = query.normalized()
        if (needle.length < 2) return emptyList()
        return merged(context, biblioteca).asSequence()
            .filter { item ->
                item.nomePt.normalized().contains(needle) ||
                    item.nomeEn.normalized().contains(needle) ||
                    item.grupo.normalized().contains(needle)
            }
            .take(limit)
            .toList()
    }

    fun findMerged(
        context: Context,
        biblioteca: List<ExercicioBibliotecaEntity>,
        catalogId: String?,
        name: String
    ): ExerciseCatalogItem? {
        val all = merged(context, biblioteca)
        if (!catalogId.isNullOrBlank()) {
            all.firstOrNull { it.id == catalogId }?.let { return it }
        }
        val needle = name.normalized()
        if (needle.isBlank()) return null
        return all.firstOrNull { it.displayName.normalized() == needle }
            ?: all.firstOrNull { it.nomeEn.normalized() == needle }
    }
}

private fun String.normalized(): String {
    val nfd = Normalizer.normalize(this.trim().lowercase(), Normalizer.Form.NFD)
    return nfd.replace("\\p{M}+".toRegex(), "")
}
