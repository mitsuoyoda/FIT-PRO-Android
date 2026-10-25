package com.fittrainer.pro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fittrainer.pro.ui.AppViewModel
import com.fittrainer.pro.ui.FitTrainerApp
import com.fittrainer.pro.ui.theme.FitTrainerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application.fitApp()
        setContent {
            FitTrainerTheme {
                val viewModel: AppViewModel = viewModel(factory = AppViewModel.factory(app.repository))
                FitTrainerApp(viewModel = viewModel, backupManager = app.backupManager)
            }
        }
    }
}
