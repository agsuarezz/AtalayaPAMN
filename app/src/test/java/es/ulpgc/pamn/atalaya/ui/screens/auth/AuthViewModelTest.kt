package es.ulpgc.pamn.atalaya.ui.screens.auth

import androidx.biometric.BiometricPrompt
import app.cash.turbine.test
import es.ulpgc.pamn.atalaya.R
import es.ulpgc.pamn.atalaya.core.security.BiometricAvailability
import es.ulpgc.pamn.atalaya.domain.usecase.AuthInitialInfo
import es.ulpgc.pamn.atalaya.domain.usecase.BiometricUnlockPreparation
import es.ulpgc.pamn.atalaya.domain.usecase.CompleteBiometricEnrollmentUseCase
import es.ulpgc.pamn.atalaya.domain.usecase.CreateMasterPasswordUseCase
import es.ulpgc.pamn.atalaya.domain.usecase.DisableBiometricsUseCase
import es.ulpgc.pamn.atalaya.domain.usecase.EvaluatePasswordStrengthUseCase
import es.ulpgc.pamn.atalaya.domain.usecase.GetAuthInitialStateUseCase
import es.ulpgc.pamn.atalaya.domain.usecase.PrepareBiometricEnrollmentUseCase
import es.ulpgc.pamn.atalaya.domain.usecase.PrepareBiometricUnlockUseCase
import es.ulpgc.pamn.atalaya.domain.usecase.UnlockResult
import es.ulpgc.pamn.atalaya.domain.usecase.UnlockVaultWithBiometricsUseCase
import es.ulpgc.pamn.atalaya.domain.usecase.UnlockVaultWithPasswordUseCase
import es.ulpgc.pamn.atalaya.ui.state.UiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import javax.crypto.Cipher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getAuthInitialStateUseCase: GetAuthInitialStateUseCase
    private val evaluatePasswordStrengthUseCase = EvaluatePasswordStrengthUseCase()
    private lateinit var createMasterPasswordUseCase: CreateMasterPasswordUseCase
    private lateinit var unlockVaultWithPasswordUseCase: UnlockVaultWithPasswordUseCase
    private lateinit var prepareBiometricEnrollmentUseCase: PrepareBiometricEnrollmentUseCase
    private lateinit var completeBiometricEnrollmentUseCase: CompleteBiometricEnrollmentUseCase
    private lateinit var prepareBiometricUnlockUseCase: PrepareBiometricUnlockUseCase
    private lateinit var unlockVaultWithBiometricsUseCase: UnlockVaultWithBiometricsUseCase
    private lateinit var disableBiometricsUseCase: DisableBiometricsUseCase

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        getAuthInitialStateUseCase = mockk()
        createMasterPasswordUseCase = mockk()
        unlockVaultWithPasswordUseCase = mockk()
        prepareBiometricEnrollmentUseCase = mockk()
        completeBiometricEnrollmentUseCase = mockk()
        prepareBiometricUnlockUseCase = mockk()
        unlockVaultWithBiometricsUseCase = mockk()
        disableBiometricsUseCase = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(): AuthViewModel = AuthViewModel(
        getAuthInitialStateUseCase = getAuthInitialStateUseCase,
        evaluatePasswordStrengthUseCase = evaluatePasswordStrengthUseCase,
        createMasterPasswordUseCase = createMasterPasswordUseCase,
        unlockVaultWithPasswordUseCase = unlockVaultWithPasswordUseCase,
        prepareBiometricEnrollmentUseCase = prepareBiometricEnrollmentUseCase,
        completeBiometricEnrollmentUseCase = completeBiometricEnrollmentUseCase,
        prepareBiometricUnlockUseCase = prepareBiometricUnlockUseCase,
        unlockVaultWithBiometricsUseCase = unlockVaultWithBiometricsUseCase,
        disableBiometricsUseCase = disableBiometricsUseCase
    )

    @Test
    fun `estado inicial sin boveda emite Loading y luego Success en modo SETUP`() = runTest {
        coEvery { getAuthInitialStateUseCase() } returns AuthInitialInfo(
            isInitialized = false,
            biometricAvailability = BiometricAvailability.AVAILABLE,
            isBiometricEnrolledInVault = false,
            hasStaleBiometricMetadata = false
        )

        val viewModel = buildViewModel()

        viewModel.uiState.test {
            assertEquals(UiState.Loading, awaitItem())
            val success = awaitItem() as UiState.Success
            assertEquals(AuthMode.SETUP, success.data.mode)
            assertTrue(success.data.isBiometricOptionEnabled)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `estado inicial con error al consultar BD emite UiState Error`() = runTest {
        coEvery { getAuthInitialStateUseCase() } throws IllegalStateException("BD dañada")

        val viewModel = buildViewModel()

        viewModel.uiState.test {
            assertEquals(UiState.Loading, awaitItem())
            val error = awaitItem()
            assertTrue(error is UiState.Error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `estado inicial con biometria obsoleta limpia metadatos y muestra aviso`() = runTest {
        coEvery { getAuthInitialStateUseCase() } returns AuthInitialInfo(
            isInitialized = true,
            biometricAvailability = BiometricAvailability.AVAILABLE,
            isBiometricEnrolledInVault = false,
            hasStaleBiometricMetadata = true
        )

        val viewModel = buildViewModel()
        advanceUntilIdle()

        coVerify(exactly = 1) { disableBiometricsUseCase(any()) }
        val state = viewModel.uiState.value as UiState.Success
        assertEquals(AuthMode.UNLOCK, state.data.mode)
        assertFalse(state.data.canUnlockWithBiometrics)
        assertEquals(R.string.auth_warning_biometric_invalidated, state.data.statusMessage?.messageRes)
    }

    @Test
    fun `validacion de setup rechaza contrasena corta y no coincidente`() = runTest {
        coEvery { getAuthInitialStateUseCase() } returns AuthInitialInfo(
            isInitialized = false,
            biometricAvailability = BiometricAvailability.UNAVAILABLE,
            isBiometricEnrolledInVault = false,
            hasStaleBiometricMetadata = false
        )

        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.onPasswordChanged("corta")
        viewModel.onCreateVaultSubmit()
        assertEquals(
            R.string.auth_error_password_too_short,
            (viewModel.uiState.value as UiState.Success).data.passwordErrorRes
        )

        viewModel.onPasswordChanged("Atalaya#2026")
        viewModel.onConfirmPasswordChanged("Diferente#2026")
        viewModel.onCreateVaultSubmit()
        assertEquals(
            R.string.auth_error_passwords_mismatch,
            (viewModel.uiState.value as UiState.Success).data.confirmPasswordErrorRes
        )
    }

    @Test
    fun `setup sin biometria crea boveda limpia campos y navega a boveda`() = runTest {
        coEvery { getAuthInitialStateUseCase() } returns AuthInitialInfo(
            isInitialized = false,
            biometricAvailability = BiometricAvailability.UNAVAILABLE,
            isBiometricEnrolledInVault = false,
            hasStaleBiometricMetadata = false
        )
        coEvery { createMasterPasswordUseCase(any(), any()) } returns Result.success(Unit)

        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.onPasswordChanged("Atalaya#Segura2026")
            viewModel.onConfirmPasswordChanged("Atalaya#Segura2026")
            viewModel.onCreateVaultSubmit()
            advanceUntilIdle()

            assertEquals(AuthUiEvent.NavigateToVault, awaitItem())
            val state = (viewModel.uiState.value as UiState.Success).data
            assertEquals("", state.password)
            assertEquals("", state.confirmPassword)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setup con biometria cancelada permite reintentar o entrar sin biometria`() = runTest {
        val cipher = mockk<Cipher>()
        coEvery { getAuthInitialStateUseCase() } returns AuthInitialInfo(
            isInitialized = false,
            biometricAvailability = BiometricAvailability.AVAILABLE,
            isBiometricEnrolledInVault = false,
            hasStaleBiometricMetadata = false
        )
        coEvery { createMasterPasswordUseCase(any(), any()) } returns Result.success(Unit)
        coEvery { prepareBiometricEnrollmentUseCase() } returns cipher

        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.onPasswordChanged("Atalaya#Segura2026")
            viewModel.onConfirmPasswordChanged("Atalaya#Segura2026")
            viewModel.onCreateVaultSubmit()
            advanceUntilIdle()

            val launchEvent = awaitItem() as AuthUiEvent.LaunchBiometricPrompt
            assertTrue(launchEvent.isEnrollment)

            viewModel.onBiometricPromptCancelled(isEnrollment = true)
            val paused = (viewModel.uiState.value as UiState.Success).data
            assertTrue(paused.isVaultCreatedPendingBiometrics)
            assertEquals(
                R.string.auth_warning_biometric_enroll_cancelled,
                paused.statusMessage?.messageRes
            )

            viewModel.onSkipBiometricEnrollment()
            advanceUntilIdle()
            assertEquals(AuthUiEvent.NavigateToVault, awaitItem())
            coVerify(atLeast = 1) { disableBiometricsUseCase(any()) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `desbloqueo por contrasena incorrecta muestra error y correcta navega`() = runTest {
        coEvery { getAuthInitialStateUseCase() } returns AuthInitialInfo(
            isInitialized = true,
            biometricAvailability = BiometricAvailability.AVAILABLE,
            isBiometricEnrolledInVault = true,
            hasStaleBiometricMetadata = false
        )
        coEvery { unlockVaultWithPasswordUseCase(any()) } returnsMany listOf(
            UnlockResult.InvalidPassword,
            UnlockResult.Success
        )

        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.onPasswordChanged("incorrecta")
        viewModel.onUnlockWithPasswordSubmit()
        advanceUntilIdle()

        val errorState = (viewModel.uiState.value as UiState.Success).data
        assertEquals(R.string.auth_error_invalid_password, errorState.statusMessage?.messageRes)

        viewModel.events.test {
            viewModel.onPasswordChanged("ClaveCorrecta#2026")
            viewModel.onUnlockWithPasswordSubmit()
            advanceUntilIdle()

            assertEquals(AuthUiEvent.NavigateToVault, awaitItem())
            assertEquals("", (viewModel.uiState.value as UiState.Success).data.password)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `desbloqueo biometrico con clave invalidada oculta boton y avisa al usuario`() = runTest {
        coEvery { getAuthInitialStateUseCase() } returns AuthInitialInfo(
            isInitialized = true,
            biometricAvailability = BiometricAvailability.AVAILABLE,
            isBiometricEnrolledInVault = true,
            hasStaleBiometricMetadata = false
        )
        coEvery { prepareBiometricUnlockUseCase(any()) } returns BiometricUnlockPreparation.KeyInvalidated

        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.onUnlockWithBiometricsClicked()
        advanceUntilIdle()

        val state = (viewModel.uiState.value as UiState.Success).data
        assertFalse(state.canUnlockWithBiometrics)
        assertEquals(R.string.auth_warning_biometric_invalidated, state.statusMessage?.messageRes)
    }

    @Test
    fun `desbloqueo biometrico exitoso navega y lockout degrada con aviso`() = runTest {
        val cipher = mockk<Cipher>()
        coEvery { getAuthInitialStateUseCase() } returns AuthInitialInfo(
            isInitialized = true,
            biometricAvailability = BiometricAvailability.AVAILABLE,
            isBiometricEnrolledInVault = true,
            hasStaleBiometricMetadata = false
        )
        coEvery { prepareBiometricUnlockUseCase(any()) } returns BiometricUnlockPreparation.Ready(cipher)
        coEvery { unlockVaultWithBiometricsUseCase(cipher) } returns UnlockResult.Success

        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.onBiometricPromptError(BiometricPrompt.ERROR_LOCKOUT, isEnrollment = false)
        val lockoutState = (viewModel.uiState.value as UiState.Success).data
        assertEquals(R.string.auth_warning_biometric_lockout, lockoutState.statusMessage?.messageRes)

        viewModel.events.test {
            viewModel.onUnlockWithBiometricsClicked()
            advanceUntilIdle()

            val promptEvent = awaitItem() as AuthUiEvent.LaunchBiometricPrompt
            assertFalse(promptEvent.isEnrollment)

            viewModel.onBiometricPromptSucceeded(cipher, isEnrollment = false)
            advanceUntilIdle()
            assertEquals(AuthUiEvent.NavigateToVault, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
