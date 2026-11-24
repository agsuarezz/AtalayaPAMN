package es.ulpgc.pamn.atalaya.domain.usecase

import es.ulpgc.pamn.atalaya.core.dispatchers.DispatcherProvider
import es.ulpgc.pamn.atalaya.core.security.BiometricAvailability
import es.ulpgc.pamn.atalaya.core.security.BiometricAvailabilityChecker
import es.ulpgc.pamn.atalaya.core.security.CryptoException
import es.ulpgc.pamn.atalaya.core.security.EncryptedPayload
import es.ulpgc.pamn.atalaya.core.security.KeyDerivation
import es.ulpgc.pamn.atalaya.core.security.KeystoreManager
import es.ulpgc.pamn.atalaya.core.security.Pbkdf2KeyDerivation
import es.ulpgc.pamn.atalaya.core.security.VaultSessionManager
import es.ulpgc.pamn.atalaya.domain.model.VaultMetadata
import es.ulpgc.pamn.atalaya.domain.repository.VaultMetaRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import javax.crypto.Cipher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BiometricAuthUseCasesTest {

    private val keyDerivation = Pbkdf2KeyDerivation()

    private fun testDispatchers(dispatcher: CoroutineDispatcher): DispatcherProvider =
        object : DispatcherProvider {
            override val main: CoroutineDispatcher = dispatcher
            override val io: CoroutineDispatcher = dispatcher
            override val default: CoroutineDispatcher = dispatcher
        }

    private fun sampleMetadata(biometricWrappedDek: String?): VaultMetadata = VaultMetadata(
        saltBase64 = "AAAAAAAAAAAAAAAAAAAAAA==",
        kdfIterations = KeyDerivation.DEFAULT_ITERATIONS,
        masterWrappedDek = "v1:AA==:AA==",
        biometricWrappedDek = biometricWrappedDek,
        createdAtEpochMillis = 1L,
        updatedAtEpochMillis = 1L
    )

    @Test
    fun `GetAuthInitialStateUseCase detecta metadatos obsoletos si Keystore perdio la clave`() = runTest {
        val repo = mockk<VaultMetaRepository>()
        val keystore = mockk<KeystoreManager>()
        val checker = mockk<BiometricAvailabilityChecker>()

        coEvery { repo.getMetadata() } returns sampleMetadata("v1:AQID:BAUG")
        every { checker.checkAvailability() } returns BiometricAvailability.AVAILABLE
        every { keystore.hasBiometricKey() } returns false

        val useCase = GetAuthInitialStateUseCase(repo, keystore, checker)
        val info = useCase()

        assertTrue(info.isInitialized)
        assertEquals(BiometricAvailability.AVAILABLE, info.biometricAvailability)
        assertFalse(info.isBiometricEnrolledInVault)
        assertTrue(info.hasStaleBiometricMetadata)
    }

    @Test
    fun `PrepareBiometricEnrollmentUseCase genera clave si sesion desbloqueada y falla si bloqueada`() = runTest {
        val keystore = mockk<KeystoreManager>()
        val sessionManager = VaultSessionManager()
        val cipher = mockk<Cipher>()
        val dispatchers = testDispatchers(UnconfinedTestDispatcher(testScheduler))

        val useCase = PrepareBiometricEnrollmentUseCase(keystore, sessionManager, dispatchers)

        var threwLocked = false
        try {
            useCase()
        } catch (_: CryptoException.VaultLockedException) {
            threwLocked = true
        }
        assertTrue(threwLocked)

        sessionManager.unlock(keyDerivation.generateRandomKey())
        every { keystore.generateBiometricKey() } returns Unit
        every { keystore.createBiometricEncryptCipher() } returns cipher

        val result = useCase()

        assertSame(cipher, result)
        verify(exactly = 1) { keystore.generateBiometricKey() }
        verify(exactly = 1) { keystore.createBiometricEncryptCipher() }
        sessionManager.lock()
    }

    @Test
    fun `CompleteBiometricEnrollmentUseCase envuelve DEK activa y guarda payload en repositorio`() = runTest {
        val keystore = mockk<KeystoreManager>()
        val sessionManager = VaultSessionManager()
        val repo = mockk<VaultMetaRepository>()
        val cipher = mockk<Cipher>()
        val dek = keyDerivation.generateRandomKey()
        sessionManager.unlock(dek)

        val expectedPayload = EncryptedPayload(byteArrayOf(1, 2, 3), byteArrayOf(4, 5, 6))
        every { keystore.encryptWithAuthenticatedCipher(cipher, any()) } returns expectedPayload
        coEvery { repo.updateBiometricWrappedDek(expectedPayload.toStorageString(), 999L) } returns Unit

        val useCase = CompleteBiometricEnrollmentUseCase(
            keystore,
            sessionManager,
            repo,
            testDispatchers(UnconfinedTestDispatcher(testScheduler))
        )
        useCase(cipher, nowEpochMillis = 999L)

        coVerify(exactly = 1) {
            repo.updateBiometricWrappedDek(expectedPayload.toStorageString(), 999L)
        }
        sessionManager.lock()
    }

    @Test
    fun `PrepareBiometricUnlockUseCase limpia clave y metadatos ante KeyInvalidatedException`() = runTest {
        val repo = mockk<VaultMetaRepository>()
        val keystore = mockk<KeystoreManager>()
        val payload = EncryptedPayload(byteArrayOf(10, 20, 30), byteArrayOf(40, 50))
        coEvery { repo.getMetadata() } returns sampleMetadata(payload.toStorageString())
        every { keystore.createBiometricDecryptCipher(any()) } throws CryptoException.KeyInvalidatedException()
        every { keystore.deleteBiometricKey() } returns Unit
        coEvery { repo.updateBiometricWrappedDek(null, 555L) } returns Unit

        val useCase = PrepareBiometricUnlockUseCase(
            repo,
            keystore,
            testDispatchers(UnconfinedTestDispatcher(testScheduler))
        )

        val result = useCase(nowEpochMillis = 555L)

        assertEquals(BiometricUnlockPreparation.KeyInvalidated, result)
        verify(exactly = 1) { keystore.deleteBiometricKey() }
        coVerify(exactly = 1) { repo.updateBiometricWrappedDek(null, 555L) }
    }

    @Test
    fun `UnlockVaultWithBiometricsUseCase descifra DEK y desbloquea VaultSessionManager`() = runTest {
        val repo = mockk<VaultMetaRepository>()
        val keystore = mockk<KeystoreManager>()
        val sessionManager = VaultSessionManager()
        val cipher = mockk<Cipher>()
        val dek = keyDerivation.generateRandomKey()
        val payload = EncryptedPayload(byteArrayOf(1, 2, 3), byteArrayOf(4, 5, 6))

        coEvery { repo.getMetadata() } returns sampleMetadata(payload.toStorageString())
        every { keystore.decryptWithAuthenticatedCipher(cipher, payload) } returns dek.copyOf()

        val useCase = UnlockVaultWithBiometricsUseCase(
            repo,
            keystore,
            sessionManager,
            testDispatchers(UnconfinedTestDispatcher(testScheduler))
        )

        val result = useCase(cipher)

        assertEquals(UnlockResult.Success, result)
        assertTrue(sessionManager.isUnlocked.value)
        sessionManager.withDek { active ->
            assertArrayEquals(dek, active)
        }
        LockVaultUseCase(sessionManager).invoke()
        assertFalse(sessionManager.isUnlocked.value)
    }

    @Test
    fun `DisableBiometricsUseCase borra clave Keystore y limpia metadato`() = runTest {
        val keystore = mockk<KeystoreManager>()
        val repo = mockk<VaultMetaRepository>()
        every { keystore.deleteBiometricKey() } returns Unit
        coEvery { repo.updateBiometricWrappedDek(null, 777L) } returns Unit

        val useCase = DisableBiometricsUseCase(
            keystore,
            repo,
            testDispatchers(UnconfinedTestDispatcher(testScheduler))
        )
        useCase(nowEpochMillis = 777L)

        verify(exactly = 1) { keystore.deleteBiometricKey() }
        coVerify(exactly = 1) { repo.updateBiometricWrappedDek(null, 777L) }
    }
}
