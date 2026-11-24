package es.ulpgc.pamn.atalaya.domain.usecase

import es.ulpgc.pamn.atalaya.core.dispatchers.DispatcherProvider
import es.ulpgc.pamn.atalaya.core.security.DefaultAesGcmCipher
import es.ulpgc.pamn.atalaya.core.security.KeyDerivation
import es.ulpgc.pamn.atalaya.core.security.Pbkdf2KeyDerivation
import es.ulpgc.pamn.atalaya.core.security.VaultSessionManager
import es.ulpgc.pamn.atalaya.domain.model.VaultMetadata
import es.ulpgc.pamn.atalaya.domain.repository.VaultMetaRepository
import io.mockk.coEvery
import io.mockk.mockk
import java.util.Base64
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UnlockVaultWithPasswordUseCaseTest {

    private val keyDerivation = Pbkdf2KeyDerivation()
    private val aesGcmCipher = DefaultAesGcmCipher()
    private val sessionManager = VaultSessionManager()

    private fun testDispatchers(dispatcher: CoroutineDispatcher): DispatcherProvider =
        object : DispatcherProvider {
            override val main: CoroutineDispatcher = dispatcher
            override val io: CoroutineDispatcher = dispatcher
            override val default: CoroutineDispatcher = dispatcher
        }

    private fun buildMetadata(password: String, dek: ByteArray): VaultMetadata {
        val salt = keyDerivation.generateSalt()
        val iterations = KeyDerivation.MIN_ITERATIONS
        val kek = keyDerivation.deriveKey(password.toCharArray(), salt, iterations)
        val wrapped = aesGcmCipher.encrypt(dek, kek, MASTER_WRAP_AAD).toStorageString()
        kek.fill(0)
        return VaultMetadata(
            saltBase64 = Base64.getEncoder().encodeToString(salt),
            kdfIterations = iterations,
            masterWrappedDek = wrapped,
            biometricWrappedDek = null,
            createdAtEpochMillis = 1L,
            updatedAtEpochMillis = 1L
        )
    }

    @Test
    fun `contrasena valida desbloquea DEK y limpia el CharArray`() = runTest {
        val expectedDek = keyDerivation.generateRandomKey()
        val metadata = buildMetadata("ClaveMaestra#2026", expectedDek)
        val repo = mockk<VaultMetaRepository>()
        coEvery { repo.getMetadata() } returns metadata

        val useCase = UnlockVaultWithPasswordUseCase(
            vaultMetaRepository = repo,
            keyDerivation = keyDerivation,
            aesGcmCipher = aesGcmCipher,
            vaultSessionManager = sessionManager,
            dispatchers = testDispatchers(UnconfinedTestDispatcher(testScheduler))
        )
        val chars = "ClaveMaestra#2026".toCharArray()

        val result = useCase(chars)

        assertEquals(UnlockResult.Success, result)
        assertTrue(sessionManager.isUnlocked.value)
        assertTrue(chars.all { it == '\u0000' })
        sessionManager.withDek { activeDek ->
            assertArrayEquals(expectedDek, activeDek)
        }
        sessionManager.lock()
    }

    @Test
    fun `contrasena incorrecta devuelve InvalidPassword y mantiene boveda cerrada`() = runTest {
        val expectedDek = keyDerivation.generateRandomKey()
        val metadata = buildMetadata("ClaveMaestra#2026", expectedDek)
        val repo = mockk<VaultMetaRepository>()
        coEvery { repo.getMetadata() } returns metadata

        val useCase = UnlockVaultWithPasswordUseCase(
            vaultMetaRepository = repo,
            keyDerivation = keyDerivation,
            aesGcmCipher = aesGcmCipher,
            vaultSessionManager = sessionManager,
            dispatchers = testDispatchers(UnconfinedTestDispatcher(testScheduler))
        )
        val chars = "OtraClaveIncorrecta".toCharArray()

        val result = useCase(chars)

        assertEquals(UnlockResult.InvalidPassword, result)
        assertFalse(sessionManager.isUnlocked.value)
        assertTrue(chars.all { it == '\u0000' })
    }

    @Test
    fun `boveda sin inicializar devuelve VaultNotInitialized`() = runTest {
        val repo = mockk<VaultMetaRepository>()
        coEvery { repo.getMetadata() } returns null

        val useCase = UnlockVaultWithPasswordUseCase(
            vaultMetaRepository = repo,
            keyDerivation = keyDerivation,
            aesGcmCipher = aesGcmCipher,
            vaultSessionManager = sessionManager,
            dispatchers = testDispatchers(UnconfinedTestDispatcher(testScheduler))
        )

        val result = useCase("ClaveMaestra#2026".toCharArray())

        assertEquals(UnlockResult.VaultNotInitialized, result)
        assertFalse(sessionManager.isUnlocked.value)
    }

    @Test
    fun `metadatos corruptos devuelven CorruptedVault`() = runTest {
        val repo = mockk<VaultMetaRepository>()
        coEvery { repo.getMetadata() } returns VaultMetadata(
            saltBase64 = "no-es-base64-valido!!!",
            kdfIterations = KeyDerivation.MIN_ITERATIONS,
            masterWrappedDek = "formato_roto",
            biometricWrappedDek = null,
            createdAtEpochMillis = 1L,
            updatedAtEpochMillis = 1L
        )

        val useCase = UnlockVaultWithPasswordUseCase(
            vaultMetaRepository = repo,
            keyDerivation = keyDerivation,
            aesGcmCipher = aesGcmCipher,
            vaultSessionManager = sessionManager,
            dispatchers = testDispatchers(UnconfinedTestDispatcher(testScheduler))
        )

        val result = useCase("ClaveMaestra#2026".toCharArray())

        assertTrue(result is UnlockResult.CorruptedVault)
        assertFalse(sessionManager.isUnlocked.value)
    }
}
