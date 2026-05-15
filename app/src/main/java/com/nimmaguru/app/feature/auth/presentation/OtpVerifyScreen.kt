package com.nimmaguru.app.feature.auth.presentation

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.nimmaguru.app.core.common.findActivity
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nimmaguru.app.R
import com.nimmaguru.app.core.ui.theme.NimmaGuruTheme

/**
 * OTP Verification Screen — second step of Auth flow.
 * 6-digit OTP input with auto-focus.
 */
@Composable
fun OtpVerifyScreen(
    phoneNumber: String,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = hiltViewModel(),
    onNavigateToHome: () -> Unit = {},
    onNavigateToBasicOnboarding: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val otpCode by viewModel.otpCode.collectAsStateWithLifecycle()
    val activity: Activity? = LocalContext.current.findActivity()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AuthEvent.NavigateToHome -> onNavigateToHome()
                is AuthEvent.NavigateToBasicOnboarding -> onNavigateToBasicOnboarding()
                is AuthEvent.NavigateToOtp -> { /* already here */ }
                is AuthEvent.ShowSnackbar -> { /* host has no snackbar here */ }
            }
        }
    }

    OtpVerifyContent(
        modifier = modifier,
        phoneNumber = phoneNumber,
        otpCode = otpCode,
        uiState = uiState,
        onOtpChanged = { viewModel.onAction(AuthAction.OtpChanged(it)) },
        onVerify = { viewModel.onAction(AuthAction.VerifyOtp) },
        onResend = { activity?.let { viewModel.onAction(AuthAction.ResendOtp(it)) } },
    )
}

@Composable
fun OtpVerifyContent(
    modifier: Modifier = Modifier,
    phoneNumber: String = "+91 9876543210",
    otpCode: String = "",
    uiState: AuthUiState = AuthUiState.OtpSent,
    onOtpChanged: (String) -> Unit = {},
    onVerify: () -> Unit = {},
    onResend: () -> Unit = {},
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "🙏",
            style = MaterialTheme.typography.displayLarge,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.otp_title),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.otp_sent_to, phoneNumber),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = otpCode,
            onValueChange = { value ->
                if (value.length <= 6 && value.all { it.isDigit() }) {
                    onOtpChanged(value)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            textStyle = MaterialTheme.typography.headlineLarge.copy(
                textAlign = TextAlign.Center,
                letterSpacing = 8.sp,
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            shape = MaterialTheme.shapes.medium,
        )

        if (uiState is AuthUiState.Error) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(uiState.errorRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onVerify,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = uiState !is AuthUiState.Loading && otpCode.length == 6,
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
            ),
        ) {
            if (uiState is AuthUiState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
            } else {
                Text(
                    text = stringResource(R.string.verify_otp),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = onResend,
            modifier = Modifier.height(48.dp),
        ) {
            Text(
                text = stringResource(R.string.resend_otp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview_OtpVerifyScreen_Empty() {
    NimmaGuruTheme {
        OtpVerifyContent(
            phoneNumber = "+91 9876543210",
            otpCode = "",
            uiState = AuthUiState.OtpSent,
        )
    }
}
