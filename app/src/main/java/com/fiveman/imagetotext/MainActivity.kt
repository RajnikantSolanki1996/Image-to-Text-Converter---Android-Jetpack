package com.example.imagetotext

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.imagetotext.screens.CropScreen
import com.fiveman.imagetotext.HomeScreen
import com.fiveman.imagetotext.TextEditScreen


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = "home"
                    ) {
                        composable("home") {
                            HomeScreen(navController = navController)
                        }
                        composable("crop/{imageUri}") { backStackEntry ->
                            val imageUri = backStackEntry.arguments?.getString("imageUri") ?: ""
                            CropScreen(
                                navController = navController,
                                imageUri = imageUri
                            )
                        }
                        composable("text_edit/{extractedText}") { backStackEntry ->
                            val extractedText = backStackEntry.arguments?.getString("extractedText") ?: ""
                            TextEditScreen(
                                navController = navController,
                                initialText = extractedText
                            )
                        }
                    }
                }
            }
        }
    }
}