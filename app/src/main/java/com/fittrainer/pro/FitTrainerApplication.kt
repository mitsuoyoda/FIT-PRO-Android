package com.fittrainer.pro

import android.app.Application
import com.fittrainer.pro.data.backup.BackupManager
import com.fittrainer.pro.data.local.AppDatabase
import com.fittrainer.pro.data.repository.FitTrainerRepository

class FitTrainerApplication : Application() {
    lateinit var repository: FitTrainerRepository
        private set
    lateinit var backupManager: BackupManager
        private set

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.get(this)
        repository = FitTrainerRepository(db)
        backupManager = BackupManager(this, repository)
    }
}

fun Application.fitApp(): FitTrainerApplication = this as FitTrainerApplication
