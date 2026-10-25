package com.fittrainer.pro.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ClienteEntity::class,
        TreinoEntity::class,
        AulaEntity::class,
        AvaliacaoEntity::class,
        ProgressoEntity::class,
        HistoricoTreinoEntity::class,
        TransacaoEntity::class,
        ExercicioBibliotecaEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clienteDao(): ClienteDao
    abstract fun treinoDao(): TreinoDao
    abstract fun aulaDao(): AulaDao
    abstract fun avaliacaoDao(): AvaliacaoDao
    abstract fun progressoDao(): ProgressoDao
    abstract fun historicoDao(): HistoricoDao
    abstract fun transacaoDao(): TransacaoDao
    abstract fun exercicioBibliotecaDao(): ExercicioBibliotecaDao

    companion object {
        const val NAME = "fittrainer.db"

        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    NAME
                ).fallbackToDestructiveMigration().build().also { instance = it }
            }
        }
    }
}
