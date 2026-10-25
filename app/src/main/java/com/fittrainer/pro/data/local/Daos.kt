package com.fittrainer.pro.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ClienteDao {
    @Query("SELECT * FROM clientes ORDER BY nome ASC")
    fun observeAll(): Flow<List<ClienteEntity>>

    @Query("SELECT * FROM clientes ORDER BY nome ASC")
    suspend fun getAll(): List<ClienteEntity>

    @Query("SELECT * FROM clientes WHERE id = :id")
    suspend fun getById(id: String): ClienteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ClienteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<ClienteEntity>)

    @Query("DELETE FROM clientes WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM clientes")
    suspend fun deleteAll()
}

@Dao
interface TreinoDao {
    @Query("SELECT * FROM treinos ORDER BY createdDate DESC")
    fun observeAll(): Flow<List<TreinoEntity>>

    @Query("SELECT * FROM treinos ORDER BY createdDate DESC")
    suspend fun getAll(): List<TreinoEntity>

    @Query("SELECT * FROM treinos WHERE id = :id")
    suspend fun getById(id: String): TreinoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: TreinoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<TreinoEntity>)

    @Query("DELETE FROM treinos WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM treinos WHERE clienteId = :clienteId")
    suspend fun deleteByCliente(clienteId: String)

    @Query("DELETE FROM treinos")
    suspend fun deleteAll()
}

@Dao
interface AulaDao {
    @Query("SELECT * FROM aulas ORDER BY data DESC, horario DESC")
    fun observeAll(): Flow<List<AulaEntity>>

    @Query("SELECT * FROM aulas ORDER BY data DESC, horario DESC")
    suspend fun getAll(): List<AulaEntity>

    @Query("SELECT * FROM aulas WHERE id = :id")
    suspend fun getById(id: String): AulaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AulaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<AulaEntity>)

    @Query("DELETE FROM aulas WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM aulas")
    suspend fun deleteAll()
}

@Dao
interface AvaliacaoDao {
    @Query("SELECT * FROM avaliacoes ORDER BY dataAvaliacao DESC")
    fun observeAll(): Flow<List<AvaliacaoEntity>>

    @Query("SELECT * FROM avaliacoes ORDER BY dataAvaliacao DESC")
    suspend fun getAll(): List<AvaliacaoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AvaliacaoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<AvaliacaoEntity>)

    @Query("DELETE FROM avaliacoes WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM avaliacoes WHERE clienteId = :clienteId")
    suspend fun deleteByCliente(clienteId: String)

    @Query("DELETE FROM avaliacoes")
    suspend fun deleteAll()
}

@Dao
interface ProgressoDao {
    @Query("SELECT * FROM progresso_cliente ORDER BY dataRegistro DESC")
    fun observeAll(): Flow<List<ProgressoEntity>>

    @Query("SELECT * FROM progresso_cliente ORDER BY dataRegistro DESC")
    suspend fun getAll(): List<ProgressoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ProgressoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<ProgressoEntity>)

    @Query("DELETE FROM progresso_cliente WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM progresso_cliente WHERE clienteId = :clienteId")
    suspend fun deleteByCliente(clienteId: String)

    @Query("DELETE FROM progresso_cliente")
    suspend fun deleteAll()
}

@Dao
interface HistoricoDao {
    @Query("SELECT * FROM historico_treinos ORDER BY dataExecucao DESC")
    fun observeAll(): Flow<List<HistoricoTreinoEntity>>

    @Query("SELECT * FROM historico_treinos ORDER BY dataExecucao DESC")
    suspend fun getAll(): List<HistoricoTreinoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: HistoricoTreinoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<HistoricoTreinoEntity>)

    @Query("DELETE FROM historico_treinos WHERE clienteId = :clienteId")
    suspend fun deleteByCliente(clienteId: String)

    @Query("DELETE FROM historico_treinos")
    suspend fun deleteAll()
}

@Dao
interface TransacaoDao {
    @Query("SELECT * FROM transacoes_financeiras ORDER BY dataTransacao DESC")
    fun observeAll(): Flow<List<TransacaoEntity>>

    @Query("SELECT * FROM transacoes_financeiras ORDER BY dataTransacao DESC")
    suspend fun getAll(): List<TransacaoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: TransacaoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<TransacaoEntity>)

    @Query("DELETE FROM transacoes_financeiras WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM transacoes_financeiras WHERE clienteId = :clienteId")
    suspend fun deleteByCliente(clienteId: String)

    @Query("DELETE FROM transacoes_financeiras")
    suspend fun deleteAll()
}

@Dao
interface ExercicioBibliotecaDao {
    @Query("SELECT * FROM exercicios_biblioteca ORDER BY nome ASC")
    suspend fun getAll(): List<ExercicioBibliotecaEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<ExercicioBibliotecaEntity>)

    @Query("DELETE FROM exercicios_biblioteca")
    suspend fun deleteAll()
}
