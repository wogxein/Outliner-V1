package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import com.example.ui.screens.MainOutlinerScreen
import com.example.ui.theme.FontManager
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
            val customPrimaryHex by viewModel.customPrimaryColorHex.collectAsState()
            val customBgHex by viewModel.customBgColorHex.collectAsState()
            val appFontFamilyKey by viewModel.appFontFamily.collectAsState()
            val customFontPath by viewModel.customFontPath.collectAsState()

            val context = LocalContext.current

            // Ensure proper system status bar icon colors (light icons on dark bg, dark icons on light bg)
            LaunchedEffect(isDarkTheme) {
                val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
                windowInsetsController.isAppearanceLightStatusBars = !isDarkTheme
                windowInsetsController.isAppearanceLightNavigationBars = !isDarkTheme
            }

            val appFontSizeIndex by viewModel.appFontSizeIndex.collectAsState()
            val currentFontFamily = FontManager.getFontFamily(context, appFontFamilyKey, customFontPath)
            val currentFontScale = com.example.ui.theme.AppFontSize.getScale(appFontSizeIndex)

            OutlinerTheme(
                darkTheme = isDarkTheme,
                customPrimaryHex = customPrimaryHex,
                customBgHex = customBgHex,
                fontFamily = currentFontFamily,
                fontScale = currentFontScale
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainOutlinerScreen(viewModel = viewModel)
                }
            }
        }
    }
}
