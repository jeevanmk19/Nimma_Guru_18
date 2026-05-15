package com.nimmaguru.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.nimmaguru.app.core.ui.theme.NimmaGuruTheme
import com.nimmaguru.app.feature.auth.domain.repository.AuthRepository
import com.nimmaguru.app.navigation.HomeRoute
import com.nimmaguru.app.navigation.PhoneEntryRoute
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Single Activity host for the entire app (ADR-01).
 * All screens are Composable functions managed by Compose Navigation.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NimmaGuruTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val startDestination = if (authRepository.isLoggedIn) HomeRoute else PhoneEntryRoute
                    NimmaGuruNavHost(startDestination = startDestination)
                }
            }
        }
    }
}
