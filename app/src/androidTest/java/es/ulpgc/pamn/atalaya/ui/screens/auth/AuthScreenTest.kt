package es.ulpgc.pamn.atalaya.ui.screens.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import es.ulpgc.pamn.atalaya.R
import es.ulpgc.pamn.atalaya.core.security.BiometricAvailability
import es.ulpgc.pamn.atalaya.ui.state.UiState
import es.ulpgc.pamn.atalaya.ui.theme.AtalayaTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun str(id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    @Test
    fun setupMode_displaysFieldsStrengthAndBiometricOption() {
        var createClicked = false
        val state = UiState.Success(
            AuthScreenData(
                mode = AuthMode.SETUP,
                biometricAvailability = BiometricAvailability.AVAILABLE,
                isBiometricOptionEnabled = true
            )
        )

        composeRule.setContent {
            AtalayaTheme {
                AuthScreenContent(
                    uiState = state,
                    onRetryLoad = {},
                    onPasswordChanged = {},
                    onConfirmPasswordChanged = {},
                    onTogglePasswordVisibility = {},
                    onToggleConfirmPasswordVisibility = {},
                    onBiometricOptionChanged = {},
                    onCreateVaultSubmit = { createClicked = true },
                    onRetryBiometricEnrollment = {},
                    onSkipBiometricEnrollment = {},
                    onUnlockWithPasswordSubmit = {},
                    onUnlockWithBiometricsClicked = {}
                )
            }
        }

        composeRule.onNodeWithText(str(R.string.auth_setup_title)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.auth_setup_password_label)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.auth_setup_confirm_label)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.auth_setup_biometric_title)).assertIsDisplayed()

        composeRule.onNodeWithContentDescription(str(R.string.auth_setup_action_create))
            .assertIsDisplayed()
            .performClick()

        assertTrue(createClicked)
    }

    @Test
    fun unlockMode_withBiometrics_showsBothUnlockActionsAndTriggersCallbacks() {
        var passwordUnlockClicked = false
        var biometricUnlockClicked = false
        val state = UiState.Success(
            AuthScreenData(
                mode = AuthMode.UNLOCK,
                biometricAvailability = BiometricAvailability.AVAILABLE,
                canUnlockWithBiometrics = true
            )
        )

        composeRule.setContent {
            AtalayaTheme {
                AuthScreenContent(
                    uiState = state,
                    onRetryLoad = {},
                    onPasswordChanged = {},
                    onConfirmPasswordChanged = {},
                    onTogglePasswordVisibility = {},
                    onToggleConfirmPasswordVisibility = {},
                    onBiometricOptionChanged = {},
                    onCreateVaultSubmit = {},
                    onRetryBiometricEnrollment = {},
                    onSkipBiometricEnrollment = {},
                    onUnlockWithPasswordSubmit = { passwordUnlockClicked = true },
                    onUnlockWithBiometricsClicked = { biometricUnlockClicked = true }
                )
            }
        }

        composeRule.onNodeWithText(str(R.string.unlock_title)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(str(R.string.unlock_action_enter))
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithContentDescription(str(R.string.unlock_action_biometric))
            .assertIsDisplayed()
            .performClick()

        assertTrue(passwordUnlockClicked)
        assertTrue(biometricUnlockClicked)
    }

    @Test
    fun unlockMode_withoutBiometrics_hidesBiometricButtonAndShowsNotice() {
        val state = UiState.Success(
            AuthScreenData(
                mode = AuthMode.UNLOCK,
                biometricAvailability = BiometricAvailability.UNAVAILABLE,
                canUnlockWithBiometrics = false
            )
        )

        composeRule.setContent {
            AtalayaTheme {
                AuthScreenContent(
                    uiState = state,
                    onRetryLoad = {},
                    onPasswordChanged = {},
                    onConfirmPasswordChanged = {},
                    onTogglePasswordVisibility = {},
                    onToggleConfirmPasswordVisibility = {},
                    onBiometricOptionChanged = {},
                    onCreateVaultSubmit = {},
                    onRetryBiometricEnrollment = {},
                    onSkipBiometricEnrollment = {},
                    onUnlockWithPasswordSubmit = {},
                    onUnlockWithBiometricsClicked = {}
                )
            }
        }

        composeRule.onNodeWithText(str(R.string.unlock_biometric_unavailable_notice))
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription(str(R.string.unlock_action_biometric))
            .assertDoesNotExist()
    }

    @Test
    fun errorState_displaysRetryAction() {
        var retryClicked = false

        composeRule.setContent {
            AtalayaTheme {
                AuthScreenContent(
                    uiState = UiState.Error("Fallo de prueba"),
                    onRetryLoad = { retryClicked = true },
                    onPasswordChanged = {},
                    onConfirmPasswordChanged = {},
                    onTogglePasswordVisibility = {},
                    onToggleConfirmPasswordVisibility = {},
                    onBiometricOptionChanged = {},
                    onCreateVaultSubmit = {},
                    onRetryBiometricEnrollment = {},
                    onSkipBiometricEnrollment = {},
                    onUnlockWithPasswordSubmit = {},
                    onUnlockWithBiometricsClicked = {}
                )
            }
        }

        composeRule.onNodeWithText("Fallo de prueba").assertIsDisplayed()
        composeRule.onNodeWithContentDescription(str(R.string.auth_action_retry))
            .assertIsDisplayed()
            .performClick()

        assertTrue(retryClicked)
    }
}
