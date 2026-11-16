package com.fittrainer.pro.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ClienteEntity::class,
        TreinoEntity::class,
        AulaEntity::class,
        AvaliacaoEntity::class,
        ProgressoEntity::class,
        HistoricoTreinoEntity::class,
        TransacaoEntity::class,
        ExercicioBibliotecaEntity::class,
        PlanoAssessoriaEntity::class
    ],
    version = 5,
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
    abstract fun planoAssessoriaDao(): PlanoAssessoriaDao

    companion object {
        const val NAME = "fittrainer.db"

        @Volatile
        private var instance: AppDatabase? = null

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exercicios_biblioteca ADD COLUMN video TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE exercicios_biblioteca ADD COLUMN videoAlt TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE exercicios_biblioteca ADD COLUMN calorias TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE planos_assessoria ADD COLUMN semanasCompartilhadasJson TEXT NOT NULL DEFAULT '[]'"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS planos_assessoria (
                        id TEXT NOT NULL PRIMARY KEY,
                        nome TEXT NOT NULL,
                        clienteId TEXT NOT NULL DEFAULT '',
                        clienteNome TEXT NOT NULL DEFAULT '',
                        duracaoSemanas INTEGER NOT NULL DEFAULT 4,
                        semanasJson TEXT NOT NULL DEFAULT '[]',
                        copiadoDeId TEXT,
                        status TEXT NOT NULL DEFAULT 'modelo',
                        ultimoCompartilhamento TEXT NOT NULL DEFAULT '',
                        statusCompartilhamento TEXT NOT NULL DEFAULT '',
                        createdDate TEXT NOT NULL,
                        updatedDate TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun get(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    NAME
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
        }
    }
}
