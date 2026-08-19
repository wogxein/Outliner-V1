package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.screens.MainOutlinerScreen
import com.example.ui.theme.OutlinerTheme
import com.example.ui.viewmodel.OutlinerViewModel
import com.example.ui.viewmodel.OutlinerViewModelFactory

class MainActivity : ComponentActivity() {

    private val viewModel: OutlinerViewModel by viewModels {
        OutlinerViewModelFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()
            OutlinerTheme(darkTheme = isDarkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainOutlinerScreen(viewModel = viewModel)
                }
            }
        }
    }
}
