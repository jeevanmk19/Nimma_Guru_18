package com.nimmaguru.app.feature.auth.presentation

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nimmaguru.app.R
import kotlinx.coroutines.launch

@Composable
fun PhoneEntryScreen(
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = hiltViewModel(),
    onNavigateToHome: () -> Unit = {},
    onNavigateToBasicOnboarding: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AuthEvent.NavigateToHome -> onNavigateToHome()
                is AuthEvent.NavigateToBasicOnboarding -> onNavigateToBasicOnboarding()
                else -> {}
            }
        }
    }

    PhoneEntryContent(
        modifier = modifier,
        uiState = uiState,
        onGoogleSignIn = {
            scope.launch {
                handleGoogleSignIn(context, viewModel)
            }
        },
        onContinueAsGuest = { viewModel.onAction(AuthAction.ContinueAsGuest) },
    )
}

private suspend fun handleGoogleSignIn(context: Context, viewModel: AuthViewModel) {
    val credentialManager = CredentialManager.create(context)
    
    // 1. Check if google-services.json provided a default ID
    val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
    val defaultWebClientId = if (resId != 0) context.getString(resId) else null

    // 2. Using your actual Web Client ID from google-services.json
    val manualWebClientId = "77819962844-munkn5qgto0sad1h4p4scoqfi82tclcc.apps.googleusercontent.com"
    
    val webClientId = defaultWebClientId ?: manualWebClientId

    if (webClientId == "YOUR_WEB_CLIENT_ID_HERE" || webClientId.isNullOrEmpty()) {
        val msg = "Missing Web Client ID. Please ensure google-services.json is valid."
        Log.e("Auth", msg)
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        return
    }

    Log.d("Auth", "Using Web Client ID: $webClientId")

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(webClientId)
        .setAutoSelectEnabled(false)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    try {
        val result = credentialManager.getCredential(context = context, request = request)
        val credential = result.credential
        if (credential is androidx.credentials.CustomCredential && 
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            viewModel.onAction(AuthAction.SignInWithGoogle(googleIdTokenCredential.idToken))
        }
    } catch (e: GetCredentialException) {
        Log.e("Auth", "Credential Manager Error: ${e.message}", e)
        val userMessage = when {
            e.message?.contains("DEVELOPER_ERROR") == true -> 
                "Configuration Error (DEVELOPER_ERROR): Register your SHA-1 fingerprint in Firebase Console."
            e.message?.contains("16:") == true -> 
                "Sign-in canceled or no accounts."
            else -> e.message ?: "Sign-in failed"
        }
        Toast.makeText(context, userMessage, Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        Log.e("Auth", "Unexpected Error", e)
    }
}

@Composable
fun PhoneEntryContent(
    modifier: Modifier = Modifier,
    uiState: AuthUiState = AuthUiState.Idle,
    onGoogleSignIn: () -> Unit = {},
    onContinueAsGuest: () -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "🙏", style = MaterialTheme.typography.displayLarge)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onGoogleSignIn,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = uiState !is AuthUiState.Loading,
            shape = MaterialTheme.shapes.medium
        ) {
            if (uiState is AuthUiState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Sign in with Google")
            }
        }

        if (uiState is AuthUiState.Error) {
            Text(
                text = stringResource(uiState.errorRes),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 16.dp),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        OutlinedButton(
            onClick = onContinueAsGuest,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(stringResource(R.string.continue_as_guest))
        }
    }
}
